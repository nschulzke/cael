package cael.parser

import cael.ast.Expr
import cael.ast.Node


private object ExprPrecedence {
    const val OR = 1
    const val AND = 2
    const val EQUALITY = 3
    const val COMPARISON = 4
    const val TERM = 5
    const val FACTOR = 6
    const val CALL = 7
}

private val binary: PeekableIterator<Token>.(left: Expr, token: Token) -> Expr = { left, token ->
    val right = parseExpr()
    Expr.Binary(left, token.lexeme, right, left.range..right.range)
}

private val unary: PeekableIterator<Token>.(token: Token) -> Expr = { token ->
    val expr = parseExpr()
    Expr.Unary(token.lexeme, expr, token.range..expr.range)
}

private val prattParser = Pratt(
    prefixes = mapOf(
        Token.Match::class to Pratt.Prefix { start ->
            val expr = parseExpr()
            var end: Node = expr
            val cases = mutableListOf<Expr.Match.Case>()
            while (match<Token.Pipe>()) {
                val pattern = parsePattern()
                expect<Token.Arrow>()
                val subExpr = parseExpr()
                end = subExpr
                cases.add(Expr.Match.Case(pattern, subExpr, pattern.range..subExpr.range))
            }
            Expr.Match(expr, cases, start.range..end.range)
        },
        Token.Identifier::class to Pratt.Prefix { token ->
            Expr.Identifier((token as Token.Identifier).name, token.range)
        },
        Token.LParen::class to Pratt.Prefix {
            val expr = parseExpr()
            expect<Token.RParen>()
            expr
        },
        Token.Plus::class to Pratt.Prefix(unary),
        Token.Minus::class to Pratt.Prefix(unary),
        Token.Bang::class to Pratt.Prefix(unary),
        Token.IntLiteral::class to Pratt.Prefix { token ->
            Expr.Literal.Int((token as Token.IntLiteral).value, token.range)
        },
        Token.FloatLiteral::class to Pratt.Prefix { token ->
            Expr.Literal.Float((token as Token.FloatLiteral).value, token.range)
        },
        Token.StringLiteral::class to Pratt.Prefix { token ->
            Expr.Literal.String((token as Token.StringLiteral).value, token.range)
        },
    ),
    infixes = mapOf(
        Token.AmpAmp::class to Pratt.Infix(ExprPrecedence.AND, binary),
        Token.PipePipe::class to Pratt.Infix(ExprPrecedence.OR, binary),

        Token.EqEq::class to Pratt.Infix(ExprPrecedence.EQUALITY, binary),
        Token.BangEq::class to Pratt.Infix(ExprPrecedence.EQUALITY, binary),
        Token.Lt::class to Pratt.Infix(ExprPrecedence.COMPARISON, binary),
        Token.LtEq::class to Pratt.Infix(ExprPrecedence.COMPARISON, binary),
        Token.Gt::class to Pratt.Infix(ExprPrecedence.COMPARISON, binary),
        Token.GtEq::class to Pratt.Infix(ExprPrecedence.COMPARISON, binary),

        Token.Plus::class to Pratt.Infix(ExprPrecedence.TERM, binary),
        Token.Minus::class to Pratt.Infix(ExprPrecedence.TERM, binary),
        Token.Star::class to Pratt.Infix(ExprPrecedence.FACTOR, binary),
        Token.Slash::class to Pratt.Infix(ExprPrecedence.FACTOR, binary),
        Token.Percent::class to Pratt.Infix(ExprPrecedence.FACTOR, binary),

        Token.LParen::class to Pratt.Infix(ExprPrecedence.CALL) { left, _ ->
            val args = parseExprTupleComponents()
            val end = expect<Token.RParen>()
            Expr.Call.Tuple(left, args, left.range..end.range)
        },
        Token.LBrace::class to Pratt.Infix(ExprPrecedence.CALL) { callee, _ ->
            val args = parseExprRecordComponents()
            val end = expect<Token.RBrace>()
            Expr.Call.Record(callee, args, callee.range..end.range)
        },
    )
)

fun PeekableIterator<Token>.parseExpr(): Expr {
    return with(prattParser) {
        this@parseExpr.parse(0)
    }
}

package cael.parser

import cael.ast.Expr
import cael.ast.ExprRecordItem
import cael.ast.Node


private object ExprPrecedence {
    val or = 1
    val and = 2
    val equality = 3
    val comparison = 4
    val term = 5
    val factor = 6
    val call = 7
}

val binary: PeekableIterator<Token>.(left: Expr, token: Token) -> Expr = { left, token ->
    val right = parseExpr()
    Expr.Binary(left, token.lexeme, right, left.range..right.range)
}

val unary: PeekableIterator<Token>.(token: Token) -> Expr = { token ->
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
        Token.AmpAmp::class to Pratt.Infix(ExprPrecedence.and, binary),
        Token.PipePipe::class to Pratt.Infix(ExprPrecedence.or, binary),

        Token.EqEq::class to Pratt.Infix(ExprPrecedence.equality, binary),
        Token.BangEq::class to Pratt.Infix(ExprPrecedence.equality, binary),
        Token.Lt::class to Pratt.Infix(ExprPrecedence.comparison, binary),
        Token.LtEq::class to Pratt.Infix(ExprPrecedence.comparison, binary),
        Token.Gt::class to Pratt.Infix(ExprPrecedence.comparison, binary),
        Token.GtEq::class to Pratt.Infix(ExprPrecedence.comparison, binary),

        Token.Plus::class to Pratt.Infix(ExprPrecedence.term, binary),
        Token.Minus::class to Pratt.Infix(ExprPrecedence.term, binary),
        Token.Times::class to Pratt.Infix(ExprPrecedence.factor, binary),
        Token.Div::class to Pratt.Infix(ExprPrecedence.factor, binary),
        Token.Mod::class to Pratt.Infix(ExprPrecedence.factor, binary),

        Token.LParen::class to Pratt.Infix<Expr>(ExprPrecedence.call) { left, _ ->
            val args = mutableListOf<Expr>()
            if (peek() !is Token.RParen) {
                args.add(parseExpr())
                while (peek() is Token.Comma) {
                    expect<Token.Comma>()
                    args.add(parseExpr())
                }
            }
            val end = expect<Token.RParen>()
            Expr.Call.Tuple(left, args, left.range..end.range)
        },
        Token.LBrace::class to Pratt.Infix<Expr>(ExprPrecedence.call) { callee, _ ->
            val args = mutableListOf<ExprRecordItem>()
            if (peek() !is Token.RBrace) {
                args.add(parseExprRecordItem())
                while (peek() is Token.Comma) {
                    expect<Token.Comma>()
                    args.add(parseExprRecordItem())
                }
            }
            val end = expect<Token.RBrace>()
            Expr.Call.Record(callee, args, callee.range..end.range)
        },
        Token.Dot::class to Pratt.Infix<Expr>(ExprPrecedence.call) { callee, _ ->
            val identifier = parseIdentifier()
            when (peek()) {
                is Token.LParen -> {
                    expect<Token.LParen>()
                    val args = mutableListOf<Expr>()
                    if (peek() !is Token.RParen) {
                        args.add(parseExpr())
                        while (peek() is Token.Comma) {
                            expect<Token.Comma>()
                            args.add(parseExpr())
                        }
                    }
                    val end = expect<Token.RParen>()
                    Expr.ExtensionCall.Tuple(callee, identifier.name, args, callee.range..end.range)
                }

                is Token.LBrace -> {
                    expect<Token.LBrace>()
                    val args = mutableListOf<ExprRecordItem>()
                    if (peek() !is Token.RBrace) {
                        args.add(parseExprRecordItem())
                        while (peek() is Token.Comma) {
                            expect<Token.Comma>()
                            args.add(parseExprRecordItem())
                        }
                    }
                    val end = expect<Token.RBrace>()
                    Expr.ExtensionCall.Record(callee, identifier.name, args, callee.range..end.range)
                }

                else -> Expr.ExtensionCall.Bare(callee, identifier.name, callee.range..identifier.range)
            }
        },
    )
)

fun PeekableIterator<Token>.parseExpr(): Expr {
    return with(prattParser) {
        this@parseExpr.parse(0)
    }
}

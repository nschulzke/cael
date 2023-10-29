package parser


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
    Expr.Binary(left, tokenToBinaryOp(token), right)
}

val unary: PeekableIterator<Token>.(token: Token) -> Expr = { token ->
    val expr = parseExpr()
    Expr.Unary(tokenToUnaryOp(token), expr)
}

private val prattParser = Pratt(
    prefixes = mapOf(
        Token.Match::class to Pratt.Prefix {
            val expr = parseExpr()
            val cases = mutableListOf<Expr.Match.Case>()
            while (match<Token.Pipe>()) {
                val pattern = parsePattern()
                expect<Token.Arrow>()
                val subExpr = parseExpr()
                cases.add(Expr.Match.Case(pattern, subExpr))
            }
            Expr.Match(expr, cases)
        },
        Token.Identifier::class to Pratt.Prefix { token ->
            Expr.Identifier((token as Token.Identifier).name)
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
            Expr.Literal.Int((token as Token.IntLiteral).value)
        },
        Token.FloatLiteral::class to Pratt.Prefix {
            Expr.Literal.Float((it as Token.FloatLiteral).value)
        },
        Token.StringLiteral::class to Pratt.Prefix {
            Expr.Literal.String((it as Token.StringLiteral).value)
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
            expect<Token.RParen>()
            Expr.Call.Tuple(left, args)
        },
        Token.LBrace::class to Pratt.Infix<Expr>(ExprPrecedence.call) { left, _ ->
            val args = mutableListOf<ExprRecordItem>()
            if (peek() !is Token.RBrace) {
                args.add(parseExprRecordItem())
                while (peek() is Token.Comma) {
                    expect<Token.Comma>()
                    args.add(parseExprRecordItem())
                }
            }
            expect<Token.RBrace>()
            Expr.Call.Record(left, args)
        },
        Token.Dot::class to Pratt.Infix<Expr>(ExprPrecedence.call) { left, _ ->
            val name = parseIdentifier()
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
                    expect<Token.RParen>()
                    Expr.ExtensionCall.Tuple(left, name, args)
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
                    expect<Token.RBrace>()
                    Expr.ExtensionCall.Record(left, name, args)
                }

                else -> Expr.ExtensionCall.Bare(left, name)
            }
        },
    )
)

fun tokenToBinaryOp(token: Token): Expr.Binary.Op {
    return when (token) {
        is Token.Plus -> Expr.Binary.Op.Plus
        is Token.Minus -> Expr.Binary.Op.Minus
        is Token.Times -> Expr.Binary.Op.Times
        is Token.Div -> Expr.Binary.Op.Div
        is Token.Mod -> Expr.Binary.Op.Mod
        is Token.EqEq -> Expr.Binary.Op.Equals
        is Token.BangEq -> Expr.Binary.Op.NotEqual
        is Token.Lt -> Expr.Binary.Op.LessThan
        is Token.LtEq -> Expr.Binary.Op.LessThanOrEqual
        is Token.Gt -> Expr.Binary.Op.GreaterThan
        is Token.GtEq -> Expr.Binary.Op.GreaterThanOrEqual
        is Token.AmpAmp -> Expr.Binary.Op.And
        is Token.PipePipe -> Expr.Binary.Op.Or
        else -> throw Exception("Expected binary operator, got $token")
    }
}

fun tokenToUnaryOp(token: Token): Expr.Unary.Op {
    return when (token) {
        is Token.Plus -> Expr.Unary.Op.Plus
        is Token.Minus -> Expr.Unary.Op.Minus
        is Token.Bang -> Expr.Unary.Op.Not
        else -> throw Exception("Expected unary operator, got $token")
    }
}

fun PeekableIterator<Token>.parseExpr(): Expr {
    return with(prattParser) {
        this@parseExpr.parse(0)
    }
}

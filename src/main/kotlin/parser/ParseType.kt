package parser


private object TypePrecedence {
    val union = 1
}

private val prattParser = Pratt(
    prefixes = mapOf(
        Token.Identifier::class to Pratt.Prefix { token ->
            Type.Identifier((token as Token.Identifier).name)
        },
        Token.LParen::class to Pratt.Prefix {
            val type = parseType()
            expect<Token.RParen>()
            type
        },
        Token.Pipe::class to Pratt.Prefix {
            parseType() // Leading | is okay
        },
    ),
    infixes = mapOf(
        Token.Pipe::class to Pratt.Infix(
            precedence = TypePrecedence.union,
        ) { left, _ ->
            val right = parseType()
            Type.Union(left, right)
        },
    )
)

fun PeekableIterator<Token>.parseType(): Type {
    return with(prattParser) {
        this@parseType.parse(0)
    }
}

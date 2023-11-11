package cael.parser

import cael.ast.Pattern

private object PatternPrecedence {
    const val OR = 1
    const val AND = 2
    const val MATCHES = 3
}

private val binary: PeekableIterator<Token>.(left: Pattern, token: Token) -> Pattern = { left, token ->
    val right = parsePattern()
    Pattern.Binary(left, token.lexeme, right, left.range..right.range)
}

private val unary: PeekableIterator<Token>.(token: Token) -> Pattern = { token ->
    val pattern = parsePattern()
    Pattern.Unary(token.lexeme, pattern, token.range..pattern.range)
}

private val prattParser = Pratt(
    prefixes = mapOf(
        Token.Identifier::class to Pratt.Prefix { token ->
            parsePatternAfterIdentifier(token as Token.Identifier)
        },
        Token.LParen::class to Pratt.Prefix { _ ->
            val type = parsePattern()
            expect<Token.RParen>()
            type
        },
        Token.Pipe::class to Pratt.Prefix {
            parsePattern() // Leading | is okay
        },
        Token.Bang::class to Pratt.Prefix(unary),
        Token.IntLiteral::class to Pratt.Prefix {
            Pattern.Literal.Int((it as Token.IntLiteral).value, it.range)
        },
        Token.FloatLiteral::class to Pratt.Prefix {
            Pattern.Literal.Float((it as Token.FloatLiteral).value, it.range)
        },
        Token.StringLiteral::class to Pratt.Prefix {
            Pattern.Literal.String((it as Token.StringLiteral).value, it.range)
        },
    ),
    infixes = mapOf(
        Token.Pipe::class to Pratt.Infix(PatternPrecedence.OR, binary),
        Token.Amp::class to Pratt.Infix(PatternPrecedence.AND, binary),
        Token.Colon::class to Pratt.Infix(PatternPrecedence.MATCHES, binary),
    )
)

fun PeekableIterator<Token>.parsePattern(): Pattern {
    return with(prattParser) {
        this@parsePattern.parse(0)
    }
}

private fun PeekableIterator<Token>.parsePatternAfterIdentifier(token: Token.Identifier): Pattern {
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleStructPatternPartial(token)
        is Token.LBrace -> parseRecordStructPatternPartial(token)
        else -> Pattern.Identifier(token.name, token.range)
    }
}

private fun PeekableIterator<Token>.parseTupleStructPatternPartial(identifier: Token.Identifier): Pattern {
    expect<Token.LParen>()
    val components = parsePatternTupleComponents()
    val end = expect<Token.RParen>()
    return Pattern.Struct.Tuple(identifier.name, components, identifier.range..end.range)
}

private fun PeekableIterator<Token>.parseRecordStructPatternPartial(identifier: Token.Identifier): Pattern {
    expect<Token.LBrace>()
    val components = parsePatternRecordComponents()
    val end = expect<Token.RBrace>()
    return Pattern.Struct.Record(identifier.name, components, identifier.range..end.range)
}

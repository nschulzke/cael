package cael.parser

import cael.ast.Pattern
import cael.ast.PatternRecordItem


fun PeekableIterator<Token>.parsePattern(): Pattern {
    return when (val token = next()) {
        is Token.Identifier -> parsePatternAfterIdentifier(token)
        is Token.IntLiteral -> Pattern.Literal.Int(token.value)
        is Token.FloatLiteral -> Pattern.Literal.Float(token.value)
        is Token.StringLiteral -> Pattern.Literal.String(token.value)
        else -> throw Exception("Expected pattern, got $token")
    }
}

private fun PeekableIterator<Token>.parsePatternAfterIdentifier(token: Token.Identifier): Pattern {
    return when (peekOrNull()) {
        Token.LParen -> parseTupleStructPatternPartial(token)
        Token.LBrace -> parseRecordStructPatternPartial(token)
        else -> Pattern.Identifier(token.name)
    }
}

private fun PeekableIterator<Token>.parseTupleStructPatternPartial(token: Token.Identifier): Pattern {
    expect<Token.LParen>()
    val components = mutableListOf<Pattern>()
    if (peek() !is Token.RParen) {
        components.add(parsePattern())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parsePattern())
        }
    }
    expect<Token.RParen>()
    return Pattern.Struct.Tuple(token.name, components)
}

private fun PeekableIterator<Token>.parseRecordStructPatternPartial(token: Token.Identifier): Pattern {
    expect<Token.LBrace>()
    val components = mutableListOf<PatternRecordItem>()
    if (peek() !is Token.RBrace) {
        components.add(parsePatternRecordItem())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parsePatternRecordItem())
        }
    }
    expect<Token.RBrace>()
    return Pattern.Struct.Record(token.name, components)
}

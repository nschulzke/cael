package cael.parser

import cael.ast.Pattern
import cael.ast.PatternRecordItem


fun PeekableIterator<Token>.parsePattern(): Pattern {
    return when (val token = next()) {
        is Token.Identifier -> parsePatternAfterIdentifier(token)
        is Token.IntLiteral -> Pattern.Literal.Int(token.value, token.range)
        is Token.FloatLiteral -> Pattern.Literal.Float(token.value, token.range)
        is Token.StringLiteral -> Pattern.Literal.String(token.value, token.range)
        else -> throw Exception("Expected pattern, got $token")
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
    val components = mutableListOf<Pattern>()
    if (peek() !is Token.RParen) {
        components.add(parsePattern())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parsePattern())
        }
    }
    val end = expect<Token.RParen>()
    return Pattern.Struct.Tuple(identifier.name, components, identifier.range..end.range)
}

private fun PeekableIterator<Token>.parseRecordStructPatternPartial(identifier: Token.Identifier): Pattern {
    expect<Token.LBrace>()
    val components = mutableListOf<PatternRecordItem>()
    if (peek() !is Token.RBrace) {
        components.add(parsePatternRecordItem())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parsePatternRecordItem())
        }
    }
    val end = expect<Token.RBrace>()
    return Pattern.Struct.Record(identifier.name, components, identifier.range..end.range)
}

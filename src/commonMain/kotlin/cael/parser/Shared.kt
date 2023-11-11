package cael.parser

import cael.ast.ExprRecordItem
import cael.ast.Pattern
import cael.ast.PatternRecordItem

fun PeekableIterator<Token>.parseExprRecordItem(): ExprRecordItem {
    val identifier = parseIdentifier()
    expect<Token.Eq>()
    val value = parseExpr()
    return ExprRecordItem(identifier.name, value, identifier.range..value.range)
}

fun PeekableIterator<Token>.parsePatternRecordItem(): PatternRecordItem {
    val identifier = parseIdentifier()
    expect<Token.Eq>()
    val pattern = parsePattern()
    return PatternRecordItem(identifier.name, pattern, identifier.range..pattern.range)
}

fun PeekableIterator<Token>.parseTupleComponents(): MutableList<Pattern> {
    val components = mutableListOf<Pattern>()
    if (peek() !is Token.RParen) {
        components.add(parsePattern())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parsePattern())
        }
    }
    return components
}

fun PeekableIterator<Token>.parseRecordComponents(): MutableList<PatternRecordItem> {
    val components = mutableListOf<PatternRecordItem>()
    if (peek() !is Token.RBrace) {
        components.add(parsePatternRecordItem())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parsePatternRecordItem())
        }
    }
    return components
}

fun PeekableIterator<Token>.parseIdentifier(): Token.Identifier {
    val token = nextOrNull() ?: throw Exception("Expected identifier, got EOF")
    return when (token) {
        is Token.Identifier -> token
        else -> throw Exception("Expected identifier, got $token")
    }
}
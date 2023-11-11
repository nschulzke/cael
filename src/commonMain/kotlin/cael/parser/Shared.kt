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

fun PeekableIterator<Token>.parseTupleComponents(): MutableList<Pattern> {
    val components = mutableListOf<Pattern>()
    if (peek() !is Token.RParen) {
        do {
            components.add(parsePattern())
        } while (match<Token.Comma>())
    }
    return components
}

fun PeekableIterator<Token>.parseRecordComponents(): MutableList<PatternRecordItem> {
    val components = mutableListOf<PatternRecordItem>()
    if (peek() !is Token.RBrace) {
        do {
            val identifier = parseIdentifier()
            expect<Token.Eq>()
            val pattern = parsePattern()
            components.add(PatternRecordItem(identifier.name, pattern, identifier.range..pattern.range))
        } while (match<Token.Comma>())
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
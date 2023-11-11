package cael.parser

import cael.ast.Expr
import cael.ast.ExprRecordItem
import cael.ast.Pattern
import cael.ast.PatternRecordItem

fun PeekableIterator<Token>.parseExprTupleComponents(): MutableList<Expr> {
    val args = mutableListOf<Expr>()
    if (peek() !is Token.RParen) {
        args.add(parseExpr())
        do {
            args.add(parseExpr())
        } while (match<Token.Comma>())
    }
    return args
}

fun PeekableIterator<Token>.parseExprRecordComponents(): MutableList<ExprRecordItem> {
    val args = mutableListOf<ExprRecordItem>()
    if (peek() !is Token.RBrace) {
        do {
            val identifier = parseIdentifier()
            expect<Token.Eq>()
            val value = parseExpr()
            args.add(ExprRecordItem(identifier.name, value, identifier.range..value.range))
        } while (match<Token.Comma>())
    }
    return args
}

fun PeekableIterator<Token>.parsePatternTupleComponents(): MutableList<Pattern> {
    val components = mutableListOf<Pattern>()
    if (peek() !is Token.RParen) {
        do {
            components.add(parsePattern())
        } while (match<Token.Comma>())
    }
    return components
}

fun PeekableIterator<Token>.parsePatternRecordComponents(): MutableList<PatternRecordItem> {
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
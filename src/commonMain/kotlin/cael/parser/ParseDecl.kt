package cael.parser

import cael.ast.*

fun PeekableIterator<Token>.parseDecl(): Decl {
    return when (val token = peek()) {
        is Token.Struct -> parseStructDecl()
        is Token.Let -> parseLetDecl()
        else -> throw Exception("Unexpected token $token")
    }
}

private fun PeekableIterator<Token>.parseStructDecl(): Decl.Struct {
    val start = expect<Token.Struct>()
    val identifier = parseIdentifier()
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleStructPartial(start, identifier.name)
        is Token.LBrace -> parseRecordStructPartial(start, identifier.name)
        else -> Decl.Struct.Bare(identifier.name, start..identifier)
    }
}

private fun PeekableIterator<Token>.parseTupleStructPartial(start: Token.Struct, name: String): Decl.Struct.Tuple {
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
    return Decl.Struct.Tuple(name, components, start..end)
}

private fun PeekableIterator<Token>.parseRecordStructPartial(start: Token.Struct, name: String): Decl.Struct.Record {
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
    return Decl.Struct.Record(name, components, start..end)
}

private fun PeekableIterator<Token>.parseLetDecl(): Decl.Let {
    val start = expect<Token.Let>()
    val (name) = parseIdentifier()
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleLetPartial(start, name)
        is Token.LBrace -> parseRecordLetPartial(start, name)
        else -> parseBareLetPartial(start, name)
    }
}

private fun PeekableIterator<Token>.parseTupleLetPartial(start: Token.Let, name: String): Decl.Let.Tuple {
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
    expect<Token.Eq>()
    val value = parseExpr()
    return Decl.Let.Tuple(name, components, value, start.range..value.range)
}

private fun PeekableIterator<Token>.parseRecordLetPartial(start: Token.Let, name: String): Decl.Let.Record {
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
    expect<Token.Eq>()
    val value = parseExpr()
    return Decl.Let.Record(name, components, value, start.range..value.range)
}

private fun PeekableIterator<Token>.parseBareLetPartial(start: Token.Let, name: String): Decl.Let.Bare {
    expect<Token.Eq>()
    val value = parseExpr()
    return Decl.Let.Bare(name, value, start.range..value.range)
}

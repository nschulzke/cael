package cael.parser

import cael.ast.*

fun PeekableIterator<Token>.parseDecl(): Decl {
    return when (val token = peek()) {
        is Token.Module -> parseModuleDecl()
        is Token.Open -> parseOpenDecl()
        is Token.Type -> parseTypeDecl()
        is Token.Protocol -> parseProtocolDecl()
        is Token.Extension -> parseExtensionDecl()
        is Token.Struct -> parseStructDecl()
        is Token.Dec -> parseDecDecl()
        is Token.Let -> parseLetDecl()
        else -> throw Exception("Unexpected token $token")
    }
}

private fun PeekableIterator<Token>.parseModuleDecl(): Decl.Module {
    val start = expect<Token.Module>()
    val (name) = parseIdentifier()
    expect<Token.Is>()
    val members = mutableListOf<Decl>()
    while (peek() !is Token.End) {
        members.add(parseDecl())
    }
    val end = expect<Token.End>()
    return Decl.Module(name, members, start..end)
}

private fun PeekableIterator<Token>.parseOpenDecl(): Decl.Open {
    val start = expect<Token.Open>()
    val identifier = parseIdentifier()
    return Decl.Open(identifier.name, start..identifier)
}

private fun PeekableIterator<Token>.parseTypeDecl(): Decl.TypeAlias {
    val start = expect<Token.Type>()
    val (name) = parseIdentifier()
    expect<Token.Eq>()
    val body = parseType()
    return Decl.TypeAlias(name, body, start.range..body.range)
}

private fun PeekableIterator<Token>.parseProtocolDecl(): Decl.Protocol {
    val start = expect<Token.Protocol>()
    val (name) = parseIdentifier()
    expect<Token.Is>()
    val declarations = mutableListOf<Decl.Protocol.Declaration>()
    while (peek() !is Token.End) {
        declarations.add(parseProtocolMember())
    }
    val end = expect<Token.End>()
    return Decl.Protocol(name, declarations, start..end)
}

private fun PeekableIterator<Token>.parseProtocolMember(): Decl.Protocol.Declaration {
    return when (val token = peek()) {
        is Token.Dec -> parseDecDecl()
        else -> throw Exception("Unexpected token $token")
    }
}

private fun PeekableIterator<Token>.parseExtensionDecl(): Decl.Extension {
    val start = expect<Token.Extension>()
    val (typeName) = parseIdentifier()
    val protocolName = if (match<Token.Colon>()) {
        parseIdentifier()
    } else {
        null
    }
    expect<Token.Is>()
    val declarations = mutableListOf<Decl.Extension.Declaration>()
    while (peek() !is Token.End) {
        declarations.add(parseExtensionMember())
    }
    val end = expect<Token.End>()
    return Decl.Extension(typeName, protocolName?.name, declarations, start..end)
}

private fun PeekableIterator<Token>.parseExtensionMember(): Decl.Extension.Declaration {
    return when (val token = peek()) {
        is Token.Dec -> parseDecDecl()
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
    val components = mutableListOf<Type>()
    if (peek() !is Token.RParen) {
        components.add(parseType())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parseType())
        }
    }
    val end = expect<Token.RParen>()
    return Decl.Struct.Tuple(name, components, start..end)
}

private fun PeekableIterator<Token>.parseRecordStructPartial(start: Token.Struct, name: String): Decl.Struct.Record {
    expect<Token.LBrace>()
    val components = mutableListOf<TypeRecordItem>()
    if (peek() !is Token.RBrace) {
        components.add(parseTypeRecordItem())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parseTypeRecordItem())
        }
    }
    val end = expect<Token.RBrace>()
    return Decl.Struct.Record(name, components, start..end)
}

private fun PeekableIterator<Token>.parseDecDecl(): Decl.Dec {
    val start = expect<Token.Dec>()
    val (name) = parseIdentifier()
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleDecPartial(start, name)
        is Token.LBrace -> parseRecordDecPartial(start, name)
        else -> parseBareDecPartial(start, name)
    }
}

private fun PeekableIterator<Token>.parseTupleDecPartial(start: Token.Dec, name: String): Decl.Dec.Tuple {
    expect<Token.LParen>()
    val components = mutableListOf<Type>()
    if (peek() !is Token.RParen) {
        components.add(parseType())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parseType())
        }
    }
    expect<Token.RParen>()
    expect<Token.Colon>()
    val type = parseType()
    return Decl.Dec.Tuple(name, components, type, start.range..type.range)
}

private fun PeekableIterator<Token>.parseRecordDecPartial(start: Token.Dec, name: String): Decl.Dec.Record {
    expect<Token.LBrace>()
    val components = mutableListOf<TypeRecordItem>()
    if (peek() !is Token.RBrace) {
        components.add(parseTypeRecordItem())
        while (peek() is Token.Comma) {
            expect<Token.Comma>()
            components.add(parseTypeRecordItem())
        }
    }
    expect<Token.RBrace>()
    expect<Token.Colon>()
    val type = parseType()
    return Decl.Dec.Record(name, components, type, start.range..type.range)
}

private fun PeekableIterator<Token>.parseBareDecPartial(start: Token.Dec, name: String): Decl.Dec.Bare {
    expect<Token.Colon>()
    val type = parseType()
    return Decl.Dec.Bare(name, type, start.range..type.range)
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

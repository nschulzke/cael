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
    expect<Token.Module>()
    val name = parseIdentifier()
    expect<Token.Is>()
    val members = mutableListOf<Decl>()
    while (peek() !is Token.End) {
        members.add(parseDecl())
    }
    expect<Token.End>()
    return Decl.Module(name, members)
}

private fun PeekableIterator<Token>.parseOpenDecl(): Decl.Open {
    expect<Token.Open>()
    val name = parseIdentifier()
    return Decl.Open(name)
}

private fun PeekableIterator<Token>.parseTypeDecl(): Decl.TypeAlias {
    expect<Token.Type>()
    val name = parseIdentifier()
    expect<Token.Eq>()
    val body = parseType()
    return Decl.TypeAlias(name, body)
}

private fun PeekableIterator<Token>.parseProtocolDecl(): Decl.Protocol {
    expect<Token.Protocol>()
    val name = parseIdentifier()
    expect<Token.Is>()
    val declarations = mutableListOf<Decl.Protocol.Declaration>()
    while (peek() !is Token.End) {
        declarations.add(parseProtocolMember())
    }
    expect<Token.End>()
    return Decl.Protocol(name, declarations)
}

private fun PeekableIterator<Token>.parseProtocolMember(): Decl.Protocol.Declaration {
    return when (val token = peek()) {
        is Token.Dec -> parseDecDecl()
        else -> throw Exception("Unexpected token $token")
    }
}

private fun PeekableIterator<Token>.parseExtensionDecl(): Decl.Extension {
    expect<Token.Extension>()
    val typeName = parseIdentifier()
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
    expect<Token.End>()
    return Decl.Extension(typeName, protocolName, declarations)
}

private fun PeekableIterator<Token>.parseExtensionMember(): Decl.Extension.Declaration {
    return when (val token = peek()) {
        is Token.Dec -> parseDecDecl()
        is Token.Let -> parseLetDecl()
        else -> throw Exception("Unexpected token $token")
    }
}

private fun PeekableIterator<Token>.parseStructDecl(): Decl.Struct {
    expect<Token.Struct>()
    val name = parseIdentifier()
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleStructPartial(name)
        is Token.LBrace -> parseRecordStructPartial(name)
        else -> Decl.Struct.Bare(name)
    }
}

private fun PeekableIterator<Token>.parseTupleStructPartial(name: String): Decl.Struct.Tuple {
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
    return Decl.Struct.Tuple(name, components)
}

private fun PeekableIterator<Token>.parseRecordStructPartial(name: String): Decl.Struct.Record {
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
    return Decl.Struct.Record(name, components)
}

private fun PeekableIterator<Token>.parseDecDecl(): Decl.Dec {
    expect<Token.Dec>()
    val name = parseIdentifier()
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleDecPartial(name)
        is Token.LBrace -> parseRecordDecPartial(name)
        else -> parseBareDecPartial(name)
    }
}

private fun PeekableIterator<Token>.parseTupleDecPartial(name: String): Decl.Dec.Tuple {
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
    return Decl.Dec.Tuple(name, components, type)
}

private fun PeekableIterator<Token>.parseRecordDecPartial(name: String): Decl.Dec.Record {
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
    return Decl.Dec.Record(name, components, type)
}

private fun PeekableIterator<Token>.parseBareDecPartial(name: String): Decl.Dec.Bare {
    expect<Token.Colon>()
    val type = parseType()
    return Decl.Dec.Bare(name, type)
}

private fun PeekableIterator<Token>.parseLetDecl(): Decl.Let {
    expect<Token.Let>()
    val name = parseIdentifier()
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleLetPartial(name)
        is Token.LBrace -> parseRecordLetPartial(name)
        else -> parseBareLetPartial(name)
    }
}

private fun PeekableIterator<Token>.parseTupleLetPartial(name: String): Decl.Let.Tuple {
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
    return Decl.Let.Tuple(name, components, value)
}

private fun PeekableIterator<Token>.parseRecordLetPartial(name: String): Decl.Let.Record {
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
    return Decl.Let.Record(name, components, value)
}

private fun PeekableIterator<Token>.parseBareLetPartial(name: String): Decl.Let.Bare {
    expect<Token.Eq>()
    val value = parseExpr()
    return Decl.Let.Bare(name, value)
}

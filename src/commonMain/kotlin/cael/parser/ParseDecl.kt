package cael.parser

import cael.ast.*

fun PeekableIterator<Token>.parseDecl(): Decl {
    return when (val token = peek()) {
        is Token.Struct -> parseStructDecl()
        is Token.Let -> parseLetDecl()
        is Token.Fun -> parseFunDecl()
        else -> throw Exception("Unexpected token $token")
    }
}

private fun PeekableIterator<Token>.parseStructDecl(): Decl.Struct {
    val start = expect<Token.Struct>()
    val identifier = parseIdentifier()
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleStructPartial(start, identifier.name)
        is Token.LBrace -> parseRecordStructPartial(start, identifier.name)
        is Token.Pipe -> parseMatchStructPartial(start, identifier.name)
        else -> Decl.Struct.Bare(identifier.name, start..identifier)
    }
}

private fun PeekableIterator<Token>.parseTupleStructPartial(start: Token.Struct, name: String): Decl.Struct.Tuple {
    expect<Token.LParen>()
    val components = parsePatternTupleComponents()
    val end = expect<Token.RParen>()
    return Decl.Struct.Tuple(name, components, start..end)
}

private fun PeekableIterator<Token>.parseRecordStructPartial(start: Token.Struct, name: String): Decl.Struct.Record {
    expect<Token.LBrace>()
    val components = parsePatternRecordComponents()
    val end = expect<Token.RBrace>()
    return Decl.Struct.Record(name, components, start..end)
}

private fun PeekableIterator<Token>.parseMatchStructPartial(start: Token.Struct, name: String): Decl.Struct.Match {
    val cases = mutableListOf<Decl.Struct.Match.Case>()
    while (match<Token.Pipe>()) {
        when (peekOrNull()) {
            is Token.LParen -> cases.add(parseTupleMatchStructCase())
            is Token.LBrace -> cases.add(parseRecordMatchStructCase())
            else -> throw Exception("Unexpected token ${peekOrNull()}")
        }
    }
    return Decl.Struct.Match(name, cases, start.range..cases.last().range)
}

private fun PeekableIterator<Token>.parseTupleMatchStructCase(): Decl.Struct.Match.Case.Tuple {
    val first = expect<Token.LParen>()
    val parameters = parsePatternTupleComponents()
    expect<Token.RParen>()
    expect<Token.Arrow>()
    val value = parseExpr()
    return Decl.Struct.Match.Case.Tuple(parameters, value, first.range..value.range)
}

private fun PeekableIterator<Token>.parseRecordMatchStructCase(): Decl.Struct.Match.Case.Record {
    val first = expect<Token.LBrace>()
    val parameters = parsePatternRecordComponents()
    expect<Token.RBrace>()
    expect<Token.Arrow>()
    val value = parseExpr()
    return Decl.Struct.Match.Case.Record(parameters, value, first.range..value.range)
}

private fun PeekableIterator<Token>.parseLetDecl(): Decl.Let {
    val start = expect<Token.Let>()
    val pattern = parsePattern()
    expect<Token.Eq>()
    val value = parseExpr()
    return Decl.Let(pattern, value, start.range..value.range)
}

private fun PeekableIterator<Token>.parseFunDecl(): Decl.Fun {
    val start = expect<Token.Fun>()
    val (name) = parseIdentifier()
    return when (peekOrNull()) {
        is Token.LParen -> parseTupleFunPartial(start, name)
        is Token.LBrace -> parseRecordFunPartial(start, name)
        is Token.Pipe -> parseMatchFunPartial(start, name)
        else -> throw Exception("Unexpected token ${peekOrNull()}")
    }
}

private fun PeekableIterator<Token>.parseTupleFunPartial(start: Token.Fun, name: String): Decl.Fun.Tuple {
    expect<Token.LParen>()
    val components = parsePatternTupleComponents()
    expect<Token.RParen>()
    expect<Token.Arrow>()
    val value = parseExpr()
    return Decl.Fun.Tuple(name, components, value, start.range..value.range)
}

private fun PeekableIterator<Token>.parseRecordFunPartial(start: Token.Fun, name: String): Decl.Fun.Record {
    expect<Token.LBrace>()
    val components = parsePatternRecordComponents()
    expect<Token.RBrace>()
    expect<Token.Arrow>()
    val value = parseExpr()
    return Decl.Fun.Record(name, components, value, start.range..value.range)
}

private fun PeekableIterator<Token>.parseMatchFunPartial(start: Token.Fun, name: String): Decl.Fun.Match {
    val cases = mutableListOf<Decl.Fun.Match.Case>()
    while (match<Token.Pipe>()) {
        when (peekOrNull()) {
            is Token.LParen -> cases.add(parseTupleMatchFunCase())
            is Token.LBrace -> cases.add(parseRecordMatchFunCase())
            else -> throw Exception("Unexpected token ${peekOrNull()}")
        }
    }
    return Decl.Fun.Match(name, cases, start.range..cases.last().range)
}

private fun PeekableIterator<Token>.parseTupleMatchFunCase(): Decl.Fun.Match.Case.Tuple {
    val first = expect<Token.LParen>()
    val parameters = parsePatternTupleComponents()
    expect<Token.RParen>()
    expect<Token.Arrow>()
    val value = parseExpr()
    return Decl.Fun.Match.Case.Tuple(parameters, value, first.range..value.range)
}

private fun PeekableIterator<Token>.parseRecordMatchFunCase(): Decl.Fun.Match.Case.Record {
    val first = expect<Token.LBrace>()
    val parameters = parsePatternRecordComponents()
    expect<Token.RBrace>()
    expect<Token.Arrow>()
    val value = parseExpr()
    return Decl.Fun.Match.Case.Record(parameters, value, first.range..value.range)
}

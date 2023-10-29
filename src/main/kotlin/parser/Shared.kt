package parser

fun PeekableIterator<Token>.parseTypeRecordItem(): TypeRecordItem {
    val name = parseIdentifier()
    expect(Token.Colon)
    val type = parseType()
    return TypeRecordItem(name, type)
}

fun PeekableIterator<Token>.parseExprRecordItem(): ExprRecordItem {
    val name = parseIdentifier()
    expect(Token.Eq)
    val value = parseExpr()
    return ExprRecordItem(name, value)
}

fun PeekableIterator<Token>.parsePatternRecordItem(): PatternRecordItem {
    val name = parseIdentifier()
    expect(Token.Eq)
    val pattern = parsePattern()
    return PatternRecordItem(name, pattern)
}

fun PeekableIterator<Token>.parseIdentifier(): String {
    val token = nextOrNull() ?: throw Exception("Expected identifier, got EOF")
    return when (token) {
        is Token.Identifier -> token.name
        else -> throw Exception("Expected identifier, got $token")
    }
}
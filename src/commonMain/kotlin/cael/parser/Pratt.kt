package cael.parser

import cael.ast.Node
import kotlin.reflect.KClassifier

class Pratt<T : Node>(
    val prefixes : Map<KClassifier, Prefix<T>>,
    val infixes : Map<KClassifier, Infix<T>>,
) {
    class Prefix<T>(
        val parse: PeekableIterator<Token>.(token: Token) -> T,
    )

    class Infix<T>(
        val precedence: Int,
        val parse: PeekableIterator<Token>.(left: T, token: Token) -> T,
    )

    fun PeekableIterator<Token>.parse(precedence: Int): T {
        var token = nextOrNull() ?: throw ParseError("Expected token, got end of file", lastRange())
        val prefix = prefixes[token::class] ?: throw ParseError("Expected prefix, got `${token.lexeme}`", token.range)

        var left = prefix.parse(this, token)

        while (precedence < getPrecendence()) {
            token = nextOrNull() ?: throw ParseError("Expected token, got end of file", lastRange())
            val infix = infixes[token::class] ?: throw ParseError("Expected infix, got `${token.lexeme}`", token.range)
            left = infix.parse(this, left, token)
        }

        return left
    }

    private fun PeekableIterator<Token>.getPrecendence(): Int {
        val peeked = peekOrNull() ?: return 0
        val infix = infixes[peeked::class] ?: return 0
        return infix.precedence
    }
}
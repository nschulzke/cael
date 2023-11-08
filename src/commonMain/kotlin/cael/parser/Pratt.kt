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
        var token = nextOrNull() ?: throw Exception("Expected token, got null")
        val prefix = prefixes[token::class] ?: throw Exception("Expected prefix, got $token")

        var left = prefix.parse(this, token)

        while (precedence < getPrecendence()) {
            token = nextOrNull() ?: throw Exception("Expected token, got null")
            val infix = infixes[token::class] ?: throw Exception("Expected infix, got $token")
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
package cael.parser

import cael.ast.Range

open class PeekableIterator<T>(
    private val iterator: Iterator<T>,
) : Iterator<T> {
    private var peek: T? = if (iterator.hasNext()) iterator.next() else null
    private var last: T? = null

    fun lastRange(): Range {
        return (last as? Token)?.range ?: Range(0, 0)
    }

    protected open val onNext: ((T?) -> Unit)? = null

    override fun next(): T {
        return nextOrNull() ?: throw NoSuchElementException()
    }

    override fun hasNext(): Boolean {
        return peek != null
    }

    fun nextOrNull(): T? {
        return peek.also {
            onNext?.invoke(it)
            last = it
            peek = if (iterator.hasNext()) iterator.next() else null
        }
    }

    fun peek(): T {
        return peek ?: throw NoSuchElementException()
    }

    fun peekOrNull(): T? {
        return peek
    }

    fun match(token: T): Boolean {
        return if (peekOrNull() == token) {
            next()
            true
        } else {
            false
        }
    }

    inline fun <reified T : Token> match(): Boolean {
        return if (peekOrNull() is T) {
            next()
            true
        } else {
            false
        }
    }

    inline fun <reified T : Token> expect(): T {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        return when (val peeked = peekOrNull()) {
            is T -> next() as T
            null -> throw ParseError("Expected `${T::class.simpleName}`, got end of file", lastRange())
            else -> throw ParseError("Expected `${T::class.simpleName}`, got `${peeked!!::class.simpleName}`", (peeked as Token).range)
        }
    }
}
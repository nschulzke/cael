package cael.parser

open class PeekableIterator<T>(
    private val iterator: Iterator<T>,
) : Iterator<T> {
    private var peek: T? = if (iterator.hasNext()) iterator.next() else null

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

    fun expect(token: T): T {
        return if (peekOrNull() == token) {
            next()
        } else {
            throw Exception("Expected $token, got ${peekOrNull()}")
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
        return if (peekOrNull() is T) {
            next() as T
        } else {
            throw Exception("Expected ${T::class.simpleName}, got ${peekOrNull()}")
        }
    }
}
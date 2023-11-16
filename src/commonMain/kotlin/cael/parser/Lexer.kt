package cael.parser

import cael.ast.FileContents
import cael.ast.Range
import cael.io.asSequenceUtf8
import cael.io.asString
import cael.io.toSource
import okio.*

private val keywords = mapOf<String, (Range) -> Token>(
    "let" to { Token.Let(it) },
    "fun" to { Token.Fun(it) },
    "match" to { Token.Match(it) },
    "struct" to { Token.Struct(it) },
)

fun Path.lex(): Sequence<Token> =
    try {
        this.toSource().lex()
    } catch (e: LexError) {
        FileContents(
            fileName = "file",
            fileContents = toSource().asString()
        ).rethrowError(e)
    }

fun Source.lex(): Sequence<Token> =
    try {
        this.asSequenceUtf8().lex()
    } catch (e: LexError) {
        FileContents(
            fileName = "file",
            fileContents = asString()
        ).rethrowError(e)
    }

fun String.lex(): Sequence<Token> =
    try {
        this.asSequence().lex()
    } catch (e: LexError) {
        FileContents(
            fileName = "file",
            fileContents = this
        ).rethrowError(e)
    }

class CharIterator(
    iterator: Iterator<Char>,
) : PeekableIterator<Char>(iterator) {
    inner class CoordsStringBuilder(
        private val stringBuilder: StringBuilder
    ) {
        private var startOffset: Int? = null
        private var length: Int = 0

        override fun toString() = stringBuilder.toString()

        fun isNotEmpty() = stringBuilder.isNotEmpty()

        fun append(c: Char) {
            if (stringBuilder.isEmpty()) {
                startOffset = offset
            }
            length++
            stringBuilder.append(c)
        }

        fun clear(): Range {
            stringBuilder.clear()
            val range = Range(startOffset ?: offset, length)
            startOffset = null
            length = 0
            return range
        }
    }

    fun stringBuilder() = CoordsStringBuilder(StringBuilder())

    var line: Int = 1
        private set

    var col: Int = 0
        private set

    var offset: Int = 0
        private set

    private var startOffset = offset

    fun markStart() {
        startOffset = offset
    }

    fun range() = Range(startOffset..offset)

    override val onNext: ((Char?) -> Unit) = {
        offset++
        if (it == '\n') {
            line++
            col = 0
        } else if (it != null) {
            col++
        }
    }
}

typealias LexerMode = CharIterator.() -> Sequence<Token>

object Mode {
    val normal: LexerMode = {
        sequence {
            while (hasNext()) {
                when (val c = next()) {
                    ' ', '\t', '\r', '\n' -> {
                        // Skip whitespace
                    }

                    '{' -> yield(Token.LBrace(Range(offset, 1)))
                    '}' -> yield(Token.RBrace(Range(offset, 1)))
                    '(' -> yield(Token.LParen(Range(offset, 1)))
                    ')' -> yield(Token.RParen(Range(offset, 1)))
                    '[' -> yield(Token.LBracket(Range(offset, 1)))
                    ']' -> yield(Token.RBracket(Range(offset, 1)))

                    ',' -> yield(Token.Comma(Range(offset, 1)))
                    ':' -> yield(Token.Colon(Range(offset, 1)))
                    '.' -> yield(Token.Dot(Range(offset, 1)))
                    '?' -> yield(Token.Question(Range(offset, 1)))
                    '+' -> yield(Token.Plus(Range(offset, 1)))
                    '-' -> yield(Token.Minus(Range(offset, 1)))
                    '*' -> yield(Token.Star(Range(offset, 1)))
                    '/' -> yield(Token.Slash(Range(offset, 1)))
                    '%' -> yield(Token.Percent(Range(offset, 1)))

                    '=' -> {
                        markStart()
                        if (hasNext() && match('>')) {
                            yield(Token.Arrow(range()))
                        } else if (hasNext() && match('=')) {
                            yield(Token.EqEq(range()))
                        } else {
                            yield(Token.Eq(range()))
                        }
                    }

                    '!' -> {
                        markStart()
                        if (hasNext() && match('=')) {
                            yield(Token.BangEq(range()))
                        } else {
                            yield(Token.Bang(range()))
                        }
                    }

                    '<' -> {
                        markStart()
                        if (hasNext() && match('=')) {
                            yield(Token.LtEq(range()))
                        } else {
                            yield(Token.Lt(range()))
                        }
                    }

                    '>' -> {
                        markStart()
                        if (hasNext() && match('=')) {
                            yield(Token.GtEq(range()))
                        } else {
                            yield(Token.Gt(range()))
                        }
                    }

                    '&' -> {
                        markStart()
                        if (hasNext() && match('&')) {
                            yield(Token.AmpAmp(range()))
                        } else {
                            yield(Token.Amp(range()))
                        }
                    }

                    '|' -> {
                        markStart()
                        if (hasNext() && match('|')) {
                            yield(Token.PipePipe(range()))
                        } else {
                            yield(Token.Pipe(range()))
                        }
                    }

                    '#' -> {
                        while (hasNext() && next() != '\n') {
                            // Skip comment
                        }
                    }

                    '"' -> {
                        val mode = string('"')
                        yieldAll(mode())
                    }

                    '\'' -> {
                        val mode = string('\'')
                        yieldAll(mode())
                    }

                    else -> {
                        if (c.isDigit()) {
                            val parsed = lexNumber(c)
                            yield(parsed)
                        } else if (c.isLetter() || c == '_') {
                            val parsed = lexIdentifier(c)
                            yield(parsed)
                        } else {
                            throw LexError("Unexpected character: `$c`", Range(offset, 1))
                        }
                    }
                }
            }
        }
    }

    fun string(closingChar: Char): LexerMode = {
        sequence {
            markStart()
            yield(Token.BeginString(range()))
            val builder = stringBuilder()
            while (hasNext()) {
                val c = next()
                val charOffset = offset
                when (c) {
                    '\\' -> {
                        when (val c2 = next()) {
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            '\\' -> builder.append('\\')
                            closingChar -> builder.append(closingChar)
                            'u' -> {
                                val codepoint = (0..3).fold(0) { acc, _ ->
                                    val c3 = next()
                                    when (c3) {
                                        in '0'..'9' -> {
                                            acc * 16 + (c3 - '0')
                                        }

                                        in 'a'..'f' -> {
                                            acc * 16 + (c3 - 'a') + 10
                                        }

                                        in 'A'..'F' -> {
                                            acc * 16 + (c3 - 'A') + 10
                                        }

                                        else -> {
                                            throw LexError(
                                                "Invalid unicode escape sequence: `\\u$c3`",
                                                Range(charOffset, 6)
                                            )
                                        }
                                    }
                                }
                                builder.append(codepoint.toChar())
                            }

                            '(' -> {
                                if (builder.isNotEmpty()) {
                                    yield(Token.StringLiteralSegment(builder.toString(), builder.clear()))
                                }
                                yield(Token.BeginInterpolating(Range(charOffset, 2)))
                                yieldAll(stringInterpolation())
                                markStart()
                            }

                            else -> throw LexError("Unexpected escape sequence: `\\$c2`", Range(charOffset, 2))
                        }
                    }

                    closingChar -> {
                        if (builder.isNotEmpty()) {
                            yield(Token.StringLiteralSegment(builder.toString(), builder.clear()))
                        }
                        yield(Token.EndString(Range(offset, 1)))
                        return@sequence
                    }

                    else -> {
                        builder.append(c)
                    }
                }
            }
            throw LexError("Unterminated string literal", Range(offset, 0))
        }
    }

    val stringInterpolation: LexerMode = {
        val delegate = normal().iterator()
        sequence {
            var parens = 1
            while (hasNext()) {
                val c = peek()
                when (c) {
                    ' ', '\t', '\r', '\n' -> {
                        next()
                    }

                    '(' -> {
                        parens++
                        yield(delegate.next())
                    }

                    ')' -> {
                        if (--parens == 0) {
                            next()
                            yield(Token.EndInterpolating(Range(offset, 1)))
                            return@sequence
                        } else {
                            yield(delegate.next())
                        }
                    }

                    else -> {
                        yield(delegate.next())
                    }
                }
            }
        }
    }
}

fun Sequence<Token>.consolidateStringLiterals(): Sequence<Token> {
    val delegate = this.iterator()
    return sequence {
        while (delegate.hasNext()) {
            val maybeBeginString = delegate.next()

            if (maybeBeginString !is Token.BeginString) {
                yield(maybeBeginString)
                continue
            }

            val maybeStringLiteralSegment = delegate.next()
            if (maybeStringLiteralSegment is Token.EndString) {
                yield(Token.StringLiteral("", maybeBeginString.range..maybeStringLiteralSegment.range))
                continue
            }
            if (maybeStringLiteralSegment !is Token.StringLiteralSegment) {
                yield(maybeBeginString)
                yield(maybeStringLiteralSegment)
                continue
            }

            val maybeEndString = delegate.next()
            if (maybeEndString !is Token.EndString) {
                yield(maybeBeginString)
                yield(maybeStringLiteralSegment)
                yield(maybeEndString)
                continue
            }

            yield(Token.StringLiteral(maybeStringLiteralSegment.value, maybeBeginString.range..maybeEndString.range))
        }
    }
}

fun Sequence<Char>.lex(): Sequence<Token> {
    val iterator = CharIterator(iterator())
    return sequence {
        yieldAll(Mode.normal(iterator))
        if (iterator.hasNext()) {
            throw LexError("Unexpected character: ${iterator.next()}", Range(0, 0))
        }
    }.consolidateStringLiterals()
}

private fun CharIterator.lexNumber(firstChar: Char): Token {
    markStart()
    val builder = StringBuilder()
    builder.append(firstChar)
    while (hasNext()) {
        val c = peek()
        if (c.isDigit()) {
            builder.append(next())
        } else if (c == '.') {
            builder.append(next())
            while (hasNext()) {
                val c2 = peek()
                if (c2.isDigit()) {
                    builder.append(next())
                } else {
                    break
                }
            }
            return Token.FloatLiteral(builder.toString().toDouble(), range())
        } else {
            break
        }
    }
    return Token.IntLiteral(builder.toString().toInt(), range())
}

private fun CharIterator.lexIdentifier(firstChar: Char): Token {
    markStart()
    val builder = StringBuilder()
    builder.append(firstChar)
    while (hasNext()) {
        val c = peek()
        if (c.isLetterOrDigit() || c == '_') {
            builder.append(next())
        } else {
            break
        }
    }
    val name = builder.toString()
    return if (name in keywords) {
        keywords[name]!!.invoke(range())
    } else {
        Token.Identifier(name, range())
    }
}


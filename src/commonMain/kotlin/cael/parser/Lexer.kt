package cael.parser

import cael.ast.Range
import cael.io.asSequenceUtf8
import cael.io.toSource
import okio.*

class LexerError(val filename: String, val line: Int, val col: Int, val description: String) :
    Error("Lexing error at $filename:${line}:${col} $description")

private val keywords = mapOf<String, (Range) -> Token>(
    "let" to { Token.Let(it) },
    "fun" to { Token.Fun(it) },
    "match" to { Token.Match(it) },
    "struct" to { Token.Struct(it) },
)

fun Path.lex(): Sequence<Token> =
    this.toSource().lex()

fun Source.lex(): Sequence<Token> =
    this.asSequenceUtf8().lex()

fun String.lex(): Sequence<Token> =
    this.asSequence().lex()

class CharIterator(
    val filename: String,
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

    var length: Int = 0
        private set

    private var startOffset = offset

    fun markStart() {
        startOffset = offset
        length = 0
    }

    fun range() = Range(startOffset..offset)

    override val onNext: ((Char?) -> Unit) = {
        offset++
        length++
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
                            throw LexerError(filename, line, col, "Unexpected character: $c")
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
                if (c == '\\') {
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
                                        throw LexerError(filename, line, col, "Invalid unicode escape sequence: \\u$c3")
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

                        else -> throw LexerError(filename, line, col, "Unexpected escape sequence: \\$c2")
                    }
                } else if (c == closingChar) {
                    if (builder.isNotEmpty()) {
                        yield(Token.StringLiteralSegment(builder.toString(), builder.clear()))
                    }
                    yield(Token.EndString(Range(offset, 1)))
                    return@sequence
                } else {
                    builder.append(c)
                }
            }
            throw LexerError(filename, line, col, "Unterminated string literal")
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

fun Sequence<Char>.lex(filename: String = "file"): Sequence<Token> {
    val iterator = CharIterator(filename, iterator())
    return sequence {
        yieldAll(Mode.normal(iterator))
        if (iterator.hasNext()) {
            throw LexerError(filename, 0, 0, "Unexpected character: ${iterator.next()}")
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


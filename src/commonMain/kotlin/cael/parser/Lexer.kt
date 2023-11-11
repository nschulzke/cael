package cael.parser

import cael.ast.Coords
import cael.ast.Range
import cael.io.asSequenceUtf8
import cael.io.toSource
import okio.*

class LexerError(val filename: String, val description: String, val coords: Coords) :
    Error("Lexing error at $filename:${coords.line}:${coords.col} $description")

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
        private var startCoords: Coords? = null
        private var endCoords: Coords? = null

        override fun toString() = stringBuilder.toString()

        fun isNotEmpty() = stringBuilder.isNotEmpty()

        fun append(c: Char) {
            if (stringBuilder.isEmpty()) {
                startCoords = coords()
            }
            endCoords = coords()
            stringBuilder.append(c)
        }

        fun clear(): Range {
            stringBuilder.clear()
            val start = startCoords ?: coords()
            val end = endCoords ?: coords()
            startCoords = null
            endCoords = null
            return start..end
        }
    }

    fun stringBuilder() = CoordsStringBuilder(StringBuilder())

    var line: Int = 1
        private set

    var col: Int = 0
        private set

    private var startCoords = coords()

    fun coords() = Coords(line, col)

    fun markStart() {
        startCoords = coords()
    }

    fun range() = startCoords..coords()

    override val onNext: ((Char?) -> Unit) = {
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

                    '{' -> yield(Token.LBrace(coords()..coords()))
                    '}' -> yield(Token.RBrace(coords()..coords()))
                    '(' -> yield(Token.LParen(coords()..coords()))
                    ')' -> yield(Token.RParen(coords()..coords()))
                    '[' -> yield(Token.LBracket(coords()..coords()))
                    ']' -> yield(Token.RBracket(coords()..coords()))

                    ',' -> yield(Token.Comma(coords()..coords()))
                    ':' -> yield(Token.Colon(coords()..coords()))
                    '.' -> yield(Token.Dot(coords()..coords()))
                    '?' -> yield(Token.Question(coords()..coords()))
                    '+' -> yield(Token.Plus(coords()..coords()))
                    '-' -> yield(Token.Minus(coords()..coords()))
                    '*' -> yield(Token.Star(coords()..coords()))
                    '/' -> yield(Token.Slash(coords()..coords()))
                    '%' -> yield(Token.Percent(coords()..coords()))

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
                            throw LexerError(filename, "Unexpected character: $c", coords())
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
                val cCoords = coords()
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
                                        throw LexerError(filename, "Invalid unicode escape sequence: \\u$c3", coords())
                                    }
                                }
                            }
                            builder.append(codepoint.toChar())
                        }
                        '(' -> {
                            if (builder.isNotEmpty()) {
                                yield(Token.StringLiteralSegment(builder.toString(), builder.clear()))
                            }
                            yield(Token.BeginInterpolating(cCoords..coords()))
                            yieldAll(stringInterpolation())
                            markStart()
                        }

                        else -> throw LexerError(filename, "Unexpected escape sequence: \\$c2", coords())
                    }
                } else if (c == closingChar) {
                    if (builder.isNotEmpty()) {
                        yield(Token.StringLiteralSegment(builder.toString(), builder.clear()))
                    }
                    yield(Token.EndString(coords()..coords()))
                    return@sequence
                } else {
                    builder.append(c)
                }
            }
            throw LexerError(filename, "Unterminated string literal", coords())
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
                            yield(Token.EndInterpolating(coords()..coords()))
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
            throw LexerError(filename, "Unexpected character: ${iterator.next()}", iterator.coords())
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


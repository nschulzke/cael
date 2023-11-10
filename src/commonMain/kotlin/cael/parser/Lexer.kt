package cael.parser

import cael.ast.Coords
import cael.ast.Range
import cael.io.asSequenceUtf8
import cael.io.toSource
import okio.*

class LexerError(val description: String, val coords: Coords) :
    Error("Lexing error at ${coords.filename}:${coords.line}:${coords.col} $description")

private val keywords = mapOf<String, (Range) -> Token>(
    "dec" to { Token.Dec(it) },
    "end" to { Token.End(it) },
    "extension" to { Token.Extension(it) },
    "for" to { Token.For(it) },
    "is" to { Token.Is(it) },
    "let" to { Token.Let(it) },
    "match" to { Token.Match(it) },
    "module" to { Token.Module(it) },
    "open" to { Token.Open(it) },
    "protocol" to { Token.Protocol(it) },
    "struct" to { Token.Struct(it) },
    "type" to { Token.Type(it) },
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
    var line: Int = 1
        private set

    var col: Int = 0
        private set

    private var startCoords = coords()

    fun coords() = Coords(filename, line, col)

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

fun Sequence<Char>.lex(): Sequence<Token> {
    val iterator = CharIterator("file", iterator())
    return sequence {
        with(iterator) {
            while (iterator.hasNext()) {
                when (val c = iterator.next()) {
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
                        if (iterator.hasNext() && iterator.match('>')) {
                            yield(Token.Arrow(range()))
                        } else if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.EqEq(range()))
                        } else {
                            yield(Token.Eq(range()))
                        }
                    }

                    '!' -> {
                        markStart()
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.BangEq(range()))
                        } else {
                            yield(Token.Bang(range()))
                        }
                    }

                    '<' -> {
                        markStart()
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.LtEq(range()))
                        } else {
                            yield(Token.Lt(range()))
                        }
                    }

                    '>' -> {
                        markStart()
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.GtEq(range()))
                        } else {
                            yield(Token.Gt(range()))
                        }
                    }

                    '&' -> {
                        markStart()
                        if (iterator.hasNext() && iterator.match('&')) {
                            yield(Token.AmpAmp(range()))
                        } else {
                            yield(Token.Amp(range()))
                        }
                    }

                    '|' -> {
                        markStart()
                        if (iterator.hasNext() && iterator.match('|')) {
                            yield(Token.PipePipe(range()))
                        } else {
                            yield(Token.Pipe(range()))
                        }
                    }

                    '#' -> {
                        while (iterator.hasNext() && iterator.next() != '\n') {
                            // Skip comment
                        }
                    }

                    '"' -> {
                        val parsed = lexString('"')
                        yield(parsed)
                    }

                    '\'' -> {
                        val parsed = lexString('\'')
                        yield(parsed)
                    }

                    else -> {
                        if (c.isDigit()) {
                            val parsed = lexNumber(c)
                            yield(parsed)
                        } else if (c.isLetter() || c == '_') {
                            val parsed = lexIdentifier(c)
                            yield(parsed)
                        } else {
                            throw LexerError("Unexpected character: $c", coords())
                        }
                    }
                }
            }
        }
    }
}

private fun CharIterator.lexString(closingChar: Char): Token {
    markStart()
    val builder = StringBuilder()
    while (hasNext()) {
        val c = next()
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
                                throw LexerError("Invalid unicode escape sequence: \\u$c3", coords())
                            }
                        }
                    }
                    builder.append(codepoint.toChar())
                }
                else -> throw LexerError("Unexpected escape sequence: \\$c2", coords())
            }
        } else if (c == closingChar) {
            return Token.StringLiteral(builder.toString(), range())
        } else {
            builder.append(c)
        }
    }
    throw LexerError("Unterminated string literal", coords())
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


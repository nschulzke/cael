package cael.parser

import java.io.InputStream

data class Coords(
    val filename: String,
    val line: Int,
    val col: Int,
)

sealed interface Token {
    val lexeme: String

    data class Dec(
        val coords: Coords
    ) : Token {
        override val lexeme = "dec"
    }

    data class End(
        val coords: Coords
    ) : Token {
        override val lexeme = "end"
    }

    data class Extension(
        val coords: Coords
    ) : Token {
        override val lexeme = "extension"
    }

    data class For(
        val coords: Coords
    ) : Token {
        override val lexeme = "for"
    }

    data class Is(
        val coords: Coords
    ) : Token {
        override val lexeme = "is"
    }

    data class Let(
        val coords: Coords
    ) : Token {
        override val lexeme = "let"
    }

    data class Match(
        val coords: Coords
    ) : Token {
        override val lexeme = "match"
    }

    data class Module(
        val coords: Coords
    ) : Token {
        override val lexeme = "module"
    }

    data class Open(
        val coords: Coords
    ) : Token {
        override val lexeme = "open"
    }

    data class Protocol(
        val coords: Coords
    ) : Token {
        override val lexeme = "protocol"
    }

    data class Struct(
        val coords: Coords
    ) : Token {
        override val lexeme = "struct"
    }

    data class Type(
        val coords: Coords
    ) : Token {
        override val lexeme = "type"
    }

    data class LBrace(
        val coords: Coords
    ) : Token {
        override val lexeme = "{"
    }

    data class RBrace(
        val coords: Coords
    ) : Token {
        override val lexeme = "}"
    }

    data class LParen(
        val coords: Coords
    ) : Token {
        override val lexeme = "("
    }

    data class RParen(
        val coords: Coords
    ) : Token {
        override val lexeme = ")"
    }

    data class LBracket(
        val coords: Coords
    ) : Token {
        override val lexeme = "["
    }

    data class RBracket(
        val coords: Coords
    ) : Token {
        override val lexeme = "]"
    }

    data class Comma(
        val coords: Coords
    ) : Token {
        override val lexeme = ","
    }

    data class Colon(
        val coords: Coords
    ) : Token {
        override val lexeme = ":"
    }

    data class Dot(
        val coords: Coords
    ) : Token {
        override val lexeme = "."
    }

    data class Question(
        val coords: Coords
    ) : Token {
        override val lexeme = "?"
    }

    data class Plus(
        val coords: Coords
    ) : Token {
        override val lexeme = "+"
    }

    data class Minus(
        val coords: Coords
    ) : Token {
        override val lexeme = "-"
    }

    data class Times(
        val coords: Coords
    ) : Token {
        override val lexeme = "*"
    }

    data class Div(
        val coords: Coords
    ) : Token {
        override val lexeme = "/"
    }

    data class Mod(
        val coords: Coords
    ) : Token {
        override val lexeme = "%"
    }

    data class Bang(
        val coords: Coords
    ) : Token {
        override val lexeme = "!"
    }

    data class BangEq(
        val coords: Coords
    ) : Token {
        override val lexeme = "!="
    }

    data class Lt(
        val coords: Coords
    ) : Token {
        override val lexeme = "<"
    }

    data class LtEq(
        val coords: Coords
    ) : Token {
        override val lexeme = "<="
    }

    data class Gt(
        val coords: Coords
    ) : Token {
        override val lexeme = ">"
    }

    data class GtEq(
        val coords: Coords
    ) : Token {
        override val lexeme = ">="
    }

    data class Eq(
        val coords: Coords
    ) : Token {
        override val lexeme = "="
    }

    data class EqEq(
        val coords: Coords
    ) : Token {
        override val lexeme = "=="
    }

    data class Arrow(
        val coords: Coords
    ) : Token {
        override val lexeme = "=>"
    }

    data class Amp(
        val coords: Coords
    ) : Token {
        override val lexeme = "&"
    }

    data class AmpAmp(
        val coords: Coords
    ) : Token {
        override val lexeme = "&&"
    }

    data class Pipe(
        val coords: Coords
    ) : Token {
        override val lexeme = "|"
    }

    data class PipePipe(
        val coords: Coords
    ) : Token {
        override val lexeme = "||"
    }

    data class IntLiteral(
        val value: Int,
        val coords: Coords,
    ) : Token {
        override val lexeme = value.toString()
    }

    data class FloatLiteral(
        val value: Double,
        val coords: Coords,
    ) : Token {
        override val lexeme = value.toString()
    }

    data class StringLiteral(
        val value: String,
        val coords: Coords,
    ) : Token {
        override val lexeme = "\"$value\""
    }

    data class Identifier(
        val name: String,
        val coords: Coords,
    ) : Token {
        override val lexeme = name
    }
}

private val keywords = mapOf<String, (Coords) -> Token>(
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

fun InputStream.lex(): Sequence<Token> =
    this.bufferedReader()
        .lineSequence()
        .flatMap { it.asSequence() }
        .lex()

fun String.lex(): Sequence<Token> =
    this.asSequence().lex()

class CoordinateIterator(
    val filename: String,
    iterator: Iterator<Char>,
) : PeekableIterator<Char>(iterator) {
    var line: Int = 1
        private set

    var col: Int = 1
        private set

    override val onNext: ((Char?) -> Unit) = {
        if (it == '\n') {
            line++
            col = 1
        } else if (it != null) {
            col++
        }
    }
}

fun Sequence<Char>.lex(): Sequence<Token> {
    val iterator = CoordinateIterator("file", this.iterator())
    return sequence {
        with(iterator) {
            while (iterator.hasNext()) {
                when (val c = iterator.next()) {
                    ' ', '\t', '\r', '\n' -> {
                        // Skip whitespace
                    }

                    '{' -> yield(Token.LBrace(Coords(filename, line, col)))
                    '}' -> yield(Token.RBrace(Coords(filename, line, col)))
                    '(' -> yield(Token.LParen(Coords(filename, line, col)))
                    ')' -> yield(Token.RParen(Coords(filename, line, col)))
                    '[' -> yield(Token.LBracket(Coords(filename, line, col)))
                    ']' -> yield(Token.RBracket(Coords(filename, line, col)))

                    ',' -> yield(Token.Comma(Coords(filename, line, col)))
                    ':' -> yield(Token.Colon(Coords(filename, line, col)))
                    '.' -> yield(Token.Dot(Coords(filename, line, col)))
                    '?' -> yield(Token.Question(Coords(filename, line, col)))
                    '+' -> yield(Token.Plus(Coords(filename, line, col)))
                    '-' -> yield(Token.Minus(Coords(filename, line, col)))
                    '*' -> yield(Token.Times(Coords(filename, line, col)))
                    '/' -> yield(Token.Div(Coords(filename, line, col)))
                    '%' -> yield(Token.Mod(Coords(filename, line, col)))

                    '=' -> {
                        if (iterator.hasNext() && iterator.match('>')) {
                            yield(Token.Arrow(Coords(filename, line, col)))
                        } else if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.EqEq(Coords(filename, line, col)))
                        } else {
                            yield(Token.Eq(Coords(filename, line, col)))
                        }
                    }

                    '!' -> {
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.BangEq(Coords(filename, line, col)))
                        } else {
                            yield(Token.Bang(Coords(filename, line, col)))
                        }
                    }

                    '<' -> {
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.LtEq(Coords(filename, line, col)))
                        } else {
                            yield(Token.Lt(Coords(filename, line, col)))
                        }
                    }

                    '>' -> {
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.GtEq(Coords(filename, line, col)))
                        } else {
                            yield(Token.Gt(Coords(filename, line, col)))
                        }
                    }

                    '&' -> {
                        if (iterator.hasNext() && iterator.match('&')) {
                            yield(Token.AmpAmp(Coords(filename, line, col)))
                        } else {
                            yield(Token.Amp(Coords(filename, line, col)))
                        }
                    }

                    '|' -> {
                        if (iterator.hasNext() && iterator.match('|')) {
                            yield(Token.PipePipe(Coords(filename, line, col)))
                        } else {
                            yield(Token.Pipe(Coords(filename, line, col)))
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
                            throw Exception("Unexpected character: $c")
                        }
                    }
                }
            }
        }
    }
}

private fun CoordinateIterator.lexString(closingChar: Char): Token {
    val builder = StringBuilder()
    while (hasNext()) {
        val c = next()
        if (c == closingChar) {
            return Token.StringLiteral(builder.toString(), Coords(filename, line, col))
        } else {
            builder.append(c)
        }
    }
    throw Exception("Unterminated string literal")
}

private fun CoordinateIterator.lexNumber(firstChar: Char): Token {
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
            return Token.FloatLiteral(builder.toString().toDouble(), Coords(filename, line, col))
        } else {
            break
        }
    }
    return Token.IntLiteral(builder.toString().toInt(), Coords(filename, line, col))
}

private fun CoordinateIterator.lexIdentifier(firstChar: Char): Token {
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
        keywords[name]!!.invoke(Coords(filename, line, col))
    } else {
        Token.Identifier(name, Coords(filename, line, col))
    }
}


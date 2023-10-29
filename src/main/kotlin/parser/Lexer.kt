package parser

import java.io.InputStream

sealed class Token {
    data object Dec : Token()
    data object End : Token()
    data object Extension : Token()
    data object For : Token()
    data object Is : Token()
    data object Let : Token()
    data object Match : Token()
    data object Module : Token()
    data object Open : Token()
    data object Protocol : Token()
    data object Struct : Token()
    data object Type : Token()

    data object LBrace : Token()
    data object RBrace : Token()
    data object LParen : Token()
    data object RParen : Token()
    data object LBracket : Token()
    data object RBracket : Token()

    data object Comma : Token()
    data object Colon : Token()
    data object Dot: Token()
    data object Question : Token()
    data object Plus : Token()
    data object Minus : Token()
    data object Times : Token()
    data object Div : Token()
    data object Mod : Token()

    data object Bang : Token()
    data object BangEq : Token()
    data object Lt : Token()
    data object LtEq : Token()
    data object Gt : Token()
    data object GtEq : Token()
    data object Eq : Token()
    data object EqEq : Token()
    data object Arrow : Token()

    data object Amp : Token()
    data object AmpAmp : Token()
    data object Pipe : Token()
    data object PipePipe : Token()

    data class IntLiteral(val value: Int) : Token()
    data class FloatLiteral(val value: Double) : Token()
    data class StringLiteral(val value: String) : Token()
    data class Identifier(val name: String) : Token()
}

private val keywords = mapOf(
    "dec" to Token.Dec,
    "end" to Token.End,
    "extension" to Token.Extension,
    "for" to Token.For,
    "is" to Token.Is,
    "let" to Token.Let,
    "match" to Token.Match,
    "module" to Token.Module,
    "open" to Token.Open,
    "protocol" to Token.Protocol,
    "struct" to Token.Struct,
    "type" to Token.Type,
)

fun InputStream.lex(): Sequence<Token> =
    this.bufferedReader()
        .lineSequence()
        .flatMap { it.asSequence() }
        .lex()

fun String.lex(): Sequence<Token> =
    this.asSequence().lex()

fun Sequence<Char>.lex(): Sequence<Token> {
    val iterator = PeekableIterator(this.iterator())
    return sequence {
        with(iterator) {
            while (iterator.hasNext()) {
                when (val c = iterator.next()) {
                    ' ', '\t', '\r', '\n' -> {
                        // Skip whitespace
                    }

                    '{' -> yield(Token.LBrace)
                    '}' -> yield(Token.RBrace)
                    '(' -> yield(Token.LParen)
                    ')' -> yield(Token.RParen)
                    '[' -> yield(Token.LBracket)
                    ']' -> yield(Token.RBracket)

                    ',' -> yield(Token.Comma)
                    ':' -> yield(Token.Colon)
                    '.' -> yield(Token.Dot)
                    '?' -> yield(Token.Question)
                    '+' -> yield(Token.Plus)
                    '-' -> yield(Token.Minus)
                    '*' -> yield(Token.Times)
                    '/' -> yield(Token.Div)
                    '%' -> yield(Token.Mod)

                    '=' -> {
                        if (iterator.hasNext() && iterator.match('>')) {
                            yield(Token.Arrow)
                        } else if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.EqEq)
                        } else {
                            yield(Token.Eq)
                        }
                    }

                    '!' -> {
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.BangEq)
                        } else {
                            yield(Token.Bang)
                        }
                    }

                    '<' -> {
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.LtEq)
                        } else {
                            yield(Token.Lt)
                        }
                    }

                    '>' -> {
                        if (iterator.hasNext() && iterator.match('=')) {
                            yield(Token.GtEq)
                        } else {
                            yield(Token.Gt)
                        }
                    }

                    '&' -> {
                        if (iterator.hasNext() && iterator.match('&')) {
                            yield(Token.AmpAmp)
                        } else {
                            yield(Token.Amp)
                        }
                    }

                    '|' -> {
                        if (iterator.hasNext() && iterator.match('|')) {
                            yield(Token.PipePipe)
                        } else {
                            yield(Token.Pipe)
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

private fun PeekableIterator<Char>.lexString(closingChar: Char): Token {
    val builder = StringBuilder()
    while (hasNext()) {
        val c = next()
        if (c == closingChar) {
            return Token.StringLiteral(builder.toString())
        } else {
            builder.append(c)
        }
    }
    throw Exception("Unterminated string literal")
}

private fun PeekableIterator<Char>.lexNumber(firstChar: Char): Token {
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
            return Token.FloatLiteral(builder.toString().toDouble())
        } else {
            break
        }
    }
    return Token.IntLiteral(builder.toString().toInt())
}

private fun PeekableIterator<Char>.lexIdentifier(firstChar: Char): Token {
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
        keywords[name]!!
    } else {
        Token.Identifier(name)
    }
}


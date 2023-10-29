package parser

import java.io.InputStream

sealed interface Token {
    val lexeme: String

    data object Dec : Token {
        override val lexeme = "dec"
    }
    data object End : Token {
        override val lexeme = "end"
    }
    data object Extension : Token {
        override val lexeme = "extension"
    }
    data object For : Token {
        override val lexeme = "for"
    }
    data object Is : Token {
        override val lexeme = "is"
    }
    data object Let : Token {
        override val lexeme = "let"
    }
    data object Match : Token {
        override val lexeme = "match"
    }
    data object Module : Token {
        override val lexeme = "module"
    }
    data object Open : Token {
        override val lexeme = "open"
    }
    data object Protocol : Token {
        override val lexeme = "protocol"
    }
    data object Struct : Token {
        override val lexeme = "struct"
    }
    data object Type : Token {
        override val lexeme = "type"
    }

    data object LBrace : Token {
        override val lexeme = "{"
    }
    data object RBrace : Token {
        override val lexeme = "}"
    }
    data object LParen : Token {
        override val lexeme = "("
    }
    data object RParen : Token {
        override val lexeme = ")"
    }
    data object LBracket : Token {
        override val lexeme = "["
    }
    data object RBracket : Token {
        override val lexeme = "]"
    }

    data object Comma : Token {
        override val lexeme = ","
    }
    data object Colon : Token {
        override val lexeme = ":"
    }
    data object Dot: Token {
        override val lexeme = "."
    }
    data object Question : Token {
        override val lexeme = "?"
    }
    data object Plus : Token {
        override val lexeme = "+"
    }
    data object Minus : Token {
        override val lexeme = "-"
    }
    data object Times : Token {
        override val lexeme = "*"
    }
    data object Div : Token {
        override val lexeme = "/"
    }
    data object Mod : Token {
        override val lexeme = "%"
    }

    data object Bang : Token {
        override val lexeme = "!"
    }
    data object BangEq : Token {
        override val lexeme = "!="
    }
    data object Lt : Token {
        override val lexeme = "<"
    }
    data object LtEq : Token {
        override val lexeme = "<="
    }
    data object Gt : Token {
        override val lexeme = ">"
    }
    data object GtEq : Token {
        override val lexeme = ">="
    }
    data object Eq : Token {
        override val lexeme = "="
    }
    data object EqEq : Token {
        override val lexeme = "=="
    }
    data object Arrow : Token {
        override val lexeme = "=>"
    }

    data object Amp : Token {
        override val lexeme = "&"
    }
    data object AmpAmp : Token {
        override val lexeme = "&&"
    }
    data object Pipe : Token {
        override val lexeme = "|"
    }
    data object PipePipe : Token {
        override val lexeme = "||"
    }

    data class IntLiteral(val value: Int) : Token {
        override val lexeme = value.toString()
    }
    data class FloatLiteral(val value: Double) : Token {
        override val lexeme = value.toString()
    }
    data class StringLiteral(val value: String) : Token {
        override val lexeme = "\"$value\""
    }
    data class Identifier(val name: String) : Token {
        override val lexeme = name
    }
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


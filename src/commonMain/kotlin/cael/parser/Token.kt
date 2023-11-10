package cael.parser

import cael.ast.Range

operator fun Token.rangeTo(other: Token) = this.range..other.range
sealed interface Token {
    val range: Range
    val lexeme: String

    data class Dec(
        override val range: Range
    ) : Token {
        override val lexeme = "dec"
    }

    data class End(
        override val range: Range
    ) : Token {
        override val lexeme = "end"
    }

    data class Extension(
        override val range: Range
    ) : Token {
        override val lexeme = "extension"
    }

    data class For(
        override val range: Range
    ) : Token {
        override val lexeme = "for"
    }

    data class Is(
        override val range: Range
    ) : Token {
        override val lexeme = "is"
    }

    data class Let(
        override val range: Range
    ) : Token {
        override val lexeme = "let"
    }

    data class Match(
        override val range: Range
    ) : Token {
        override val lexeme = "match"
    }

    data class Module(
        override val range: Range
    ) : Token {
        override val lexeme = "module"
    }

    data class Open(
        override val range: Range
    ) : Token {
        override val lexeme = "open"
    }

    data class Protocol(
        override val range: Range
    ) : Token {
        override val lexeme = "protocol"
    }

    data class Struct(
        override val range: Range
    ) : Token {
        override val lexeme = "struct"
    }

    data class Type(
        override val range: Range
    ) : Token {
        override val lexeme = "type"
    }

    data class LBrace(
        override val range: Range
    ) : Token {
        override val lexeme = "{"
    }

    data class RBrace(
        override val range: Range
    ) : Token {
        override val lexeme = "}"
    }

    data class LParen(
        override val range: Range
    ) : Token {
        override val lexeme = "("
    }

    data class RParen(
        override val range: Range
    ) : Token {
        override val lexeme = ")"
    }

    data class LBracket(
        override val range: Range
    ) : Token {
        override val lexeme = "["
    }

    data class RBracket(
        override val range: Range
    ) : Token {
        override val lexeme = "]"
    }

    data class Comma(
        override val range: Range
    ) : Token {
        override val lexeme = ","
    }

    data class Colon(
        override val range: Range
    ) : Token {
        override val lexeme = ":"
    }

    data class Dot(
        override val range: Range
    ) : Token {
        override val lexeme = "."
    }

    data class Question(
        override val range: Range
    ) : Token {
        override val lexeme = "?"
    }

    data class Plus(
        override val range: Range
    ) : Token {
        override val lexeme = "+"
    }

    data class Minus(
        override val range: Range
    ) : Token {
        override val lexeme = "-"
    }

    data class Star(
        override val range: Range
    ) : Token {
        override val lexeme = "*"
    }

    data class Slash(
        override val range: Range
    ) : Token {
        override val lexeme = "/"
    }

    data class Percent(
        override val range: Range
    ) : Token {
        override val lexeme = "%"
    }

    data class Bang(
        override val range: Range
    ) : Token {
        override val lexeme = "!"
    }

    data class BangEq(
        override val range: Range
    ) : Token {
        override val lexeme = "!="
    }

    data class Lt(
        override val range: Range
    ) : Token {
        override val lexeme = "<"
    }

    data class LtEq(
        override val range: Range
    ) : Token {
        override val lexeme = "<="
    }

    data class Gt(
        override val range: Range
    ) : Token {
        override val lexeme = ">"
    }

    data class GtEq(
        override val range: Range
    ) : Token {
        override val lexeme = ">="
    }

    data class Eq(
        override val range: Range
    ) : Token {
        override val lexeme = "="
    }

    data class EqEq(
        override val range: Range
    ) : Token {
        override val lexeme = "=="
    }

    data class Arrow(
        override val range: Range
    ) : Token {
        override val lexeme = "=>"
    }

    data class Amp(
        override val range: Range
    ) : Token {
        override val lexeme = "&"
    }

    data class AmpAmp(
        override val range: Range
    ) : Token {
        override val lexeme = "&&"
    }

    data class Pipe(
        override val range: Range
    ) : Token {
        override val lexeme = "|"
    }

    data class PipePipe(
        override val range: Range
    ) : Token {
        override val lexeme = "||"
    }

    data class IntLiteral(
        val value: Int,
        override val range: Range,
    ) : Token {
        override val lexeme = value.toString()
    }

    data class FloatLiteral(
        val value: Double,
        override val range: Range,
    ) : Token {
        override val lexeme = value.toString()
    }

    data class StringLiteral(
        val value: String,
        override val range: Range,
    ) : Token {
        override val lexeme = "\"$value\""
    }

    data class InterpolatedStringStart(
        val value: String,
        override val range: Range,
    ) : Token {
        override val lexeme = "\"$value\\("
    }

    data class InterpolatedStringMiddle(
        val value: String,
        override val range: Range,
    ) : Token {
        override val lexeme = ")$value\\("
    }

    data class InterpolatedStringEnd(
        val value: String,
        override val range: Range,
    ) : Token {
        override val lexeme = ")$value\""
    }

    data class Identifier(
        val name: String,
        override val range: Range,
    ) : Token {
        override val lexeme = name
    }
}
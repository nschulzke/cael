package cael.parser

import cael.ast.Coords
import cael.ast.Range
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual

fun singleTokenRange(length: Int, start: Int = 1, line: Int = 1) = Range(
    Coords("file", line, start),
    Coords("file", line, start + length - 1)
)

fun range(startLine: Int, startCol: Int, endLine: Int, endCol: Int) = Range(
    Coords("file", startLine, startCol),
    Coords("file", endLine, endCol),
)

val shouldLexToToken: suspend ContainerScope.(Pair<String, Token>) -> Unit = { (string, token) ->
    string.lex().single() shouldBeEqual token
}

class LexerTests : DescribeSpec({
    describe("keywords") {
        withData(
            nameFn = { "keyword: ${it.first}" },
            "let" to Token.Let(singleTokenRange(3)),
            "match" to Token.Match(singleTokenRange(5)),
            "struct" to Token.Struct(singleTokenRange(6)),
            test = shouldLexToToken,
        )
    }

    describe("punctuation") {
        withData(
            nameFn = { "punctuation: ${it.first}" },
            "{" to Token.LBrace(singleTokenRange(1)),
            "}" to Token.RBrace(singleTokenRange(1)),
            "(" to Token.LParen(singleTokenRange(1)),
            ")" to Token.RParen(singleTokenRange(1)),
            "[" to Token.LBracket(singleTokenRange(1)),
            "]" to Token.RBracket(singleTokenRange(1)),
            "," to Token.Comma(singleTokenRange(1)),
            ":" to Token.Colon(singleTokenRange(1)),
            "." to Token.Dot(singleTokenRange(1)),
            "?" to Token.Question(singleTokenRange(1)),
            "+" to Token.Plus(singleTokenRange(1)),
            "-" to Token.Minus(singleTokenRange(1)),
            "*" to Token.Star(singleTokenRange(1)),
            "/" to Token.Slash(singleTokenRange(1)),
            "%" to Token.Percent(singleTokenRange(1)),
            "=" to Token.Eq(singleTokenRange(1)),
            "=>" to Token.Arrow(singleTokenRange(2)),
            "==" to Token.EqEq(singleTokenRange(2)),
            "!" to Token.Bang(singleTokenRange(1)),
            "!=" to Token.BangEq(singleTokenRange(2)),
            "<" to Token.Lt(singleTokenRange(1)),
            "<=" to Token.LtEq(singleTokenRange(2)),
            ">" to Token.Gt(singleTokenRange(1)),
            ">=" to Token.GtEq(singleTokenRange(2)),
            "&" to Token.Amp(singleTokenRange(1)),
            "&&" to Token.AmpAmp(singleTokenRange(2)),
            "|" to Token.Pipe(singleTokenRange(1)),
            "||" to Token.PipePipe(singleTokenRange(2)),
            test = shouldLexToToken,
        )
    }

    describe("numbers") {
        withData(
            nameFn = { "number: ${it.first}" },
            "0" to Token.IntLiteral(0, singleTokenRange(1)),
            "1" to Token.IntLiteral(1, singleTokenRange(1)),
            "0.0" to Token.FloatLiteral(0.0, singleTokenRange(3)),
            "0.5" to Token.FloatLiteral(0.5, singleTokenRange(3)),
            test = shouldLexToToken,
        )
    }

    describe("strings") {
        withData(
            nameFn = { "string: ${it.first}" },
            "\"\"" to Token.StringLiteral("", singleTokenRange(2)),
            "\"a\"" to Token.StringLiteral("a", singleTokenRange(3)),
            "\"abc\"" to Token.StringLiteral("abc", singleTokenRange(5)),
            "''" to Token.StringLiteral("", singleTokenRange(2)),
            "'a'" to Token.StringLiteral("a", singleTokenRange(3)),
            "'abc'" to Token.StringLiteral("abc", singleTokenRange(5)),
            test = shouldLexToToken,
        )

        withData(
            nameFn = { "escape sequence: ${it.first}"},
            """ "\n" """ to Token.StringLiteral("\n", singleTokenRange(4, 2)),
            """ "\t" """ to Token.StringLiteral("\t", singleTokenRange(4, 2)),
            """ "\r" """ to Token.StringLiteral("\r", singleTokenRange(4, 2)),
            """ "\"" """ to Token.StringLiteral("\"", singleTokenRange(4, 2)),
            """ "\\" """ to Token.StringLiteral("\\", singleTokenRange(4, 2)),
            """ '\'' """ to Token.StringLiteral("'", singleTokenRange(4, 2)),
            """ "\u2764\uFE0F" """ to Token.StringLiteral("\u2764\uFE0F", singleTokenRange(14, 2)),
            test = shouldLexToToken,
        )

        it("tokenizes a plain interpolated string") {
            val lexed = """ "\(name)" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(singleTokenRange(1, 2)),
                Token.BeginInterpolating(singleTokenRange(2, 3)),
                Token.Identifier("name", singleTokenRange(4, 5)),
                Token.EndInterpolating(singleTokenRange(1, 9)),
                Token.EndString(singleTokenRange(1, 10)),
            )
        }

        it("tokenizes an interpolated string with prefix") {
            val lexed = """ "Hello, \(name)" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(singleTokenRange(1, 2)),
                Token.StringLiteralSegment("Hello, ", singleTokenRange(7, 3)),
                Token.BeginInterpolating(singleTokenRange(2, 10)),
                Token.Identifier("name", singleTokenRange(4, 12)),
                Token.EndInterpolating(singleTokenRange(1, 16)),
                Token.EndString(singleTokenRange(1, 17)),
            )
        }

        it("tokenizes an interpolated string with suffix") {
            val lexed = """ "\(name), hello" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(singleTokenRange(1, 2)),
                Token.BeginInterpolating(singleTokenRange(2, 3)),
                Token.Identifier("name", singleTokenRange(4, 5)),
                Token.EndInterpolating(singleTokenRange(1, 9)),
                Token.StringLiteralSegment(", hello", singleTokenRange(7, 10)),
                Token.EndString(singleTokenRange(1, 17)),
            )
        }

        it("tokenizes an interpolated string with a nested set of parenthesis") {
            val lexed = """ "\((name))" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(singleTokenRange(1, 2)),
                Token.BeginInterpolating(singleTokenRange(2, 3)),
                Token.LParen(singleTokenRange(1, 5)),
                Token.Identifier("name", singleTokenRange(4, 6)),
                Token.RParen(singleTokenRange(1, 10)),
                Token.EndInterpolating(singleTokenRange(1, 11)),
                Token.EndString(singleTokenRange(1, 12)),
            )
        }

        it("tokenizes an interpolated string with two nested sets of parenthesis") {
            val lexed = """ "\((name + (1)))" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(singleTokenRange(1, 2)),
                Token.BeginInterpolating(singleTokenRange(2, 3)),
                Token.LParen(singleTokenRange(1, 5)),
                Token.Identifier("name", singleTokenRange(4, 6)),
                Token.Plus(singleTokenRange(1, 11)),
                Token.LParen(singleTokenRange(1, 13)),
                Token.IntLiteral(1, singleTokenRange(1, 14)),
                Token.RParen(singleTokenRange(1, 15)),
                Token.RParen(singleTokenRange(1, 16)),
                Token.EndInterpolating(singleTokenRange(1, 17)),
                Token.EndString(singleTokenRange(1, 18)),
            )
        }

        it("tokenizes an interpolated string with both a prefix and a suffix") {
            val lexed = """ "Hello, \(name), hello" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(singleTokenRange(1, 2)),
                Token.StringLiteralSegment("Hello, ", singleTokenRange(7, 3)),
                Token.BeginInterpolating(singleTokenRange(2, 10)),
                Token.Identifier("name", singleTokenRange(4, 12)),
                Token.EndInterpolating(singleTokenRange(1, 16)),
                Token.StringLiteralSegment(", hello", singleTokenRange(7, 17)),
                Token.EndString(singleTokenRange(1, 24)),
            )
        }

        it("tokenizes an interpolated string with two interpolations") {
            val lexed = """ "\(name), \(name)" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(singleTokenRange(1, 2)),
                Token.BeginInterpolating(singleTokenRange(2, 3)),
                Token.Identifier("name", singleTokenRange(4, 5)),
                Token.EndInterpolating(singleTokenRange(1, 9)),
                Token.StringLiteralSegment(", ", singleTokenRange(2, 10)),
                Token.BeginInterpolating(singleTokenRange(2, 12)),
                Token.Identifier("name", singleTokenRange(4, 14)),
                Token.EndInterpolating(singleTokenRange(1, 18)),
                Token.EndString(singleTokenRange(1, 19)),
            )
        }

        it("tokenizes an interpolated string with newlines") {
            val lexed = """
                "\(name),
                \(name)"
            """.trimIndent().also { println(it)}.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(singleTokenRange(1, 1)),
                Token.BeginInterpolating(singleTokenRange(2, 2)),
                Token.Identifier("name", singleTokenRange(4, 4)),
                Token.EndInterpolating(singleTokenRange(1, 8)),
                Token.StringLiteralSegment(",\n", range(1, 9, 2, 0)),
                Token.BeginInterpolating(singleTokenRange(2, 1, 2)),
                Token.Identifier("name", singleTokenRange(4, 3, 2)),
                Token.EndInterpolating(singleTokenRange(1, 7, 2)),
                Token.EndString(singleTokenRange(1, 8, 2)),
            )
        }
    }

    describe("identifiers") {
        withData(
            nameFn = { "identifier: ${it.first}" },
            "a" to Token.Identifier("a", singleTokenRange(1)),
            "abc" to Token.Identifier("abc", singleTokenRange(3)),
            "a1" to Token.Identifier("a1", singleTokenRange(2)),
            "a_b" to Token.Identifier("a_b", singleTokenRange(3)),
            "a_b_c" to Token.Identifier("a_b_c", singleTokenRange(5)),
            "_a" to Token.Identifier("_a", singleTokenRange(2)),
            "_" to Token.Identifier("_", singleTokenRange(1)),
            test = shouldLexToToken,
        )
    }

    describe("comments") {
        it("ignores comments") {
            val lexed = """
                # this is a comment
                "a" # this is a another comment
                # this is a third comment
            """.trimIndent().lex().toList()
            lexed shouldContainExactly listOf(
                Token.StringLiteral("a", Range(
                    Coords("file", 2, 1),
                    Coords("file", 2, 3),
                )),
            )
        }
    }

    describe("coordinate handling") {
        it("tracks newlines correctly") {
            val lexed = """
                "a"
                "b"
            """.trimIndent().lex().toList()
            lexed shouldContainExactly listOf(
                Token.StringLiteral("a", Range(
                    Coords("file", 1, 1),
                    Coords("file", 1, 3),
                )),
                Token.StringLiteral("b", Range(
                    Coords("file", 2, 1),
                    Coords("file", 2, 3),
                )),
            )
        }
    }
});
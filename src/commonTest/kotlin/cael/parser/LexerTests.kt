package cael.parser

import cael.ast.Range
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual

val shouldLexToToken: suspend ContainerScope.(Pair<String, Token>) -> Unit = { (string, token) ->
    string.lex().single() shouldBeEqual token
}

class LexerTests : DescribeSpec({
    describe("keywords") {
        withData(
            nameFn = { "keyword: ${it.first}" },
            "let" to Token.Let(Range(1, 3)),
            "fun" to Token.Fun(Range(1, 3)),
            "match" to Token.Match(Range(1, 5)),
            "struct" to Token.Struct(Range(1, 6)),
            test = shouldLexToToken,
        )
    }

    describe("punctuation") {
        withData(
            nameFn = { "punctuation: ${it.first}" },
            "{" to Token.LBrace(Range(1, 1)),
            "}" to Token.RBrace(Range(1, 1)),
            "(" to Token.LParen(Range(1, 1)),
            ")" to Token.RParen(Range(1, 1)),
            "[" to Token.LBracket(Range(1, 1)),
            "]" to Token.RBracket(Range(1, 1)),
            "," to Token.Comma(Range(1, 1)),
            ":" to Token.Colon(Range(1, 1)),
            "." to Token.Dot(Range(1, 1)),
            "?" to Token.Question(Range(1, 1)),
            "+" to Token.Plus(Range(1, 1)),
            "-" to Token.Minus(Range(1, 1)),
            "*" to Token.Star(Range(1, 1)),
            "/" to Token.Slash(Range(1, 1)),
            "%" to Token.Percent(Range(1, 1)),
            "=" to Token.Eq(Range(1, 1)),
            "=>" to Token.Arrow(Range(1, 2)),
            "==" to Token.EqEq(Range(1, 2)),
            "!" to Token.Bang(Range(1, 1)),
            "!=" to Token.BangEq(Range(1, 2)),
            "<" to Token.Lt(Range(1, 1)),
            "<=" to Token.LtEq(Range(1, 2)),
            ">" to Token.Gt(Range(1, 1)),
            ">=" to Token.GtEq(Range(1, 2)),
            "&" to Token.Amp(Range(1, 1)),
            "&&" to Token.AmpAmp(Range(1, 2)),
            "|" to Token.Pipe(Range(1, 1)),
            "||" to Token.PipePipe(Range(1, 2)),
            test = shouldLexToToken,
        )
    }

    describe("numbers") {
        withData(
            nameFn = { "number: ${it.first}" },
            "0" to Token.IntLiteral(0, Range(1, 1)),
            "1" to Token.IntLiteral(1, Range(1, 1)),
            "0.0" to Token.FloatLiteral(0.0, Range(1, 3)),
            "0.5" to Token.FloatLiteral(0.5, Range(1, 3)),
            test = shouldLexToToken,
        )
    }

    describe("strings") {
        withData(
            nameFn = { "string: ${it.first}" },
            "\"\"" to Token.StringLiteral("", Range(1, 2)),
            "\"a\"" to Token.StringLiteral("a", Range(1, 3)),
            "\"abc\"" to Token.StringLiteral("abc", Range(1, 5)),
            "''" to Token.StringLiteral("", Range(1, 2)),
            "'a'" to Token.StringLiteral("a", Range(1, 3)),
            "'abc'" to Token.StringLiteral("abc", Range(1, 5)),
            test = shouldLexToToken,
        )

        withData(
            nameFn = { "escape sequence: ${it.first}"},
            """ "\n" """ to Token.StringLiteral("\n", Range(2, 4)),
            """ "\t" """ to Token.StringLiteral("\t", Range(2, 4)),
            """ "\r" """ to Token.StringLiteral("\r", Range(2, 4)),
            """ "\"" """ to Token.StringLiteral("\"", Range(2, 4)),
            """ "\\" """ to Token.StringLiteral("\\", Range(2, 4)),
            """ '\'' """ to Token.StringLiteral("'", Range(2, 4)),
            """ "\u2764\uFE0F" """ to Token.StringLiteral("\u2764\uFE0F", Range(2, 14)),
            test = shouldLexToToken,
        )

        it("tokenizes a plain interpolated string") {
            val lexed = """ "\(name)" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(Range(2, 1)),
                Token.BeginInterpolating(Range(3, 2)),
                Token.Identifier("name", Range(5, 4)),
                Token.EndInterpolating(Range(9, 1)),
                Token.EndString(Range(10, 1)),
            )
        }

        it("tokenizes an interpolated string with prefix") {
            val lexed = """ "Hello, \(name)" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(Range(2, 1)),
                Token.StringLiteralSegment("Hello, ", Range(3, 7)),
                Token.BeginInterpolating(Range(10, 2)),
                Token.Identifier("name", Range(12, 4)),
                Token.EndInterpolating(Range(16, 1)),
                Token.EndString(Range(17, 1)),
            )
        }

        it("tokenizes an interpolated string with suffix") {
            val lexed = """ "\(name), hello" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(Range(2, 1)),
                Token.BeginInterpolating(Range(3, 2)),
                Token.Identifier("name", Range(5, 4)),
                Token.EndInterpolating(Range(9, 1)),
                Token.StringLiteralSegment(", hello", Range(10, 7)),
                Token.EndString(Range(17, 1)),
            )
        }

        it("tokenizes an interpolated string with a nested set of parenthesis") {
            val lexed = """ "\((name))" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(Range(2, 1)),
                Token.BeginInterpolating(Range(3, 2)),
                Token.LParen(Range(5, 1)),
                Token.Identifier("name", Range(6, 4)),
                Token.RParen(Range(10, 1)),
                Token.EndInterpolating(Range(11, 1)),
                Token.EndString(Range(12, 1)),
            )
        }

        it("tokenizes an interpolated string with two nested sets of parenthesis") {
            val lexed = """ "\((name + (1)))" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(Range(2, 1)),
                Token.BeginInterpolating(Range(3, 2)),
                Token.LParen(Range(5, 1)),
                Token.Identifier("name", Range(6, 4)),
                Token.Plus(Range(11, 1)),
                Token.LParen(Range(13, 1)),
                Token.IntLiteral(1, Range(14, 1)),
                Token.RParen(Range(15, 1)),
                Token.RParen(Range(16, 1)),
                Token.EndInterpolating(Range(17, 1)),
                Token.EndString(Range(18, 1)),
            )
        }

        it("tokenizes an interpolated string with both a prefix and a suffix") {
            val lexed = """ "Hello, \(name), hello" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(Range(2, 1)),
                Token.StringLiteralSegment("Hello, ", Range(3, 7)),
                Token.BeginInterpolating(Range(10, 2)),
                Token.Identifier("name", Range(12, 4)),
                Token.EndInterpolating(Range(16, 1)),
                Token.StringLiteralSegment(", hello", Range(17, 7)),
                Token.EndString(Range(24, 1)),
            )
        }

        it("tokenizes an interpolated string with two interpolations") {
            val lexed = """ "\(name), \(name)" """.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(Range(2, 1)),
                Token.BeginInterpolating(Range(3, 2)),
                Token.Identifier("name", Range(5, 4)),
                Token.EndInterpolating(Range(9, 1)),
                Token.StringLiteralSegment(", ", Range(10, 2)),
                Token.BeginInterpolating(Range(12, 2)),
                Token.Identifier("name", Range(14, 4)),
                Token.EndInterpolating(Range(18, 1)),
                Token.EndString(Range(19, 1)),
            )
        }

        it("tokenizes an interpolated string with newlines") {
            val lexed = """
                "\(name),
                \(name)"
            """.trimIndent().also { println(it)}.lex().toList()
            lexed shouldContainExactly listOf(
                Token.BeginString(Range(1, 1)),
                Token.BeginInterpolating(Range(2, 2)),
                Token.Identifier("name", Range(4, 4)),
                Token.EndInterpolating(Range(8, 1)),
                Token.StringLiteralSegment(",\n", Range(9, 2)),
                Token.BeginInterpolating(Range(11, 2)),
                Token.Identifier("name", Range(13, 4)),
                Token.EndInterpolating(Range(17, 1)),
                Token.EndString(Range(18, 1)),
            )
        }
    }

    describe("identifiers") {
        withData(
            nameFn = { "identifier: ${it.first}" },
            "a" to Token.Identifier("a", Range(1, 1)),
            "abc" to Token.Identifier("abc", Range(1, 3)),
            "a1" to Token.Identifier("a1", Range(1, 2)),
            "a_b" to Token.Identifier("a_b", Range(1, 3)),
            "a_b_c" to Token.Identifier("a_b_c", Range(1, 5)),
            "_a" to Token.Identifier("_a", Range(1, 2)),
            "_" to Token.Identifier("_", Range(1, 1)),
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
                Token.StringLiteral("a", Range(21, 3)),
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
                Token.StringLiteral("a", Range(1, 3)),
                Token.StringLiteral("b", Range(5, 3)),
            )
        }
    }
});

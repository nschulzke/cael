package cael.parser

import cael.ast.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ParserTests : DescribeSpec({
    describe("let declaration") {
        it("should parse a simple let declaration") {
            "let x = 1".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Let(
                        name = "x",
                        value = Expr.Literal.Int(
                            value = 1,
                            range = Range(9, 1),
                        ),
                        range = Range(1, 9),
                    )
                ),
                range = Range(1, 9),
            )
        }
    }
})

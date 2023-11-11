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

    describe("fun declaration") {
        it("should parse a simple fun declaration") {
            "fun f() => 1".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Tuple(
                        name = "f",
                        parameters = emptyList(),
                        value = Expr.Literal.Int(
                            value = 1,
                            range = Range(12, 1),
                        ),
                        range = Range(1, 12),
                    )
                ),
                range = Range(1, 12),
            )
        }

        it("should parse a tuple fun declaration") {
            "fun f(x, y) => 1".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Tuple(
                        name = "f",
                        parameters = listOf(
                            Pattern.Identifier(
                                name = "x",
                                range = Range(7, 1),
                            ),
                            Pattern.Identifier(
                                name = "y",
                                range = Range(10, 1),
                            ),
                        ),
                        value = Expr.Literal.Int(
                            value = 1,
                            range = Range(16, 1),
                        ),
                        range = Range(1, 16),
                    )
                ),
                range = Range(1, 16),
            )
        }

        it("should parse a record fun declaration") {
            "fun f { x = Int } => 1".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Record(
                        name = "f",
                        parameters = listOf(
                            PatternRecordItem(
                                name = "x",
                                pattern = Pattern.Identifier(
                                    name = "Int",
                                    range = Range(13, 3),
                                ),
                                range = Range(9, 7),
                            )
                        ),
                        value = Expr.Literal.Int(
                            value = 1,
                            range = Range(22, 1),
                        ),
                        range = Range(1, 22),
                    )
                ),
                range = Range(1, 22),
            )
        }
    }
})

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
                        pattern = Pattern.Identifier("x", Range(5, 1)),
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

        it("should support destructuring in let declaration") {
            "let Foo(x, y) = foo".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Let(
                        pattern = Pattern.Struct.Tuple(
                            name = "Foo",
                            components = listOf(
                                Pattern.Identifier(
                                    name = "x",
                                    range = Range(9, 1),
                                ),
                                Pattern.Identifier(
                                    name = "y",
                                    range = Range(12, 1),
                                ),
                            ),
                            range = Range(5, 9),
                        ),
                        value = Expr.Identifier(
                            name = "foo",
                            range = Range(17, 3),
                        ),
                        range = Range(1, 19),
                    )
                ),
                range = Range(1, 19),
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

        it("should parse a tuple fun declaration with destructuring") {
            "fun f(Foo(x, y)) => 1".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Tuple(
                        name = "f",
                        parameters = listOf(
                            Pattern.Struct.Tuple(
                                name = "Foo",
                                components = listOf(
                                    Pattern.Identifier(
                                        name = "x",
                                        range = Range(11, 1),
                                    ),
                                    Pattern.Identifier(
                                        name = "y",
                                        range = Range(14, 1),
                                    ),
                                ),
                                range = Range(7, 9),
                            ),
                        ),
                        value = Expr.Literal.Int(
                            value = 1,
                            range = Range(21, 1),
                        ),
                        range = Range(1, 21),
                    )
                ),
                range = Range(1, 21),
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

        it("should parse record fun declaration with destructuring") {
            "fun f { x = Foo(x, y) } => 1".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Record(
                        name = "f",
                        parameters = listOf(
                            PatternRecordItem(
                                name = "x",
                                pattern = Pattern.Struct.Tuple(
                                    name = "Foo",
                                    components = listOf(
                                        Pattern.Identifier(
                                            name = "x",
                                            range = Range(17, 1),
                                        ),
                                        Pattern.Identifier(
                                            name = "y",
                                            range = Range(20, 1),
                                        ),
                                    ),
                                    range = Range(13, 9),
                                ),
                                range = Range(9, 13),
                            )
                        ),
                        value = Expr.Literal.Int(
                            value = 1,
                            range = Range(28, 1),
                        ),
                        range = Range(1, 28),
                    )
                ),
                range = Range(1, 28),
            )
        }

        it("parses a fun decl with two arms") {
            "fun f | (Bar) => 1 | { baz = Baz } => 2".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Match(
                        name = "f",
                        cases = listOf(
                            Decl.Fun.Match.Case.Tuple(
                                parameters = listOf(
                                    Pattern.Identifier(
                                        name = "Bar",
                                        range = Range(10, 3),
                                    )
                                ),
                                value = Expr.Literal.Int(
                                    value = 1,
                                    range = Range(18, 1),
                                ),
                                range = Range(9, 10),
                            ),
                            Decl.Fun.Match.Case.Record(
                                parameters = listOf(
                                    PatternRecordItem(
                                        name = "baz",
                                        pattern = Pattern.Identifier(
                                            name = "Baz",
                                            range = Range(30, 3),
                                        ),
                                        range = Range(24, 9),
                                    )
                                ),
                                value = Expr.Literal.Int(
                                    value = 2,
                                    range = Range(39, 1),
                                ),
                                range = Range(22, 18),
                            ),
                        ),
                        range = Range(1, 39),
                    )
                ),
                range = Range(1, 39),
            )
        }
    }

    describe("pattern operators") {
        it("should parse or pattern") {
            "fun t(Foo | Bar) => 'foobar'".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Tuple(
                        name = "t",
                        parameters = listOf(
                            Pattern.Binary(
                                left = Pattern.Identifier(
                                    name = "Foo",
                                    range = Range(7, 3),
                                ),
                                op = "|",
                                right = Pattern.Identifier(
                                    name = "Bar",
                                    range = Range(13, 3),
                                ),
                                range = Range(7, 9),
                            )
                        ),
                        value = Expr.Literal.String(
                            value = "foobar",
                            range = Range(21, 8),
                        ),
                        range = Range(1, 28),
                    )
                ),
                range = Range(1, 28),
            )
        }

        it("parses and with lower precedence than or") {
            "fun t(Foo | Bar & Baz) => 'foobar'".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Tuple(
                        name = "t",
                        parameters = listOf(
                            Pattern.Binary(
                                left = Pattern.Identifier(
                                    name = "Foo",
                                    range = Range(7, 3),
                                ),
                                op = "|",
                                right = Pattern.Binary(
                                    left = Pattern.Identifier(
                                        name = "Bar",
                                        range = Range(13, 3),
                                    ),
                                    op = "&",
                                    right = Pattern.Identifier(
                                        name = "Baz",
                                        range = Range(19, 3),
                                    ),
                                    range = Range(13, 9),
                                ),
                                range = Range(7, 15),
                            )
                        ),
                        value = Expr.Literal.String(
                            value = "foobar",
                            range = Range(27, 8),
                        ),
                        range = Range(1, 34),
                    )
                ),
                range = Range(1, 34),
            )
        }

        it("parses : as the matches operator") {
            "fun t(foo : Bar) => 'foobar'".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Tuple(
                        name = "t",
                        parameters = listOf(
                            Pattern.Binary(
                                left = Pattern.Identifier(
                                    name = "foo",
                                    range = Range(7, 3),
                                ),
                                op = ":",
                                right = Pattern.Identifier(
                                    name = "Bar",
                                    range = Range(13, 3),
                                ),
                                range = Range(7, 9),
                            )
                        ),
                        value = Expr.Literal.String(
                            value = "foobar",
                            range = Range(21, 8),
                        ),
                        range = Range(1, 28),
                    )
                ),
                range = Range(1, 28),
            )
        }

        it("parses the not operator") {
            "fun t(!Foo) => 'foobar'".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Fun.Tuple(
                        name = "t",
                        parameters = listOf(
                            Pattern.Unary(
                                op = "!",
                                operand = Pattern.Identifier(
                                    name = "Foo",
                                    range = Range(8, 3),
                                ),
                                range = Range(7, 4),
                            )
                        ),
                        value = Expr.Literal.String(
                            value = "foobar",
                            range = Range(16, 8),
                        ),
                        range = Range(1, 23),
                    )
                ),
                range = Range(1, 23),
            )
        }
    }

    describe("match expression") {
        it("parses a match expression with two arms") {
            "let x = match foo | Bar => 1 | Baz => 2".lex().parse() shouldBe Program(
                declarations = listOf(
                    Decl.Let(
                        pattern = Pattern.Identifier(
                            name = "x",
                            range = Range(5, 1),
                        ),
                        value = Expr.Match(
                            expr = Expr.Identifier(
                                name = "foo",
                                range = Range(15, 3),
                            ),
                            cases = listOf(
                                Expr.Match.Case(
                                    pattern = Pattern.Identifier(
                                        name = "Bar",
                                        range = Range(21, 3),
                                    ),
                                    value = Expr.Literal.Int(
                                        value = 1,
                                        range = Range(28, 1),
                                    ),
                                    range = Range(21, 8),
                                ),
                                Expr.Match.Case(
                                    pattern = Pattern.Identifier(
                                        name = "Baz",
                                        range = Range(32, 3),
                                    ),
                                    value = Expr.Literal.Int(
                                        value = 2,
                                        range = Range(39, 1),
                                    ),
                                    range = Range(32, 8),
                                ),
                            ),
                            range = Range(9, 31),
                        ),
                        range = Range(1, 39),
                    )
                ),
                range = Range(1, 39),
            )
        }
    }
})

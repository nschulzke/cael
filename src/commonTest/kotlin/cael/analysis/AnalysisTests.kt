package cael.analysis

import cael.ast.Range
import cael.parser.lex
import cael.parser.parse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeTypeOf

class AnalysisTests : DescribeSpec({
    describe("let") {
        it("analyzes a simple int let") {
            val program = """
                let x = 5
            """.trimIndent().lex().parse().analyze()
            program.declarations.size shouldBeEqual 1
            program.declarations[0].apply {
                this.shouldBeTypeOf<Decl.Let>()
                range shouldBeEqual Range(1, 9)
                pattern.typeMatched shouldBeEqual Type.Primitive.Int
                pattern.boundNames().apply {
                    size shouldBeEqual 1
                    this[0].apply {
                        name shouldBeEqual "x"
                        type shouldBeEqual Type.Primitive.Int
                    }
                }
                value shouldBeEqual Expr.Literal.Int(5, Range(9, 1))
            }
        }

        it("analyzes a simple string let") {
            val program = """
                let x = "hello world"
            """.trimIndent().lex().parse().analyze()
            program.declarations.size shouldBeEqual 1
            program.declarations[0].apply {
                this.shouldBeTypeOf<Decl.Let>()
                range shouldBeEqual Range(1, 21)
                pattern.typeMatched shouldBeEqual Type.Primitive.String
                pattern.boundNames().apply {
                    size shouldBeEqual 1
                    this[0].apply {
                        name shouldBeEqual "x"
                        type shouldBeEqual Type.Primitive.String
                    }
                }
                value shouldBeEqual Expr.Literal.String("hello world", Range(9, 13))
            }
        }

        it("analyzes a simple struct let") {
            val program = """
                struct Foo
                let x = Foo
            """.trimIndent().lex().parse().analyze()
            program.declarations.size shouldBeEqual 2
            val expectedType = Type.StructConstructor.Bare("Foo")

            program.declarations[0].apply {
                this.shouldBeTypeOf<Decl.Struct.Bare>()
                range shouldBeEqual Range(1, 10)
                constructorType shouldBeEqual expectedType
            }
            program.declarations[1].apply {
                this.shouldBeTypeOf<Decl.Let>()
                range shouldBeEqual Range(12, 11)
                pattern.typeMatched shouldBeEqual expectedType
                pattern.boundNames().apply {
                    size shouldBeEqual 1
                    this[0].apply {
                        name shouldBeEqual "x"
                        type shouldBeEqual expectedType
                    }
                }
                value shouldBeEqual Expr.Identifier("Foo", expectedType, Range(20, 3))
            }
        }
    }
})

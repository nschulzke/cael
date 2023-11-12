package cael.ast

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual

class RangeTests : DescribeSpec({
    describe("printRange") {
        it("correctly converts a single-line reference") {
            val fileContents = FileContents("foo.cl", "hello world")
            val range = Range(0, 5)
            fileContents.printRange(range) shouldBeEqual """
                |  --> foo.cl:1:1
                |   |
                | 1 | hello world
                |   | ^^^^^
            """.trimMargin()
        }

        it("correctly converts a second line reference") {
            val fileContents = FileContents("foo.cl", """
                hello
                world
            """.trimIndent())
            val range = Range(6, 5)
            fileContents.printRange(range) shouldBeEqual """
                |  --> foo.cl:2:1
                |   |
                | 2 | world
                |   | ^^^^^
            """.trimMargin()
        }

        it("correctly marks single-character ranges") {
            val fileContents = FileContents("foo.cl", "hello world")
            val range = Range(3, 1)
            fileContents.printRange(range) shouldBeEqual """
                |  --> foo.cl:1:4
                |   |
                | 1 | hello world
                |   |    ^
            """.trimMargin()
        }

        it("handles correctly for large line numbers") {
            val fileContents = FileContents("foo.cl", "\n".repeat(100) + "hello world")
            val range = Range(100, 5)
            fileContents.printRange(range) shouldBeEqual """
                |    --> foo.cl:101:1
                |     |
                | 101 | hello world
                |     | ^^^^^
            """.trimMargin()
        }

        it("renders multiline errors correctly") {
            val fileContents = FileContents("foo.cl", """
                hello
                world
            """.trimIndent())
            val range = Range(0, 11)
            fileContents.printRange(range) shouldBeEqual """
                |  --> foo.cl:1:1
                |   |
                | 1 > hello
                | 2 > world
                |   |
            """.trimMargin()
        }

        it("accounts for large line numbers in multiline errors") {
            val fileContents = FileContents("foo.cl", "\n".repeat(98) + """
                hello
                world
            """.trimIndent())
            val range = Range(98, 11)
            fileContents.printRange(range) shouldBeEqual """
                |    --> foo.cl:99:1
                |     |
                | 99  > hello
                | 100 > world
                |     |
            """.trimMargin()
        }
    }
})
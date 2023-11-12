package cael.ast

import kotlinx.serialization.Serializable

@Serializable
data class FileContents(
    private val fileName: String,
    private val lines: List<String>,
) {
    constructor(fileName: String, fileContents: String) : this(fileName, fileContents.split("\n"))

    fun printRange(range: Range): String {
        return this.printRange(makeLineCol(range))
    }

    private data class LineCol(
        val line: Int,
        val col: Int,
    )

    private data class LineColRange(
        val start: LineCol,
        val end: LineCol,
    )

    private val lengths: List<Int> = lines.map { it.length }

    private fun makeLineCol(range: Range): LineColRange {
        val start = makeLineCol(range.start)
        val end = makeLineCol(range.endInclusive)
        return LineColRange(start, end)
    }

    private fun makeLineCol(offset: Int): LineCol {
        var line = 1
        var col = 1
        var currentOffset = 0
        for (length in lengths) {
            if (currentOffset + length >= offset) {
                col = offset - currentOffset + 1
                break
            }
            line++
            currentOffset += length + 1
        }
        return LineCol(line, col)
    }

    private fun printRange(range: LineColRange): String {
        if (range.start.line == range.end.line) {
            return printSingleLineHighlighted(range)
        } else {
            return printMultipleLinesHighlighted(range)
        }
    }

    private fun printSingleLineHighlighted(range: LineColRange): String {
        val i = range.start.line
        val extraIndent = " ".repeat(i.toString().length - 1)
        val fileNamePart =
            "  --> ${fileName}:${range.start.line}:${range.start.col}".prependIndent(extraIndent)
        val paddingPart =
            "   |".prependIndent(extraIndent)
        val linePart =
            " $i | ${lines[range.start.line - 1]}"
        val caretPart =
            "$paddingPart ${" ".repeat(range.start.col - 1)}${"^".repeat(range.end.col - range.start.col + 1)}"

        return """
            |$fileNamePart
            |$paddingPart
            |$linePart
            |$caretPart
        """.trimMargin()
    }

    private fun printMultipleLinesHighlighted(range: LineColRange): String {
        val start = range.start
        val end = range.end
        val lengthOfLastNumber = range.end.line.toString().length
        val extraIndent = " ".repeat(lengthOfLastNumber - 1)
        val fileNamePart =
            "  --> ${fileName}:${start.line}:${start.col}".prependIndent(extraIndent)
        val paddingPart =
            "   |".prependIndent(extraIndent)
        val linesPart =
            (start.line..end.line).joinToString("\n") { lineNumber ->
                val line = lines[lineNumber - 1]
                val i = "$lineNumber".padEnd(lengthOfLastNumber)
                " $i > $line"
            }

        return """
            |$fileNamePart
            |$paddingPart
            |$linesPart
            |$paddingPart
        """.trimMargin()
    }
}

@Serializable
data class Range(
    override val start: Int,
    val length: Int,
) : ClosedRange<Int> {
    constructor(from: ClosedRange<Int>) : this(from.start, from.endInclusive - from.start + 1)

    override val endInclusive: Int
        get() = start + length - 1

    operator fun rangeTo(other: ClosedRange<Int>) = Range(start..other.endInclusive)
}

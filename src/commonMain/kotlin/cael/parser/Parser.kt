package cael.parser

import cael.ast.Decl
import cael.ast.FileContents
import cael.ast.Program
import cael.io.asSequenceUtf8
import cael.io.asString
import cael.io.toSource
import okio.Path
import okio.Source

fun Path.parse(): Program =
    try {
        this.toSource().lex().parse()
    } catch (e: ParseError) {
        FileContents(
            fileName = "file",
            fileContents = toSource().asString()
        ).rethrowError(e)
    }

fun Source.parse(): Program =
    try {
        this.asSequenceUtf8().lex().parse()
    } catch (e: ParseError) {
        FileContents(
            fileName = "file",
            fileContents = asString()
        ).rethrowError(e)
    }

fun String.parse(): Program =
    try {
        this.asSequence().lex().parse()
    } catch (e: ParseError) {
        FileContents(
            fileName = "file",
            fileContents = this
        ).rethrowError(e)
    }

fun Sequence<Token>.parse(): Program {
    val iterator = PeekableIterator(this.iterator())
    val startToken = iterator.peek()
    val members = mutableListOf<Decl>()
    while (iterator.hasNext()) {
        members.add(iterator.parseDecl())
    }
    val endRange = if (members.isEmpty()) startToken.range else members.last().range
    return Program(members, startToken.range..endRange)
}

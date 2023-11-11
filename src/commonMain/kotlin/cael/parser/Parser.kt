package cael.parser

import cael.ast.Decl
import cael.ast.Program

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

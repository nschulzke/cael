package cael.parser

import cael.ast.Decl
import cael.ast.Program

fun Sequence<Token>.parse(): Program {
    val iterator = PeekableIterator(this.iterator())
    val startToken = iterator.peek()
    val members = mutableListOf<Decl>()
    var endToken: Token = startToken
    while (iterator.hasNext()) {
        endToken = iterator.peek()
        members.add(iterator.parseDecl())
    }
    return Program(members, startToken..endToken)
}

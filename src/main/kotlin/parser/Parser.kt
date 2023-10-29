package parser

import ast.Decl
import ast.Program

fun Sequence<Token>.parse(): Program {
    val iterator = PeekableIterator(this.iterator())
    val members = mutableListOf<Decl>()
    while (iterator.hasNext()) {
        members.add(iterator.parseDecl())
    }
    return Program(members)
}

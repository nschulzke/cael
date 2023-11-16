package cael.parser

import cael.ast.PrintableError
import cael.ast.Range

class LexError(
    override val message: String,
    override val range: Range
) : Throwable(), PrintableError

data class ParseError(
    override val message: String,
    override val range: Range,
) : Throwable(), PrintableError

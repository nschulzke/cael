package cael.ast

import kotlinx.serialization.Serializable

@Serializable
data class Coords(
    val filename: String,
    val line: Int,
    val col: Int,
) {
    operator fun rangeTo(other: Coords) = Range(this, other)
}

@Serializable
data class Range(
    val start: Coords,
    val end: Coords,
) {
    operator fun rangeTo(other: Range) = Range(start, other.end)
}
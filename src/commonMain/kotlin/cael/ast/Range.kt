package cael.ast

import kotlinx.serialization.Serializable

@Serializable
data class Range(
    override val start: Int,
    val length: Int,
): ClosedRange<Int> {
    constructor(from: ClosedRange<Int>) : this(from.start, from.endInclusive - from.start + 1)

    override val endInclusive: Int
        get() = start + length - 1

    operator fun rangeTo(other: ClosedRange<Int>) = Range(start..other.endInclusive)
}

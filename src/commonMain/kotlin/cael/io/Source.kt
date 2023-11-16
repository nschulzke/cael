package cael.io

import okio.Source
import okio.buffer
import okio.use

fun Source.asLines(): Sequence<String> = sequence {
    this@asLines.use { source ->
        source.buffer().use { buffer ->
            while (true) {
                val line = buffer.readUtf8Line() ?: break
                yield("$line\n")
            }
        }
    }
}

fun Source.asString(): String = this.asLines().joinToString("\n")

fun Source.asSequenceUtf8(): Sequence<Char> =
    this.asLines()
        .flatMap {
            it.asSequence()
        }

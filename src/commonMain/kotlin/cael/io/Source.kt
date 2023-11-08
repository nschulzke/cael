package cael.io

import okio.Source
import okio.buffer
import okio.use

fun Source.asSequenceUtf8(): Sequence<Char> = sequence {
    this@asSequenceUtf8.use { source ->
        source.buffer().use { buffer ->
            while (true) {
                val line = buffer.readUtf8Line() ?: break
                yield("$line\n")
            }
        }
    }
}.flatMap {
    it.asSequence()
}

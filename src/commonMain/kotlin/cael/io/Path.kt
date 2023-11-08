package cael.io

import okio.Path
import okio.Sink
import okio.Source

fun Path.toSource(): Source = fileSystem.source(this)

fun Path.toSink(): Sink = fileSystem.sink(this)

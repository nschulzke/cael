package python

import cael.io.toSink
import okio.*

// Write out python AST

fun Path.write(sourceFile: SourceFile) {
    this.toSink().use {
        it.write(sourceFile)
    }
}

fun Sink.write(sourceFile: SourceFile) {
    this.buffer().use { buffer ->
        buffer.writeUtf8("from dataclasses import dataclass\n")
        buffer.writeUtf8("from typing import *\n")
        buffer.writeUtf8("from cael_runtime import singleton\n")
        buffer.writeUtf8("\n\n")
        sourceFile.statements.forEach {
            it.writeTo(buffer)
        }
    }
}

private fun nextIndent(indent: String): String {
    return "$indent    "
}

private fun Stmt.writeTo(buffer: BufferedSink, indent: String = "") {
    when (this) {
        is Stmt.Assignment -> {
            buffer.writeUtf8("${indent}$name: $type = $value\n")
            buffer.writeUtf8("\n")
        }

        is Stmt.PosFunctionDef -> {
            val parameters = parameters.mapIndexed { index, type ->
                Stmt.KwFunctionDef.Parameter(
                    name = "i$index",
                    type = type
                )
            }
            buffer.writeUtf8("${indent}def $name(${parameters.joinToString(", ") { it.toString() }}, /) -> $returnType:\n")
            if (body.isEmpty()) {
                buffer.writeUtf8("${nextIndent(indent)}pass\n")
            } else {
                body.forEach { it.writeTo(buffer, nextIndent(indent)) }
            }
            buffer.writeUtf8("\n\n")
        }

        is Stmt.KwFunctionDef -> {
            buffer.writeUtf8("${indent}def $name(*, ${parameters.joinToString(", ") { it.toString() }}) -> $returnType:\n")
            if (body.isEmpty()) {
                buffer.writeUtf8("${nextIndent(indent)}pass\n")
            } else {
                body.forEach { it.writeTo(buffer, nextIndent(indent)) }
            }
            buffer.writeUtf8("\n\n")
        }

        is Stmt.ClassDef -> {
            buffer.writeUtf8("${indent}${decorator}\n")
            buffer.writeUtf8("${indent}class $name:\n")
            val innerIndent = nextIndent(indent)
            if (parameters.isEmpty()) {
                buffer.writeUtf8("${innerIndent}pass\n")
            } else {
                parameters.forEach {
                    buffer.writeUtf8("${innerIndent}$it\n")
                }
            }
            buffer.writeUtf8("\n\n")
        }

        is Stmt.Return -> {
            buffer.writeUtf8("${indent}return $value\n")
        }

        Stmt.Pass -> {
            buffer.writeUtf8("${indent}pass\n")
        }
    }
}

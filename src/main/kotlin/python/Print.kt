package python

import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Paths

// Write out python AST

fun SourceFile.writeTo(directory: String) {
    val file = Paths.get(directory, this.path).toFile()
    writeTo(file.outputStream().writer(Charsets.UTF_8))
}

fun SourceFile.writeToString(): String {
    val stream = ByteArrayOutputStream()
    val writer = stream.writer(Charsets.UTF_8)
    writeTo(writer)
    stream.flush()
    return stream.toString(Charsets.UTF_8)
}

fun SourceFile.writeTo(writer: OutputStreamWriter) {
    writer.write("from dataclasses import dataclass\n")
    writer.write("from typing import *\n")
    writer.write("from cael_runtime import singleton\n")
    writer.write("\n\n")
    this.statements.forEach {
        it.writeTo(writer)
    }
    writer.flush()
}

private fun nextIndent(indent: String): String {
    return "$indent    "
}

private fun Stmt.writeTo(writer: OutputStreamWriter, indent: String = "") {
    when (this) {
        is Stmt.Assignment -> {
            writer.write("${indent}$name: $type = $value\n")
            writer.write("\n")
        }

        is Stmt.PosFunctionDef -> {
            val parameters = parameters.mapIndexed { index, type ->
                Stmt.KwFunctionDef.Parameter(
                    name = "i$index",
                    type = type
                )
            }
            writer.write("${indent}def $name(${parameters.joinToString(", ") { it.toString() }}, /) -> $returnType:\n")
            if (body.isEmpty()) {
                writer.write("${nextIndent(indent)}pass\n")
            } else {
                body.forEach { it.writeTo(writer, nextIndent(indent)) }
            }
            writer.write("\n\n")
        }

        is Stmt.KwFunctionDef -> {
            writer.write("${indent}def $name(*, ${parameters.joinToString(", ") { it.toString() }}) -> $returnType:\n")
            if (body.isEmpty()) {
                writer.write("${nextIndent(indent)}pass\n")
            } else {
                body.forEach { it.writeTo(writer, nextIndent(indent)) }
            }
            writer.write("\n\n")
        }

        is Stmt.ClassDef -> {
            writer.write("${indent}${decorator}\n")
            writer.write("${indent}class $name:\n")
            val innerIndent = nextIndent(indent)
            if (parameters.isEmpty()) {
                writer.write("${innerIndent}pass\n")
            } else {
                parameters.forEach {
                    writer.write("${innerIndent}$it\n")
                }
            }
            writer.write("\n\n")
        }

        is Stmt.Return -> {
            writer.write("${indent}return $value\n")
        }

        Stmt.Pass -> {
            writer.write("${indent}pass\n")
        }
    }
}

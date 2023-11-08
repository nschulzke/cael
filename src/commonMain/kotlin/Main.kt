import cael.compiler.toPython
import cael.parser.lex
import cael.parser.parse
import okio.Path.Companion.toPath
import python.write

fun main(args: Array<String>) {
    val inputPath = if (args.isEmpty()) {
        "examples/python.cl"
    } else {
        args[0]
    }.toPath()
    val program = inputPath.lex().parse()

    val outputPath = "./runtime/example.py".toPath()
    outputPath.write(program.toPython())
}

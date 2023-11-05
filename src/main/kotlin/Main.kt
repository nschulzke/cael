import cael.compiler.toPython
import cael.parser.lex
import cael.parser.parse
import python.writeTo
import java.io.File

fun main(args: Array<String>) {
    val path = if (args.isEmpty()) {
        "examples/python.cl"
    } else {
        args[0]
    }
    val file = File(path)
    val program = file.readText().lex().parse()
    program.toPython().writeTo(File("./runtime/example.py").writer(Charsets.UTF_8))
}

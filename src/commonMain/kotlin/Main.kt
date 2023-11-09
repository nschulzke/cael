import cael.compiler.toPython
import cael.parser.lex
import cael.parser.parse
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import okio.Path.Companion.toPath
import python.write

class CaelC : CliktCommand() {
    val input by argument()
    val output by option("-o", "--output", help = "Path to the output file")

    override fun run() {
        val program = input.toPath().lex().parse()
        val python = program.toPython()
        val output = output ?: input.replace(".cl", ".py")
        output.toPath().write(python)
    }
}

fun main(args: Array<String>) = CaelC().main(args)

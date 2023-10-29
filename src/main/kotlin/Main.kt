import parser.lex
import parser.parse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun main(args: Array<String>) {
    val path = if (args.isEmpty()) {
        "examples/type.cl"
    } else {
        args[0]
    }
    val file = File(path)
    val program = file.readText().lex().parse()
    val json = Json {
        prettyPrint = true
        classDiscriminator = "nodeType"
    }
    println(json.encodeToString(program))
}

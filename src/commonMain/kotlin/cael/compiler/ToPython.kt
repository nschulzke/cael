package cael.compiler

import cael.ast.Decl
import cael.ast.Expr
import cael.ast.Decl as CaelDecl
import cael.ast.Program as CaelProgram
import cael.ast.Pattern as CaelPattern
import cael.ast.Expr as CaelExpr
import python.Stmt as PythonStmt
import python.Expr as PythonExpr
import python.SourceFile as PythonSourceFile
import python.Type as PythonType

fun CaelProgram.toPython(): PythonSourceFile {
    val statements = mutableListOf<PythonStmt>()
    for (decl in declarations) {
        statements.addAll(decl.toPython())
    }
    return PythonSourceFile(
        path = "cael",
        statements = statements
    )
}

fun temporaryAnyType(): PythonType = PythonType.Raw("Any")

fun CaelDecl.toPython(): List<PythonStmt> {
    return when (this) {
        is Decl.Let -> {
            if (pattern !is CaelPattern.Identifier) {
                TODO()
            }
            val type = temporaryAnyType()
            val value = value.toPython()
            listOf(
                PythonStmt.Assignment(
                    name = pattern.name,
                    type = type,
                    value = value
                )
            )
        }

        is Decl.Fun.Record -> {
            val type = temporaryAnyType()
            val returnValue = value.toPython()
            val parameters = parameters.map {
                if (it.pattern !is CaelPattern.Identifier) {
                    TODO()
                }
                PythonStmt.KwFunctionDef.Parameter(
                    name = it.name,
                    type = temporaryAnyType()
                )
            }
            listOf(
                PythonStmt.KwFunctionDef(
                    name = name,
                    parameters = parameters,
                    returnType = type,
                    body = listOf(PythonStmt.Return(returnValue))
                )
            )
        }

        is Decl.Fun.Tuple -> {
            val returnType = temporaryAnyType()
            val returnValue = value.toPython()
            val parameters = parameters.mapIndexed { index, pattern ->
                if (pattern !is CaelPattern.Identifier) {
                    TODO()
                } else {
                    PythonType.Raw(pattern.name)
                }
            }
            listOf(
                PythonStmt.PosFunctionDef(
                    name = name,
                    parameters = parameters,
                    returnType = returnType,
                    body = listOf(PythonStmt.Return(returnValue))
                )
            )
        }

        is Decl.Fun.Match -> TODO()

        is Decl.Struct.Bare -> {
            listOf(
                PythonStmt.ClassDef(
                    decorator = PythonStmt.ClassDef.Decorator("singleton"),
                    name = name,
                    parameters = emptyList()
                )
            )
        }

        is Decl.Struct.Record -> {
            val parameters = components.map {
                PythonStmt.ClassDef.Parameter(
                    name = it.name,
                    type = temporaryAnyType()
                )
            }
            listOf(
                PythonStmt.ClassDef(
                    decorator = PythonStmt.ClassDef.Decorator("dataclass", listOf(
                        PythonStmt.ClassDef.Decorator.Argument("kw_only", PythonExpr.Identifier("True"))
                    )),
                    name = name,
                    parameters = parameters
                )
            )
        }

        is Decl.Struct.Tuple -> {
            val parameters = components.mapIndexed { index, type ->
                PythonStmt.ClassDef.Parameter(
                    name = "i$index",
                    type = temporaryAnyType()
                )
            }
            listOf(
                PythonStmt.ClassDef(
                    decorator = PythonStmt.ClassDef.Decorator("dataclass"),
                    name = name,
                    parameters = parameters
                )
            )
        }

        is Decl.Struct.Match -> TODO()
    }
}

fun CaelExpr.toPython(): PythonExpr {
    return when (this) {
        is Expr.Binary -> PythonExpr.Binary(
            left = left.toPython(),
            op = op,
            right = right.toPython()
        )
        is Expr.Call.Record -> PythonExpr.Call.Kw(
            callee = callee.toPython(),
            arguments = arguments.map {
                PythonExpr.Call.Kw.Argument(
                    name = it.name,
                    value = it.value.toPython()
                )
            }
        )
        is Expr.Call.Tuple -> PythonExpr.Call.Pos(
            callee = callee.toPython(),
            arguments = arguments.map { it.toPython() }
        )
        is Expr.Identifier -> PythonExpr.Identifier(name)
        is Expr.Literal.Float -> PythonExpr.Literal.Float(value)
        is Expr.Literal.Int -> PythonExpr.Literal.Int(value)
        is Expr.Literal.String -> PythonExpr.Literal.String(value)
        is Expr.Match -> TODO()
        is Expr.Unary -> PythonExpr.Unary(
            op = op,
            operand = operand.toPython()
        )
    }
}

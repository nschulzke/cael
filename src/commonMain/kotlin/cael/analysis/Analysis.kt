package cael.analysis

import cael.ast.Range
import cael.ast.Program as ASTProgram
import cael.ast.Decl as ASTDecl
import cael.ast.Expr as ASTExpr
import cael.ast.Pattern as ASTPattern

class Environment(
    val parent: Environment? = null,
) {
    private val definitions: MutableMap<String, RegistryEntry> = mutableMapOf()

    private data class RegistryEntry(
        val type: Type,
        val node: Node?,
    )

    fun findType(name: String): Type? {
        return definitions[name]?.type ?: parent?.findType(name)
    }

    fun register(name: String, type: Type, node: Node?) {
        definitions[name] = RegistryEntry(type, node)
    }
}

fun rootEnvironment(): Environment = Environment().apply {
    register("Int", Type.Primitive.Int, null)
    register("Float", Type.Primitive.Float, null)
    register("String", Type.Primitive.String, null)
}

fun ASTProgram.analyze(environment: Environment = rootEnvironment()): Program {
    val newDeclarations = mutableListOf<Decl>()
    for (decl in declarations) {
        newDeclarations.add(decl.analyze(environment))
    }
    return Program(newDeclarations, range)
}

fun ASTDecl.analyze(environment: Environment): Decl {
    return when (this) {
        is ASTDecl.Let -> {
            val analyzedValue = value.analyze(environment)
            val analyzedPattern = pattern.analyze(environment, analyzedValue.type)
            val boundNames = analyzedPattern.boundNames()
            for (name in boundNames) {
                environment.register(name.name, name.type, name.node)
            }
            Decl.Let(analyzedPattern, analyzedValue, range)
        }
        is ASTDecl.Fun.Match -> TODO()
        is ASTDecl.Fun.Record -> TODO()
        is ASTDecl.Fun.Tuple -> TODO()
        is ASTDecl.Struct.Bare -> {
            val type = Type.StructConstructor.Bare(name)
            val node = Decl.Struct.Bare(type,  range)
            environment.register(name, type, node)
            node
        }
        is ASTDecl.Struct.Record -> TODO()
        is ASTDecl.Struct.Tuple -> {
            val analyzedComponents = components.map { it.analyze(environment) }
            val type = Type.StructConstructor.Tuple(name, analyzedComponents.map { it.typeMatched })
            val node = Decl.Struct.Tuple(type, range)
            environment.register(name, type, node)
            node
        }
    }
}

fun ASTExpr.analyze(environment: Environment, expectedType: Type? = null): Expr {
    return when (this) {
        is ASTExpr.Binary -> {
            val analyzedLeft = left.analyze(environment)
            val analyzedRight = right.analyze(environment)
            val type = when (op) {
                "+", "-", "*", "/" -> {
                    if (analyzedLeft.type !is Type.Primitive.Number) {
                        Type.Error.Other("Expected number, got ${analyzedLeft.type}", analyzedLeft.range)
                    } else if (analyzedRight.type !is Type.Primitive.Number) {
                        Type.Error.Other("Expected number, got ${analyzedRight.type}", analyzedRight.range)
                    } else {
                        analyzedLeft.type
                    }
                }
                "==" -> {
                    if (analyzedLeft.type != analyzedRight.type) {
                        Type.Error.Other("Expected ${analyzedLeft.type}, got ${analyzedRight.type}", analyzedRight.range)
                    } else {
                        Type.Primitive.Boolean
                    }
                }
                else -> Type.Error.Other("Unknown binary operator `${op}`", this.range)
            }
            Expr.Binary(analyzedLeft, op, analyzedRight, type, range)
        }
        is ASTExpr.Call.Record -> TODO()
        is ASTExpr.Call.Tuple -> {
            val analyzedCallee = callee.analyze(environment)
            val analyzedArguments = arguments.map { it.analyze(environment) }
            val type = when (val calleeType = analyzedCallee.type) {
                is Type.Fun.Tuple -> {
                    if (calleeType.parameters.size != analyzedArguments.size) {
                        Type.Error.Other("Expected ${calleeType.parameters.size} arguments, got ${analyzedArguments.size}", range)
                    } else {
                        for ((expected, actual) in calleeType.parameters.zip(analyzedArguments)) {
                            assertEquals(actual.type, expected, actual.range)
                        }
                        calleeType.returnType
                    }
                }
                is Type.Fun.Record -> TODO()
                is Type.StructConstructor.Tuple -> {
                    if (calleeType.components.size != analyzedArguments.size) {
                        Type.Error.Other("Expected ${calleeType.components.size} arguments, got ${analyzedArguments.size}", range)
                    } else {
                        for ((expected, actual) in calleeType.components.zip(analyzedArguments)) {
                            assertEquals(actual.type, expected, actual.range)
                        }
                        calleeType.struct
                    }
                }
                is Type.StructConstructor.Record -> TODO()
                else -> Type.Error.Other("Expected callable, got ${analyzedCallee.type}", analyzedCallee.range)
            }
            Expr.Call.Tuple(analyzedCallee, analyzedArguments, type, range)
        }
        is ASTExpr.Identifier -> {
            Expr.Identifier(
                name,
                when (val actualType = environment.findType(name)) {
                    null -> Type.Error.UnboundName(name, this.range)
                    is Type.Error -> Type.Error.Propagated(actualType)
                    is Type.StructConstructor.Bare -> assertEquals(actualType.struct, expectedType, this.range)
                    else -> assertEquals(actualType, expectedType, this.range)
                },
                range
            )
        }

        is ASTExpr.Literal.Float -> Expr.Literal.Float(value, range)
        is ASTExpr.Literal.Int -> Expr.Literal.Int(value, range)
        is ASTExpr.Literal.String -> Expr.Literal.String(value, range)
        is ASTExpr.Match -> TODO()
        is ASTExpr.Unary -> {
            val analyzedOperand = operand.analyze(environment)
            when (op) {
                "+", "-" -> {
                    val type = if (analyzedOperand.type !is Type.Primitive.Number) {
                        Type.Error.Other("Expected number, got ${analyzedOperand.type}", analyzedOperand.range)
                    } else {
                        analyzedOperand.type
                    }
                    Expr.Unary(op, analyzedOperand, type, range)
                }
                "!" -> {
                    val type = if (analyzedOperand.type !is Type.Primitive.Boolean) {
                        Type.Error.Other("Expected boolean, got ${analyzedOperand.type}", analyzedOperand.range)
                    } else {
                        Type.Primitive.Boolean
                    }
                    Expr.Unary(op, analyzedOperand, type, range)
                }
                else -> Expr.Unary(op, analyzedOperand, Type.Error.Other("Unknown unary operator `${op}`", this.range), range)
            }
        }
    }
}

fun ASTPattern.analyze(environment: Environment, expectedType: Type? = null): Pattern {
    return when (this) {
        is ASTPattern.Binary -> TODO()
        is ASTPattern.Literal.Float -> TODO()
        is ASTPattern.Literal.Int -> TODO()
        is ASTPattern.Struct.Record -> TODO()
        is ASTPattern.Literal.String -> TODO()
        is ASTPattern.Struct.Tuple -> {
            return when (val constructorType = environment.findType(name)) {
                is Type.StructConstructor.Tuple -> {
                    val analyzedComponents = components.zip(constructorType.components).map { (component, expected) ->
                        component.analyze(environment, expected)
                    }
                    Pattern.Struct.Tuple(name, analyzedComponents, constructorType.struct, range)
                }
                null -> {
                    val analyzedComponents = components.map { it.analyze(environment) }
                    Pattern.Struct.Tuple(name, analyzedComponents, Type.Error.UnboundName(name, range), range)
                }
                else -> {
                    val analyzedComponents = components.map { it.analyze(environment) }
                    Pattern.Struct.Tuple(name, analyzedComponents, Type.Error.Other("$name is not a tuple struct, it is a $constructorType", range), range)
                }
            }
        }
        is ASTPattern.Identifier -> {
            if (name == "_") {
                return Pattern.Wildcard(expectedType ?: Type.Error.Other("Unable to infer type of wildcard", this.range), range)
            }
            when (val environmentType = environment.findType(name)) {
                is Type.StructConstructor -> Pattern.Struct.Bare(name, environmentType.struct, range)
                else -> {
                    val type = expectedType ?: Type.Error.Other("Unable to infer type of `$name`", this.range)
                    Pattern.NameBinding(name, type, range).also {
                        environment.register(name, type, it)
                    }
                }
            }
        }
    }
}

fun assertEquals(actual: Type, expected: Type?, range: Range): Type {
    if (expected is Type.Error) {
        return Type.Error.Propagated(expected)
    }
    if (expected == null) {
        return actual
    }
    if (actual == expected) {
        return actual
    }
    return Type.Error.Other("Expected $expected, got $actual", range)
}

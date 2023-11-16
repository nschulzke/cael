package cael.analysis

import cael.ast.Range
import kotlinx.serialization.Serializable


@Serializable
sealed interface Type {
    @Serializable
    data object Any

    @Serializable
    sealed interface Primitive : Type {
        @Serializable
        sealed interface Number : Primitive

        @Serializable
        data object Int : Number

        @Serializable
        data object Float : Number

        @Serializable
        data object String : Primitive

        @Serializable
        data object Boolean : Primitive
    }

    sealed interface StructConstructor : Type {
        val struct: Struct

        @Serializable
        data class Bare(
            val name: String,
        ) : StructConstructor {
            override val struct = Struct(this)
        }

        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Type>,
        ) : StructConstructor {
            override val struct = Struct(this)
        }

        @Serializable
        data class Record(
            val name: String,
            val components: List<TypeRecordItem>,
        ) : StructConstructor {
            override val struct = Struct(this)
        }
    }

    @Serializable
    data class Struct(val constructor: StructConstructor) : Type

    @Serializable
    sealed interface Fun : Type {
        @Serializable
        data class Tuple(
            val node: Decl.Fun.Tuple
        ) : Fun

        @Serializable
        data class Record(
            val node: Decl.Fun.Record
        ) : Fun

        @Serializable
        data class Match(
            val node: Decl.Fun.Match
        ) : Fun
    }

    sealed interface Error : Type {
        fun primaryError(): Primary

        sealed interface Primary : Error {
            fun message(fileName: String, fileContents: String): String
        }

        data class UnboundName(
            val name: String,
            val range: Range,
        ) : Primary {
            override fun primaryError(): Primary = this

            override fun message(fileName: String, fileContents: String): String {
                return """
                    Unbound name `$name` at ${range.start}:${range.start}
                """.trimIndent()
            }
        }

        data class Other(
            val message: String,
            val range: Range,
        ) : Primary {
            override fun primaryError(): Primary = this

            override fun message(fileName: String, fileContents: String): String {
                TODO("Not yet implemented")
            }
        }

        data class Propagated(
            val parent: Error
        ) : Error {
            override fun primaryError(): Primary {
                var next = this.parent
                while (next !is Primary) {
                    if (next !is Propagated) {
                        throw Error("Unkown error type")
                    } else {
                        next = next.parent
                    }
                }
                return next
            }
        }
    }

    data class Union(
        val types: List<Type>,
    ) : Type
}

@Serializable
sealed interface Node {
    val range: Range
}

@Serializable
data class Program(
    val declarations: List<Decl>,
    override val range: Range,
) : Node

@Serializable
sealed interface Decl : Node {

    @Serializable
    sealed interface Struct : Decl {
        val constructorType: Type.StructConstructor

        @Serializable
        data class Bare(
            override val constructorType: Type.StructConstructor.Bare,
            override val range: Range,
        ) : Struct

        @Serializable
        data class Tuple(
            override val constructorType: Type.StructConstructor.Tuple,
            override val range: Range,
        ) : Struct

        @Serializable
        data class Record(
            override val constructorType: Type.StructConstructor.Tuple,
            override val range: Range,
        ) : Struct
    }

    @Serializable
    data class Let(
        val pattern: Pattern,
        val value: Expr,
        override val range: Range,
    ) : Decl

    @Serializable
    sealed interface Fun : Decl {
        @Serializable
        data class Tuple(
            val name: String,
            val parameters: List<Pattern>,
            val value: Expr,
            override val range: Range,
        ) : Fun

        @Serializable
        data class Record(
            val name: String,
            val parameters: List<PatternRecordItem>,
            val value: Expr,
            override val range: Range,
        ) : Fun

        @Serializable
        data class Match(
            val name: String,
            val cases: List<Case>,
            override val range: Range,
        ) : Fun {
            sealed interface Case : Node {
                @Serializable
                data class Tuple(
                    val parameters: List<Pattern>,
                    val value: Expr,
                    override val range: Range,
                ) : Case

                @Serializable
                data class Record(
                    val parameters: List<PatternRecordItem>,
                    val value: Expr,
                    override val range: Range,
                ) : Case
            }
        }
    }
}

@Serializable
sealed interface Expr : Node {
    val type: Type

    @Serializable
    data class Identifier(
        val name: String,
        override val type: Type,
        override val range: Range,
    ) : Expr

    @Serializable
    sealed interface Literal : Expr {
        @Serializable
        data class Int(
            val value: kotlin.Int,
            override val range: Range,
        ) : Literal {
            override val type = Type.Primitive.Int
        }

        @Serializable
        data class Float(
            val value: Double,
            override val range: Range,
        ) : Literal {
            override val type = Type.Primitive.Float
        }

        @Serializable
        data class String(
            val value: kotlin.String,
            override val range: Range,
        ) : Literal {
            override val type = Type.Primitive.String
        }
    }

    @Serializable
    sealed interface Call : Expr {
        @Serializable
        data class Tuple(
            val callee: Expr,
            val arguments: List<Expr>,
            override val type: Type,
            override val range: Range,
        ) : Call

        @Serializable
        data class Record(
            val callee: Expr,
            val arguments: List<ExprRecordItem>,
            override val type: Type,
            override val range: Range,
        ) : Call
    }

    @Serializable
    data class Binary(
        val left: Expr,
        val op: String,
        val right: Expr,
        override val type: Type,
        override val range: Range,
    ) : Expr

    @Serializable
    data class Unary(
        val op: String,
        val operand: Expr,
        override val type: Type,
        override val range: Range,
    ) : Expr

    @Serializable
    data class Match(
        val expr: Expr,
        val cases: List<Case>,
        override val type: Type,
        override val range: Range,
    ) : Expr {
        @Serializable
        data class Case(
            val pattern: Pattern,
            val value: Expr,
            val type: Type,
            override val range: Range,
        ) : Node
    }
}

@Serializable
sealed interface Pattern : Node {
    val typeMatched: Type
    fun boundNames(): List<BoundName>
    
    @Serializable
    data class BoundName(
        val name: String,
        val type: Type,
        val node: Node,
    )

    @Serializable
    data class NameBinding(
        val name: String,
        override val typeMatched: Type,
        override val range: Range,
    ) : Pattern {
        override fun boundNames(): List<BoundName> {
            return listOf(BoundName(name, typeMatched, this))
        }
    }

    @Serializable
    data class Wildcard(
        override val typeMatched: Type,
        override val range: Range,
    ) : Pattern {
        override fun boundNames(): List<BoundName> {
            return emptyList()
        }
    }

    @Serializable
    sealed interface Literal {
        data class Int(
            val value: kotlin.Int,
            override val range: Range,
        ) : Pattern {
            override val typeMatched = Type.Primitive.Int

            override fun boundNames(): List<BoundName> {
                return emptyList()
            }
        }

        @Serializable
        data class Float(
            val value: Double,
            override val range: Range,
        ) : Pattern {
            override val typeMatched = Type.Primitive.Float

            override fun boundNames(): List<BoundName> {
                return emptyList()
            }
        }

        @Serializable
        data class String(
            val value: kotlin.String,
            override val range: Range,
        ) : Pattern {
            override val typeMatched = Type.Primitive.String

            override fun boundNames(): List<BoundName> {
                return emptyList()
            }
        }
    }

    @Serializable
    sealed interface Struct {
        @Serializable
        data class Bare(
            val name: String,
            override val typeMatched: Type.Struct,
            override val range: Range,
        ) : Pattern {
            override fun boundNames(): List<BoundName> {
                return emptyList()
            }
        }

        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Pattern>,
            override val typeMatched: Type.Struct,
            override val range: Range,
        ) : Pattern {
            override fun boundNames(): List<BoundName> {
                return components.flatMap { it.boundNames() }
            }
        }

        @Serializable
        data class Record(
            val name: String,
            val components: List<PatternRecordItem>,
            override val typeMatched: Type.Struct,
            override val range: Range,
        ) : Pattern {
            override fun boundNames(): List<BoundName> {
                return components.flatMap { it.pattern.boundNames() }
            }
        }
    }

    data class Binary(
        val left: Pattern,
        val op: String,
        val right: Pattern,
        override val typeMatched: Type,
        override val range: Range,
    ) : Pattern {
        override fun boundNames(): List<BoundName> {
            return left.boundNames() + right.boundNames()
        }
    }

    data class Unary(
        val op: String,
        val operand: Pattern,
        override val typeMatched: Type,
        override val range: Range,
    ) : Pattern {
        override fun boundNames(): List<BoundName> {
            return operand.boundNames()
        }
    }
}

@Serializable
data class TypeRecordItem(
    val name: String,
    val type: Type,
    override val range: Range,
) : Node

@Serializable
data class ExprRecordItem(
    val name: String,
    val value: Expr,
    override val range: Range,
) : Node

@Serializable
data class PatternRecordItem(
    val name: String,
    val pattern: Pattern,
    override val range: Range,
) : Node

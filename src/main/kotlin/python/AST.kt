package python

import kotlinx.serialization.Serializable

data class SourceFile(
    val path: String,
    val statements: List<Stmt>
)

sealed interface Stmt {
    data class Assignment(
        val name: String,
        val type: Type,
        val value: Expr
    ) : Stmt

    data class PosFunctionDef(
        val name: String,
        val parameters: List<Type>,
        val returnType: Type,
        val body: List<Stmt>
    ) : Stmt

    data class KwFunctionDef(
        val name: String,
        val parameters: List<Parameter>,
        val returnType: Type,
        val body: List<Stmt>
    ) : Stmt {
        data class Parameter(
            val name: String,
            val type: Type
        ) {
            override fun toString(): String = "$name: $type"
        }
    }

    data class ClassDef(
        val decorator: Decorator,
        val name: String,
        val parameters: List<Parameter>
    ) : Stmt {
        data class Parameter(
            val name: String,
            val type: Type
        ) {
            override fun toString(): String = "$name: $type"
        }

        data class Decorator(
            val name: String,
            val arguments: List<Argument> = emptyList()
        ) {
            override fun toString(): String = "@$name(${arguments.joinToString(", ")})"

            data class Argument(
                val name: String,
                val value: Expr
            ) {
                override fun toString(): String = "$name=$value"
            }
        }
    }

    data class Return(
        val value: Expr
    ) : Stmt

    data object Pass : Stmt
}

sealed interface Type {
    data class Raw(
        val name: String
    ) : Type {
        override fun toString(): String = name
    }

    data class Final(
        val type: Type,
    ) : Type {
        override fun toString(): String = "Final[$type]"
    }
}

sealed interface Expr {
    @Serializable
    data class Identifier(
        val name: String
    ) : Expr {
        override fun toString(): String = name
    }

    @Serializable
    sealed interface Literal : Expr {
        @Serializable
        data class Int(
            val value: kotlin.Int
        ) : Literal {
            override fun toString(): kotlin.String = value.toString()
        }

        @Serializable
        data class Float(
            val value: Double
        ) : Literal {
            override fun toString(): kotlin.String = value.toString()
        }

        @Serializable
        data class String(
            val value: kotlin.String
        ) : Literal {
            override fun toString(): kotlin.String = "\"${value}\""
        }
    }

    @Serializable
    sealed interface Call : Expr {
        @Serializable
        data class Pos(
            val callee: Expr,
            val arguments: List<Expr>
        ) : Call {
            override fun toString(): String = "$callee(${arguments.joinToString(", ")})"
        }

        @Serializable
        data class Kw(
            val callee: Expr,
            val arguments: List<Argument>
        ) : Call {
            override fun toString(): String = "$callee(${arguments.joinToString(", ")})"

            @Serializable
            data class Argument(
                val name: String,
                val value: Expr
            ) {
                override fun toString(): String = "$name=$value"
            }
        }
    }

    @Serializable
    data class Binary(
        val left: Expr,
        val op: String,
        val right: Expr
    ) : Expr {
        override fun toString(): String = "($left $op $right)"
    }

    @Serializable
    data class Unary(
        val op: String,
        val operand: Expr
    ) : Expr {
        override fun toString(): String = "($op $operand)"
    }
}

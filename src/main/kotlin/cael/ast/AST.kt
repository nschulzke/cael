package cael.ast

import kotlinx.serialization.Serializable

@Serializable
sealed interface Node

@Serializable
data class Program(
    val declarations: List<Decl>
) : Node

@Serializable
sealed interface Decl : Node {
    @Serializable
    data class Module(
        val name: String,
        val declarations: List<Decl>
    ) : Decl

    @Serializable
    data class Open(
        val name: String,
    ) : Decl

    @Serializable
    data class TypeAlias(
        val name: String,
        val body: Type
    ) : Decl

    @Serializable
    data class Protocol(
        val name: String,
        val declarations: List<Declaration>
    ) : Decl {
        @Serializable
        sealed interface Declaration
    }

    @Serializable
    data class Extension(
        val structName: String,
        val protocolName: String?,
        val declarations: List<Declaration>
    ) : Decl {
        @Serializable
        sealed interface Declaration
    }

    @Serializable
    sealed interface Struct : Decl, Protocol.Declaration {
        @Serializable
        data class Bare(
            val name: String,
        ) : Struct

        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Type>
        ) : Struct

        @Serializable
        data class Record(
            val name: String,
            val components: List<TypeRecordItem>
        ) : Struct
    }

    @Serializable
    sealed interface Dec : Decl, Protocol.Declaration, Extension.Declaration {
        @Serializable
        data class Bare(
            val name: String,
            val type: Type
        ) : Dec

        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Type>,
            val type: Type
        ) : Dec

        @Serializable
        data class Record(
            val name: String,
            val components: List<TypeRecordItem>,
            val type: Type
        ) : Dec
    }

    @Serializable
    sealed interface Let : Decl, Extension.Declaration {
        @Serializable
        data class Bare(
            val name: String,
            val value: Expr
        ) : Let

        @Serializable
        data class Tuple(
            val name: String,
            val parameters: List<Pattern>,
            val value: Expr
        ) : Let

        @Serializable
        data class Record(
            val name: String,
            val parameters: List<PatternRecordItem>,
            val value: Expr
        ) : Let
    }
}

@Serializable
sealed interface Type : Node {
    @Serializable
    data class Identifier(
        val name: String
    ) : Type

    @Serializable
    data class Union(
        val left: Type,
        val right: Type
    ) : Type
}

@Serializable
sealed interface Expr : Node {
    @Serializable
    data class Identifier(
        val name: String
    ) : Expr

    @Serializable
    sealed interface Literal : Expr {
        @Serializable
        data class Int(
            val value: kotlin.Int
        ) : Literal

        @Serializable
        data class Float(
            val value: Double
        ) : Literal

        @Serializable
        data class String(
            val value: kotlin.String
        ) : Literal
    }

    @Serializable
    sealed interface Call : Expr {
        @Serializable
        data class Tuple(
            val callee: Expr,
            val arguments: List<Expr>
        ) : Call

        @Serializable
        data class Record(
            val callee: Expr,
            val arguments: List<ExprRecordItem>
        ) : Call
    }

    @Serializable
    sealed interface ExtensionCall : Expr {
        @Serializable
        data class Bare(
            val callee: Expr,
            val name: String
        ) : ExtensionCall

        @Serializable
        data class Tuple(
            val callee: Expr,
            val name: String,
            val arguments: List<Expr>
        ) : ExtensionCall

        @Serializable
        data class Record(
            val callee: Expr,
            val name: String,
            val arguments: List<ExprRecordItem>
        ) : ExtensionCall
    }

    @Serializable
    data class Binary(
        val left: Expr,
        val op: String,
        val right: Expr
    ) : Expr

    @Serializable
    data class Unary(
        val op: String,
        val operand: Expr
    ) : Expr

    @Serializable
    data class Match(
        val value: Expr,
        val cases: List<Case>
    ) : Expr {
        @Serializable
        data class Case(
            val pattern: Pattern,
            val body: Expr
        )
    }
}

@Serializable
sealed interface Pattern : Node {
    @Serializable
    data class Identifier(
        val name: String
    ) : Pattern

    @Serializable
    sealed interface Literal {
        data class Int(
            val value: kotlin.Int
        ) : Pattern

        @Serializable
        data class Float(
            val value: Double
        ) : Pattern

        @Serializable
        data class String(
            val value: kotlin.String
        ) : Pattern
    }

    @Serializable
    sealed interface Struct {
        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Pattern>
        ) : Pattern

        @Serializable
        data class Record(
            val name: String,
            val components: List<PatternRecordItem>
        ) : Pattern
    }
}

@Serializable
data class TypeRecordItem(
    val name: String,
    val type: Type
) : Node

@Serializable
data class ExprRecordItem(
    val name: String,
    val value: Expr
) : Node

@Serializable
data class PatternRecordItem(
    val name: String,
    val pattern: Pattern
) : Node

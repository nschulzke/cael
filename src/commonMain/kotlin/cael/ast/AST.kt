package cael.ast

import kotlinx.serialization.Serializable

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
    data class Module(
        val name: String,
        val declarations: List<Decl>,
        override val range: Range,
    ) : Decl

    @Serializable
    data class Open(
        val name: String,
        override val range: Range,
    ) : Decl

    @Serializable
    data class TypeAlias(
        val name: String,
        val body: Type,
        override val range: Range,
    ) : Decl

    @Serializable
    data class Protocol(
        val name: String,
        val declarations: List<Declaration>,
        override val range: Range,
    ) : Decl {
        @Serializable
        sealed interface Declaration
    }

    @Serializable
    data class Extension(
        val structName: String,
        val protocolName: String?,
        val declarations: List<Declaration>,
        override val range: Range,
    ) : Decl {
        @Serializable
        sealed interface Declaration
    }

    @Serializable
    sealed interface Struct : Decl, Protocol.Declaration {
        @Serializable
        data class Bare(
            val name: String,
            override val range: Range,
        ) : Struct

        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Type>,
            override val range: Range,
        ) : Struct

        @Serializable
        data class Record(
            val name: String,
            val components: List<TypeRecordItem>,
            override val range: Range,
        ) : Struct
    }

    @Serializable
    sealed interface Dec : Decl, Protocol.Declaration, Extension.Declaration {
        @Serializable
        data class Bare(
            val name: String,
            val type: Type,
            override val range: Range,
        ) : Dec

        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Type>,
            val type: Type,
            override val range: Range,
        ) : Dec

        @Serializable
        data class Record(
            val name: String,
            val components: List<TypeRecordItem>,
            val type: Type,
            override val range: Range,
        ) : Dec
    }

    @Serializable
    sealed interface Let : Decl, Extension.Declaration {
        @Serializable
        data class Bare(
            val name: String,
            val value: Expr,
            override val range: Range,
        ) : Let

        @Serializable
        data class Tuple(
            val name: String,
            val parameters: List<Pattern>,
            val value: Expr,
            override val range: Range,
        ) : Let

        @Serializable
        data class Record(
            val name: String,
            val parameters: List<PatternRecordItem>,
            val value: Expr,
            override val range: Range,
        ) : Let
    }
}

@Serializable
sealed interface Type : Node {
    @Serializable
    data class Identifier(
        val name: String,
        override val range: Range,
    ) : Type

    @Serializable
    data class Union(
        val left: Type,
        val right: Type,
        override val range: Range,
    ) : Type
}

@Serializable
sealed interface Expr : Node {
    @Serializable
    data class Identifier(
        val name: String,
        override val range: Range,
    ) : Expr

    @Serializable
    sealed interface Literal : Expr {
        @Serializable
        data class Int(
            val value: kotlin.Int,
            override val range: Range,
        ) : Literal

        @Serializable
        data class Float(
            val value: Double,
            override val range: Range,
        ) : Literal

        @Serializable
        data class String(
            val value: kotlin.String,
            override val range: Range,
        ) : Literal
    }

    @Serializable
    sealed interface Call : Expr {
        @Serializable
        data class Tuple(
            val callee: Expr,
            val arguments: List<Expr>,
            override val range: Range,
        ) : Call

        @Serializable
        data class Record(
            val callee: Expr,
            val arguments: List<ExprRecordItem>,
            override val range: Range,
        ) : Call
    }

    @Serializable
    sealed interface ExtensionCall : Expr {
        @Serializable
        data class Bare(
            val callee: Expr,
            val name: String,
            override val range: Range,
        ) : ExtensionCall

        @Serializable
        data class Tuple(
            val callee: Expr,
            val name: String,
            val arguments: List<Expr>,
            override val range: Range,
        ) : ExtensionCall

        @Serializable
        data class Record(
            val callee: Expr,
            val name: String,
            val arguments: List<ExprRecordItem>,
            override val range: Range,
        ) : ExtensionCall
    }

    @Serializable
    data class Binary(
        val left: Expr,
        val op: String,
        val right: Expr,
        override val range: Range,
    ) : Expr

    @Serializable
    data class Unary(
        val op: String,
        val operand: Expr,
        override val range: Range,
    ) : Expr

    @Serializable
    data class Match(
        val value: Expr,
        val cases: List<Case>,
        override val range: Range,
    ) : Expr {
        @Serializable
        data class Case(
            val pattern: Pattern,
            val body: Expr,
            override val range: Range,
        ) : Node
    }
}

@Serializable
sealed interface Pattern : Node {
    @Serializable
    data class Identifier(
        val name: String,
        override val range: Range,
    ) : Pattern

    @Serializable
    sealed interface Literal {
        data class Int(
            val value: kotlin.Int,
            override val range: Range,
        ) : Pattern

        @Serializable
        data class Float(
            val value: Double,
            override val range: Range,
        ) : Pattern

        @Serializable
        data class String(
            val value: kotlin.String,
            override val range: Range,
        ) : Pattern
    }

    @Serializable
    sealed interface Struct {
        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Pattern>,
            override val range: Range,
        ) : Pattern

        @Serializable
        data class Record(
            val name: String,
            val components: List<PatternRecordItem>,
            override val range: Range,
        ) : Pattern
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

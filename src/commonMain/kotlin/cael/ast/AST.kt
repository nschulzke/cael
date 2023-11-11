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
    sealed interface Struct : Decl {
        @Serializable
        data class Bare(
            val name: String,
            override val range: Range,
        ) : Struct

        @Serializable
        data class Tuple(
            val name: String,
            val components: List<Pattern>,
            override val range: Range,
        ) : Struct

        @Serializable
        data class Record(
            val name: String,
            val components: List<PatternRecordItem>,
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
    }
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

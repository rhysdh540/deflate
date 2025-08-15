package dev.rdh.deflate.core

sealed interface Token

@JvmInline
value class Literal(val value: Byte) : Token {
    override fun toString(): String = "Literal($value)"

    val intValue: Int get() = value.toInt() and 0xFF
}

@JvmRecord
data class Match(val len: Int, val dist: Int) : Token {
    override fun toString(): String = "Match(len=$len, dist=$dist)"
}
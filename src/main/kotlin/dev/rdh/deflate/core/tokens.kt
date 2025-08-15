package dev.rdh.deflate.core

sealed interface Token

@JvmInline
value class Literal(val value: Byte) : Token {
    override fun toString(): String = "Literal($value)"

    val intValue: Int get() = value.toInt() and 0xFF
}

@JvmInline
value class Match(private val packed: Int) : Token {
    constructor(len: Int, dist: Int) : this((len shl 16) or (dist and 0xFFFF))
    val len: Int get() = packed ushr 16
    val dist: Int get() = packed and 0xFFFF
    override fun toString() = "Match(len=$len, dist=$dist)"
}
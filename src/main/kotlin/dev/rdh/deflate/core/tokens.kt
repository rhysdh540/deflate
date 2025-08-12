package dev.rdh.deflate.core

sealed class Token

data class Literal(val value: Byte) : Token() {
    constructor(value: Int) : this(value.toByte())
    override fun toString(): String = "Literal($value)"

    val intValue: Int = value.toInt() and 0xFF
}

data class Match(val len: Int, val dist: Int) : Token() {
    override fun toString(): String = "Match(len=$len, dist=$dist)"
}
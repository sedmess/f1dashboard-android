package ru.n1ks.f1dashboard

internal const val OneByte = 1.toByte()

internal infix fun Byte.plusByte(increment: Byte): Byte = this.plus(increment).toByte()

internal infix fun Byte.minusByte(decrement: Byte): Byte = this.minus(decrement).toByte()
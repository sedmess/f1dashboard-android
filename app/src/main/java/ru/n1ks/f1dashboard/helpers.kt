package ru.n1ks.f1dashboard

internal object Bytes {

    const val Zero = 0.toByte()
    const val One = 1.toByte()
    const val Two = 2.toByte()
}

internal infix fun Byte.plusByte(increment: Byte): Byte = this.plus(increment).toByte()

internal infix fun Byte.minusByte(decrement: Byte): Byte = this.minus(decrement).toByte()
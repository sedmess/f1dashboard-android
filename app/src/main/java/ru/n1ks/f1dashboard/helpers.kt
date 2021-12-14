package ru.n1ks.f1dashboard

import android.graphics.Color

internal object Colors {

    internal fun complimentColor(color: Int): Int = Color.argb(
        Color.alpha(color),
        Color.red(color).inv() and 0xff,
        Color.green(color).inv() and 0xff,
        Color.blue(color).inv() and 0xff
    )
}

internal object Bytes {

    const val Zero = 0.toByte()
    const val One = 1.toByte()
    const val Two = 2.toByte()
}

internal infix fun Byte.plusByte(increment: Byte): Byte = this.plus(increment).toByte()

internal infix fun Byte.minusByte(decrement: Byte): Byte = this.minus(decrement).toByte()

internal fun Short.toUnsignedInt(): Int = this.toInt() and 0xffff
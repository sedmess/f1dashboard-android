package ru.n1ks.f1dashboard

@ExperimentalUnsignedTypes
internal val UByteOne = 1.toUByte()

@ExperimentalUnsignedTypes
internal fun UByte.plusOne() = this.plus(UByteOne).toUByte()

@ExperimentalUnsignedTypes
internal fun UByte.minusOne() = this.minus(UByteOne).toUByte()
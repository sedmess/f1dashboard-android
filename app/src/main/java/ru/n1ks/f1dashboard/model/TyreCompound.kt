package ru.n1ks.f1dashboard.model

import ru.n1ks.f1dashboard.R

enum class TyreCompound(val char: Char, val color: Int) {
    X('X', R.color.white),
    C1('⑴', R.color.tyreH),
    C2('⑵', R.color.tyreM),
    C3('⑶', R.color.tyreS),
    C4('⑷', R.color.tyreSS),
    C5('⑸', R.color.tyreUS),
    Inter('Ⓘ', R.color.tyreInter),
    Wet('Ⓦ', R.color.tyreWet),
    DryClassic('ⓓ', R.color.tyreDry),
    WetClassic('ⓦ', R.color.tyreWet),
    SuperSoftF2('Ⓠ', R.color.tyreSS),
    SoftF2('Ⓢ', R.color.tyreS),
    MediumF2('Ⓜ', R.color.tyreM),
    HardF2('Ⓗ', R.color.tyreH),
    WetF2('Ⓦ', R.color.tyreWet),
    Soft('Ⓢ', R.color.tyreSS),
    Medium('Ⓜ', R.color.tyreS),
    Hard('Ⓗ', R.color.tyreM),
    ;

    companion object {
        fun defineActualByCode(code: Byte) = when (code.toInt()) {
            16 -> C5
            17 -> C4
            18 -> C3
            19 -> C2
            20 -> C1
            7 -> Inter
            8 -> Wet
            9 -> DryClassic
            10 -> WetClassic
            11 -> SuperSoftF2
            12 -> SoftF2
            13 -> MediumF2
            14 -> HardF2
            15 -> WetF2
            else -> X
        }

        fun defineVisualByCode(code: Byte) = when (code.toInt()) {
            16 -> Soft
            17 -> Medium
            18 -> Hard
            7 -> Inter
            8 -> Wet
            9 -> DryClassic
            10 -> WetClassic
            11 -> SuperSoftF2
            12 -> SoftF2
            13 -> MediumF2
            14 -> HardF2
            15 -> WetF2
            else -> X
        }
    }
}
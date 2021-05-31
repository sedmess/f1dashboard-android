package ru.n1ks.f1dashboard.model

enum class TyreCompound(val shortValue: Char) {
    X('X'),
    C1('⑴'),
    C2('⑵'),
    C3('⑶'),
    C4('⑷'),
    C5('⑸'),
    Inter('Ⓘ'),
    Wet('Ⓦ'),
    DryClassic('ⓓ'),
    WetClassic('ⓦ'),
    SuperSoftF2('Ⓠ'),
    SoftF2('Ⓢ'),
    MediumF2('Ⓜ'),
    HardF2('Ⓗ'),
    WetF2('Ⓦ'),
    Soft('Ⓢ'),
    Medium('Ⓜ'),
    Hard('Ⓗ');

    @ExperimentalUnsignedTypes
    companion object {
        fun defineActualByCode(code: UByte) = when (code.toInt()) {
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

        fun defineVisualByCode(code: UByte) = when (code.toInt()) {
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
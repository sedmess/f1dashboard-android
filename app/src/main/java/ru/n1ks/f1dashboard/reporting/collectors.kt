package ru.n1ks.f1dashboard.reporting

object UDPPacketTail {

    private const val tailSize = 20
    private val tail = Array<ByteArray?>(tailSize) { null }
    private var tailIdx = 0

    fun onPacket(packet: ByteArray) {
        synchronized(this) {
            tail[tailIdx++] = packet
            if (tailIdx == tailSize)
                tailIdx = 0
        }
    }

    fun tail(): Array<ByteArray> {
        synchronized(this) {
            val res = ArrayList<ByteArray>(0)
            var idx = tailIdx + 1
            for (i in 0 until tailSize) {
                if (idx == tailSize)
                    idx = 0
                val packet = tail[idx++]
                if (packet != null) {
                    res.add(packet)
                }
            }
            return res.toArray(Array(0) { ByteArray(0) })
        }
    }
}
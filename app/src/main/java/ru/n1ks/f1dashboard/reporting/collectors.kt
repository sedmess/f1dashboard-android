package ru.n1ks.f1dashboard.reporting

import android.util.Base64
import org.json.JSONArray

internal object ReportingKeys {
    const val LastPacketsData = "last_packets"
}

object PacketTail {

    private const val tailSize = 10
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

object ByteArraysJSONUtils {

    fun toJSON(byteArrays: Array<ByteArray>): String {
        val jsonArray = JSONArray()
        byteArrays.forEach { jsonArray.put(Base64.encode(it, Base64.NO_WRAP or Base64.DEFAULT).decodeToString()) }
        return jsonArray.toString()
    }

    fun fromJSON(json: String): Array<ByteArray> {
        val jsonArray = JSONArray(json)
        return generateSequence(0) { it + 1 }
            .take(jsonArray.length())
            .map { Base64.decode(jsonArray.getString(it), Base64.NO_WRAP or Base64.DEFAULT) }
            .toList().toTypedArray()
    }
}
package ru.n1ks.f1dashboard.capture

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LiveCaptureFrame(
    val timestamp: Long,
    val data: ByteArray
) {

    companion object {

        fun readFrom(inputStream: InputStream): LiveCaptureFrame? {
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES + Int.SIZE_BYTES).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                if (!readFromStream(inputStream)) {
                    return null
                }
            }
            val timestamp = buffer.long
            val dataSize = buffer.int
            val data = ByteBuffer.allocate(dataSize).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                if (!readFromStream(inputStream))
                    return null
            }.array()
            return LiveCaptureFrame(timestamp, data)
        }

        private fun ByteBuffer.readFromStream(inputStream: InputStream): Boolean {
            var pos = 0
            var remain = capacity()
            var read = 0
            do {
                pos += read
                remain -= read
                read = inputStream.read(array(), pos, remain)
                if (read == -1) {
                    return false
                }
            } while (read != remain)
            return true
        }
    }

    fun writeTo(outputStream: OutputStream) {
        ByteBuffer.allocate(Long.SIZE_BYTES + Int.SIZE_BYTES).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            putLong(timestamp)
            putInt(data.size)
            outputStream.write(array())
        }
        outputStream.write(data)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LiveCaptureFrame

        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        return timestamp.hashCode()
    }
}
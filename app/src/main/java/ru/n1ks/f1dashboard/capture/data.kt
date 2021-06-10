package ru.n1ks.f1dashboard.capture

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LiveCaptureFrame(
    val timestamp: Long,
    val data: ByteArray
) {

    companion object {

        fun flow(inputStream: InputStream): Flowable<LiveCaptureFrame> =
            Flowable.create<LiveCaptureFrame>(
                    {
                        var frame = readFrom(inputStream)
                        while (frame != null) {
                            it.onNext(frame)
                            frame = readFrom(inputStream)
                        }
                        it.onComplete()
                    },
                    BackpressureStrategy.ERROR
                )
                .doOnTerminate { inputStream.close() }

        private fun readFrom(inputStream: InputStream): LiveCaptureFrame? {
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES + Int.SIZE_BYTES).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                if (inputStream.read(array()) != capacity()) {
                    return null
                }
            }
            val timestamp = buffer.long
            val dataSize = buffer.int
            val data = ByteBuffer.allocate(dataSize).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                if (inputStream.read(array()) != capacity()) {
                    return null
                }
            }.array()
            return LiveCaptureFrame(timestamp, data)
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
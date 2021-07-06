package ru.n1ks.f1dashboard.capture

import android.content.Context
import android.net.Uri
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

class LiveCaptureFrame(
    val timestamp: Long,
    val data: ByteArray
) {

    companion object {

        private const val TAG = "LiveCaptureFrame"

        fun flow(captureInputStream: InputStream): Flowable<LiveCaptureFrame> {
            val inputStream = GZIPInputStream(BufferedInputStream(captureInputStream))
            Log.d(TAG, "open capture file ${captureInputStream.hashCode()}")
            return Flowable.create<LiveCaptureFrame>(
                {
                    try {
                        var frame = readFrom(inputStream)
                        while (frame != null && !it.isCancelled) {
                            it.onNext(frame)
                            frame = readFrom(inputStream)
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "can't read from stream: ${e.message}")
                    } finally {
                        it.onComplete()
                    }
                }, BackpressureStrategy.BUFFER
            )
                .doFinally { Log.d(TAG, "close capture file ${captureInputStream.hashCode()}"); captureInputStream.close() }
        }

        private fun readFrom(inputStream: InputStream): LiveCaptureFrame? {
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
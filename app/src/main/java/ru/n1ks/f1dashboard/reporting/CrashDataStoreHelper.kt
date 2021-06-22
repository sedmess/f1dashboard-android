package ru.n1ks.f1dashboard.reporting

import android.content.Context
import org.json.JSONObject
import java.io.*

class CrashDataStoreHelper(private val context: Context) {

    companion object {
        private const val filename = "last_crash_data"
    }

    fun store(records: Map<String, String>) {
        val file = File(context.filesDir, filename)
        if (file.exists()) {
            file.delete()
        }
        BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
            JSONObject(records).apply {
                outputStream.write(toString().encodeToByteArray())
            }
        }
    }

    fun load(): Map<String, String> {
        val file = File(context.filesDir, filename)
        if (!file.exists()) {
            return emptyMap()
        }
        BufferedInputStream(FileInputStream(file)).use { inputStream ->
            JSONObject(inputStream.readBytes().decodeToString()).apply {
                return keys().asSequence().map { it to getString(it) }.toMap()
            }
        }
    }

    fun delete() {
        val file = File(context.filesDir, filename)
        if (file.exists()) {
            file.delete()
        }
    }
}
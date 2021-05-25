package ru.n1ks.f1dashboard

import android.content.SharedPreferences
import androidx.core.content.edit

class Properties private constructor(val port: Int) {

    companion object {

        const val Name = "listener_properties"

        const val Port = "port"

        fun SharedPreferences.loadProperties(): Properties =
            Properties(this.getInt(Port, 20777))

        fun SharedPreferences.setPort(port: Int) = this.edit { putInt(Port, port) }
    }
}
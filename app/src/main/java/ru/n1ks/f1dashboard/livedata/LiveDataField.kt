package ru.n1ks.f1dashboard.livedata

import ru.n1ks.f1dashboard.model.TelemetryPacket

class LiveDataField<T : Any>(
    val name: String,
    private var dataProvider: () -> T,
    private val initUIFunc: ViewProvider.(T) -> Unit,
    private val extractDataFunc: (data: T, packet: TelemetryPacket<*>) -> T,
    private val onUpdateFunc: ViewProvider.(T) -> Unit
) {

    private lateinit var data: T

    fun init(viewProvider: ViewProvider) {
        this.data = dataProvider()
        this.initUIFunc(viewProvider, data)
    }

    fun onUpdate(viewProvider: ViewProvider, update: TelemetryPacket<*>) {
        val newData = extractDataFunc(this.data, update)
        if (newData == data)
            return
        data = newData
        onUpdateFunc(viewProvider, data)
    }
}
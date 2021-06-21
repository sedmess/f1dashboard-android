package ru.n1ks.f1dashboard.capture

interface Recorder {

    fun startRecording()

    fun stopRecording(): Long
}
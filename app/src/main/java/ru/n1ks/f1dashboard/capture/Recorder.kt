package ru.n1ks.f1dashboard.capture

interface Recorder {

    companion object {
        const val LastestCaptureFilename = "latest.cap"
    }

    fun startRecording()

    fun stopRecording(): Long
}
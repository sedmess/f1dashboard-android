package ru.n1ks.f1dashboard

import android.content.Intent
import android.net.Uri
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import ru.n1ks.f1dashboard.model.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReplayServiceTest {

    private lateinit var replayService: ReplayService

    @Before
    fun setup() {
        replayService = Robolectric.setupService(ReplayService::class.java)
    }

    @Test
    fun replayCaptureTest() {
        try {
            val captureFIS = startServiceForCaptureFile()

            var count = 0
            val typeCount = HashMap<KClass<out TelemetryData>, Int>()
            val waitLatch = CountDownLatch(1)
            val disposable = replayService.flow()
                .observeOn(Schedulers.single())
                .doFinally { waitLatch.countDown() }
                .doOnNext { count++ }
                .map { TelemetryPacketDeserializer.map(it) }
                .subscribe { assertNotNull(it); typeCount.compute(it.data::class) { _, v -> v?.plus(1) ?: 1 } }
            waitLatch.await()

            disposable.dispose()

            assertThrows("Stream closed", IOException::class.java) { captureFIS.available() }
            assertEquals(3571, count)
            assertEquals(55, typeCount[CarSetupDataPacket::class])
            assertEquals(864, typeCount[CarStatusDataPacket::class])
            assertEquals(864, typeCount[CarTelemetryDataPacket::class])
            assertEquals(   5, typeCount[ParticipantDataPacket::class])
            assertEquals(864, typeCount[LapDataPacket::class])
            assertEquals(864, typeCount[EmptyData::class])
        } finally {
            replayService.onUnbind(null)
        }
    }


    @Test
    fun replayCaptureFileCloseOnStopTest() {
        try {
            val captureFIS = startServiceForCaptureFile()
            replayService.onUnbind(null)
            assertThrows("Stream closed", IOException::class.java) { captureFIS.available() }
        } finally {
            replayService.onUnbind(null)
        }
    }

    private fun startServiceForCaptureFile(): FileInputStream {
        val captureFile = File(javaClass.classLoader?.getResource("replay/test.cap")!!.toURI())
        val captureUri = Uri.fromFile(captureFile)
        val captureFIS = FileInputStream(captureFile)
        assertTrue(captureFIS.available() > 0)

        Shadows.shadowOf(replayService.contentResolver).registerInputStream(captureUri, captureFIS)

        replayService.onBind(Intent().apply {
            putExtra(ReplayService.SourcePath, captureUri.toString())
            putExtra(ReplayService.NoDelays, true)
            putExtra(ReplayService.SourceType, ReplayService.SourceTypeCapture)
        })

        assertNotNull(replayService.flow())

        return captureFIS
    }
}
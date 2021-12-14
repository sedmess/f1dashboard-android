package ru.n1ks.f1dashboard.livedata

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import ru.n1ks.f1dashboard.*
import ru.n1ks.f1dashboard.model.*
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue


interface ViewProvider {

    fun <T : View> findViewById(id: Int): T
    fun getDrawable(id: Int): Drawable
    fun getColor(id: Int): Int
}

enum class DrsCommonState {
    Available, Unavailable, Upcoming, Fault
}

enum class PaceIndicator(val color: Int) {
    OverallBest(R.color.fastestTime), PersonalBest(R.color.betterTime), Worse(R.color.worseTime), NotSet(R.color.inop)
}

data class CompetitorDriver(
    val code: Int,
    val driver: ParticipantData.Driver
)

data class Competitor(
    val id: Int,
    val position: Int,
    val lastLapTime: Float,
    val bestLapTime: Float,
    val lap: Int,
    val visualTyreType: TyreCompound,
    val actualTyreType: TyreCompound,
    val tyreAge: Int,
    val driver: CompetitorDriver?
) {

    val positionString: String
        get() = position.toString().padEnd(2, ' ')

    val areTyresNew: Boolean
        get() = tyreAge < 3

    val typeDataValue: String
        get() = tyreAge.toString() + tyreTypeValue


    val tyreTyreColor: Int
        get() = if (visualTyreType != TyreCompound.X) visualTyreType.color else actualTyreType.color

    fun inBound(size: Int): Boolean = id in 0 until size

    private val tyreTypeValue: String
        get() {
            return if (visualTyreType == actualTyreType) {
                visualTyreType.char.toString()
            } else {
                when {
                    visualTyreType == TyreCompound.X -> actualTyreType.char.toString()
                    actualTyreType == TyreCompound.X -> visualTyreType.char.toString()
                    else -> visualTyreType.char.toString()
                }
            }
        }
}

data class TyreStateField(
    val wear: Int,
    val innerTemperature: Int,
    val outerTemperature: Int
) {

    val wearColor: Int
        get() {
            return when {
                wear < 5 -> R.color.wear0
                wear < 15 -> R.color.wear10
                wear < 23 -> R.color.wear20
                wear < 32 -> R.color.wear30
                wear < 40 -> R.color.wear40
                wear < 50 -> R.color.wear50
                wear < 60 -> R.color.wear60
                wear < 70 -> R.color.wear70
                wear < 80 -> R.color.wear80
                wear < 90 -> R.color.wear90
                wear <= 100 -> R.color.wear100
                else -> R.color.inop
            }
        }

    val wearValue: String
        get() {
            return if (wear < 0 || wear > 99) {
                "XX"
            } else {
                wear.toString()
            }
        }

    val innerTemperatureColor: Int
        get() = defineTempColor(innerTemperature)

    val outerTemperatureColor: Int
        get() = defineTempColor(outerTemperature)

    private fun defineTempColor(temp: Int): Int {
        return when {
            temp < 82 -> R.color.lowTemp
            temp < 103 -> R.color.normalTemp
            temp < 110 -> R.color.warmTemp
            else -> R.color.highTemp
        }
    }
}

data class SectorsIndicatorField(
    val s1Pace: PaceIndicator = PaceIndicator.NotSet,
    val s1Time: Int = 0,
    val s2Pace: PaceIndicator = PaceIndicator.NotSet,
    val s2Time: Int = 0,
    val s3Pace: PaceIndicator = PaceIndicator.NotSet,
    val s3Time: Int = 0
) {

    fun setS1Pace(pace: PaceIndicator, time: Int): SectorsIndicatorField =
        if (time == this.s1Time && pace == this.s1Pace) this else this.copy(s1Pace = pace, s1Time = time)

    fun setS2Pace(pace: PaceIndicator, time: Int): SectorsIndicatorField =
        if (time == this.s2Time && pace == this.s2Pace) this else this.copy(s2Pace = pace, s2Time = time)

    fun setS3Pace(pace: PaceIndicator, time: Int): SectorsIndicatorField =
        if (time == this.s3Time && pace == this.s3Pace) this else this.copy(s3Pace = pace, s3Time = time)
}

data class LapsField(
    val lapsCount: Int,
    val currentLap: Int
)

data class DrsField(
    val state: DrsCommonState,
    val isAllowed: Boolean,
    val isOpened: Boolean
)

data class RivalsField(
    val ahead2: Competitor?,
    val ahead: Competitor?,
    val player: Competitor?,
    val behind: Competitor?,
    val behind2: Competitor?
)

data class BestLapField(
    val competitorId: Int,
    val driver: ParticipantData.Driver?,
    val bestLapTime: Float
)

data class TypesField(
    val tyreFL: TyreStateField,
    val tyreFR: TyreStateField,
    val tyreRL: TyreStateField,
    val tyreRR: TyreStateField
)

private val secondsAndMsFormat: DecimalFormat = DecimalFormat("00.000")
private val secondsFormat: DecimalFormat = DecimalFormat("00")
private val fuelFormat: DecimalFormat = DecimalFormat("+#,#0.00;-#")
private const val timeNotSet = "X:XX.XXX"
private val timeFormatter: (Float) -> String = {
    val n1 = it.toInt() / 60
    val n2 = it % 60
    if (n1 == 0 && n2.absoluteValue < 0.001f) {
        timeNotSet
    } else {
        "${n1}:${secondsAndMsFormat.format(n2)}"
    }
}

class LiveData (
    private val activity: Activity
) : Disposable {

    private val compositeDisposable = CompositeDisposable()

    @SuppressLint("SetTextI18n")
    private val fields = listOf<LiveDataField<*>>(
        LiveDataField(
            "lapRemaining",
            { LapsField(0, 0) },
            {
                val lapField = findViewById<TextView>(R.id.lapValue)
                lapField.text = "X"
            },
            { data, packet ->
                packet.asType<SessionDataPacket> {
                    val totalLaps = it.data.totalLaps.toInt()
                    if (totalLaps != data.lapsCount)
                        return@LiveDataField data.copy(lapsCount = totalLaps)
                }
                packet.asType<LapDataPacket> {
                    val currentLap =
                        it.data.items[packet.header.playerCarIndex].currentLapNum.toInt()
                    if (currentLap != data.currentLap)
                        return@LiveDataField data.copy(currentLap = currentLap)
                }
                return@LiveDataField data
            },
            {
                val lapField = findViewById<TextView>(R.id.lapValue)
                lapField.text = if (it.lapsCount - it.currentLap >= 0) {
                    "+${it.lapsCount - it.currentLap}"
                } else {
                    "${it.currentLap}"
                }
            }
        ),
        LiveDataField(
            "fuelRemaining",
            { 0.0f },
            {
                val fuelField = findViewById<TextView>(R.id.fuelValue)
                fuelField.text = "+X.XX"
            },
            { data, packet ->
                packet.asType<CarStatusDataPacket> {
                    return@LiveDataField it.data.items[it.header.playerCarIndex].fuelRemainingLaps
                }
                return@LiveDataField data
            },
            {
                val fuelField = findViewById<TextView>(R.id.fuelValue)
                fuelField.text = fuelFormat.format(it)
            }
        ),
        LiveDataField(
            "fuelMixMode",
            { 0 },
            {
                val fuelField = findViewById<TextView>(R.id.fuelValue)
                fuelField.background = getDrawable(R.color.inop)
            },
            { data, packet ->
                packet.asType<CarStatusDataPacket> {
                    return@LiveDataField it.data.items[it.header.playerCarIndex].fuelMix
                }
                return@LiveDataField data
            },
            {
                val fuelField = findViewById<TextView>(R.id.fuelValue)
                when (it) {
                    1 -> {
                        fuelField.background = getDrawable(R.color.leanMode)
                    }
                    4 -> {
                        fuelField.background = getDrawable(R.color.fastestMode)
                    }
                    3 -> {
                        fuelField.background = getDrawable(R.color.highMode)
                    }
                    else -> {
                        fuelField.background = getDrawable(R.color.normalMode)
                    }
                }
            }
        ),
        LiveDataField(
            "ersMode",
            { -1 },
            {
                val ersField = findViewById<TextView>(R.id.ersValue)
                ersField.text = "X"
                ersField.background = getDrawable(R.color.inop)
            },
            { data, packet ->
                packet.asType<CarStatusDataPacket> {
                    return@LiveDataField it.data.items[it.header.playerCarIndex].ersDeployMode.toInt()
                }
                return@LiveDataField data
            },
            {
                val ersField = findViewById<TextView>(R.id.ersValue)
                ersField.text = it.toString()
                when (it) {
                    0 -> {
                        ersField.background = getDrawable(R.color.leanMode)
                    }
                    2 -> {
                        ersField.background = getDrawable(R.color.highMode)
                    }
                    3 -> {
                        ersField.background = getDrawable(R.color.fastestMode)
                    }
                    else -> {
                        ersField.background = getDrawable(R.color.normalMode)
                    }
                }
            }
        ),
        LiveDataField(
            "bb",
            { 0 },
            {
                val bbField = findViewById<TextView>(R.id.bbValue)
                bbField.text = "X"
            },
            { data, packet ->
                packet.asType<CarSetupDataPacket> {
                    return@LiveDataField it.data.items[it.header.playerCarIndex].brakeBias.toInt()
                }
                return@LiveDataField data
            },
            {
                val bbField = findViewById<TextView>(R.id.bbValue)
                bbField.text = it.toString()
                bbField.background = getDrawable(R.color.warn)
                val tag = System.nanoTime()
                bbField.tag = tag
                Single.just(bbField)
                    .delay(1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { field ->
                        if (field.tag == tag) field.background = null
                    }
                    .addTo(compositeDisposable)
            }
        ),
        LiveDataField(
            "diff",
            { 0 },
            {
                val diffField = findViewById<TextView>(R.id.diffValue)
                diffField.text = "X"
            },
            { data, packet ->
                packet.asType<CarSetupDataPacket> {
                    return@LiveDataField it.data.items[it.header.playerCarIndex].onThrottle.toInt()
                }
                return@LiveDataField data
            },
            {
                val diffField = findViewById<TextView>(R.id.diffValue)
                diffField.text = it.toString()
                diffField.background = getDrawable(R.color.warn)
                val tag = System.nanoTime()
                diffField.tag = tag
                Single.just(diffField)
                    .delay(1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { field ->
                        if (field.tag == tag) field.background = null
                    }
                    .addTo(compositeDisposable)
            }
        ),
        LiveDataField(
            "sectorIndicator",
            { SectorsIndicatorField() },
            {
                findViewById<TextView>(R.id.sector1Value).apply {
                    background = getDrawable(R.color.inop)
                    text = ""
                }
                findViewById<TextView>(R.id.sector2Value).apply {
                    background = getDrawable(R.color.inop)
                    text = ""
                }
                findViewById<TextView>(R.id.sector3Value).apply {
                    background = getDrawable(R.color.inop)
                    text = ""
                }
            },
            { data, packet ->
                packet.asType<LapDataPacket> {
                    val playerData = it.data.items[it.header.playerCarIndex]
                    if (playerData.driverStatus != LapData.DriverStatus.FlyingLap && playerData.driverStatus != LapData.DriverStatus.OnTrack) {
                        return@LiveDataField SectorsIndicatorField()
                    }
                    if (playerData.sector == Bytes.Zero) {
                        if (playerData.currentLapNum < 2) {
                            return@LiveDataField data
                        }
                        val s3Time =
                            (playerData.lastLapTime * 1000 - playerData.sector1TimeInMS - playerData.sector2TimeInMS).toInt()
                        return@LiveDataField data.setS3Pace(if (s3Time <= playerData.bestLapSector3TimeInMS) PaceIndicator.PersonalBest else PaceIndicator.Worse, s3Time)
                    }
                    if (playerData.sector == Bytes.One) {
                        return@LiveDataField data.setS1Pace(if (playerData.bestLapSector1TimeInMS <= 0 || playerData.sector1TimeInMS <= playerData.bestLapSector1TimeInMS) PaceIndicator.PersonalBest else PaceIndicator.Worse, playerData.sector1TimeInMS).setS2Pace(PaceIndicator.NotSet, 0).setS3Pace(PaceIndicator.NotSet, 0)
                    }
                    if (playerData.sector == Bytes.Two) {
                        return@LiveDataField data.setS2Pace(if (playerData.bestLapSector2TimeInMS <= 0 || playerData.sector2TimeInMS <= playerData.bestLapSector2TimeInMS) PaceIndicator.PersonalBest else PaceIndicator.Worse, playerData.sector2TimeInMS).setS3Pace(PaceIndicator.NotSet, 0)
                    }
                }
                return@LiveDataField data
            },
            {
                findViewById<TextView>(R.id.sector1Value).apply {
                    background = getDrawable(it.s1Pace.color)
                    text = timeFormatter(it.s1Time.toFloat() / 1000)
                }
                findViewById<TextView>(R.id.sector2Value).apply {
                    background = getDrawable(it.s2Pace.color)
                    text = timeFormatter(it.s2Time.toFloat() / 1000)
                }
                findViewById<TextView>(R.id.sector3Value).apply {
                    background = getDrawable(it.s3Pace.color)
                    text = timeFormatter(it.s3Time.toFloat() / 1000)
                }
            }
        ),
        LiveDataField(
            "recommendedGear",
            { 0 },
            {
                val recommendedGearField = findViewById<TextView>(R.id.recommendedGearValue)
                recommendedGearField.text = ""
            },
            { data, packet ->
                packet.asType<CarTelemetryDataPacket> {
                    return@LiveDataField it.data.suggestedGear.toInt()
                }
                return@LiveDataField data
            },
            {
                val recommendedGearField = findViewById<TextView>(R.id.recommendedGearValue)
                if (it > 0) {
                    recommendedGearField.text = it.toString()
                } else {
                    recommendedGearField.text = ""
                }
            }
        ),
        LiveDataField(
            "drsState",
            { DrsField(DrsCommonState.Unavailable, isAllowed = false, isOpened = false) },
            {
                val drsField = findViewById<TextView>(R.id.drsValue)
                drsField.text = "-"
                drsField.background = null
            },
            { data, packet ->
                packet.asType<CarStatusDataPacket> {
                    val carStatusData = it.data.items[it.header.playerCarIndex]
                    when {
                        carStatusData.drsFault -> {
                            if (data.state != DrsCommonState.Fault)
                                return@LiveDataField data.copy(state = DrsCommonState.Fault)
                        }
                        carStatusData.drsAvailable -> {
                            if (data.state != DrsCommonState.Upcoming)
                                return@LiveDataField data.copy(state = DrsCommonState.Upcoming)
                        }
                        else -> {
                            if (data.state != DrsCommonState.Available || data.isAllowed != carStatusData.drsAllowed) {
                                return@LiveDataField data.copy(
                                    state = DrsCommonState.Available,
                                    isAllowed = carStatusData.drsAllowed
                                )
                            }
                        }
                    }
                }
                packet.asType<CarTelemetryDataPacket> {
                    val isOpened = it.data.items[it.header.playerCarIndex].drs
                    if (data.isOpened != isOpened)
                        return@LiveDataField data.copy(isOpened = isOpened)
                }
                return@LiveDataField data
            },
            {
                val drsField = findViewById<TextView>(R.id.drsValue)
                when (it.state) {
                    DrsCommonState.Unavailable -> {
                        drsField.text = "X"
                        if (it.isOpened)
                            drsField.background = null
                        else
                            drsField.background = getDrawable(R.color.warn)
                    }
                    DrsCommonState.Fault -> {
                        drsField.text = "X"
                        drsField.background = getDrawable(R.color.drsFault)
                    }
                    DrsCommonState.Upcoming -> {
                        drsField.text = "↑"
                        drsField.background = getDrawable(R.color.drsUpcoming)
                    }
                    DrsCommonState.Available -> {
                        if (it.isAllowed && !it.isOpened) {
                            drsField.text = "-"
                            drsField.background = getDrawable(R.color.warn)
                        } else {
                            drsField.text = "-"
                            drsField.background = null
                        }
                    }
                }
            }
        ),
        LiveDataField(
            "rivals",
            { RivalsField(ahead2 = null, ahead = null, player = null, behind = null, behind2 = null) },
            {
                findViewById<TextView>(R.id.myBestValue).apply {
                    text = timeNotSet
                }
                sequenceOf(R.id.aheadDriverValue, R.id.ahead2DriverValue, R.id.behindDriverValue, R.id.behind2DriverValue).map { findViewById<TextView>(it) }
                    .forEach {
                        it.text = "X"
                    }
                sequenceOf(R.id.myTimeValue, R.id.aheadTimeValue, R.id.ahead2TimeValue, R.id.behindTimeValue, R.id.behind2TimeValue).map { findViewById<TextView>(it) }
                    .forEach {
                        it.text = timeNotSet
                        it.setTextColor(getColor(R.color.white))
                    }
                sequenceOf(R.id.myTyreValue, R.id.aheadTyreValue, R.id.ahead2TyreValue, R.id.behindTyreValue, R.id.behind2TyreValue).map { findViewById<TextView>(it) }
                    .forEach {
                        it.text = "X"
                        it.setTextColor(getColor(R.color.white))
                        it.background = null
                    }
            },
            { data, packet ->
                packet.asType<LapDataPacket> { it ->
                    val playerData = it.data.items[it.header.playerCarIndex]
                    val playerIndex =
                        it.data.items.indexOfFirst { item -> item.carPosition == playerData.carPosition }
                    val rivalAheadIndex =
                        it.data.items.indexOfFirst { item -> item.carPosition == playerData.carPosition minusByte Bytes.One }
                    val rivalAheadData = it.data.items.getOrNull(rivalAheadIndex)
                    val rivalAhead2Index =
                        it.data.items.indexOfFirst { item -> item.carPosition == playerData.carPosition minusByte Bytes.Two }
                    val rivalAhead2Data = it.data.items.getOrNull(rivalAhead2Index)
                    val rivalBehindIndex =
                        it.data.items.indexOfFirst { item -> item.carPosition == playerData.carPosition plusByte Bytes.One }
                    val rivalBehindData = it.data.items.getOrNull(rivalBehindIndex)
                    val rivalBehind2Index =
                        it.data.items.indexOfFirst { item -> item.carPosition == playerData.carPosition plusByte Bytes.Two }
                    val rivalBehind2Data = it.data.items.getOrNull(rivalBehind2Index)

                    val player = Competitor(
                        id = playerIndex,
                        position = playerData.carPosition.toInt(),
                        lastLapTime = playerData.lastLapTime,
                        bestLapTime = playerData.bestLapTime,
                        lap = playerData.currentLapNum.toInt(),
                        visualTyreType = data.player?.visualTyreType ?: TyreCompound.X,
                        actualTyreType = data.player?.actualTyreType ?: TyreCompound.X,
                        tyreAge = data.player?.tyreAge ?: Int.MAX_VALUE,
                        driver = null
                    )
                    val ahead = rivalAheadData?.let {
                        Competitor(
                            id = rivalAheadIndex,
                            position = it.carPosition.toInt(),
                            lastLapTime = it.lastLapTime,
                            bestLapTime = it.bestLapTime,
                            lap = it.currentLapNum.toInt(),
                            visualTyreType = if (data.ahead?.id == rivalAheadIndex) data.ahead.visualTyreType else TyreCompound.X,
                            actualTyreType = if (data.ahead?.id == rivalAheadIndex) data.ahead.actualTyreType else TyreCompound.X,
                            tyreAge = data.ahead?.tyreAge ?: Int.MAX_VALUE,
                            driver = if (data.ahead?.id == rivalAheadIndex) data.ahead.driver else null
                        )
                    }
                    val ahead2 = rivalAhead2Data?.let {
                        Competitor(
                            id = rivalAhead2Index,
                            position = it.carPosition.toInt(),
                            lastLapTime = it.lastLapTime,
                            bestLapTime = it.bestLapTime,
                            lap = it.currentLapNum.toInt(),
                            visualTyreType = if (data.ahead2?.id == rivalAhead2Index) data.ahead2.visualTyreType else TyreCompound.X,
                            actualTyreType = if (data.ahead2?.id == rivalAhead2Index) data.ahead2.actualTyreType else TyreCompound.X,
                            tyreAge = data.ahead2?.tyreAge ?: Int.MAX_VALUE,
                            driver = if (data.ahead2?.id == rivalAhead2Index) data.ahead2.driver else null
                        )
                    }
                    val behind = rivalBehindData?.let {
                        Competitor(
                            id = rivalBehindIndex,
                            position = it.carPosition.toInt(),
                            lastLapTime = it.lastLapTime,
                            bestLapTime = it.bestLapTime,
                            lap = it.currentLapNum.toInt(),
                            visualTyreType = if (data.behind?.id == rivalBehindIndex) data.behind.visualTyreType else TyreCompound.X,
                            actualTyreType = if (data.behind?.id == rivalBehindIndex) data.behind.actualTyreType else TyreCompound.X,
                            tyreAge = data.behind?.tyreAge ?: Int.MAX_VALUE,
                            driver = if (data.behind?.id == rivalBehindIndex) data.behind.driver else null
                        )
                    }
                    val behind2 = rivalBehind2Data?.let {
                        Competitor(
                            id = rivalBehind2Index,
                            position = it.carPosition.toInt(),
                            lastLapTime = it.lastLapTime,
                            bestLapTime = it.bestLapTime,
                            lap = it.currentLapNum.toInt(),
                            visualTyreType = if (data.behind2?.id == rivalBehind2Index) data.behind2.visualTyreType else TyreCompound.X,
                            actualTyreType = if (data.behind2?.id == rivalBehind2Index) data.behind2.actualTyreType else TyreCompound.X,
                            tyreAge = data.behind2?.tyreAge ?: Int.MAX_VALUE,
                            driver = if (data.behind2?.id == rivalBehind2Index) data.behind2.driver else null
                        )
                    }
                    return@LiveDataField RivalsField(ahead2, ahead, player, behind, behind2)
                }
                packet.asType<ParticipantDataPacket> {
                    var newData = data
                    if (data.ahead?.inBound(it.data.items.size) == true) {
                        newData = newData.copy(
                            ahead = data.ahead.copy(
                                driver = CompetitorDriver(
                                    it.data.items[data.ahead.id].raceNumber.toInt(),
                                    it.data.items[data.ahead.id].driver
                                )
                            )
                        )
                    }
                    if (data.ahead2?.inBound(it.data.items.size) == true) {
                        newData = newData.copy(
                            ahead2 = data.ahead2.copy(
                                driver = CompetitorDriver(
                                    it.data.items[data.ahead2.id].raceNumber.toInt(),
                                    it.data.items[data.ahead2.id].driver
                                )
                            )
                        )
                    }
                    if (data.behind?.inBound(it.data.items.size) == true) {
                        newData = newData.copy(
                            behind = data.behind.copy(
                                driver = CompetitorDriver(
                                    it.data.items[data.behind.id].raceNumber.toInt(),
                                    it.data.items[data.behind.id].driver
                                )
                            )
                        )
                    }
                    if (data.behind2?.inBound(it.data.items.size) == true) {
                        newData = newData.copy(
                            behind2 = data.behind2.copy(
                                driver = CompetitorDriver(
                                    it.data.items[data.behind2.id].raceNumber.toInt(),
                                    it.data.items[data.behind2.id].driver
                                )
                            )
                        )
                    }
                    return@LiveDataField newData
                }
                packet.asType<CarStatusDataPacket> {
                    var newData = data
                    if (data.player?.inBound(it.data.items.size) == true) {
                        newData = data.copy(
                            player = data.player.copy(
                                visualTyreType = it.data.items[data.player.id].visualTyreCompound,
                                actualTyreType = it.data.items[data.player.id].actualTyreCompound,
                                tyreAge = it.data.items[data.player.id].tyresAgeLaps.toInt()
                            )
                        )
                    }
                    if (data.ahead?.inBound(it.data.items.size) == true) {
                        newData = newData.copy(
                            ahead = data.ahead.copy(
                                visualTyreType = it.data.items[data.ahead.id].visualTyreCompound,
                                actualTyreType = it.data.items[data.ahead.id].actualTyreCompound,
                                tyreAge = it.data.items[data.ahead.id].tyresAgeLaps.toInt()
                            )
                        )
                    }
                    if (data.ahead2?.inBound(it.data.items.size) == true) {
                        newData = newData.copy(
                            ahead2 = data.ahead2.copy(
                                visualTyreType = it.data.items[data.ahead2.id].visualTyreCompound,
                                actualTyreType = it.data.items[data.ahead2.id].actualTyreCompound,
                                tyreAge = it.data.items[data.ahead2.id].tyresAgeLaps.toInt()
                            )
                        )
                    }
                    if (data.behind?.inBound(it.data.items.size) == true) {
                        newData = newData.copy(
                            behind = data.behind.copy(
                                visualTyreType = it.data.items[data.behind.id].visualTyreCompound,
                                actualTyreType = it.data.items[data.behind.id].actualTyreCompound,
                                tyreAge = it.data.items[data.behind.id].tyresAgeLaps.toInt()
                            )
                        )
                    }
                    if (data.behind2?.inBound(it.data.items.size) == true) {
                        newData = newData.copy(
                            behind2 = data.behind2.copy(
                                visualTyreType = it.data.items[data.behind2.id].visualTyreCompound,
                                actualTyreType = it.data.items[data.behind2.id].actualTyreCompound,
                                tyreAge = it.data.items[data.behind2.id].tyresAgeLaps.toInt()
                            )
                        )
                    }
                    return@LiveDataField newData
                }
                return@LiveDataField data
            },
            { context ->
                run {
                    val aheadDriverField = findViewById<TextView>(R.id.aheadDriverValue)
                    val aheadTimeField = findViewById<TextView>(R.id.aheadTimeValue)
                    val aheadTyreField = findViewById<TextView>(R.id.aheadTyreValue)

                    if (context.ahead != null && context.ahead.position > 0) {
                        aheadDriverField.text =
                            context.ahead.positionString + context.ahead.driver.let { if (it != null) " ${it.driver.name}" else "" }
                        aheadTimeField.text =
                            timeFormatter(context.ahead.lastLapTime)

                        when {
                            context.ahead.lastLapTime < context.player?.lastLapTime ?: Float.MIN_VALUE -> aheadTimeField.setTextColor(
                                getColor(R.color.timeBetter)
                            )
                            context.ahead.lastLapTime > context.player?.lastLapTime ?: Float.MAX_VALUE -> aheadTimeField.setTextColor(
                                getColor(R.color.timeWorse)
                            )
                            else -> aheadTimeField.setTextColor(getColor(R.color.white))
                        }

                        aheadTyreField.text = context.ahead.typeDataValue
                        aheadTyreField.setTextColor(getColor(context.ahead.tyreTyreColor))
                        aheadTyreField.background =
                            if (context.ahead.areTyresNew) getDrawable(R.color.tyreNew) else null
                    } else {
                        aheadDriverField.text = "X"
                        aheadTimeField.text = timeNotSet
                        aheadTimeField.setTextColor(getColor(R.color.white))
                        aheadTyreField.text = "X"
                        aheadTyreField.setTextColor(getColor(R.color.white))
                        aheadTyreField.background = null
                    }
                }

                run {
                    val ahead2DriverField = findViewById<TextView>(R.id.ahead2DriverValue)
                    val ahead2TimeField = findViewById<TextView>(R.id.ahead2TimeValue)
                    val ahead2TyreField = findViewById<TextView>(R.id.ahead2TyreValue)

                    if (context.ahead2 != null && context.ahead2.position > 0) {
                        ahead2DriverField.text =
                            context.ahead2.positionString + context.ahead2.driver.let { if (it != null) " ${it.driver.name}" else "" }
                        ahead2TimeField.text =
                            timeFormatter(context.ahead2.lastLapTime)

                        when {
                            context.ahead2.lastLapTime < context.player?.lastLapTime ?: Float.MIN_VALUE -> ahead2TimeField.setTextColor(
                                getColor(R.color.timeBetter)
                            )
                            context.ahead2.lastLapTime > context.player?.lastLapTime ?: Float.MAX_VALUE -> ahead2TimeField.setTextColor(
                                getColor(R.color.timeWorse)
                            )
                            else -> ahead2TimeField.setTextColor(getColor(R.color.white))
                        }

                        ahead2TyreField.text = context.ahead2.typeDataValue
                        ahead2TyreField.setTextColor(getColor(context.ahead2.tyreTyreColor))
                        ahead2TyreField.background =
                            if (context.ahead2.areTyresNew) getDrawable(R.color.tyreNew) else null
                    } else {
                        ahead2DriverField.text = "X"
                        ahead2TimeField.text = timeNotSet
                        ahead2TimeField.setTextColor(getColor(R.color.white))
                        ahead2TyreField.text = "X"
                        ahead2TyreField.setTextColor(getColor(R.color.white))
                        ahead2TyreField.background = null
                    }
                }

                run {
                    val playerBestTimeField = findViewById<TextView>(R.id.myBestValue)
                    val playerLastTimeField = findViewById<TextView>(R.id.myTimeValue)
                    val playerTyreField = findViewById<TextView>(R.id.myTyreValue)

                    if (context.player != null) {
                        playerBestTimeField.text = timeFormatter(context.player.bestLapTime)
                        playerLastTimeField.text = timeFormatter(context.player.lastLapTime)

                        if (context.player.lap < context.ahead?.lap ?: context.player.lap) {
                            playerLastTimeField.setTextColor(getColor(R.color.timeIrrelevant))
                        } else {
                            playerLastTimeField.setTextColor(getColor(R.color.white))
                        }

                        playerTyreField.text = context.player.typeDataValue
                        playerTyreField.setTextColor(getColor(context.player.tyreTyreColor))
                        playerTyreField.background =
                            if (context.player.areTyresNew) getDrawable(R.color.tyreNew) else null
                    } else {
                        playerBestTimeField.text = timeNotSet
                        playerLastTimeField.text = timeNotSet
                        playerTyreField.text = "X"
                        playerTyreField.setTextColor(getColor(R.color.white))
                        playerTyreField.background = null
                    }
                }

                run {
                    val behindDriverField = findViewById<TextView>(R.id.behindDriverValue)
                    val behindTimeField = findViewById<TextView>(R.id.behindTimeValue)
                    val behindTyreFiled = findViewById<TextView>(R.id.behindTyreValue)

                    if (context.behind != null) {
                        behindDriverField.text =
                            context.behind.positionString + context.behind.driver.let { if (it != null) " ${it.driver.name}" else "" }
                        behindTimeField.text = timeFormatter(context.behind.lastLapTime)

                        if (context.behind.lap < context.player?.lap ?: context.behind.lap) {
                            behindTimeField.setTextColor(getColor(R.color.timeIrrelevant))
                        } else {
                            when {
                                context.behind.lastLapTime < context.player?.lastLapTime ?: Float.MIN_VALUE -> behindTimeField.setTextColor(
                                    getColor(R.color.timeBetter)
                                )
                                context.behind.lastLapTime > context.player?.lastLapTime ?: Float.MAX_VALUE -> behindTimeField.setTextColor(
                                    getColor(R.color.timeWorse)
                                )
                                else -> behindTimeField.setTextColor(getColor(R.color.white))
                            }
                        }

                        behindTyreFiled.text = context.behind.typeDataValue
                        behindTyreFiled.setTextColor(getColor(context.behind.tyreTyreColor))
                        behindTyreFiled.background =
                            if (context.behind.areTyresNew) getDrawable(R.color.tyreNew) else null

                    } else {
                        behindDriverField.text = "X"
                        behindTimeField.text = timeNotSet
                        behindTimeField.setTextColor(getColor(R.color.white))
                        behindTyreFiled.text = "X"
                        behindTyreFiled.setTextColor(getColor(R.color.white))
                        behindTyreFiled.background = null
                    }
                }

                run {
                    val behind2DriverField = findViewById<TextView>(R.id.behind2DriverValue)
                    val behind2TimeField = findViewById<TextView>(R.id.behind2TimeValue)
                    val behind2TyreFiled = findViewById<TextView>(R.id.behind2TyreValue)

                    if (context.behind2 != null) {
                        behind2DriverField.text =
                            context.behind2.positionString + context.behind2.driver.let { if (it != null) " ${it.driver.name}" else "" }
                        behind2TimeField.text = timeFormatter(context.behind2.lastLapTime)

                        if (context.behind2.lap < context.player?.lap ?: context.behind2.lap) {
                            behind2TimeField.setTextColor(getColor(R.color.timeIrrelevant))
                        } else {
                            when {
                                context.behind2.lastLapTime < context.player?.lastLapTime ?: Float.MIN_VALUE -> behind2TimeField.setTextColor(
                                    getColor(R.color.timeBetter)
                                )
                                context.behind2.lastLapTime > context.player?.lastLapTime ?: Float.MAX_VALUE -> behind2TimeField.setTextColor(
                                    getColor(R.color.timeWorse)
                                )
                                else -> behind2TimeField.setTextColor(getColor(R.color.white))
                            }
                        }

                        behind2TyreFiled.text = context.behind2.typeDataValue
                        behind2TyreFiled.setTextColor(getColor(context.behind2.tyreTyreColor))
                        behind2TyreFiled.background =
                            if (context.behind2.areTyresNew) getDrawable(R.color.tyreNew) else null
                    } else {
                        behind2DriverField.text = "X"
                        behind2TimeField.text = timeNotSet
                        behind2TimeField.setTextColor(getColor(R.color.white))
                        behind2TyreFiled.text = "X"
                        behind2TyreFiled.setTextColor(getColor(R.color.white))
                        behind2TyreFiled.background = null
                    }
                }
            }
        ),
        LiveDataField(
            "bestTime",
            { BestLapField(
                competitorId = -1,
                driver = null,
                bestLapTime = 0.0f
            ) },
            {
                findViewById<TextView>(R.id.bestSessionTime).text = timeNotSet
            },
            { data, packet ->
                packet.asType<LapDataPacket> {
                    val bestLapTime = it.data.items.filter { it.bestLapTime > 0 }
                        .minOfOrNull { it.bestLapTime }
                    if (bestLapTime != null && (data.competitorId == -1 || data.bestLapTime > bestLapTime)) {
                        return@LiveDataField data.copy(
                            competitorId = -1,
                            driver = null,
                            bestLapTime = it.data.items.filter { it.bestLapTime > 0 }
                                .minOfOrNull { it.bestLapTime } ?: 0.0f
                        )
                    }
                }
                packet.asType<EventDataPacket> {
                    if (it.data.eventType == EventDetails.Type.SessionStarted) {
                        return@LiveDataField BestLapField(
                            competitorId = -1,
                            driver = null,
                            bestLapTime = 0.0f
                        )
                    }
                    it.data.asType<FastestLapData> { event ->
                        val cid = event.vehicleIdx.toInt()
                        return@LiveDataField data.copy(
                            competitorId = cid,
                            driver = if (data.competitorId == cid) data.driver else null,
                            bestLapTime = event.lapTime
                        )
                    }
                }
                packet.asType<ParticipantDataPacket> {
                    if (data.competitorId >= 0 && data.competitorId < it.data.items.size) {
                        return@LiveDataField data.copy(
                            driver = it.data.items[data.competitorId].driver
                        )
                    }
                }
                return@LiveDataField data
            },
            {
                findViewById<TextView>(R.id.bestSessionTime).text =
                    timeFormatter(it.bestLapTime) + " " + (it.driver?.name ?: "")
            }
        ),
        LiveDataField(
            "tyres",
            {
                TypesField(
                    tyreFL = TyreStateField(0, 0, 0),
                    tyreFR = TyreStateField(0, 0, 0),
                    tyreRL = TyreStateField(0, 0, 0),
                    tyreRR = TyreStateField(0, 0, 0)
                )
            },
            {
                sequenceOf(
                    R.id.surfaceFLValue,
                    R.id.surfaceFRValue,
                    R.id.surfaceRLValue,
                    R.id.surfaceRRValue,
                    R.id.innerFLValue,
                    R.id.innerFRValue,
                    R.id.innerRLValue,
                    R.id.innerRRValue
                )
                    .map { findViewById<TextView>(it) }
                    .forEach {
                        it.background = getDrawable(R.color.inop)
                    }
                sequenceOf(
                    R.id.wearFLValue,
                    R.id.wearFRValue,
                    R.id.wearRLValue,
                    R.id.wearRRValue,
                )
                    .map { findViewById<TextView>(it) }
                    .forEach {
                        it.text = "X"
                        it.background = getDrawable(R.color.inop)
                    }
            },
            { data, packet ->
                packet.asType<CarTelemetryDataPacket> {
                    val carTelemetryData = it.data.items[it.header.playerCarIndex]

                    var tyreFL = data.tyreFL

                    if (carTelemetryData.tyresSurfaceTemperatureFL != tyreFL.outerTemperature)
                        tyreFL =
                            tyreFL.copy(outerTemperature = carTelemetryData.tyresSurfaceTemperatureFL)
                    if (carTelemetryData.tyresInnerTemperatureFL != tyreFL.innerTemperature)
                        tyreFL =
                            tyreFL.copy(innerTemperature = carTelemetryData.tyresInnerTemperatureFL)

                    var tyreFR = data.tyreFR

                    if (carTelemetryData.tyresSurfaceTemperatureFR != tyreFR.outerTemperature)
                        tyreFR =
                            tyreFR.copy(outerTemperature = carTelemetryData.tyresSurfaceTemperatureFR)
                    if (carTelemetryData.tyresInnerTemperatureFR != tyreFR.innerTemperature)
                        tyreFR =
                            tyreFR.copy(innerTemperature = carTelemetryData.tyresInnerTemperatureFR)

                    var tyreRL = data.tyreRL

                    if (carTelemetryData.tyresSurfaceTemperatureRL != tyreRL.outerTemperature)
                        tyreRL =
                            tyreRL.copy(outerTemperature = carTelemetryData.tyresSurfaceTemperatureRL)
                    if (carTelemetryData.tyresInnerTemperatureRL != tyreRL.innerTemperature)
                        tyreRL =
                            tyreRL.copy(innerTemperature = carTelemetryData.tyresInnerTemperatureRL)

                    var tyreRR = data.tyreRR

                    if (carTelemetryData.tyresSurfaceTemperatureRR != tyreRR.outerTemperature)
                        tyreRR =
                            tyreRR.copy(outerTemperature = carTelemetryData.tyresSurfaceTemperatureRR)
                    if (carTelemetryData.tyresInnerTemperatureRR != tyreRR.innerTemperature)
                        tyreRR =
                            tyreRR.copy(innerTemperature = carTelemetryData.tyresInnerTemperatureRR)

                    return@LiveDataField TypesField(tyreFL, tyreFR, tyreRL, tyreRR)
                }
                packet.asType<CarStatusDataPacket> {
                    val carStatusData = it.data.items[it.header.playerCarIndex]

                    var tyreFL = data.tyreFL

                    if (carStatusData.tyresWearFL != tyreFL.wear)
                        tyreFL = tyreFL.copy(wear = carStatusData.tyresWearFL)

                    var tyreFR = data.tyreFR

                    if (carStatusData.tyresWearFR != tyreFR.wear)
                        tyreFR = tyreFR.copy(wear = carStatusData.tyresWearFR)

                    var tyreRL = data.tyreRL

                    if (carStatusData.tyresWearRL != tyreRL.wear)
                        tyreRL = tyreRL.copy(wear = carStatusData.tyresWearRL)

                    var tyreRR = data.tyreRR

                    if (carStatusData.tyresWearRR != tyreRR.wear)
                        tyreRR = tyreRR.copy(wear = carStatusData.tyresWearRR)

                    return@LiveDataField TypesField(tyreFL, tyreFR, tyreRL, tyreRR)
                }
                return@LiveDataField data
            },
            {
                findViewById<TextView>(R.id.wearFLValue).apply {
                    background = getDrawable(it.tyreFL.wearColor)
                    text = it.tyreFL.wearValue
                    setTextColor(Colors.complimentColor(getColor(it.tyreFL.wearColor)))
                }
                findViewById<TextView>(R.id.wearFRValue).apply {
                    background = getDrawable(it.tyreFR.wearColor)
                    text = it.tyreFR.wearValue
                    setTextColor(Colors.complimentColor(getColor(it.tyreFR.wearColor)))
                }
                findViewById<TextView>(R.id.wearRLValue).apply {
                    background = getDrawable(it.tyreRL.wearColor)
                    text = it.tyreRL.wearValue
                    setTextColor(Colors.complimentColor(getColor(it.tyreRL.wearColor)))
                }
                findViewById<TextView>(R.id.wearRRValue).apply {
                    background = getDrawable(it.tyreRR.wearColor)
                    text = it.tyreRR.wearValue
                    setTextColor(Colors.complimentColor(getColor(it.tyreRR.wearColor)))
                }

                findViewById<View>(R.id.surfaceFLValue).background =
                    getDrawable(it.tyreFL.outerTemperatureColor)
                findViewById<View>(R.id.surfaceFRValue).background =
                    getDrawable(it.tyreFR.outerTemperatureColor)
                findViewById<View>(R.id.surfaceRLValue).background =
                    getDrawable(it.tyreRL.outerTemperatureColor)
                findViewById<View>(R.id.surfaceRRValue).background =
                    getDrawable(it.tyreRR.outerTemperatureColor)

                findViewById<View>(R.id.innerFLValue).background =
                    getDrawable(it.tyreFL.innerTemperatureColor)
                findViewById<View>(R.id.innerFRValue).background =
                    getDrawable(it.tyreFR.innerTemperatureColor)
                findViewById<View>(R.id.innerRLValue).background =
                    getDrawable(it.tyreRL.innerTemperatureColor)
                findViewById<View>(R.id.innerRRValue).background =
                    getDrawable(it.tyreRR.innerTemperatureColor)
            }
        ),
        LiveDataField(
            "frontWing",
            { arrayOf(0, 0) },
            {
                sequenceOf(R.id.frontWingLeftDamage, R.id.frontWingRightDamage).map { findViewById<TextView>(it) }
                    .forEach {
                        it.text = ""
                        it.background = null
                    }
            },
            { data, packet ->
                packet.asType<CarStatusDataPacket> {
                    return@LiveDataField arrayOf(
                        it.data.items[it.header.playerCarIndex].frontLeftWingDamage.toInt(),
                        it.data.items[it.header.playerCarIndex].frontRightWingDamage.toInt()
                    )
                }
                return@LiveDataField data
            },
            {
                val frontLeftWingField = findViewById<TextView>(R.id.frontWingLeftDamage)
                val frontRightWingField = findViewById<TextView>(R.id.frontWingRightDamage)
                if (it[0] > 0) {
                    frontLeftWingField.text = it[0].toString()
                    frontLeftWingField.background = getDrawable(R.color.warn)
                } else {
                    frontLeftWingField.text = ""
                    frontLeftWingField.background = null
                }
                if (it[1] > 0) {
                    frontRightWingField.text = it[1].toString()
                    frontRightWingField.background = getDrawable(R.color.warn)
                } else {
                    frontRightWingField.text = ""
                    frontRightWingField.background = null
                }
            }
        ),
        LiveDataField(
            "rearWing",
            { 0 },
            {
                findViewById<TextView>(R.id.rearWingDamage).apply {
                    text = ""
                    background = null
                }
            },
            { data, packet ->
                packet.asType<CarStatusDataPacket> {
                    return@LiveDataField it.data.items[it.header.playerCarIndex].rearWingDamage.toInt()
                }
                return@LiveDataField data
            },
            {
                val rearWingField = findViewById<TextView>(R.id.rearWingDamage)
                if (it > 0) {
                    rearWingField.text = it.toString()
                    rearWingField.background = getDrawable(R.color.warn)
                } else {
                    rearWingField.text = ""
                    rearWingField.background = null
                }
            }
        ),
        LiveDataField(
            "engine",
            { 0 },
            {
                findViewById<TextView>(R.id.engineTempValue).apply {
                    text = "X"
                    background = getDrawable(R.color.inop)
                }
            },
            { data, packet ->
                packet.asType<CarTelemetryDataPacket> {
                    return@LiveDataField it.data.items[it.header.playerCarIndex].engineTemperature.toInt()
                }
                return@LiveDataField data
            },
            {
                val engineField = findViewById<TextView>(R.id.engineTempValue)
                engineField.text = it.toString()
                when {
                    it >= 130 -> {
                        engineField.background = getDrawable(R.color.highTemp)
                    }
                    it > 120 -> {
                        engineField.background = getDrawable(R.color.warmTemp)
                    }
                    it >= 100 -> {
                        engineField.background = getDrawable(R.color.normalTemp)
                    }
                    else -> {
                        engineField.background = getDrawable(R.color.lowTemp)
                    }
                }
            }
        ),
        LiveDataField(
            "sessionTime",
            { -1 },
            {
                findViewById<TextView>(R.id.sessionTimeValue).text = "XX:XX"
            },
            { data, packet ->
                packet.asType<SessionDataPacket> {
                    val timeLeft = it.data.sessionTimeLeft.toInt()
                    if (timeLeft != data)
                        return@LiveDataField timeLeft
                }
                return@LiveDataField data
            },
            {
                val sessionTimeField = findViewById<TextView>(R.id.sessionTimeValue)
                if (it > 0)
                    sessionTimeField.text = "${it / 60}:${secondsFormat.format(it % 60)}"
                else
                    sessionTimeField.text = ""
            }
        ),
        LiveDataField(
            "counter",
            { 0 },
            {
                findViewById<TextView>(R.id.debugFrameCount).text = "X"
            },
            { data, _ -> data + 1 },
            {
                if (it % 100 == 0) {
                    val debugField = findViewById<TextView>(R.id.debugFrameCount)
                    debugField.text = it.toString()
                }
            }
        )
    )

    private var sessionId: Long? = null

    private val views = HashMap<Int, View>()
    private val viewProvider = object : ViewProvider {

        @Suppress("UNCHECKED_CAST")
        override fun <T : View> findViewById(id: Int): T =
            views[id].let {
                if (it == null) {
                    val view = activity.findViewById<T>(id)
                    views[id] = view
                    view
                } else {
                    it
                }
            } as T

        override fun getDrawable(id: Int): Drawable =
            ContextCompat.getDrawable(activity, id)!!

        override fun getColor(id: Int): Int =
            ContextCompat.getColor(activity, id)
    }

    fun onUpdate(packet: TelemetryPacket<*>) {
        if (packet.header.sessionId != sessionId) {
            sessionId = packet.header.sessionId
            init()
        }
        fields.iterator().forEach {
            it.onUpdate(viewProvider, packet)
        }
    }

    init {
        init()
    }

    override fun dispose() = compositeDisposable.dispose()

    override fun isDisposed() = compositeDisposable.isDisposed

    private fun init() {
        fields.forEach { it.init(viewProvider) }
    }
}

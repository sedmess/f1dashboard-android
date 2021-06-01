package ru.n1ks.f1dashboard.livedata

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.toSingle
import ru.n1ks.f1dashboard.R
import ru.n1ks.f1dashboard.minusOne
import ru.n1ks.f1dashboard.model.*
import ru.n1ks.f1dashboard.plusOne
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

enum class DrsCommonState {
    Available, Unavailable, Upcoming, Fault
}

@ExperimentalUnsignedTypes
data class CompetitorDriver(
    val code: Int,
    val driver: ParticipantData.Driver
)

@ExperimentalUnsignedTypes
data class Competitor(
    val id: Int,
    val position: Int,
    val lastLapTime: Float,
    val bestLapTime: Float,
    val lap: Int,
    val tyreType: TyreCompound,
    val tyreAge: Int = -1,
    val driver: CompetitorDriver?
) {

    fun inBound(size: Int): Boolean = id in 0 until size

    fun positionString(): String = position.toString().padEnd(2, ' ')

    fun isTyresNew(): Boolean = tyreAge < 3
}

data class TyreState(
    val wear: Int,
    val innerTemperature: Int,
    val outerTemperature: Int
)

interface ViewProvider {

    fun findViewById(id: Int): View
    fun getDrawable(id: Int): Drawable
    fun getColor(id: Int): Int
}

@ExperimentalUnsignedTypes
class LiveDataField<T>(
    val name: String,
    private var data: T,
    private val extractDataFunc: (data: T, packet: TelemetryPacket<*>) -> T,
    private val onUpdateFunc: ViewProvider.(T) -> Unit
) {

    fun onUpdate(viewProvider: ViewProvider, update: TelemetryPacket<*>) {
        val newData = extractDataFunc(this.data, update)
        if (newData == data)
            return
        data = newData
        onUpdateFunc(viewProvider, data)
    }
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

@ExperimentalUnsignedTypes
data class RivalsField(
    val ahead: Competitor?,
    val player: Competitor?,
    val behind: Competitor?
)

data class TypesField(
    val tyreFL: TyreState,
    val tyreFR: TyreState,
    val tyreRL: TyreState,
    val tyreRR: TyreState
)

private fun defineWearColor(wear: Int): Int {
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

private fun defineTempColor(temp: Int): Int {
    return when {
        temp < 82 -> R.color.lowTemp
        temp < 103 -> R.color.normalTemp
        temp < 110 -> R.color.warmTemp
        else -> R.color.highTemp
    }
}

private val secondsAndMsFormat: DecimalFormat = DecimalFormat("00.000")
private val secondsFormat: DecimalFormat = DecimalFormat("00")
private val fuelFormat: DecimalFormat = DecimalFormat("+#,#0.00;-#")
private val timeFormatter: (Float) -> String = {
    val n1 = it.toInt() / 60
    val n2 = it % 60
    if (n1 == 0 && n2.absoluteValue < 0.001f) {
        "X:XX.XXX"
    } else {
        "${n1}:${secondsAndMsFormat.format(n2)}"
    }
}

@ExperimentalUnsignedTypes
class LiveData(
    private val activity: Activity,
    private val fields: Collection<LiveDataField<*>>
) {

    private val views = HashMap<Int, View>()
    private val viewProvider = object : ViewProvider {

        override fun findViewById(id: Int): View =
            views[id].let {
                if (it == null) {
                    val view = activity.findViewById<View>(id)
                    views[id] = view
                    view
                } else {
                    it
                }
            }

        override fun getDrawable(id: Int): Drawable =
            ContextCompat.getDrawable(activity, id)!!

        override fun getColor(id: Int): Int =
            ContextCompat.getColor(activity, id)
    }

    fun onUpdate(packet: TelemetryPacket<*>) {
        fields.iterator().forEach {
            it.onUpdate(viewProvider, packet)
        }
    }
}

@SuppressLint("SetTextI18n")
@ExperimentalUnsignedTypes
val LiveDataFields = listOf<LiveDataField<*>>(
    LiveDataField(
        "lapRemaining",
        LapsField(0, 0),
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
            val lapField = findViewById(R.id.lapValue) as TextView
            lapField.text = if (it.lapsCount - it.currentLap >= 0) {
                "+${it.lapsCount - it.currentLap}"
            } else {
                "${it.currentLap}"
            }
        }
    ),
    LiveDataField(
        "fuelRemaining",
        0.0f,
        { data, packet ->
            packet.asType<CarStatusDataPacket> {
                return@LiveDataField it.data.items[it.header.playerCarIndex].fuelRemainingLaps
            }
            return@LiveDataField data
        },
        {
            val fuelField = findViewById(R.id.fuelValue) as TextView
            fuelField.text = fuelFormat.format(it)
        }
    ),
    LiveDataField(
        "fuelMixMode",
        0,
        { data, packet ->
            packet.asType<CarStatusDataPacket> {
                return@LiveDataField it.data.items[it.header.playerCarIndex].fuelMix
            }
            return@LiveDataField data
        },
        {
            val fuelField = findViewById(R.id.fuelValue) as TextView
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
        -1,
        { data, packet ->
            packet.asType<CarStatusDataPacket> {
                return@LiveDataField it.data.items[it.header.playerCarIndex].ersDeployMode.toInt()
            }
            return@LiveDataField data
        },
        {
            val ersField = findViewById(R.id.ersValue) as TextView
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
        0,
        { data, packet ->
            packet.asType<CarSetupDataPacket> {
                return@LiveDataField it.data.items[it.header.playerCarIndex].brakeBias.toInt()
            }
            return@LiveDataField data
        },
        {
            val bbField = findViewById(R.id.bbValue) as TextView
            bbField.text = it.toString()
            bbField.background = getDrawable(R.color.warn)
            val tag = System.nanoTime()
            bbField.tag = tag
            bbField.toSingle()
                .delay(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { field ->
                    if (field.tag == tag) field.background = getDrawable(R.color.black)
                }
        }
    ),
    LiveDataField(
        "diff",
        0,
        { data, packet ->
            packet.asType<CarSetupDataPacket> {
                return@LiveDataField it.data.items[it.header.playerCarIndex].onThrottle.toInt()
            }
            return@LiveDataField data
        },
        {
            val diffField = findViewById(R.id.diffValue) as TextView
            diffField.text = it.toString()
            diffField.background = getDrawable(R.color.warn)
            val tag = System.nanoTime()
            diffField.tag = tag
            diffField.toSingle()
                .delay(1000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { field ->
                    if (field.tag == tag) field.background = getDrawable(R.color.black)
                }
        }
    ),
    LiveDataField(
        "recommendedGear",
        0,
        { data, packet ->
            packet.asType<CarTelemetryDataPacket> {
                return@LiveDataField it.data.suggestedGear.toInt()
            }
            return@LiveDataField data
        },
        {
            val recommendedGearField = findViewById(R.id.recommendedGearValue) as TextView
            if (it > 0) {
                recommendedGearField.text = it.toString()
            } else {
                recommendedGearField.text = ""
            }
        }
    ),
    LiveDataField(
        "drsState",
        data = DrsField(DrsCommonState.Unavailable, isAllowed = false, isOpened = false),
        extractDataFunc = { data, packet ->
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
        onUpdateFunc = {
            val drsField = findViewById(R.id.drsValue) as TextView
            when (it.state) {
                DrsCommonState.Unavailable -> {
                    drsField.text = "X"
                    if (it.isOpened)
                        drsField.background = getDrawable(R.color.black)
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
                        drsField.background = getDrawable(R.color.black)
                    }
                }
            }
        }
    ),
    LiveDataField(
        "rivals",
        RivalsField(ahead = null, player = null, behind = null),
        { data, packet ->
            packet.asType<LapDataPacket> { it ->
                val playerData = it.data.items[it.header.playerCarIndex]
                val playerIndex = it.data.items.indexOfFirst { item -> item.carPosition == playerData.carPosition }
                val rivalAheadIndex =
                    it.data.items.indexOfFirst { item -> item.carPosition == playerData.carPosition.minusOne() }
                val rivalAheadData = it.data.items.getOrNull(rivalAheadIndex)
                val rivalBehindIndex =
                    it.data.items.indexOfFirst { item -> item.carPosition == playerData.carPosition.plusOne() }
                val rivalBehindData = it.data.items.getOrNull(rivalBehindIndex)

                val player = Competitor(
                    id = playerIndex,
                    position = playerData.carPosition.toInt(),
                    lastLapTime = playerData.lastLapTime,
                    bestLapTime = playerData.bestLapTime,
                    lap = playerData.currentLapNum.toInt(),
                    tyreType = data.player?.tyreType ?: TyreCompound.X,
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
                        tyreType = if (data.ahead?.id == rivalAheadIndex) data.ahead.tyreType else TyreCompound.X,
                        tyreAge = data.ahead?.tyreAge ?: Int.MAX_VALUE,
                        driver = if (data.ahead?.id == rivalAheadIndex) data.ahead.driver else null
                    )
                }
                val behind = rivalBehindData?.let {
                    Competitor(
                        id = rivalBehindIndex,
                        position = it.carPosition.toInt(),
                        lastLapTime = it.lastLapTime,
                        bestLapTime = it.bestLapTime,
                        lap = it.currentLapNum.toInt(),
                        tyreType = if (data.behind?.id == rivalBehindIndex) data.behind.tyreType else TyreCompound.X,
                        tyreAge = data.behind?.tyreAge ?: Int.MAX_VALUE,
                        driver = if (data.behind?.id == rivalBehindIndex) data.behind.driver else null
                    )
                }
                return@LiveDataField RivalsField(ahead, player, behind)
            }
            packet.asType<ParticipantDataPacket> {
                var newData = data
                if (data.ahead?.inBound(it.data.items.size) == true) {
                    newData = data.copy(
                        ahead = data.ahead.copy(
                            driver = CompetitorDriver(
                                it.data.items[data.ahead.id].raceNumber.toInt(),
                                it.data.items[data.ahead.id].driver
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
                return@LiveDataField newData
            }
            packet.asType<CarStatusDataPacket> {
                var newData = data
                if (data.player?.inBound(it.data.items.size) == true) {
                    newData = data.copy(
                        player = data.player.copy(
                            tyreType = it.data.items[data.player.id].visualTyreCompound,
                            tyreAge = it.data.items[data.player.id].tyresAgeLaps.toInt()
                        )
                    )
                }
                if (data.ahead?.inBound(it.data.items.size) == true) {
                    newData = newData.copy(
                        ahead = data.ahead.copy(
                            tyreType = it.data.items[data.ahead.id].visualTyreCompound,
                            tyreAge = it.data.items[data.ahead.id].tyresAgeLaps.toInt()
                        )
                    )
                }
                if (data.behind?.inBound(it.data.items.size) == true) {
                    newData = newData.copy(
                        behind = data.behind.copy(
                            tyreType = it.data.items[data.behind.id].visualTyreCompound,
                            tyreAge = it.data.items[data.behind.id].tyresAgeLaps.toInt()
                        )
                    )
                }
                return@LiveDataField newData
            }
            return@LiveDataField data
        },
        { context ->
            val aheadDriverField = findViewById(R.id.aheadDriverValue) as TextView
            val aheadTimeField = findViewById(R.id.aheadTimeValue) as TextView
            val aheadTyreField = findViewById(R.id.aheadTyreValue) as TextView

            if (context.ahead != null && context.ahead.position > 0) {
                aheadDriverField.text =
                    context.ahead.positionString() + context.ahead.driver.let { if (it != null) " ${it.driver.name}" else "" }
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

                aheadTyreField.text = context.ahead.tyreType.char.toString()
                aheadTyreField.setTextColor(getColor(context.ahead.tyreType.color))
                aheadTyreField.background = if (context.ahead.isTyresNew()) getDrawable(R.color.tyreNew) else null
            } else {
                aheadDriverField.text = "XX"
                aheadTimeField.text = "X:XX.XXX"
                aheadTimeField.setTextColor(getColor(R.color.white))
                aheadTyreField.text = "X"
                aheadDriverField.setTextColor(getColor(R.color.white))
                aheadTyreField.background = null
            }

            val playerBestTimeField = findViewById(R.id.myBestValue) as TextView
            val playerLastTimeField = findViewById(R.id.myTimeValue) as TextView
            val playerTyreField = findViewById(R.id.myTyreValue) as TextView

            if (context.player != null) {
                playerBestTimeField.text = timeFormatter(context.player.bestLapTime)
                playerLastTimeField.text = timeFormatter(context.player.lastLapTime)

                if (context.player.lap < context.ahead?.lap ?: context.player.lap) {
                    playerLastTimeField.setTextColor(getColor(R.color.timeIrrelevant))
                } else {
                    playerLastTimeField.setTextColor(getColor(R.color.white))
                }

                playerTyreField.text = context.player.tyreType.char.toString()
                playerTyreField.setTextColor(getColor(context.player.tyreType.color))
                playerTyreField.background = if (context.player.isTyresNew()) getDrawable(R.color.tyreNew) else null
            } else {
                playerBestTimeField.text = "X:XX.XXX"
                playerLastTimeField.text = "X:XX.XXX"
                playerTyreField.text = "X"
                playerTyreField.setTextColor(getColor(R.color.white))
                playerTyreField.background = null
            }

            val behindDriverField = findViewById(R.id.behindDriverValue) as TextView
            val behindTimeField = findViewById(R.id.behindTimeValue) as TextView
            val behindTyreFiled = findViewById(R.id.behindTyreValue) as TextView

            if (context.behind != null) {
                behindDriverField.text =
                    context.behind.positionString() + context.behind.driver.let { if (it != null) " ${it.driver.name}" else "" }
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

                behindTyreFiled.text = context.behind.tyreType.char.toString()
                behindTyreFiled.setTextColor(getColor(context.behind.tyreType.color))
                behindTyreFiled.background = if (context.behind.isTyresNew()) getDrawable(R.color.tyreNew) else null

            } else {
                behindDriverField.text = "XX"
                behindTimeField.text = "X:XX.XXX"
                behindTimeField.setTextColor(getColor(R.color.white))
                behindTyreFiled.text = "X"
                behindTyreFiled.setTextColor(getColor(R.color.white))
                behindTyreFiled.background = null
            }
        }
    ),
    LiveDataField(
        "bestTime",
        0.0f,
        { data, packet ->
            packet.asType<LapDataPacket> {
                return@LiveDataField it.data.items.filter { it.bestLapTime > 0 }
                    .minOfOrNull { it.bestLapTime } ?: 0.0f
            }
            return@LiveDataField data
        },
        {
            (findViewById(R.id.bestSessionTime) as TextView).text = timeFormatter(it)
        }
    ),
    LiveDataField(
        "tyres",
        TypesField(
            tyreFL = TyreState(0, 0, 0),
            tyreFR = TyreState(0, 0, 0),
            tyreRL = TyreState(0, 0, 0),
            tyreRR = TyreState(0, 0, 0)
        ),
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
            findViewById(R.id.wearFLValue).background =
                getDrawable(defineWearColor(it.tyreFL.wear))
            findViewById(R.id.wearFRValue).background =
                getDrawable(defineWearColor(it.tyreFR.wear))
            findViewById(R.id.wearRLValue).background =
                getDrawable(defineWearColor(it.tyreRL.wear))
            findViewById(R.id.wearRRValue).background =
                getDrawable(defineWearColor(it.tyreRR.wear))

            findViewById(R.id.surfaceFLValue).background =
                getDrawable(defineTempColor(it.tyreFL.outerTemperature))
            findViewById(R.id.surfaceFRValue).background =
                getDrawable(defineTempColor(it.tyreFR.outerTemperature))
            findViewById(R.id.surfaceRLValue).background =
                getDrawable(defineTempColor(it.tyreRL.outerTemperature))
            findViewById(R.id.surfaceRRValue).background =
                getDrawable(defineTempColor(it.tyreRR.outerTemperature))

            findViewById(R.id.innerFLValue).background =
                getDrawable(defineTempColor(it.tyreFL.innerTemperature))
            findViewById(R.id.innerFRValue).background =
                getDrawable(defineTempColor(it.tyreFR.innerTemperature))
            findViewById(R.id.innerRLValue).background =
                getDrawable(defineTempColor(it.tyreRL.innerTemperature))
            findViewById(R.id.innerRRValue).background =
                getDrawable(defineTempColor(it.tyreRR.innerTemperature))
        }
    ),
    LiveDataField(
        "frontWing",
        arrayOf(0, 0),
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
            val frontLeftWingField = findViewById(R.id.frontWingLeftDamage) as TextView
            val frontRightWingField = findViewById(R.id.frontWingRightDamage) as TextView
            if (it[0] > 0) {
                frontLeftWingField.text = it[0].toString()
                frontLeftWingField.background = getDrawable(R.color.warn)
            } else {
                frontLeftWingField.text = ""
                frontLeftWingField.background = getDrawable(R.color.black)
            }
            if (it[1] > 0) {
                frontRightWingField.text = it[1].toString()
                frontRightWingField.background = getDrawable(R.color.warn)
            } else {
                frontRightWingField.text = ""
                frontRightWingField.background = getDrawable(R.color.black)
            }
        }
    ),
    LiveDataField(
        "rearWing",
        0,
        { data, packet ->
            packet.asType<CarStatusDataPacket> {
                return@LiveDataField it.data.items[it.header.playerCarIndex].rearWingDamage.toInt()
            }
            return@LiveDataField data
        },
        {
            val rearWingField = findViewById(R.id.rearWingDamage) as TextView
            if (it > 0) {
                rearWingField.text = it.toString()
                rearWingField.background = getDrawable(R.color.warn)
            } else {
                rearWingField.text = ""
                rearWingField.background = getDrawable(R.color.black)
            }
        }
    ),
    LiveDataField(
        "engine",
        0,
        { data, packet ->
            packet.asType<CarTelemetryDataPacket> {
                return@LiveDataField it.data.items[it.header.playerCarIndex].engineTemperature.toInt()
            }
            return@LiveDataField data
        },
        {
            val engineField = findViewById(R.id.engineTempValue) as TextView
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
        -1,
        { data, packet ->
            packet.asType<SessionDataPacket> {
                val timeLeft = it.data.sessionTimeLeft.toInt()
                if (timeLeft != data)
                    return@LiveDataField timeLeft
            }
            return@LiveDataField data
        },
        {
            val sessionTimeField = findViewById(R.id.sessionTimeValue) as TextView
            if (it > 0)
                sessionTimeField.text = "${it / 60}:${secondsFormat.format(it % 60)}"
            else
                sessionTimeField.text = ""
        }
    ),
    LiveDataField(
        "counter",
        0,
        { data, _ -> data + 1 },
        {
            if (it % 100 == 0) {
                val debugField = findViewById(R.id.debugFrameCount) as TextView
                debugField.text = it.toString()
            }
        }
    )
)
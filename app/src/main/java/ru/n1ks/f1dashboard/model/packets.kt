package ru.n1ks.f1dashboard.model

import ru.n1ks.f1dashboard.UByteOne

interface TelemetryData

@ExperimentalUnsignedTypes
enum class PackageType(val id: UByte) {
    MotionType(0u),
    SessionType(1u),
    LapDataType(2u),
    EventType(3u),
    ParticipantsType(4u),
    CarSetupsType(5u),
    CarTelemetryType(6u),
    CarStatusType(7u),
    FinalClassificationType(8u),
    LobbyInfoType(9u)
}

@ExperimentalUnsignedTypes
data class TelemetryHeader(
    val packetFormat: UShort,
    val gameMajorVersion: UByte,
    val gameMinorVersion: UByte,
    val packetVersion: UByte,
    val packetTypeId: UByte,
    val sessionId: ULong,
    val sessionTimestamp: Float,
    val frameId: UInt,
    val playerCarIndex: Int,
    val secondaryPlayerCarIndex: UByte
)

@ExperimentalUnsignedTypes
data class TelemetryPacket<T : TelemetryData>(
    val header: TelemetryHeader,
    val data: T
) {

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : TelemetryData> asType(block: (TelemetryPacket<T>) -> Unit) {
        if (this.data is T) {
            block(this as TelemetryPacket<T>)
        }
    }
}

object EmptyData : TelemetryData

@ExperimentalUnsignedTypes
class CarTelemetryData(
    /**  Speed of car in kilometres per hour */
    val speed: UShort,
    /**  Amount of throttle applied (0.0 to 1.0) */
    val throttle: Float,
    /**  Steering (-1.0 (full lock left) to 1.0 (full lock right)) */
    val steer: Float,
    /**  Amount of brake applied (0.0 to 1.0) */
    val brake: Float,
    /**  Amount of clutch applied (0 to 100) */
    val clutch: UByte,
    /**  Gear selected (1-8, N=0, R=-1) */
    val gear: Byte,
    /**  Engine RPM */
    val engineRPM: UShort,
    /**  0 = off, 1 = on */
    private val _drs: UByte,
    /**  Rev lights indicator (percentage) */
    val revLightsPercent: UByte,
    /**  Brakes temperature (celsius) */
    private val _brakesTemperature: Array<UShort>,
    /**  Tyres surface temperature (celsius) */
    private val _tyresSurfaceTemperature: Array<UByte>,
    /**  Tyres inner temperature (celsius) */
    private val _tyresInnerTemperature: Array<UByte>,
    /**  Engine temperature (celsius) */
    val engineTemperature: UShort,
    /**  Tyres pressure (PSI) */
    private val _tyresPressure: Array<Float>,
    /**  Driving surface, see appendices */
    private val _surfaceType: Array<UByte>
) {

    val drs: Boolean
        get() = _drs == UByteOne

    val brakesTemperatureRL: UShort
        get() = _brakesTemperature[0]

    val brakesTemperatureRR: UShort
        get() = _brakesTemperature[1]

    val brakesTemperatureFL: UShort
        get() = _brakesTemperature[2]

    val brakesTemperatureFR: UShort
        get() = _brakesTemperature[3]

    val tyresSurfaceTemperatureRL: Int
        get() = _tyresSurfaceTemperature[0].toInt()

    val tyresSurfaceTemperatureRR: Int
        get() = _tyresSurfaceTemperature[1].toInt()

    val tyresSurfaceTemperatureFL: Int
        get() = _tyresSurfaceTemperature[2].toInt()

    val tyresSurfaceTemperatureFR: Int
        get() = _tyresSurfaceTemperature[3].toInt()

    val tyresInnerTemperatureRL: Int
        get() = _tyresInnerTemperature[0].toInt()

    val tyresInnerTemperatureRR: Int
        get() = _tyresInnerTemperature[1].toInt()

    val tyresInnerTemperatureFL: Int
        get() = _tyresInnerTemperature[2].toInt()

    val tyresInnerTemperatureFR: Int
        get() = _tyresInnerTemperature[3].toInt()

    val tyresPressureRL: Float
        get() = _tyresPressure[0]

    val tyresPressureRR: Float
        get() = _tyresPressure[1]

    val tyresPressureFL: Float
        get() = _tyresPressure[2]

    val tyresPressureFR: Float
        get() = _tyresPressure[3]

    val surfaceTypeRL: UByte
        get() = _surfaceType[0]

    val surfaceTypeRR: UByte
        get() = _surfaceType[1]

    val surfaceTypeFL: UByte
        get() = _surfaceType[2]

    val surfaceTypeFR: UByte
        get() = _surfaceType[3]

}

@ExperimentalUnsignedTypes
class CarTelemetryDataPacket(
    val items: List<CarTelemetryData>,
    /**  Bit flags specifying which buttons are being pressed, currently - see appendices */
    val buttonStatus: UInt,
    /**  Index of MFD panel open - 255 = MFD closed: Single player, race – 0 = Car setup, 1 = Pits, 2 = Damage, 3 =  Engine, 4 = Temperatures. May vary depending on game mode */
    val mfdPanelIndex: UByte,
    /**  See above */
    val mfdPanelIndexSecondaryPlayer: UByte,
    /**  Suggested gear for the player (1-8), 0 if no gear suggested */
    val suggestedGear: UByte
) : TelemetryData

@ExperimentalUnsignedTypes
class LapData(
    /**  Last lap time in seconds */
    val lastLapTime: Float,
    /**  Current time around the lap in seconds */
    val currentLapTime: Float,
    /**  Sector 1 time in milliseconds */
    val sector1TimeInMS: UShort,
    /**  Sector 2 time in milliseconds */
    val sector2TimeInMS: UShort,
    /**  Best lap time of the session in seconds */
    val bestLapTime: Float,
    /**  Lap number best time achieved on */
    val bestLapNum: UByte,
    /**  Sector 1 time of best lap in the session in milliseconds */
    val bestLapSector1TimeInMS: UShort,
    /**  Sector 2 time of best lap in the session in milliseconds */
    val bestLapSector2TimeInMS: UShort,
    /**  Sector 3 time of best lap in the session in milliseconds */
    val bestLapSector3TimeInMS: UShort,
    /**  Best overall sector 1 time of the session in milliseconds */
    val bestOverallSector1TimeInMS: UShort,
    /**  Lap number best overall sector 1 time achieved on */
    val bestOverallSector1LapNum: UByte,
    /**  Best overall sector 2 time of the session in milliseconds */
    val bestOverallSector2TimeInMS: UShort,
    /**  Lap number best overall sector 2 time achieved on */
    val bestOverallSector2LapNum: UByte,
    /**  Best overall sector 3 time of the session in milliseconds */
    val bestOverallSector3TimeInMS: UShort,
    /**  Lap number best overall sector 3 time achieved on */
    val bestOverallSector3LapNum: UByte,
    /**  Distance vehicle is around current lap in metres – could be negative if line hasn’t been crossed yet */
    val lapDistance: Float,
    /**  Total distance travelled in session in metres – could be negative if line hasn’t been crossed yet */
    val totalDistance: Float,
    /**  Delta in seconds for safety car */
    val safetyCarDelta: Float,
    /**  Car race position */
    val carPosition: UByte,
    /**  Current lap number */
    val currentLapNum: UByte,
    /**  0 = none, 1 = pitting, 2 = in pit area */
    private val _pitStatus: UByte,
    /**  0 = sector1, 1 = sector2, 2 = sector3 */
    val sector: UByte,
    /**  Current lap invalid - 0 = valid, 1 = invalid */
    private val _currentLapInvalid: UByte,
    /**  Accumulated time penalties in seconds to be added */
    val penalties: UByte,
    /**  Grid position the vehicle started the race in */
    val gridPosition: UByte,
    /**  Status of driver - 0 = in garage, 1 = flying lap 2 = in lap, 3 = out lap, 4 = on track */
    private val _driverStatus: UByte,
    /**  Result status - 0 = invalid, 1 = inactive, 2 = active 3 = finished, 4 = disqualified, 5 = not classified 6 = retired */
    private val _resultStatus: UByte
) {

    enum class PitStatus {
        None, Pitting, InPitArea
    }

    enum class DriverStatus {
        InGarage, FlyingLap, InLap, OutLap, OnTrack
    }

    enum class ResultStatus {
        Invalid, Inactive, Active, Finished, Disqualified, NotClassified, Retired
    }

    val pitStatus: PitStatus
        get() = when (_pitStatus.toInt()) {
            0 -> PitStatus.None
            1 -> PitStatus.Pitting
            2 -> PitStatus.InPitArea
            else -> throw IllegalStateException("unknown status $_pitStatus")
        }

    val currentLapInvalid: Boolean
        get() = _currentLapInvalid != UByte.MIN_VALUE

    val driverStatus: DriverStatus
        get() = when (_driverStatus.toInt()) {
            0 -> DriverStatus.InGarage
            1 -> DriverStatus.FlyingLap
            2 -> DriverStatus.InLap
            3 -> DriverStatus.OutLap
            4 -> DriverStatus.OnTrack
            else -> throw IllegalStateException("unknown status $_driverStatus")
        }

    val resultStatus: ResultStatus
        get() = when (_resultStatus.toInt()) {
            0 -> ResultStatus.Invalid
            1 -> ResultStatus.Inactive
            2 -> ResultStatus.Active
            3 -> ResultStatus.Finished
            4 -> ResultStatus.Disqualified
            5 -> ResultStatus.NotClassified
            6 -> ResultStatus.Retired
            else -> throw IllegalStateException("unknown status $_resultStatus")
        }
}

@ExperimentalUnsignedTypes
class LapDataPacket(
    val items: List<LapData>
) : TelemetryData

@ExperimentalUnsignedTypes
class CarStatusData(
    /**  0 (off) - 2 (high) */
    val tractionControl: UByte,
    /**  0 (off) - 1 (on) */
    val antiLockBrakes: UByte,
    /**  Fuel mix - 0 = lean, 1 = standard, 2 = rich, 3 = max */
    private val _fuelMix: UByte,
    /**  Front brake bias (percentage) */
    val frontBrakeBias: UByte,
    /**  Pit limiter status - 0 = off, 1 = on */
    private val _pitLimiterStatus: UByte,
    /**  Current fuel mass */
    val fuelInTank: Float,
    /**  Fuel capacity */
    val fuelCapacity: Float,
    /**  Fuel remaining in terms of laps (value on MFD) */
    val fuelRemainingLaps: Float,
    /**  Cars max RPM, point of rev limiter */
    val maxRPM: UShort,
    /**  Cars idle RPM */
    val idleRPM: UShort,
    /**  Maximum number of gears */
    val maxGears: UByte,
    /**  0 = not allowed, 1 = allowed, -1 = unknown */
    private val _drsAllowed: UByte,
    /**  0 = DRS not available, non-zero - DRS will be available in [X] metres */
    val drsActivationDistance: UShort,
    /**  Tyre wear percentage */
    private val _tyresWear: Array<UByte>,
    /**  F1 Modern - 16 = C5, 17 = C4, 18 = C3, 19 = C2, 20 = C1 7 = inter, 8 = wet F1 Classic - 9 = dry, 10 = wet F2 – 11 = super soft, 12 = soft, 13 = medium, 14 = hard 15 = wet */
    private val _actualTyreCompound: UByte,
    /**  F1 visual (can be different from actual compound) 16 = soft, 17 = medium, 18 = hard, 7 = inter, 8 = wet F1 Classic – same as above F2 – same as above */
    private val _visualTyreCompound: UByte,
    /**  Age in laps of the current set of tyres */
    val tyresAgeLaps: UByte,
    /**  Tyre damage (percentage) */
    private val _tyresDamage: Array<UByte>,
    /**  Front left wing damage (percentage) */
    val frontLeftWingDamage: UByte,
    /**  Front right wing damage (percentage) */
    val frontRightWingDamage: UByte,
    /**  Rear wing damage (percentage) */
    val rearWingDamage: UByte,
    /**  Indicator for DRS fault, 0 = OK, 1 = fault */
    private val _drsFault: UByte,
    /**  Engine damage (percentage) */
    val engineDamage: UByte,
    /**  Gear box damage (percentage) */
    val gearBoxDamage: UByte,
    /**  -1 = invalid/unknown, 0 = none, 1 = green 2 = blue, 3 = yellow, 4 = red */
    private val _vehicleFiaFlags: Byte,
    /**  ERS energy store in Joules */
    val ersStoreEnergy: Float,
    /**  ERS deployment mode, 0 = none, 1 = medium 2 = overtake, 3 = hotlap */
    val ersDeployMode: UByte,
    /**  ERS energy harvested this lap by MGU-K */
    val ersHarvestedThisLapMGUK: Float,
    /**  ERS energy harvested this lap by MGU-H */
    val ersHarvestedThisLapMGUH: Float,
    /**  ERS energy deployed this lap */
    val ersDeployedThisLap: Float
) {

    enum class FiaFlag {
        Unknown, None, Green, Blue, Yellow, Red
    }

    val fuelMix: Int
        get() = (_fuelMix + UByteOne).toInt()

    val pitLimiter: Boolean
        get() = _pitLimiterStatus == UByteOne

    val drsAllowed: Boolean
        get() = _drsAllowed == UByteOne

    val drsAvailable: Boolean
        get() = drsActivationDistance > 0u

    val tyresWearRL: Int
        get() = _tyresWear[0].toInt()

    val tyresWearRR: Int
        get() = _tyresWear[1].toInt()

    val tyresWearFL: Int
        get() = _tyresWear[2].toInt()

    val tyresWearFR: Int
        get() = _tyresWear[3].toInt()

    val actualTyreCompound: TyreCompound
        get() = TyreCompound.defineActualByCode(_actualTyreCompound)

    val visualTyreCompound: TyreCompound
        get() = TyreCompound.defineVisualByCode(_visualTyreCompound)

    val tyresDamageRL: UByte
        get() = _tyresDamage[0]

    val tyresDamageRR: UByte
        get() = _tyresDamage[1]

    val tyresDamageFL: UByte
        get() = _tyresDamage[2]

    val tyresDamageFR: UByte
        get() = _tyresDamage[3]

    val drsFault: Boolean
        get() = _drsFault == UByteOne

    val fiaFlag: FiaFlag
        get() = when (_vehicleFiaFlags.toInt()) {
            -1 -> FiaFlag.Unknown
            0 -> FiaFlag.None
            1 -> FiaFlag.Green
            2 -> FiaFlag.Blue
            3 -> FiaFlag.Yellow
            4 -> FiaFlag.Red
            else -> throw IllegalStateException("unknown code $_vehicleFiaFlags")
        }
}

@ExperimentalUnsignedTypes
class CarStatusDataPacket(
    val items: List<CarStatusData>
) : TelemetryData

class MarshalZone(
    /**  Fraction (0..1) of way through the lap the marshal zone starts */
    val zoneStart: Float,
    /**  -1 = invalid/unknown, 0 = none, 1 = green, 2 = blue, 3 = yellow, 4 = red */
    val zoneFlag: Byte
)

@ExperimentalUnsignedTypes
class WeatherForecastSample(
    /**  0 = unknown, 1 = P1, 2 = P2, 3 = P3, 4 = Short P, 5 = Q1 6 = Q2, 7 = Q3, 8 = Short Q, 9 = OSQ, 10 = R, 11 = R2  12 = Time Trial */
    val sessionType: UByte,
    /**  Time in minutes the forecast is for */
    val timeOffset: UByte,
    /**  Weather - 0 = clear, 1 = light cloud, 2 = overcast 3 = light rain, 4 = heavy rain, 5 = storm */
    val weather: UByte,
    /**  Track temp. in degrees celsius */
    val trackTemperature: Byte,
    /**  Air temp. in degrees celsius */
    val airTemperature: Byte
)

@ExperimentalUnsignedTypes
class SessionDataPacket(
    /**  Weather - 0 = clear, 1 = light cloud, 2 = overcast 3 = light rain, 4 = heavy rain, 5 = storm */
    val weather: UByte,
    /**  Track temp. in degrees celsius */
    val trackTemperature: UByte,
    /**  Air temp. in degrees celsius */
    val airTemperature: Byte,
    /**  Total number of laps in this race */
    val totalLaps: UByte,
    /**  Track length in metres */
    val trackLength: UShort,
    /**  0 = unknown, 1 = P1, 2 = P2, 3 = P3, 4 = Short P 5 = Q1, 6 = Q2, 7 = Q3, 8 = Short Q, 9 = OSQ 10 = R, 11 = R2, 12 = Time Trial */
    val sessionType: UByte,
    /**  -1 for unknown, 0-21 for tracks, see appendix */
    val trackId: Byte,
    /**  Formula, 0 = F1 Modern, 1 = F1 Classic, 2 = F2, 3 = F1 Generic */
    val formula: UByte,
    /**  Time left in session in seconds */
    val sessionTimeLeft: UShort,
    /**  Session duration in seconds */
    val sessionDuration: UShort,
    /**  Pit speed limit in kilometres per hour */
    val pitSpeedLimit: UByte,
    /**  Whether the game is paused */
    val gamePaused: UByte,
    /**  Whether the player is spectating */
    val isSpectating: UByte,
    /**  Index of the car being spectated */
    val spectatorCarIndex: UByte,
    /**  SLI Pro support, 0 = inactive, 1 = active */
    val sliProNativeSupport: UByte
//    /**  Number of marshal zones to follow */
//    val numMarshalZones: UByte,
//    /**  List of marshal zones – max 21 */
//    val marshalZones: Array<MarshalZone>,
//    /**  0 = no safety car, 1 = full safety car 2 = virtual safety car */
//    val safetyCarStatus: UByte,
//    /**  0 = offline, 1 = online */
//    val networkGame: UByte,
//    /**  Number of weather samples to follow */
//    val numWeatherForecastSamples: UByte,
//    /**  Array of weather forecast samples */
//    val weatherForecastSamples: Array<WeatherForecastSample>
) : TelemetryData

@ExperimentalUnsignedTypes
class ParticipantData(
    /**  Whether the vehicle is AI (1) or Human (0) controlled */
    val aiControlled: UByte,
    /**  Driver id - see appendix */
    private val _driverId: UByte,
    /**  Team id - see appendix */
    val teamId: UByte,
    /**  Race number of the car */
    val raceNumber: UByte,
    /**  Nationality of the driver */
    val nationality: UByte
) {

    enum class Driver(private val id: Int) {
        SAI(0),
        KVY(1),
        RIC(2),
        RAI(6),
        HAM(7),
        VER(9),
        HUL(10),
        MAG(11),
        GRO(12),
        VET(13),
        PER(14),
        BOT(15),
        OCO(17),
        STR(19),
        MAR(45),
        GAL(47),
        DEV(48),
        AIT(49),
        RUS(50),
        GHI(53),
        NOR(54),
        DEL(56),
        FUO(57),
        LEC(58),
        GAS(59),
        ALB(62),
        LAT(63),
        BOC(64),
        MER(66),
        MAI(67),
        LOR(68),
        GIO(74),
        KUB(75),
        MAT(78),
        MAZ(79),
        ZHO(80),
        SCH(81),
        ILO(82),
        COR(83),
        KIN(84),
        RAG(85),
        CAL(86),
        HUB(87),
        ALE(88),
        BOS(89),
        ZZZ(-1);

        companion object {

            private val map = values().groupBy { it.id }.mapValues { it.value.first() }

            fun getById(id: Int): Driver = map[id] ?: ZZZ
        }
    }

    val driver: Driver
        get() = Driver.getById(_driverId.toInt())
}

@ExperimentalUnsignedTypes
class ParticipantDataPacket(
    val items: List<ParticipantData>
) : TelemetryData

@ExperimentalUnsignedTypes
class CarSetupData(
/**  Front wing aero */ val frontWing: UByte,
/**  Rear wing aero */ val rearWing: UByte,
/**  Differential adjustment on throttle (percentage) */ val onThrottle: UByte,
/**  Differential adjustment off throttle (percentage) */ val offThrottle: UByte,
/**  Front camber angle (suspension geometry) */ val frontCamber: Float,
/**  Rear camber angle (suspension geometry) */ val rearCamber: Float,
/**  Front toe angle (suspension geometry) */ val frontToe: Float,
/**  Rear toe angle (suspension geometry) */ val rearToe: Float,
/**  Front suspension */ val frontSuspension: UByte,
/**  Rear suspension */ val rearSuspension: UByte,
/**  Front anti-roll bar */ val frontAntiRollBar: UByte,
/**  Front anti-roll bar */ val rearAntiRollBar: UByte,
/**  Front ride height */ val frontSuspensionHeight: UByte,
/**  Rear ride height */ val rearSuspensionHeight: UByte,
/**  Brake pressure (percentage) */ val brakePressure: UByte,
/**  Brake bias (percentage) */ val brakeBias: UByte,
/**  Rear left tyre pressure (PSI) */ val rearLeftTyrePressure: Float,
/**  Rear right tyre pressure (PSI) */ val rearRightTyrePressure: Float,
/**  Front left tyre pressure (PSI) */ val frontLeftTyrePressure: Float,
/**  Front right tyre pressure (PSI) */ val frontRightTyrePressure: Float,
/**  Ballast */ val ballast: UByte,
/**  Fuel load */ val fuelLoad: Float
)

@ExperimentalUnsignedTypes
class CarSetupDataPacket(
    val items: List<CarSetupData>
) : TelemetryData

interface EventDetails {

    enum class Type(private val code: String) {
        SessionStarted("SSTA"),
        SessionEnded("SEND"),
        FastestLap("FTLP"),
        Retirement("RTMT"),
        DRSEnabled("DRSE"),
        DRSDisabled("DRSD"),
        TeammateInPits("TMPT"),
        ChequeredFlag("CHQF"),
        RaceWinner("RCWN"),
        PenaltyIssued("PENA"),
        SpeedTrap("SPTP"),
        Empty("XXXX")
        ;

        companion object {

            private val map = values().groupBy { it.code }.mapValues { it.value.first() }

            fun getByCode(code: String): Type = map[code] ?: Empty
        }
    }
}

object EmptyEventData : EventDetails

@ExperimentalUnsignedTypes
class FastestLapData(
/** Vehicle index of car achieving fastest lap */ val vehicleIdx: UByte,
/** Lap time is in seconds */ val lapTime: Float
) : EventDetails

@ExperimentalUnsignedTypes
class Retirement(
/** Vehicle index of car retiring */ val vehicleIdx: UByte
) : EventDetails

@ExperimentalUnsignedTypes
class TeamMateInPits(
/** Vehicle index of team mate */ val vehicleIdx: UByte
) : EventDetails

@ExperimentalUnsignedTypes
class RaceWinner(
/** Vehicle index of the race winner */ val vehicleIdx: UByte
) : EventDetails

@ExperimentalUnsignedTypes
class Penalty(
/** Penalty type – see Appendices */ val penaltyType: UByte,
/** Infringement type – see Appendices */ val infringementType: UByte,
/** Vehicle index of the car the penalty is applied to */ val vehicleIdx: UByte,
/** Vehicle index of the other car involved */ val otherVehicleIdx: UByte,
/** Time gained, or time spent doing action in seconds */ val time: UByte,
/** Lap the penalty occurred on */ val lapNum: UByte,
/** Number of places gained by this */ val placesGained: UByte
) : EventDetails

@ExperimentalUnsignedTypes
class SpeedTrap(
/** Vehicle index of the vehicle triggering speed trap */ val vehicleIdx: UByte,
/** Top speed achieved in kilometres per hour */ val speed: Float
) : EventDetails

@ExperimentalUnsignedTypes
class EventDataPacket(
    /** Event string code, see below */ val eventType: EventDetails.Type,
    /** Event details - should be interpreted differently for each type */ val details: EventDetails
) : TelemetryData {

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : EventDetails> asType(block: (T) -> Unit) {
        if (this.details is T) {
            block(this.details)
        }
    }
}
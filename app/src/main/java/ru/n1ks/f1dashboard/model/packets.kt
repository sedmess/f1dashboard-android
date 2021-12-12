package ru.n1ks.f1dashboard.model

import ru.n1ks.f1dashboard.Bytes
import ru.n1ks.f1dashboard.toUnsignedInt

interface TelemetryData

enum class PackageType(val id: Byte) {
    MotionType(0),
    SessionType(1),
    LapDataType(2),
    EventType(3),
    ParticipantsType(4),
    CarSetupsType(5),
    CarTelemetryType(6),
    CarStatusType(7),
    FinalClassificationType(8),
    LobbyInfoType(9)
}

data class TelemetryHeader(
    val packetFormat: Short,
    val gameMajorVersion: Byte,
    val gameMinorVersion: Byte,
    val packetVersion: Byte,
    val packetTypeId: Byte,
    val sessionId: Long,
    val sessionTimestamp: Float,
    val frameId: Int,
    val playerCarIndex: Int,
    val secondaryPlayerCarIndex: Byte
)

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

class CarTelemetryData(
    /**  Speed of car in kilometres per hour */
    val speed: Short,
    /**  Amount of throttle applied (0.0 to 1.0) */
    val throttle: Float,
    /**  Steering (-1.0 (full lock left) to 1.0 (full lock right)) */
    val steer: Float,
    /**  Amount of brake applied (0.0 to 1.0) */
    val brake: Float,
    /**  Amount of clutch applied (0 to 100) */
    val clutch: Byte,
    /**  Gear selected (1-8, N=0, R=-1) */
    val gear: Byte,
    /**  Engine RPM */
    val engineRPM: Short,
    /**  0 = off, 1 = on */
    private val _drs: Byte,
    /**  Rev lights indicator (percentage) */
    val revLightsPercent: Byte,
    /**  Brakes temperature (celsius) */
    private val _brakesTemperature: Array<Short>,
    /**  Tyres surface temperature (celsius) */
    private val _tyresSurfaceTemperature: Array<Byte>,
    /**  Tyres inner temperature (celsius) */
    private val _tyresInnerTemperature: Array<Byte>,
    /**  Engine temperature (celsius) */
    val engineTemperature: Short,
    /**  Tyres pressure (PSI) */
    private val _tyresPressure: Array<Float>,
    /**  Driving surface, see appendices */
    private val _surfaceType: Array<Byte>
) {

    val drs: Boolean
        get() = _drs == Bytes.One

    val brakesTemperatureRL: Short
        get() = _brakesTemperature[0]

    val brakesTemperatureRR: Short
        get() = _brakesTemperature[1]

    val brakesTemperatureFL: Short
        get() = _brakesTemperature[2]

    val brakesTemperatureFR: Short
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

    val surfaceTypeRL: Byte
        get() = _surfaceType[0]

    val surfaceTypeRR: Byte
        get() = _surfaceType[1]

    val surfaceTypeFL: Byte
        get() = _surfaceType[2]

    val surfaceTypeFR: Byte
        get() = _surfaceType[3]

}

class CarTelemetryDataPacket(
    val items: List<CarTelemetryData>,
    /**  Bit flags specifying which buttons are being pressed, currently - see appendices */
    val buttonStatus: Int,
    /**  Index of MFD panel open - 255 = MFD closed: Single player, race – 0 = Car setup, 1 = Pits, 2 = Damage, 3 =  Engine, 4 = Temperatures. May vary depending on game mode */
    val mfdPanelIndex: Byte,
    /**  See above */
    val mfdPanelIndexSecondaryPlayer: Byte,
    /**  Suggested gear for the player (1-8), 0 if no gear suggested */
    val suggestedGear: Byte
) : TelemetryData

class LapData(
    /**  Last lap time in seconds */
    val lastLapTime: Float,
    /**  Current time around the lap in seconds */
    val currentLapTime: Float,
    /**  Sector 1 time in milliseconds */
    private val _sector1TimeInMS: Short,
    /**  Sector 2 time in milliseconds */
    private val _sector2TimeInMS: Short,
    /**  Best lap time of the session in seconds */
    val bestLapTime: Float,
    /**  Lap number best time achieved on */
    val bestLapNum: Byte,
    /**  Sector 1 time of best lap in the session in milliseconds */
    private val _bestLapSector1TimeInMS: Short,
    /**  Sector 2 time of best lap in the session in milliseconds */
    private val _bestLapSector2TimeInMS: Short,
    /**  Sector 3 time of best lap in the session in milliseconds */
    private val _bestLapSector3TimeInMS: Short,
    /**  Best overall sector 1 time of the session in milliseconds */
    private val _bestOverallSector1TimeInMS: Short,
    /**  Lap number best overall sector 1 time achieved on */
    val bestOverallSector1LapNum: Byte,
    /**  Best overall sector 2 time of the session in milliseconds */
    private val _bestOverallSector2TimeInMS: Short,
    /**  Lap number best overall sector 2 time achieved on */
    private val _bestOverallSector2LapNum: Byte,
    /**  Best overall sector 3 time of the session in milliseconds */
    private val _bestOverallSector3TimeInMS: Short,
    /**  Lap number best overall sector 3 time achieved on */
    val bestOverallSector3LapNum: Byte,
    /**  Distance vehicle is around current lap in metres – could be negative if line hasn’t been crossed yet */
    val lapDistance: Float,
    /**  Total distance travelled in session in metres – could be negative if line hasn’t been crossed yet */
    val totalDistance: Float,
    /**  Delta in seconds for safety car */
    val safetyCarDelta: Float,
    /**  Car race position */
    val carPosition: Byte,
    /**  Current lap number */
    val currentLapNum: Byte,
    /**  0 = none, 1 = pitting, 2 = in pit area */
    private val _pitStatus: Byte,
    /**  0 = sector1, 1 = sector2, 2 = sector3 */
    val sector: Byte,
    /**  Current lap invalid - 0 = valid, 1 = invalid */
    private val _currentLapInvalid: Byte,
    /**  Accumulated time penalties in seconds to be added */
    val penalties: Byte,
    /**  Grid position the vehicle started the race in */
    val gridPosition: Byte,
    /**  Status of driver - 0 = in garage, 1 = flying lap 2 = in lap, 3 = out lap, 4 = on track */
    private val _driverStatus: Byte,
    /**  Result status - 0 = invalid, 1 = inactive, 2 = active 3 = finished, 4 = disqualified, 5 = not classified 6 = retired */
    private val _resultStatus: Byte
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
        get() = _currentLapInvalid != Byte.MIN_VALUE

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

    val sector1TimeInMS: Int
        get() = _sector1TimeInMS.toUnsignedInt()

    val sector2TimeInMS: Int
        get() = _sector2TimeInMS.toUnsignedInt()

    val bestLapSector1TimeInMS: Int
        get() = _bestLapSector1TimeInMS.toUnsignedInt()

    val bestLapSector2TimeInMS: Int
        get() = _bestLapSector1TimeInMS.toUnsignedInt()

    val bestLapSector3TimeInMS: Int
        get() = _bestLapSector1TimeInMS.toUnsignedInt()
}

class LapDataPacket(
    val items: List<LapData>
) : TelemetryData

class CarStatusData(
    /**  0 (off) - 2 (high) */
    val tractionControl: Byte,
    /**  0 (off) - 1 (on) */
    val antiLockBrakes: Byte,
    /**  Fuel mix - 0 = lean, 1 = standard, 2 = rich, 3 = max */
    private val _fuelMix: Byte,
    /**  Front brake bias (percentage) */
    val frontBrakeBias: Byte,
    /**  Pit limiter status - 0 = off, 1 = on */
    private val _pitLimiterStatus: Byte,
    /**  Current fuel mass */
    val fuelInTank: Float,
    /**  Fuel capacity */
    val fuelCapacity: Float,
    /**  Fuel remaining in terms of laps (value on MFD) */
    val fuelRemainingLaps: Float,
    /**  Cars max RPM, point of rev limiter */
    val maxRPM: Short,
    /**  Cars idle RPM */
    val idleRPM: Short,
    /**  Maximum number of gears */
    val maxGears: Byte,
    /**  0 = not allowed, 1 = allowed, -1 = unknown */
    private val _drsAllowed: Byte,
    /**  0 = DRS not available, non-zero - DRS will be available in X metres */
    val drsActivationDistance: Short,
    /**  Tyre wear percentage */
    private val _tyresWear: Array<Byte>,
    /**  F1 Modern - 16 = C5, 17 = C4, 18 = C3, 19 = C2, 20 = C1 7 = inter, 8 = wet F1 Classic - 9 = dry, 10 = wet F2 – 11 = super soft, 12 = soft, 13 = medium, 14 = hard 15 = wet */
    private val _actualTyreCompound: Byte,
    /**  F1 visual (can be different from actual compound) 16 = soft, 17 = medium, 18 = hard, 7 = inter, 8 = wet F1 Classic – same as above F2 – same as above */
    private val _visualTyreCompound: Byte,
    /**  Age in laps of the current set of tyres */
    val tyresAgeLaps: Byte,
    /**  Tyre damage (percentage) */
    private val _tyresDamage: Array<Byte>,
    /**  Front left wing damage (percentage) */
    val frontLeftWingDamage: Byte,
    /**  Front right wing damage (percentage) */
    val frontRightWingDamage: Byte,
    /**  Rear wing damage (percentage) */
    val rearWingDamage: Byte,
    /**  Indicator for DRS fault, 0 = OK, 1 = fault */
    private val _drsFault: Byte,
    /**  Engine damage (percentage) */
    val engineDamage: Byte,
    /**  Gear box damage (percentage) */
    val gearBoxDamage: Byte,
    /**  -1 = invalid/unknown, 0 = none, 1 = green 2 = blue, 3 = yellow, 4 = red */
    private val _vehicleFiaFlags: Byte,
    /**  ERS energy store in Joules */
    val ersStoreEnergy: Float,
    /**  ERS deployment mode, 0 = none, 1 = medium 2 = overtake, 3 = hotlap */
    val ersDeployMode: Byte,
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
        get() = _fuelMix + Bytes.One

    val pitLimiter: Boolean
        get() = _pitLimiterStatus == Bytes.One

    val drsAllowed: Boolean
        get() = _drsAllowed == Bytes.One

    val drsAvailable: Boolean
        get() = drsActivationDistance > 0

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

    val tyresDamageRL: Byte
        get() = _tyresDamage[0]

    val tyresDamageRR: Byte
        get() = _tyresDamage[1]

    val tyresDamageFL: Byte
        get() = _tyresDamage[2]

    val tyresDamageFR: Byte
        get() = _tyresDamage[3]

    val drsFault: Boolean
        get() = _drsFault == Bytes.One

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

class CarStatusDataPacket(
    val items: List<CarStatusData>
) : TelemetryData

class MarshalZone(
    /**  Fraction (0..1) of way through the lap the marshal zone starts */
    val zoneStart: Float,
    /**  -1 = invalid/unknown, 0 = none, 1 = green, 2 = blue, 3 = yellow, 4 = red */
    val zoneFlag: Byte
)

class WeatherForecastSample(
    /**  0 = unknown, 1 = P1, 2 = P2, 3 = P3, 4 = Short P, 5 = Q1 6 = Q2, 7 = Q3, 8 = Short Q, 9 = OSQ, 10 = R, 11 = R2  12 = Time Trial */
    val sessionType: Byte,
    /**  Time in minutes the forecast is for */
    val timeOffset: Byte,
    /**  Weather - 0 = clear, 1 = light cloud, 2 = overcast 3 = light rain, 4 = heavy rain, 5 = storm */
    val weather: Byte,
    /**  Track temp. in degrees celsius */
    val trackTemperature: Byte,
    /**  Air temp. in degrees celsius */
    val airTemperature: Byte
)

class SessionDataPacket(
    /**  Weather - 0 = clear, 1 = light cloud, 2 = overcast 3 = light rain, 4 = heavy rain, 5 = storm */
    val weather: Byte,
    /**  Track temp. in degrees celsius */
    val trackTemperature: Byte,
    /**  Air temp. in degrees celsius */
    val airTemperature: Byte,
    /**  Total number of laps in this race */
    val totalLaps: Byte,
    /**  Track length in metres */
    val trackLength: Short,
    /**  0 = unknown, 1 = P1, 2 = P2, 3 = P3, 4 = Short P 5 = Q1, 6 = Q2, 7 = Q3, 8 = Short Q, 9 = OSQ 10 = R, 11 = R2, 12 = Time Trial */
    val sessionType: Byte,
    /**  -1 for unknown, 0-21 for tracks, see appendix */
    val trackId: Byte,
    /**  Formula, 0 = F1 Modern, 1 = F1 Classic, 2 = F2, 3 = F1 Generic */
    val formula: Byte,
    /**  Time left in session in seconds */
    val sessionTimeLeft: Short,
    /**  Session duration in seconds */
    val sessionDuration: Short,
    /**  Pit speed limit in kilometres per hour */
    val pitSpeedLimit: Byte,
    /**  Whether the game is paused */
    val gamePaused: Byte,
    /**  Whether the player is spectating */
    val isSpectating: Byte,
    /**  Index of the car being spectated */
    val spectatorCarIndex: Byte,
    /**  SLI Pro support, 0 = inactive, 1 = active */
    val sliProNativeSupport: Byte
//    /**  Number of marshal zones to follow */
//    val numMarshalZones: Byte,
//    /**  List of marshal zones – max 21 */
//    val marshalZones: Array<MarshalZone>,
//    /**  0 = no safety car, 1 = full safety car 2 = virtual safety car */
//    val safetyCarStatus: Byte,
//    /**  0 = offline, 1 = online */
//    val networkGame: Byte,
//    /**  Number of weather samples to follow */
//    val numWeatherForecastSamples: Byte,
//    /**  Array of weather forecast samples */
//    val weatherForecastSamples: Array<WeatherForecastSample>
) : TelemetryData

class ParticipantData(
    /**  Whether the vehicle is AI (1) or Human (0) controlled */
    val aiControlled: Byte,
    /**  Driver id - see appendix */
    private val _driverId: Byte,
    /**  Team id - see appendix */
    val teamId: Byte,
    /**  Race number of the car */
    val raceNumber: Byte,
    /**  Nationality of the driver */
    val nationality: Byte
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

class ParticipantDataPacket(
    val items: List<ParticipantData>
) : TelemetryData

class CarSetupData(
/**  Front wing aero */ val frontWing: Byte,
/**  Rear wing aero */ val rearWing: Byte,
/**  Differential adjustment on throttle (percentage) */ val onThrottle: Byte,
/**  Differential adjustment off throttle (percentage) */ val offThrottle: Byte,
/**  Front camber angle (suspension geometry) */ val frontCamber: Float,
/**  Rear camber angle (suspension geometry) */ val rearCamber: Float,
/**  Front toe angle (suspension geometry) */ val frontToe: Float,
/**  Rear toe angle (suspension geometry) */ val rearToe: Float,
/**  Front suspension */ val frontSuspension: Byte,
/**  Rear suspension */ val rearSuspension: Byte,
/**  Front anti-roll bar */ val frontAntiRollBar: Byte,
/**  Front anti-roll bar */ val rearAntiRollBar: Byte,
/**  Front ride height */ val frontSuspensionHeight: Byte,
/**  Rear ride height */ val rearSuspensionHeight: Byte,
/**  Brake pressure (percentage) */ val brakePressure: Byte,
/**  Brake bias (percentage) */ val brakeBias: Byte,
/**  Rear left tyre pressure (PSI) */ val rearLeftTyrePressure: Float,
/**  Rear right tyre pressure (PSI) */ val rearRightTyrePressure: Float,
/**  Front left tyre pressure (PSI) */ val frontLeftTyrePressure: Float,
/**  Front right tyre pressure (PSI) */ val frontRightTyrePressure: Float,
/**  Ballast */ val ballast: Byte,
/**  Fuel load */ val fuelLoad: Float
)

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

class FastestLapData(
/** Vehicle index of car achieving fastest lap */ val vehicleIdx: Byte,
/** Lap time is in seconds */ val lapTime: Float
) : EventDetails

class Retirement(
/** Vehicle index of car retiring */ val vehicleIdx: Byte
) : EventDetails

class TeamMateInPits(
/** Vehicle index of team mate */ val vehicleIdx: Byte
) : EventDetails

class RaceWinner(
/** Vehicle index of the race winner */ val vehicleIdx: Byte
) : EventDetails

class Penalty(
/** Penalty type – see Appendices */ val penaltyType: Byte,
/** Infringement type – see Appendices */ val infringementType: Byte,
/** Vehicle index of the car the penalty is applied to */ val vehicleIdx: Byte,
/** Vehicle index of the other car involved */ val otherVehicleIdx: Byte,
/** Time gained, or time spent doing action in seconds */ val time: Byte,
/** Lap the penalty occurred on */ val lapNum: Byte,
/** Number of places gained by this */ val placesGained: Byte
) : EventDetails

class SpeedTrap(
/** Vehicle index of the vehicle triggering speed trap */ val vehicleIdx: Byte,
/** Top speed achieved in kilometres per hour */ val speed: Float
) : EventDetails

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
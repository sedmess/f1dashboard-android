package ru.n1ks.f1dashboard.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

@ExperimentalUnsignedTypes
object TelemetryPacketDeserializer {

    private const val CarCount = 22

    fun map(buffer: ByteBuffer): TelemetryPacket<out TelemetryData> {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(0)
        val header = mapHeader(buffer)
        return when (header.packetTypeId) {
            PackageType.CarTelemetryType.id -> {
                TelemetryPacket(header, mapCarTelemetryDataPacket(buffer))
            }
            PackageType.LapDataType.id -> {
                TelemetryPacket(header, mapLapDataPacket(buffer))
            }
            PackageType.CarStatusType.id -> {
                TelemetryPacket(header, mapCarStatusDataPacket(buffer))
            }
            PackageType.SessionType.id -> {
                TelemetryPacket(header, mapSessionData(buffer))
            }
            PackageType.ParticipantsType.id -> {
                TelemetryPacket(header, mapParticipantDataPacket(buffer))
            }
            PackageType.CarSetupsType.id -> {
                TelemetryPacket(header, mapCarSetupDataPacket(buffer))
            }
            PackageType.EventType.id -> {
                TelemetryPacket(header, mapEventDataPacket(buffer))
            }
            else -> {
                TelemetryPacket(header, EmptyData)
            }
        }
    }

    private fun mapHeader(buffer: ByteBuffer): TelemetryHeader =
        TelemetryHeader(
            packetFormat = buffer.short.toUShort(),
            gameMajorVersion = buffer.get().toUByte(),
            gameMinorVersion = buffer.get().toUByte(),
            packetVersion = buffer.get().toUByte(),
            packetTypeId = buffer.get().toUByte(),
            sessionId = buffer.long.toULong(),
            sessionTimestamp = buffer.float,
            frameId = buffer.int.toUInt(),
            playerCarIndex = buffer.get().toInt(),
            secondaryPlayerCarIndex = buffer.get().toUByte()
        )

    private fun mapCarTelemetryData(buffer: ByteBuffer): CarTelemetryData =
        CarTelemetryData(
            speed = buffer.short.toUShort(),
            throttle = buffer.float,
            steer = buffer.float,
            brake = buffer.float,
            clutch = buffer.get().toUByte(),
            gear = buffer.get(),
            engineRPM = buffer.short.toUShort(),
            _drs = buffer.get().toUByte(),
            revLightsPercent = buffer.get().toUByte(),
            _brakesTemperature = arrayOf(
                buffer.short.toUShort(),
                buffer.short.toUShort(),
                buffer.short.toUShort(),
                buffer.short.toUShort()
            ),
            _tyresSurfaceTemperature = arrayOf(
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte()
            ),
            _tyresInnerTemperature = arrayOf(
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte()
            ),
            engineTemperature = buffer.short.toUShort(),
            _tyresPressure = arrayOf(
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float
            ),
            _surfaceType = arrayOf(
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte()
            )
        )


    private fun mapCarTelemetryDataPacket(buffer: ByteBuffer): CarTelemetryDataPacket =
        CarTelemetryDataPacket(
            items = generateSequence(0) { it + 1 }.take(CarCount)
                .map { mapCarTelemetryData(buffer) }.toList(),
            buttonStatus = buffer.int.toUInt(),
            mfdPanelIndex = buffer.get().toUByte(),
            mfdPanelIndexSecondaryPlayer = buffer.get().toUByte(),
            suggestedGear = buffer.get().toUByte()
        )

    private fun mapLapData(buffer: ByteBuffer): LapData =
        LapData(
            lastLapTime = buffer.float,
            currentLapTime = buffer.float,
            sector1TimeInMS = buffer.short.toUShort(),
            sector2TimeInMS = buffer.short.toUShort(),
            bestLapTime = buffer.float,
            bestLapNum = buffer.get().toUByte(),
            bestLapSector1TimeInMS = buffer.short.toUShort(),
            bestLapSector2TimeInMS = buffer.short.toUShort(),
            bestLapSector3TimeInMS = buffer.short.toUShort(),
            bestOverallSector1TimeInMS = buffer.short.toUShort(),
            bestOverallSector1LapNum = buffer.get().toUByte(),
            bestOverallSector2TimeInMS = buffer.short.toUShort(),
            bestOverallSector2LapNum = buffer.get().toUByte(),
            bestOverallSector3TimeInMS = buffer.short.toUShort(),
            bestOverallSector3LapNum = buffer.get().toUByte(),
            lapDistance = buffer.float,
            totalDistance = buffer.float,
            safetyCarDelta = buffer.float,
            carPosition = buffer.get().toUByte(),
            currentLapNum = buffer.get().toUByte(),
            _pitStatus = buffer.get().toUByte(),
            sector = buffer.get().toUByte(),
            _currentLapInvalid = buffer.get().toUByte(),
            penalties = buffer.get().toUByte(),
            gridPosition = buffer.get().toUByte(),
            _driverStatus = buffer.get().toUByte(),
            _resultStatus = buffer.get().toUByte()
        )

    private fun mapLapDataPacket(buffer: ByteBuffer): LapDataPacket =
        LapDataPacket(
            items = generateSequence(0) { it + 1 }.take(CarCount).map { mapLapData(buffer) }
                .toList()
        )

    private fun mapCarStatusData(buffer: ByteBuffer): CarStatusData =
        CarStatusData(
            tractionControl = buffer.get().toUByte(),
            antiLockBrakes = buffer.get().toUByte(),
            _fuelMix = buffer.get().toUByte(),
            frontBrakeBias = buffer.get().toUByte(),
            _pitLimiterStatus = buffer.get().toUByte(),
            fuelInTank = buffer.float,
            fuelCapacity = buffer.float,
            fuelRemainingLaps = buffer.float,
            maxRPM = buffer.short.toUShort(),
            idleRPM = buffer.short.toUShort(),
            maxGears = buffer.get().toUByte(),
            _drsAllowed = buffer.get().toUByte(),
            drsActivationDistance = buffer.short.toUShort(),
            _tyresWear = arrayOf(
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte()
            ),
            _actualTyreCompound = buffer.get().toUByte(),
            _visualTyreCompound = buffer.get().toUByte(),
            tyresAgeLaps = buffer.get().toUByte(),
            _tyresDamage = arrayOf(
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte(),
                buffer.get().toUByte()
            ),
            frontLeftWingDamage = buffer.get().toUByte(),
            frontRightWingDamage = buffer.get().toUByte(),
            rearWingDamage = buffer.get().toUByte(),
            _drsFault = buffer.get().toUByte(),
            engineDamage = buffer.get().toUByte(),
            gearBoxDamage = buffer.get().toUByte(),
            _vehicleFiaFlags = buffer.get(),
            ersStoreEnergy = buffer.float,
            ersDeployMode = buffer.get().toUByte(),
            ersHarvestedThisLapMGUK = buffer.float,
            ersHarvestedThisLapMGUH = buffer.float,
            ersDeployedThisLap = buffer.float
        )

    private fun mapCarStatusDataPacket(buffer: ByteBuffer): CarStatusDataPacket =
        CarStatusDataPacket(
            items = generateSequence(0) { it + 1 }.take(CarCount).map { mapCarStatusData(buffer) }
                .toList()
        )

    private fun mapSessionData(buffer: ByteBuffer): SessionDataPacket =
        SessionDataPacket(
            weather = buffer.get().toUByte(),
            trackTemperature = buffer.get().toUByte(),
            airTemperature = buffer.get(),
            totalLaps = buffer.get().toUByte(),
            trackLength = buffer.short.toUShort(),
            sessionType = buffer.get().toUByte(),
            trackId = buffer.get(),
            formula = buffer.get().toUByte(),
            sessionTimeLeft = buffer.short.toUShort(),
            sessionDuration = buffer.short.toUShort(),
            pitSpeedLimit = buffer.get().toUByte(),
            gamePaused = buffer.get().toUByte(),
            isSpectating = buffer.get().toUByte(),
            spectatorCarIndex = buffer.get().toUByte(),
            sliProNativeSupport = buffer.get().toUByte()
        )

    private fun mapParticipantData(buffer: ByteBuffer): ParticipantData {
        val participantData = ParticipantData(
            aiControlled = buffer.get().toUByte(),
            _driverId = buffer.get().toUByte(),
            teamId = buffer.get().toUByte(),
            raceNumber = buffer.get().toUByte(),
            nationality = buffer.get().toUByte()
        )
        buffer.position(buffer.position() + 49)
        return participantData
    }

    private fun mapParticipantDataPacket(buffer: ByteBuffer): ParticipantDataPacket {
        val count = buffer.get().toUByte()
        return ParticipantDataPacket(
            generateSequence(0) { it + 1 }.take(count.toInt())
                .map { mapParticipantData(buffer) }.toList()
        )
    }

    private fun mapCarSetupData(buffer: ByteBuffer): CarSetupData =
        CarSetupData(
            frontWing = buffer.get().toUByte(),
            rearWing = buffer.get().toUByte(),
            onThrottle = buffer.get().toUByte(),
            offThrottle = buffer.get().toUByte(),
            frontCamber = buffer.float,
            rearCamber = buffer.float,
            frontToe = buffer.float,
            rearToe = buffer.float,
            frontSuspension = buffer.get().toUByte(),
            rearSuspension = buffer.get().toUByte(),
            frontAntiRollBar = buffer.get().toUByte(),
            rearAntiRollBar = buffer.get().toUByte(),
            frontSuspensionHeight = buffer.get().toUByte(),
            rearSuspensionHeight = buffer.get().toUByte(),
            brakePressure = buffer.get().toUByte(),
            brakeBias = buffer.get().toUByte(),
            rearLeftTyrePressure = buffer.float,
            rearRightTyrePressure = buffer.float,
            frontLeftTyrePressure = buffer.float,
            frontRightTyrePressure = buffer.float,
            ballast = buffer.get().toUByte(),
            fuelLoad = buffer.float
        )

    private fun mapCarSetupDataPacket(buffer: ByteBuffer): CarSetupDataPacket =
        CarSetupDataPacket(
            items = generateSequence(0) { it + 1 }.take(CarCount).map { mapCarSetupData(buffer) }
                .toList()
        )

    private fun mapEventData(buffer: ByteBuffer, eventType: EventDetails.Type): EventDetails {
        return when (eventType) {
            EventDetails.Type.SessionStarted -> EmptyEventData // Sent when the session starts
            EventDetails.Type.SessionEnded -> EmptyEventData // Sent when the session ends
            EventDetails.Type.FastestLap -> FastestLapData(
                vehicleIdx = buffer.get().toUByte(),
                lapTime = buffer.float
            ) // When a driver achieves the fastest lap
            EventDetails.Type.Retirement -> Retirement(
                vehicleIdx = buffer.get().toUByte()
            ) // When a driver retires
            EventDetails.Type.DRSEnabled -> EmptyEventData // Race control have enabled DRS
            EventDetails.Type.DRSDisabled -> EmptyEventData // Race control have disabled DRS
            EventDetails.Type.TeammateInPits -> TeamMateInPits(
                vehicleIdx = buffer.get().toUByte()
            ) // Your team mate has entered the pits
            EventDetails.Type.ChequeredFlag -> EmptyEventData // The chequered flag has been waved
            EventDetails.Type.RaceWinner -> RaceWinner(
                vehicleIdx = buffer.get().toUByte()
            ) // The race winner is announced
            EventDetails.Type.PenaltyIssued -> Penalty(
                penaltyType = buffer.get().toUByte(),
                infringementType = buffer.get().toUByte(),
                vehicleIdx = buffer.get().toUByte(),
                otherVehicleIdx = buffer.get().toUByte(),
                time = buffer.get().toUByte(),
                lapNum = buffer.get().toUByte(),
                placesGained = buffer.get().toUByte()
            )  // A penalty has been issued – details in event
            EventDetails.Type.SpeedTrap -> SpeedTrap(
                vehicleIdx = buffer.get().toUByte(),
                speed = buffer.float
            ) // Speed trap has been triggered by fastest speed
            else -> EmptyEventData
        }
    }

    private fun mapEventDataPacket(buffer: ByteBuffer): EventDataPacket {
        val eventTypeBytes = ByteArray(4)
        buffer.get(eventTypeBytes)
        val eventType = EventDetails.Type.getByCode(String(eventTypeBytes))
        return EventDataPacket(
            eventType = eventType,
            details = mapEventData(buffer, eventType)
        )
    }
}
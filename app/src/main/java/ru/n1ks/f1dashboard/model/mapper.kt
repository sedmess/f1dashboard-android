package ru.n1ks.f1dashboard.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

object TelemetryPacketDeserializer {

    private const val CarCount = 22

    fun map(packet: ByteArray): TelemetryPacket<out TelemetryData> {
        val buffer = ByteBuffer.wrap(packet)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
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
            packetFormat = buffer.short,
            gameMajorVersion = buffer.get(),
            gameMinorVersion = buffer.get(),
            packetVersion = buffer.get(),
            packetTypeId = buffer.get(),
            sessionId = buffer.long,
            sessionTimestamp = buffer.float,
            frameId = buffer.int,
            playerCarIndex = buffer.get().toInt(),
            secondaryPlayerCarIndex = buffer.get()
        )

    private fun mapCarTelemetryData(buffer: ByteBuffer): CarTelemetryData =
        CarTelemetryData(
            speed = buffer.short,
            throttle = buffer.float,
            steer = buffer.float,
            brake = buffer.float,
            clutch = buffer.get(),
            gear = buffer.get(),
            engineRPM = buffer.short,
            _drs = buffer.get(),
            revLightsPercent = buffer.get(),
            _brakesTemperature = arrayOf(
                buffer.short,
                buffer.short,
                buffer.short,
                buffer.short
            ),
            _tyresSurfaceTemperature = arrayOf(
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get()
            ),
            _tyresInnerTemperature = arrayOf(
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get()
            ),
            engineTemperature = buffer.short,
            _tyresPressure = arrayOf(
                buffer.float,
                buffer.float,
                buffer.float,
                buffer.float
            ),
            _surfaceType = arrayOf(
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get()
            )
        )


    private fun mapCarTelemetryDataPacket(buffer: ByteBuffer): CarTelemetryDataPacket =
        CarTelemetryDataPacket(
            items = generateSequence(0) { it + 1 }.take(CarCount)
                .map { mapCarTelemetryData(buffer) }.toList(),
            buttonStatus = buffer.int,
            mfdPanelIndex = buffer.get(),
            mfdPanelIndexSecondaryPlayer = buffer.get(),
            suggestedGear = buffer.get()
        )

    private fun mapLapData(buffer: ByteBuffer): LapData =
        LapData(
            lastLapTime = buffer.float,
            currentLapTime = buffer.float,
            _sector1TimeInMS = buffer.short,
            _sector2TimeInMS = buffer.short,
            bestLapTime = buffer.float,
            bestLapNum = buffer.get(),
            _bestLapSector1TimeInMS = buffer.short,
            _bestLapSector2TimeInMS = buffer.short,
            _bestLapSector3TimeInMS = buffer.short,
            _bestOverallSector1TimeInMS = buffer.short,
            bestOverallSector1LapNum = buffer.get(),
            _bestOverallSector2TimeInMS = buffer.short,
            _bestOverallSector2LapNum = buffer.get(),
            _bestOverallSector3TimeInMS = buffer.short,
            bestOverallSector3LapNum = buffer.get(),
            lapDistance = buffer.float,
            totalDistance = buffer.float,
            safetyCarDelta = buffer.float,
            carPosition = buffer.get(),
            currentLapNum = buffer.get(),
            _pitStatus = buffer.get(),
            sector = buffer.get(),
            _currentLapInvalid = buffer.get(),
            penalties = buffer.get(),
            gridPosition = buffer.get(),
            _driverStatus = buffer.get(),
            _resultStatus = buffer.get()
        )

    private fun mapLapDataPacket(buffer: ByteBuffer): LapDataPacket =
        LapDataPacket(
            items = generateSequence(0) { it + 1 }.take(CarCount).map { mapLapData(buffer) }
                .toList()
        )

    private fun mapCarStatusData(buffer: ByteBuffer): CarStatusData =
        CarStatusData(
            tractionControl = buffer.get(),
            antiLockBrakes = buffer.get(),
            _fuelMix = buffer.get(),
            frontBrakeBias = buffer.get(),
            _pitLimiterStatus = buffer.get(),
            fuelInTank = buffer.float,
            fuelCapacity = buffer.float,
            fuelRemainingLaps = buffer.float,
            maxRPM = buffer.short,
            idleRPM = buffer.short,
            maxGears = buffer.get(),
            _drsAllowed = buffer.get(),
            drsActivationDistance = buffer.short,
            _tyresWear = arrayOf(
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get()
            ),
            _actualTyreCompound = buffer.get(),
            _visualTyreCompound = buffer.get(),
            tyresAgeLaps = buffer.get(),
            _tyresDamage = arrayOf(
                buffer.get(),
                buffer.get(),
                buffer.get(),
                buffer.get()
            ),
            frontLeftWingDamage = buffer.get(),
            frontRightWingDamage = buffer.get(),
            rearWingDamage = buffer.get(),
            _drsFault = buffer.get(),
            engineDamage = buffer.get(),
            gearBoxDamage = buffer.get(),
            _vehicleFiaFlags = buffer.get(),
            ersStoreEnergy = buffer.float,
            ersDeployMode = buffer.get(),
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
            weather = buffer.get(),
            trackTemperature = buffer.get(),
            airTemperature = buffer.get(),
            totalLaps = buffer.get(),
            trackLength = buffer.short,
            sessionType = buffer.get(),
            trackId = buffer.get(),
            formula = buffer.get(),
            sessionTimeLeft = buffer.short,
            sessionDuration = buffer.short,
            pitSpeedLimit = buffer.get(),
            gamePaused = buffer.get(),
            isSpectating = buffer.get(),
            spectatorCarIndex = buffer.get(),
            sliProNativeSupport = buffer.get()
        )

    private fun mapParticipantData(buffer: ByteBuffer): ParticipantData {
        val participantData = ParticipantData(
            aiControlled = buffer.get(),
            _driverId = buffer.get(),
            teamId = buffer.get(),
            raceNumber = buffer.get(),
            nationality = buffer.get()
        )
        buffer.position(buffer.position() + 49)
        return participantData
    }

    private fun mapParticipantDataPacket(buffer: ByteBuffer): ParticipantDataPacket {
        val count = buffer.get()
        return ParticipantDataPacket(
            generateSequence(0) { it + 1 }.take(count.toInt())
                .map { mapParticipantData(buffer) }.toList()
        )
    }

    private fun mapCarSetupData(buffer: ByteBuffer): CarSetupData =
        CarSetupData(
            frontWing = buffer.get(),
            rearWing = buffer.get(),
            onThrottle = buffer.get(),
            offThrottle = buffer.get(),
            frontCamber = buffer.float,
            rearCamber = buffer.float,
            frontToe = buffer.float,
            rearToe = buffer.float,
            frontSuspension = buffer.get(),
            rearSuspension = buffer.get(),
            frontAntiRollBar = buffer.get(),
            rearAntiRollBar = buffer.get(),
            frontSuspensionHeight = buffer.get(),
            rearSuspensionHeight = buffer.get(),
            brakePressure = buffer.get(),
            brakeBias = buffer.get(),
            rearLeftTyrePressure = buffer.float,
            rearRightTyrePressure = buffer.float,
            frontLeftTyrePressure = buffer.float,
            frontRightTyrePressure = buffer.float,
            ballast = buffer.get(),
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
                vehicleIdx = buffer.get(),
                lapTime = buffer.float
            ) // When a driver achieves the fastest lap
            EventDetails.Type.Retirement -> Retirement(
                vehicleIdx = buffer.get()
            ) // When a driver retires
            EventDetails.Type.DRSEnabled -> EmptyEventData // Race control have enabled DRS
            EventDetails.Type.DRSDisabled -> EmptyEventData // Race control have disabled DRS
            EventDetails.Type.TeammateInPits -> TeamMateInPits(
                vehicleIdx = buffer.get()
            ) // Your team mate has entered the pits
            EventDetails.Type.ChequeredFlag -> EmptyEventData // The chequered flag has been waved
            EventDetails.Type.RaceWinner -> RaceWinner(
                vehicleIdx = buffer.get()
            ) // The race winner is announced
            EventDetails.Type.PenaltyIssued -> Penalty(
                penaltyType = buffer.get(),
                infringementType = buffer.get(),
                vehicleIdx = buffer.get(),
                otherVehicleIdx = buffer.get(),
                time = buffer.get(),
                lapNum = buffer.get(),
                placesGained = buffer.get()
            )  // A penalty has been issued – details in event
            EventDetails.Type.SpeedTrap -> SpeedTrap(
                vehicleIdx = buffer.get(),
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
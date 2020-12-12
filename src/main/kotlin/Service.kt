package com.xobotun.rxproject.martianagrobot

import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.HashMap
import kotlin.collections.HashSet

// ChatId to whatever
val users: MutableMap<Long, Any?> = HashMap()
// PlantId to sensor
val plantSensors: MutableMap<Long, MutableMap<SensorType, MutableSet<FakeSensor>>> = HashMap()
// Code to sensor
val sensors: MutableMap<String, FakeSensor> = HashMap()

fun userPresent(chatId: Long) = users.containsKey(chatId)

fun registerUser(chatId: Long) { users[chatId] = null }

fun deleteUser(chatId: Long) { users.remove(chatId) }

fun sensorPresent(code: String) = sensors.containsKey(code)

fun getSensor(code: String) = sensors[code]

fun getSensors(plantId: Long) = plantSensors[plantId]

fun createSensor(code: String): FakeSensor? {
    return try {
        val new = decryptSensorCode(code)
        sensors[code] = new
        plantSensors.computeIfAbsent(new.plantId) { EnumMap(SensorType::class.java) }.computeIfAbsent(new.sensorType) { HashSet() }.add(new)
        new
    } catch (e: Exception) {
        null
    }
}

private fun decryptSensorCode(code: String): FakeSensor {
    val regex = "Т(\\d+)([ДТВУКСОМ])(\\d+)".toRegex()
    val matchResult = regex.matchEntire(code)!!

    val plantId = matchResult.groups[1]!!.value.toLong()
    val sensorId = matchResult.groups[3]!!.value.toLong()

    val type = when(matchResult.groups[2]!!.value) {
        "Д" -> SensorType.PRESSURE
        "Т" -> SensorType.TEMPERATURE
        "В" -> SensorType.HUMIDITY
        "У" -> SensorType.ACCELEROMETER
        "К" -> SensorType.ACIDITY
        "С" -> SensorType.LIGHT
        "О" -> SensorType.LIGHT
        "М" -> SensorType.MICROELEMENTS
        else -> throw IllegalArgumentException()
    }

    return FakeSensor(code, plantId, sensorId, type)
}

class FakeSensor (
    val code: String,
    val plantId: Long,
    val sensorId: Long,
    val sensorType: SensorType,
) {

    fun lowBoundary() = sensorType.mean - sensorType.deviation
    fun highBoundary() = sensorType.mean + sensorType.deviation
    fun current() = ThreadLocalRandom.current().nextDouble(lowBoundary(), highBoundary())
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FakeSensor) return false

        if (code != other.code) return false
        if (plantId != other.plantId) return false
        if (sensorId != other.sensorId) return false
        if (sensorType != other.sensorType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + plantId.hashCode()
        result = 31 * result + sensorId.hashCode()
        result = 31 * result + sensorType.hashCode()
        return result
    }
}

enum class SensorType(
    val type: String,
    val unit: String,
    val mean: Double,
    val deviation: Double,
) {
    PRESSURE("Давление", "кПа", 101.325, 2.0),
    TEMPERATURE("Температура", "К", 293.2, 15.0),
    HUMIDITY("Влажность", "%", 50.0, 15.0),
    ACCELEROMETER("Ускорение", "м/c²", 3.86, 0.001),
    ACIDITY("Кислотность", "pH", 5.75, 0.75),
    LIGHT("Освещённость", "люкс", 7500.0, 1500.0),
    MICROELEMENTS("Микроэлементы", "ед.", 10.0, 1.0),
}

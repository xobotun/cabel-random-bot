package com.xobotun.rxproject.martianagrobot

import java.util.concurrent.ThreadLocalRandom

// ChatId to whatever
val users: MutableMap<Long, Any?> = HashMap()
// PlantId to sensor
val plantSensors: MutableMap<Long, MutableSet<FakeSensor>> = HashMap()
// Code to sensor
val sensors: MutableMap<String, FakeSensor> = HashMap()

fun userPresent(chatId: Long) = users.containsKey(chatId)

fun registerUser(chatId: Long) { users[chatId] = null }

fun deleteUser(chatId: Long) { users.remove(chatId) }

fun sensorPresent(code: String) = sensors.containsKey(code)

fun getSensor(code: String) = sensors[code]

fun createSensor(code: String): FakeSensor? {
    return try {
        val new = decryptSensorCode(code)
        sensors[code] = new
        plantSensors.computeIfAbsent(new.plantId) { HashSet() }.add(new)
        new
    } catch (e: Exception) {
        null
    }
}

private fun decryptSensorCode(code: String): FakeSensor {
    val regex = "Т(\\d+)([ДТВУКСО])(\\d+)".toRegex()
    val matchResult = regex.matchEntire(code)!!

    val plantId = matchResult.groups[1]!!.value.toLong()
    val sensorId = matchResult.groups[3]!!.value.toLong()

    val type = when(matchResult.groups[2]!!.value) {
        "Д" -> SensorType.PRESSURE
        "T" -> SensorType.TEMPERATURE
        "В" -> SensorType.HUMIDITY
        "У" -> SensorType.ACCELEROMETER
        "К" -> SensorType.ACIDITY
        "С" -> SensorType.LIGHT
        "О" -> SensorType.LIGHT
        else -> throw IllegalArgumentException()
    }

    return FakeSensor(plantId, sensorId, type)
}

class FakeSensor (
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

        if (plantId != other.plantId) return false
        if (sensorId != other.sensorId) return false
        if (sensorType != other.sensorType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = plantId.hashCode()
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
}

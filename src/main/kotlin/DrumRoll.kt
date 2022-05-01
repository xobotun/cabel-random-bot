package com.xobotun.tinkoff.cabel

import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

const val animationLength = 10 // seconds
const val fps = 1 // message updates per second
const val totalFrames = animationLength * fps
const val sleepMilliTime = 1000L / fps

// It happened the way positive are upwards and negatives are downwards. Whatever.
const val drumRevolutions = -1
const val pooRevolutions = 3
const val tadaRevolutions = 5

class DrumRoll {
    val randomToday = run {
        println("Initializing the daily random")
        Random(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.of("+03:00")).toEpochMilli())
    }
    val winnerId = randomToday.randomExcept().also { println("Winner will be #$it") }
    val loserId = randomToday.randomExcept(winnerId).also { println("Loser will be #$it") }

    val shuffledUsers = userList.shuffled().also { println("Shuffled the list") }
    val winnerPos = shuffledUsers.indexOf(userList[winnerId]).also { println("Winner's position is #$it in the shuffled list") }
    val loserPos = shuffledUsers.indexOf(userList[loserId]).also { println("Loser's position is #$it in the shuffled list") }

    var currentFrame = 0

    fun advanceFrame(): Boolean {
        if (currentFrame < totalFrames) {
            currentFrame++
            return true
        }
        return false
    }

    fun render(): String {
        val drumRevolution = getRotationPos_linear(drumRevolutions, currentFrame)
        val tadaRevolution = getRotationPos_linear(tadaRevolutions, currentFrame)
        val pooRevolution = getRotationPos_linear(pooRevolutions, currentFrame)
        println("Roll revolutions: $drumRevolution, $tadaRevolution, $pooRevolution")

        val drumOffset = (drumRevolution * userList.size).roundToInt() % userList.size
        val tadaOffset = (tadaRevolution * userList.size + winnerPos).roundToInt() % userList.size
        val pooOffset = (pooRevolution * userList.size + loserPos).roundToInt() % userList.size
        println("Roll offsets: $drumOffset, $tadaOffset, $pooOffset")

        val rotatedList = shuffledUsers.offset(drumOffset)
        val printableList = rotatedList.map { "$rollWallLeft${it.padEnd(maxUserLength, ' ')}$rollWallRight" }.toMutableList()
        if (tadaOffset == pooOffset) {
            printableList[tadaOffset] = "$pooChar$rollWallSpecialLeft${rotatedList[tadaOffset].padEnd(maxUserLength, ' ')}$rollWallSpecialRight$tadaChar"
        } else {
            printableList[tadaOffset] = "$tadaChar$rollWallSpecialLeft${rotatedList[tadaOffset].padEnd(maxUserLength, ' ')}$rollWallSpecialRight$tadaChar"
            printableList[pooOffset] = "$pooChar$rollWallSpecialLeft${rotatedList[pooOffset].padEnd(maxUserLength, ' ')}$rollWallSpecialRight$pooChar"
        }

        val result = StringBuilder()
        result.append(rollTop).append('\n')
        printableList.forEach { result.append(it).append('\n') }
        result.append(rollBottom)

        println(result)
        return result.toString()
    }
}

fun Random.randomExcept(winnerPos: Int? = null): Int {
    var loserPos = nextInt().absoluteValue % userList.size
    while (loserPos == winnerPos) {
        loserPos = nextInt().absoluteValue % userList.size
    }
    return loserPos
}


fun getRotationPos_linear(revolutions: Int, currentFrame: Int): Double {
    val elapsedFraction = 1.0 * currentFrame / totalFrames

    // y = ax + b
    // b is number of revolutions,
    // a is deceleration rate, must end within [animationLength]
    // y is roll position,
    // x is current time

    // a = (y-b)/x. At y == 0: -b/x, where x == 100%, so it is just -b.
    val pos = -revolutions * elapsedFraction + revolutions

    return pos
}

fun <T> List<T>.offset(offset: Int): List<T> {
    if (offset == 0) return this
    val positiveWards = offset > 0

    if (positiveWards) {
        return subList(offset, size) + subList(0, offset)
    } else {
        // Offset is negative here
        val offsetFromTail = size + offset
        return subList(offsetFromTail, size) + subList(0, offsetFromTail)
    }
}

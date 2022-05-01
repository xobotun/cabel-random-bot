package com.xobotun.tinkoff.cabel

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ParseMode.MARKDOWN
import com.github.kotlintelegrambot.entities.polls.PollType
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {

    val bot = describeBot()
    bot.startWebhook()

    val server = embeddedServer(Netty, port = 80) {
        routing {
            post("/") {
                val receivedBody = call.receiveText()
                println("received: $receivedBody")
                bot.processUpdate(receivedBody)
                call.respondText("OK", ContentType.Text.Plain);
            }
        }
    }
    server.start(wait = true)
}

fun describeBot() = bot {

    token = botToken
    timeout = 30
    logLevel = LogLevel.Network.Body
    webhook {
        url = "https://cabel-random-bot.xobotun.com/"
    }

    dispatch {
        command("start") {
            val chatId = update.message!!.chat.id;

            println("Message received: $update")
            bot.playStartAnimation(chatId)
        }

        telegramError {
            println("Error: " + error.getErrorMessage())
            println("Full error: $error")
        }
    }
}

fun Bot.playStartAnimation(chatId: Long) {
    println("Creating DrumRoll")
    val drumRoll = DrumRoll()

    println("Sendining initial message")
    val init = sendMessage(chatId = chatId, text = "```\n${drumRoll.render()}\n```", parseMode = MARKDOWN, disableNotification = true)
    val msgId = init.first!!.body()!!.result!!.messageId
    println("Sent message #$msgId")

    while (drumRoll.advanceFrame()) {
        Thread.sleep(sleepMilliTime)
        println("Editing message #$msgId with frame #${drumRoll.currentFrame}")
        editMessageText(chatId = chatId, messageId = msgId, text = "```\n${drumRoll.render()}\n```", parseMode = MARKDOWN)
    }

    println("Sending ulitimative poll")
    sendPoll(chatId, "Роли определены", listOf("Радоваться", "Грустить"), type = PollType.QUIZ, correctOptionId = 0, disableNotification = false)
}


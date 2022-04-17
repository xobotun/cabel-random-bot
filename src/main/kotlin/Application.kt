package com.xobotun.tinkoff.cabel

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ParseMode.MARKDOWN
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApp
import com.github.badoualy.telegram.tl.api.auth.TLSentCode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

val client = Kotlogram.getDefaultClient(config, InMemoryApiStorage())
var codeHash: TLSentCode? = null

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

            val fullChatInfo = client.messagesGetFullChat(chatId.toInt())
            val message = "Users: " + fullChatInfo.users.map { it.id }.joinToString()

            bot.sendMessage(chatId, message)
//            bot.playStartAnimation(chatId)
        }

        command("reg") {
            client.authSendCode(false, phoneNumber, true)
        }

        command("reg-code") {
            client.authSignIn(phoneNumber, codeHash!!.phoneCodeHash, message.text!!.removePrefix("/reg-code "))
        }

        command("reg-pass") {
            val authorization = client.authCheckPassword(message.text!!.removePrefix("/reg-pass "))
            authorization.user.asUser.apply {
                println("You are now signed in as $firstName $lastName @$username")
            }
        }

        telegramError {
            println(error.getErrorMessage())
        }
    }
}

fun Bot.playStartAnimation(chatId: Long) {
    val dotDelay = 750L;
    val uncertainConnectionDelay = 2500L;
    val fastDataFetchInitDelay = 500L;
    val finalMessageDelay = 1750L;

    var animation = "Connecting to RxCorporation database"

    val init = sendMessage(chatId = chatId, text = "```\n$animation\n```", parseMode = MARKDOWN)
    val msgId = init.first!!.body()!!.result!!.messageId;


    for (i in 0..2) {
        animation += "."
        editAfterDelay(chatId, msgId, "```\n$animation\n```", MARKDOWN, dotDelay);
    }

    animation += "\nMartian database connection established."
    editAfterDelay(chatId, msgId, "```\n$animation\n```", MARKDOWN, uncertainConnectionDelay);

    animation += "\nReading user profile data"
    editAfterDelay(chatId, msgId, "```\n$animation\n```", MARKDOWN, fastDataFetchInitDelay);

    for (i in 0..2) {
        animation += "."
        editAfterDelay(chatId, msgId, "```\n$animation\n```", MARKDOWN, dotDelay);
    }

    animation = """
        Подключаемся к базе данных RxCorporation...
        Подключение к марсианской базе данных установлено.
        Считываем профиль пользователя...
        
        Добро пожаловать в систему уведомлений и мониторинга RxMartianBot!
    """.trimIndent()

    editAfterDelay(chatId, msgId, "```\n$animation\n```", MARKDOWN, finalMessageDelay);
}

fun Bot.editAfterDelay(chatId: Long, messageId: Long, newMessage: String, parseMode: ParseMode, delay: Long) {
    if (delay > 0) Thread.sleep(delay)

    editMessageText(chatId = chatId, messageId = messageId, text = newMessage, parseMode = parseMode)
}

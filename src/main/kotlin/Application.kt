package com.xobotun.rxproject.martianagrobot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ParseMode.MARKDOWN
import com.github.kotlintelegrambot.entities.ParseMode.MARKDOWN_V2
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.TelegramFile.ByUrl
import com.github.kotlintelegrambot.entities.dice.DiceEmoji
import com.github.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult
import com.github.kotlintelegrambot.entities.inlinequeryresults.InputMessageContent
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
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

    token = "secret"
    timeout = 30
    logLevel = LogLevel.Network.Body
    webhook {
        url = "https://rx-martian-agro-bot.xobotun.com/"
    }

    dispatch {
        command("start") {
            val chatId = update.message!!.chat.id;

            if (userPresent(chatId)) {
                bot.sendRepeatingStart(chatId);
            } else {
                registerUser(chatId)

                bot.playStartAnimation(chatId)
                Thread.sleep(750);
                bot.sendAbout(chatId)
                Thread.sleep(250);
                bot.sendMessage(chatId = chatId, text = "Вам наверняка не терпится начать. Вот краткая справка:")
                bot.sendHelp(chatId)
            }
        }

        command("about") {
            val chatId = update.message!!.chat.id;
            bot.sendAbout(chatId)
        }

        command("help") {
            val chatId = update.message!!.chat.id;
            bot.sendHelp(chatId)
        }

        command("exit") {
            val chatId = update.message!!.chat.id;

            deleteUser(chatId)
            bot.sendExit(chatId)
        }

        command("status") {
            val chatId = update.message!!.chat.id;

            val joinedArgs = args.joinToString()
            val response = if (joinedArgs.isNotBlank()) "`TODO: Показывает статус датчика`" else "`TODO: Показывает статус теплицы в целом`"
            bot.sendMessage(chatId = chatId, parseMode = MARKDOWN, text = response)
        }

        command("add") {
            val chatId = update.message!!.chat.id;

            val joinedArgs = args.joinToString()
            if (joinedArgs.isBlank()) {
                bot.sendMessage(chatId = chatId, text = "Укажите идентификатор датчика, находящийся на ближайшей к вам стороне корпуса")
                return@command
            }

            bot.sendMessage(chatId = chatId, parseMode = MARKDOWN, text = "`TODO: Добавляет датчик или ругается`")
        }

        command("markdownV2") {
            val markdownV2Text = """
                    *bold \*text*
                    _italic \*text_
                    __underline__
                    ~strikethrough~
                    *bold _italic bold ~italic bold strikethrough~ __underline italic bold___ bold*
                    [inline URL](http://www.example.com/)
                    [inline mention of a user](tg://user?id=123456789)
                    `inline fixed-width code`
                    ```kotlin
                    fun main() {
                        println("Hello Kotlin!")
                    }
                    ```
                    test-test-test
                """.trimIndent()
            bot.sendMessage(
                    chatId = message.chat.id,
                    text = markdownV2Text,
                    parseMode = MARKDOWN_V2
            )
        }

        command("inlineButtons") {
            val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(InlineKeyboardButton.CallbackData(text = "Test Inline Button", callbackData = "testButton")),
                    listOf(InlineKeyboardButton.CallbackData(text = "Show alert", callbackData = "showAlert"))
            )
            bot.sendMessage(
                    chatId = message.chat.id,
                    text = "Hello, inline buttons!",
                    replyMarkup = inlineKeyboardMarkup
            )
        }

        command("userButtons") {
            val keyboardMarkup = KeyboardReplyMarkup(keyboard = generateUsersButton(), resizeKeyboard = true)
            bot.sendMessage(
                    chatId = message.chat.id,
                    text = "Hello, users buttons!",
                    replyMarkup = keyboardMarkup
            )
        }

        command("mediaGroup") {
            bot.sendMediaGroup(
                    chatId = message.chat.id,
                    mediaGroup = MediaGroup.from(
                            InputMediaPhoto(
                                    media = ByUrl("https://www.sngular.com/wp-content/uploads/2019/11/Kotlin-Blog-1400x411.png"),
                                    caption = "I come from an url :P"
                            ),
                            InputMediaPhoto(
                                    media = ByUrl("https://www.sngular.com/wp-content/uploads/2019/11/Kotlin-Blog-1400x411.png"),
                                    caption = "Me too!"
                            )
                    ),
                    replyToMessageId = message.messageId
            )
        }

        callbackQuery("testButton") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(chatId, callbackQuery.data)
        }

        callbackQuery(
                callbackData = "showAlert",
                callbackAnswerText = "HelloText",
                callbackAnswerShowAlert = true
        ) {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(chatId, callbackQuery.data)
        }

        text("ping") {
            bot.sendMessage(chatId = message.chat.id, text = "Pong")
        }

        location {
            bot.sendMessage(
                    chatId = message.chat.id,
                    text = "Your location is (${location.latitude}, ${location.longitude})",
                    replyMarkup = ReplyKeyboardRemove()
            )
        }

        contact {
            bot.sendMessage(
                    chatId = message.chat.id,
                    text = "Hello, ${contact.firstName} ${contact.lastName}",
                    replyMarkup = ReplyKeyboardRemove()
            )
        }

        channel {
            // Handle channel update
        }

        inlineQuery {
            val queryText = inlineQuery.query

            if (queryText.isBlank() or queryText.isEmpty()) return@inlineQuery

            val inlineResults = (0 until 5).map {
                InlineQueryResult.Article(
                        id = it.toString(),
                        title = "$it. $queryText",
                        inputMessageContent = InputMessageContent.Text("$it. $queryText"),
                        description = "Add $it. before you word"
                )
            }
            bot.answerInlineQuery(inlineQuery.id, inlineResults)
        }

        photos {
            bot.sendMessage(
                    chatId = message.chat.id,
                    text = "Wowww, awesome photos!!! :P"
            )
        }

        command("diceAsDartboard") {
            bot.sendDice(message.chat.id, DiceEmoji.Dartboard)
        }

        dice {
            bot.sendMessage(message.chat.id, "A dice ${dice.emoji.emojiValue} with value ${dice.value} has been received!")
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

fun Bot.sendRepeatingStart(chatId: Long) = sendMessage(chatId = chatId, text = """
    Вы уже зарегистрированы в системе. Используйте /help, если вы заблудились.

    Если вам понравилась заставка, и вы хотите увидеть её ещё раз – увы, она крайне нестабильная. Да и вообще, до Марса путь неблизкий, не надо к нему лишний раз подключаться. :)
""".trimIndent())

fun Bot.sendAbout(chatId: Long) = sendMessage(chatId = chatId, parseMode = MARKDOWN, text = """
    Система уведомлений и мониторинга позволяет:
     • быстро получать уведомления и отчёты на личный коммуникатор колониста (настраивается в личном кабинете в приложении),
     • запрашивать статус датчиков в теплице и пересылать их другим, оставаясь в экосистеме чата.
     • санкционировать регистрацию свежеустановленных датчиков в системе мониторинга.

    Это демо-версия приложения. Стоимость полной версии вы можете запросить в марсианском отделе инфоагромаркетинга RxCorporation.
    
    ```
    >> RxMartianBot v0.3.14 <<
    >> $chatId <<
    ```
""".trimIndent())

fun Bot.sendHelp(chatId: Long) = sendMessage(chatId = chatId, parseMode = MARKDOWN, text = """
    Список доступных команд:
     • /about – общая информация о приложении
     • /help – показывает это сообщение
     • /exit – выход из системы
     ► /status – показывает статус теплицы [демо-версия ограничена одной теплицей]
     ► /status `id датчика` – показывает статус датчика
     ► /add `id датчика` – регистрирует датчик в системе 

    Команды, помеченные "►" так же доступны в виде кнопок. Это удобнее для людей.

    Более полную информацию вы можете получить в марсианском отделе моральной поддержки RxCorporation. 
""".trimIndent())

fun Bot.sendExit(chatId: Long) = sendMessage(chatId = chatId, text = """
    Вы вышли из системы.

    Уведомления отключены.
    
    История сообщений не удаляется, вы можете переслать важную информацию коллегам.
    Удалите историю вручную, если в ней были конфиденциальные данные! 
""".trimIndent())

fun generateUsersButton(): List<List<KeyboardButton>> {
    return listOf(
            listOf(KeyboardButton("Request location (not supported on desktop)", requestLocation = true)),
            listOf(KeyboardButton("Request contact", requestContact = true))
    )
}

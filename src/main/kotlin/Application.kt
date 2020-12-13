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
import java.text.DecimalFormat
import java.util.*
import java.util.Collections.sort
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors
import kotlin.collections.ArrayList

fun main(args: Array<String>) {

    initGrowPlant()

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

fun initGrowPlant() {
    val max = 3;
    val skipChance = 0.15;

    listOf("Д", "Т", "В", "У", "К", "С", "М").forEach {
//        if (it != "М" && ThreadLocalRandom.current().nextDouble() < skipChance) return@forEach
        for (i in 1..ThreadLocalRandom.current().nextInt(1, max + 1)) createSensor("Т1$it$i")
    }
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
            if (joinedArgs.isBlank()) {
                val plantId = 1L
                bot.processPlant(chatId, plantId)
            } else {
                val sensor = getSensor(joinedArgs)
                if (sensor == null) {
                    bot.sendMessage(chatId = chatId, text = "Датчик с таким идентификатором не найден")
                    return@command
                } else {
                    bot.sendSensor(chatId, sensor)
                }
            }
        }

        command("add") {
            val chatId = update.message!!.chat.id;

            val joinedArgs = args.joinToString()
            if (joinedArgs.isBlank()) {
                bot.sendMessage(chatId = chatId, text = "Укажите идентификатор датчика, находящийся на ближайшей к вам стороне корпуса")
                return@command
            }

            if (sensorPresent(joinedArgs)) {
                bot.sendMessage(chatId = chatId, text = "Этот датчик уже зарегистрирован. Вот его показания:")
                bot.sendSensor(chatId, getSensor(joinedArgs)!!)
                return@command
            }

            val sensor = createSensor(joinedArgs)
            if (sensor == null ) {
                bot.sendMessage(chatId = chatId, text = "Датчик с таким идентификатором не найден. Если вы считаете, что ввели его номер корректно и он – не галлюцинация, обратитесь в службу поддержки для замены датчика")
                return@command
            }

            bot.sendMessage(chatId = chatId, text = "Добавлен новый датчик:")
            bot.sendSensor(chatId, sensor)
        }

        callbackQuery("add") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(chatId = chatId, parseMode = MARKDOWN, text = "Регистрация датчиков в текущей версии осуществляется по команде `/add Т<идентификатор теплицы><тип датчика><идентификатор датчика>`. Литеры датчиков: `ДТВУКСОМ`. В следующей версии под это будет отдельный интерфейс.")
        }

        callbackQuery("t_status") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.processPlant(chatId, 1L)
        }

        // Does not trigger. :/
        callbackQuery("s_status") {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(chatId, callbackQuery.toString())
        }

        callbackQuery(
                callbackData = "showAlert",
                callbackAnswerText = "Для покупки полной версии обратитесь в марсианской отдел инфоагромаркетинга RxCorporation. Функциональность платежей в демо-версии приложения отключена",
                callbackAnswerShowAlert = true
        ) {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            // Do nothing, alert is shown on the client
        }

        callbackQuery {
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery

            if (callbackQuery.data.startsWith("s_status")) {
                val sensor = getSensor("s_status (.*)".toRegex().matchEntire(callbackQuery.data)!!.groups[1]!!.value)
                if (sensor == null) {
                    bot.sendMessage(chatId = chatId, text = "Датчик с таким идентификатором не найден")
                    setLatestSensorMessage(chatId, null)
                    return@callbackQuery
                } else {
                    if (getLatestSensorMessage(chatId) != null && getLatestMessage(chatId) == getLatestSensorMessage(chatId)) bot.deleteMessage(chatId, getLatestSensorMessage(chatId)!!)

                    val result = bot.sendSensor(chatId, sensor)
                    setLatestSensorMessage(chatId, result.first!!.body()!!.result!!.messageId)
                    setLatestMessage(chatId, result.first!!.body()!!.result!!.messageId)
                }
            }
        }

        text {
            val chatId = update.message!!.chat.id;
            setLatestMessage(chatId, update.message!!.messageId)
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
""".trimIndent(),
    replyMarkup = InlineKeyboardMarkup.create(
        listOf(InlineKeyboardButton.CallbackData(text = "Статус теплицы", callbackData = "t_status")),
        listOf(InlineKeyboardButton.CallbackData(text = "Добавить датчик", callbackData = "add"), InlineKeyboardButton.CallbackData(text = "Купить приложение", callbackData = "showAlert"))
    )
)

fun Bot.sendExit(chatId: Long) = sendMessage(chatId = chatId, text = """
    Вы вышли из системы.

    Уведомления отключены.
    
    История сообщений не удаляется, вы можете переслать важную информацию коллегам.
    Удалите историю вручную, если в ней были конфиденциальные данные! 
""".trimIndent())

fun Bot.sendSensor(chatId: Long, sensor: FakeSensor) = sendMessage(chatId = chatId, parseMode = MARKDOWN, text = """
    `Т${sensor.plantId}${sensor.sensorType.type[0]}${sensor.sensorId}`
    Теплица: `${sensor.plantId}`
    Датчик: `${sensor.sensorId}`
    Тип: `${sensor.sensorType.type}`
    Нижняя граница: `${sensor.lowBoundary().format(2)} ${sensor.sensorType.unit}`
    Верхняя граница: `${sensor.highBoundary().format(2)} ${sensor.sensorType.unit}`
    Текущее значение: `${sensor.current().format(2)} ${sensor.sensorType.unit} (норма)`
""".trimIndent())

fun Bot.processPlant(chatId: Long, plantId: Long) {
    val sensors = getSensors(1)
    if (sensors == null || sensors.isEmpty()) {
        sendMessage(chatId = chatId, text = "В теплице $plantId не установлены датчики! Возможно, последствия вчерашней пылевой бури и потери электроснабжения. Необходима ручная перерегистрация")
        return
    }

    val messages = ArrayList<String>()
    val sensorButtons = ArrayList<List<InlineKeyboardButton>>()

    Arrays.stream(SensorType.values())
        .map { sensors[it] }
        .filter(Objects::nonNull)
        .map { it!!.toMutableList() }
        .peek { it.sortBy { it.code } }
        .forEach {
            messages.add(
            """
                Тип: ${it[0].sensorType.type}
                Датчики: ${it.joinToString(", ") { "`${it.code}`" }}
                Среднее значение: `${it.map { it.current() }.average().format(2)}` ${it[0].sensorType.unit}
            """.trimIndent()
            )

            sensorButtons.add(it.map { InlineKeyboardButton.CallbackData(text = it.code, callbackData = "s_status ${it.code}") })
        }

    sendPlant(chatId, plantId, messages, sensorButtons)
}

fun Bot.sendPlant(chatId: Long, plantId: Long, messages: List<String>, buttons: List<List<InlineKeyboardButton>>) = sendMessage(chatId = chatId, parseMode = MARKDOWN, text = """
    Теплица: `$plantId`
    
${messages.joinToString("\n\n") { it }}
""".trimIndent(),
replyMarkup = InlineKeyboardMarkup.create(buttons))


fun Double.format(fracDigits: Int): String {
    val df = DecimalFormat()
    df.maximumFractionDigits = fracDigits
    return df.format(this)
}

package com.xobotun.rxproject.martianagrobot

// ChatId to whatever
val database: MutableMap<Long, Any?> = HashMap()

fun userPresent(chatId: Long) = database.containsKey(chatId)

fun registerUser(chatId: Long) { database[chatId] = null }

fun deleteUser(chatId: Long) { database.remove(chatId) }

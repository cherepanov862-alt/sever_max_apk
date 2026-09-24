package ru.severmax.control

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Мини-клиент Telegram Bot API и обработка команд. */
object Tg {
    private const val BASE = "https://api.telegram.org/bot"

    private const val HELP =
        "Команды:\n" +
            "▶ Запуск — старт на 30 мин (или /run 45)\n" +
            "⏹ Стоп — остановка\n" +
            "📊 Статус — запрос статуса\n" +
            "/temp 65 — температура нагрева (30–90)\n" +
            "/boost 90 30 — режим догрева (верх низ)"

    private val keyboard: String = JSONObject()
        .put(
            "keyboard",
            JSONArray()
                .put(JSONArray().put("▶ Запуск").put("⏹ Стоп"))
                .put(JSONArray().put("📊 Статус").put("ℹ️ Помощь"))
        )
        .put("resize_keyboard", true)
        .toString()

    fun call(
        token: String,
        method: String,
        params: Map<String, String>,
        readTimeoutMs: Int = 15000
    ): JSONObject {
        val body = params.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
        val conn = URL("$BASE$token/$method").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = readTimeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(body.toByteArray()) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    /** Отправка сообщения (вызывать не из главного потока). */
    fun send(chatId: String, text: String) {
        val token = Repo.tgToken
        if (token.isBlank() || chatId.isBlank()) return
        try {
            call(
                token, "sendMessage",
                mapOf("chat_id" to chatId, "text" to text, "reply_markup" to keyboard)
            )
        } catch (_: Exception) {
        }
    }

    /** Пересылка SMS подогревателя в Telegram. Вызывается из SmsReceiver (главный поток). */
    fun forward(text: String) {
        if (!Repo.tgEnabled || Repo.tgChat.isBlank() || Repo.tgToken.isBlank()) return
        val extra = ErrorCodes.find(text)?.let { "\n⚠️ ${it.first}: ${it.second}" } ?: ""
        val chat = Repo.tgChat
        Thread { send(chat, "📩 Подогреватель: $text$extra") }.start()
    }

    /** Разбор команды из Telegram. Работает в потоке службы. */
    fun handle(ctx: Context, chatId: String, textRaw: String) {
        if (Repo.tgChat.isBlank() || chatId != Repo.tgChat) {
            send(
                chatId,
                "Доступ закрыт. Ваш chat id: $chatId\n" +
                    "Впишите его в приложении Севермакс (Настройки → Telegram-бот)."
            )
            return
        }

        val t = textRaw.trim()
        val parts = t.split(Regex("\\s+"))
        val cmd = parts[0].lowercase().substringBefore("@")
        val num = parts.drop(1).mapNotNull { it.toIntOrNull() }
        val model = Repo.model

        var sms: String? = null
        when {
            cmd == "/start" || cmd == "/help" || t.startsWith("ℹ") -> {
                send(chatId, "$HELP\n\nТекущая модель: ${model.title} (меняется в приложении, Настройки).")
                return
            }
            cmd == "/run" || t.startsWith("▶") -> {
                val m = num.firstOrNull() ?: 30
                if (m !in 1..999) {
                    send(chatId, "Время работы: от 1 до 999 минут")
                    return
                }
                sms = Cmd.start(model, m)
            }
            cmd == "/stop" || t.startsWith("⏹") -> sms = Cmd.stop(model)
            cmd == "/status" || t.startsWith("📊") -> sms = Cmd.status(model)
            cmd == "/temp" -> {
                val v = num.firstOrNull()
                if (v == null || v !in 30..90) {
                    send(chatId, "Пример: /temp 65 (от 30 до 90 °C)")
                    return
                }
                sms = Cmd.setTemp(model, v)
                if (sms == null) {
                    send(chatId, "Модель ${model.title} не поддерживает установку температуры по SMS")
                    return
                }
            }
            cmd == "/boost" -> {
                val u = num.getOrNull(0)
                val d = num.getOrNull(1)
                if (u == null || d == null || u > 90 || d < 30 || u - d < 10) {
                    send(chatId, "Пример: /boost 90 30 (30–90 °C, разница не менее 10)")
                    return
                }
                sms = Cmd.boost(model, u, d)
                if (sms == null) {
                    send(chatId, "Модель ${model.title} не поддерживает режим догрева")
                    return
                }
            }
            else -> {
                send(chatId, "Не понял команду. Напишите /help")
                return
            }
        }

        val smsText: String = sms ?: return
        if (Repo.phone.isBlank()) {
            send(chatId, "В приложении не указан номер SIM подогревателя")
            return
        }
        Sms.send(ctx, Repo.phone, smsText)
            .onSuccess {
                Repo.addLog(false, smsText)
                send(chatId, "✅ SMS отправлено: $smsText\nЖду ответ подогревателя…")
            }
            .onFailure { send(chatId, "❌ Не удалось отправить SMS: ${it.message}") }
    }
}

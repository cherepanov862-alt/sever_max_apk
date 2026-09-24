package ru.severmax.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import org.json.JSONArray

/** Служба переднего плана: опрашивает Telegram и выполняет команды. */
class TelegramService : Service() {
    @Volatile
    private var running = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Repo.init(this)
        ServiceCompat.startForeground(
            this, 1, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        if (!running) {
            running = true
            worker = Thread { loop() }.also { it.start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("bot", "Telegram-бот", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, "bot")
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Севермакс")
            .setContentText("Telegram-бот работает")
            .setOngoing(true)
            .build()
    }

    private fun loop() {
        val ctx = applicationContext
        var offset = Repo.tgOffset
        while (running) {
            try {
                val token = Repo.tgToken
                if (token.isBlank()) {
                    Thread.sleep(5000)
                    continue
                }
                val res = Tg.call(
                    token, "getUpdates",
                    mapOf("timeout" to "25", "offset" to offset.toString()),
                    40000
                )
                if (!res.optBoolean("ok")) {
                    Thread.sleep(5000)
                    continue
                }
                val arr = res.optJSONArray("result") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val u = arr.getJSONObject(i)
                    offset = u.getLong("update_id") + 1
                    Repo.tgOffset = offset
                    val msg = u.optJSONObject("message") ?: continue
                    val text = msg.optString("text", "")
                    if (text.isEmpty()) continue
                    // Защита: старые сообщения (накопившиеся, пока бот был выключен) не выполняем.
                    val date = msg.optLong("date", 0L)
                    if (System.currentTimeMillis() / 1000 - date > 120) continue
                    val chatId = msg.getJSONObject("chat").getLong("id").toString()
                    Tg.handle(ctx, chatId, text)
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                try {
                    Thread.sleep(5000)
                } catch (e2: InterruptedException) {
                    break
                }
            }
        }
    }
}

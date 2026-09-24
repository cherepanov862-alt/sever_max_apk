package ru.severmax.control

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** После перезагрузки телефона снова запускает Telegram-бота, если он был включён. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Repo.init(context)
        if (Repo.tgEnabled) {
            ContextCompat.startForegroundService(
                context, Intent(context, TelegramService::class.java)
            )
        }
    }
}

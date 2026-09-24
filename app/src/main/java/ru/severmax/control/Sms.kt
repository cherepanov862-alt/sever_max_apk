package ru.severmax.control

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

object Sms {
    @Suppress("DEPRECATION")
    private fun manager(ctx: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }

    fun send(ctx: Context, number: String, text: String): Result<Unit> = runCatching {
        manager(ctx).sendTextMessage(number, null, text, null, null)
    }
}

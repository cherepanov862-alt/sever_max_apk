package ru.severmax.control

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Принимает SMS-ответы только от номера SIM подогревателя. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        Repo.init(context)
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val from = msgs.firstOrNull()?.originatingAddress ?: return
        if (!Repo.samePhone(from, Repo.phone)) return
        val body = msgs.joinToString("") { it.messageBody ?: "" }
        Repo.onIncoming(body)
        Tg.forward(body)
    }
}

package ru.severmax.control

import java.util.Locale

/** Построение SMS-команд под выбранный профиль подогревателя (см. HeaterModels). */
object Cmd {
    private fun pad3(n: Int): String = String.format(Locale.US, "%03d", n)

    fun start(m: HeaterModel, minutes: Int): String = when (m.protocol) {
        Protocol.A -> "K"
        Protocol.B -> if (m.supportsStartMinutes) "K*" + pad3(minutes) else "K"
        Protocol.C -> "start"
    }

    fun stop(m: HeaterModel): String = when (m.protocol) {
        Protocol.A, Protocol.B -> "G"
        Protocol.C -> "stop"
    }

    fun status(m: HeaterModel): String = when (m.protocol) {
        Protocol.A, Protocol.B -> "C"
        Protocol.C -> "status"
    }

    /** Таймер предпрогрева для протокола C: time#30. Для A/B не используется. */
    fun timer(m: HeaterModel, minutes: Int): String? =
        if (m.protocol == Protocol.C) "time#$minutes" else null

    fun setTemp(m: HeaterModel, temp: Int): String? =
        m.tempPrefix?.let { "$it*$temp" }

    fun boost(m: HeaterModel, up: Int, down: Int): String? =
        m.boostPrefix?.let { "$it*$up*$down" }

    fun bind(m: HeaterModel, code: String, slot: String): String = when (m.protocol) {
        Protocol.A, Protocol.B -> "TJSQ*$code*$slot"
        Protocol.C -> "REFS#$code#"
    }

    fun reset(m: HeaterModel): String? = if (m.supportsReset) "HFCC" else null
}

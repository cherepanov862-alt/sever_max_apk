package ru.severmax.control

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class LogEntry(val time: Long, val incoming: Boolean, val text: String)

data class HeaterStatus(
    val on: Boolean? = null,
    val up: Int? = null,
    val down: Int? = null,
    val csq: Int? = null,
    val updated: Long = 0L
)

object Repo {
    private var prefs: SharedPreferences? = null

    val log = MutableStateFlow<List<LogEntry>>(emptyList())
    val status = MutableStateFlow(HeaterStatus())

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("severmax", Context.MODE_PRIVATE)
        prefs = p
        log.value = loadLog(p)
        status.value = loadStatus(p)
    }

    var phone: String
        get() = prefs?.getString("phone", "") ?: ""
        set(v) { prefs?.edit()?.putString("phone", v)?.apply() }

    var code: String
        get() = prefs?.getString("code", "123456") ?: "123456"
        set(v) { prefs?.edit()?.putString("code", v)?.apply() }

    /** Выбранная модель подогревателя, см. HeaterModels. */
    var modelId: String
        get() = prefs?.getString("model", "sm5000_4mini") ?: "sm5000_4mini"
        set(v) { prefs?.edit()?.putString("model", v)?.apply() }

    val model: HeaterModel get() = HeaterModels.byId(modelId)

    var tgToken: String
        get() = prefs?.getString("tg_token", "") ?: ""
        set(v) { prefs?.edit()?.putString("tg_token", v)?.apply() }

    /** Единственный chat id, которому разрешено управлять подогревателем. */
    var tgChat: String
        get() = prefs?.getString("tg_chat", "") ?: ""
        set(v) { prefs?.edit()?.putString("tg_chat", v)?.apply() }

    var tgEnabled: Boolean
        get() = prefs?.getBoolean("tg_on", false) ?: false
        set(v) { prefs?.edit()?.putBoolean("tg_on", v)?.apply() }

    var tgOffset: Long
        get() = prefs?.getLong("tg_offset", 0L) ?: 0L
        set(v) { prefs?.edit()?.putLong("tg_offset", v)?.apply() }

    fun samePhone(a: String, b: String): Boolean {
        val x = a.filter { it.isDigit() }.takeLast(10)
        val y = b.filter { it.isDigit() }.takeLast(10)
        return x.isNotEmpty() && x == y
    }

    @Synchronized fun addLog(incoming: Boolean, text: String) {
        val list = (log.value + LogEntry(System.currentTimeMillis(), incoming, text)).takeLast(200)
        log.value = list
        saveLog(list)
    }

    @Synchronized fun clearLog() {
        log.value = emptyList()
        saveLog(emptyList())
    }

    /** Разбор входящего SMS от подогревателя. */
    @Synchronized fun onIncoming(text: String) {
        addLog(true, text)
        val old = status.value
        val upper = text.uppercase()
        var on = old.on
        if (upper.contains("HEATER ON")) on = true
        if (upper.contains("HEATER OFF")) on = false
        fun num(re: String): Int? =
            Regex(re, RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()
        val s = old.copy(
            on = on,
            up = num("UP\\s*TEM:\\s*(\\d+)") ?: old.up,
            down = num("DOWN\\s*TEM:\\s*(\\d+)") ?: old.down,
            csq = num("CSQ:\\s*(\\d+)") ?: old.csq,
            updated = System.currentTimeMillis()
        )
        status.value = s
        saveStatus(s)
    }

    private fun saveLog(list: List<LogEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("t", it.time).put("in", it.incoming).put("x", it.text))
        }
        prefs?.edit()?.putString("log", arr.toString())?.apply()
    }

    private fun loadLog(p: SharedPreferences): List<LogEntry> = try {
        val arr = JSONArray(p.getString("log", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            LogEntry(o.getLong("t"), o.getBoolean("in"), o.getString("x"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun saveStatus(s: HeaterStatus) {
        prefs?.edit()
            ?.putInt("on", when (s.on) { true -> 1; false -> 0; null -> -1 })
            ?.putInt("up", s.up ?: -1)
            ?.putInt("down", s.down ?: -1)
            ?.putInt("csq", s.csq ?: -1)
            ?.putLong("upd", s.updated)
            ?.apply()
    }

    private fun loadStatus(p: SharedPreferences): HeaterStatus = HeaterStatus(
        on = when (p.getInt("on", -1)) { 1 -> true; 0 -> false; else -> null },
        up = p.getInt("up", -1).takeIf { it >= 0 },
        down = p.getInt("down", -1).takeIf { it >= 0 },
        csq = p.getInt("csq", -1).takeIf { it >= 0 },
        updated = p.getLong("upd", 0L)
    )
}

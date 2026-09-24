package ru.severmax.control

enum class Protocol { A, B, C }

/**
 * Профиль подогревателя: у каждого поколения Severmax свой набор SMS-команд.
 * Тип A — 5000-4mini (современная прошивка), Тип B — 5000 (старая/переходная),
 * Тип C — 5500 / PRO (текстовый протокол).
 */
data class HeaterModel(
    val id: String,
    val title: String,
    val protocol: Protocol,
    val supportsStartMinutes: Boolean,
    val supportsTemp: Boolean,
    val tempPrefix: String?,
    val supportsBoost: Boolean,
    val boostPrefix: String?,
    /** Слоты привязки номера: буквы (A-D) или цифры (1-4), либо один слот для протокола C. */
    val bindSlots: List<String>,
    val supportsReset: Boolean
)

object HeaterModels {
    val ALL: List<HeaterModel> = listOf(
        HeaterModel(
            id = "sm5000_4mini",
            title = "Severmax 5000-4mini",
            protocol = Protocol.A,
            supportsStartMinutes = false,
            supportsTemp = true,
            tempPrefix = "NFPRZ",
            supportsBoost = true,
            boostPrefix = "XHRPZ",
            bindSlots = listOf("A", "B", "C", "D"),
            supportsReset = true
        ),
        HeaterModel(
            id = "sm5000",
            title = "Severmax 5000",
            protocol = Protocol.B,
            supportsStartMinutes = true,
            supportsTemp = true,
            tempPrefix = "CGPZ",
            supportsBoost = false,
            boostPrefix = null,
            bindSlots = listOf("1", "2", "3", "4"),
            supportsReset = false
        ),
        HeaterModel(
            id = "sm5500_pro",
            title = "Severmax 5500 / PRO",
            protocol = Protocol.C,
            supportsStartMinutes = true,
            supportsTemp = false,
            tempPrefix = null,
            supportsBoost = false,
            boostPrefix = null,
            bindSlots = listOf("1"),
            supportsReset = false
        )
    )

    fun byId(id: String): HeaterModel = ALL.firstOrNull { it.id == id } ?: ALL[0]
}

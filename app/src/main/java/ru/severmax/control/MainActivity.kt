package ru.severmax.control

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        setContent { AppTheme { App() } }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFFFF8A4C))
    } else {
        lightColorScheme(primary = Color(0xFFE8590C))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

fun toast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

fun fmt(t: Long): String =
    SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(t))

fun sendCommand(ctx: Context, text: String) {
    if (Repo.phone.isBlank()) {
        toast(ctx, "Сначала укажите номер SIM подогревателя во вкладке «Настройки»")
        return
    }
    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        toast(ctx, "Нет разрешения на отправку SMS")
        return
    }
    Sms.send(ctx, Repo.phone, text)
        .onSuccess {
            Repo.addLog(false, text)
            toast(ctx, "Отправлено: $text")
        }
        .onFailure { toast(ctx, "Ошибка отправки: ${it.message}") }
}

@Composable
fun App() {
    var tab by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(Unit) {
        val perms = mutableListOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        launcher.launch(perms.toTypedArray())
    }
    Scaffold(
        topBar = { Header() },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Text("🔥") }, label = { Text("Управление") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Text("📋") }, label = { Text("Журнал") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Text("⚙️") }, label = { Text("Настройки") }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ControlScreen()
                1 -> LogScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Севермакс",
            modifier = Modifier.height(40.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Text("$label: ${value.roundToInt()}")
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
}

@Composable
fun StatusCard(model: HeaterModel) {
    val st by Repo.status.collectAsState()
    val ctx = LocalContext.current
    Section("Состояние (по последнему SMS)") {
        Text(model.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        val label = when (st.on) {
            true -> "🔥 Работает"
            false -> "⏹ Выключен"
            null -> "Нет данных"
        }
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (st.up != null) {
            Text("Верхний порог: ${st.up} °C, нижний: ${st.down ?: "—"} °C")
        }
        if (st.csq != null) {
            Text("Сигнал GSM (CSQ): ${st.csq} из 31")
        }
        Text(
            if (st.updated > 0) "Последний ответ: ${fmt(st.updated)}" else "Ответов пока нет",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = { sendCommand(ctx, Cmd.status(model)) }) {
            Text("Запросить статус")
        }
    }
}

@Composable
fun ControlScreen() {
    val ctx = LocalContext.current
    val model = Repo.model
    var minutes by remember { mutableFloatStateOf(30f) }
    var temp by remember { mutableFloatStateOf(65f) }
    var up by remember { mutableFloatStateOf(90f) }
    var down by remember { mutableFloatStateOf(30f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(model)

        Section("Запуск и остановка") {
            if (model.supportsStartMinutes) {
                LabeledSlider(
                    "Время работы, мин", minutes, { minutes = (it / 5f).roundToInt() * 5f },
                    5f..180f, 34
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { sendCommand(ctx, Cmd.start(model, minutes.roundToInt())) },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("▶ Запуск") }
                Button(
                    onClick = { sendCommand(ctx, Cmd.stop(model)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) { Text("⏹ Стоп") }
            }
            Text(
                "После остановки повторный запуск возможен не ранее чем через 3 минуты.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (model.supportsTemp) {
            Section("Температура нагрева") {
                LabeledSlider("Температура, °C", temp, { temp = it }, 30f..90f, 59)
                Button(
                    onClick = {
                        Cmd.setTemp(model, temp.roundToInt())?.let { sendCommand(ctx, it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Установить температуру") }
                Text(
                    "Только записывает значение, подогреватель не запускает. Следующий запуск будет с этой температурой.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (model.supportsBoost) {
            Section("Режим догрева") {
                LabeledSlider("Верхний порог, °C", up, { up = it }, 40f..90f, 49)
                LabeledSlider("Нижний порог, °C", down, { down = it }, 30f..80f, 49)
                Button(
                    onClick = {
                        val u = up.roundToInt()
                        val d = down.roundToInt()
                        if (u - d < 10) {
                            toast(ctx, "Разница между порогами должна быть не менее 10 °C")
                        } else {
                            Cmd.boost(model, u, d)?.let { sendCommand(ctx, it) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Запустить в режиме догрева") }
                Text(
                    "Поддерживает температуру жидкости между порогами. Следующий запуск снова пойдёт в обычном режиме.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (model.supportsReset) {
            Section("Сброс настроек") {
                OutlinedButton(
                    onClick = { sendCommand(ctx, Cmd.reset(model) ?: return@OutlinedButton) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Сбросить настройки подогревателя") }
            }
        }
    }
}

@Composable
fun LogScreen() {
    val entries by Repo.log.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Журнал SMS", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { Repo.clearLog() }) { Text("Очистить") }
        }
        if (entries.isEmpty()) Text("Пока пусто")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries.asReversed()) { e -> LogRow(e) }
        }
    }
}

@Composable
fun LogRow(e: LogEntry) {
    val err = if (e.incoming) ErrorCodes.find(e.text) else null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (e.incoming) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                (if (e.incoming) "← Получено  " else "→ Отправлено  ") + fmt(e.time),
                style = MaterialTheme.typography.labelSmall
            )
            Text(e.text, fontWeight = FontWeight.Medium)
            if (err != null) {
                Text("${err.first}: ${err.second}", color = MaterialTheme.colorScheme.error)
                Text(err.third, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var phone by remember { mutableStateOf(Repo.phone) }
    var code by remember { mutableStateOf(Repo.code) }
    var modelId by remember { mutableStateOf(Repo.modelId) }
    var tgToken by remember { mutableStateOf(Repo.tgToken) }
    var tgChat by remember { mutableStateOf(Repo.tgChat) }
    var tgOn by remember { mutableStateOf(Repo.tgEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Section("Модель подогревателя") {
            HeaterModels.ALL.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { modelId = m.id; Repo.modelId = m.id },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(selected = modelId == m.id, onClick = { modelId = m.id; Repo.modelId = m.id })
                    Text(m.title)
                }
            }
            Text(
                "От модели зависит набор SMS-команд: доступные функции ниже и на вкладке «Управление» подстроятся автоматически.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Section("Подогреватель") {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; Repo.phone = it.trim() },
                label = { Text("Номер SIM подогревателя") },
                placeholder = { Text("+79001234567") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Section("Привязка этого телефона") {
            val model = Repo.model
            Text(
                if (model.bindSlots.size > 1)
                    "Отправьте команду привязки с этого телефона в свободный слот. " +
                        "Подтверждение: SMS «ADDAUTH OK!». На SIM подогревателя должен быть положительный баланс."
                else
                    "Отправьте команду привязки с этого телефона. На SIM подогревателя должен быть положительный баланс."
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it; Repo.code = it.trim() },
                label = { Text("Код в команде привязки (по инструкции 123456)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                model.bindSlots.forEachIndexed { i, slot ->
                    OutlinedButton(
                        onClick = { sendCommand(ctx, Cmd.bind(model, Repo.code, slot)) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (model.bindSlots.size > 1) "Слот ${i + 1}" else "Привязать") }
                }
            }
        }

        Section("Telegram-бот") {
            Text(
                "1) Создайте бота у @BotFather (команда /newbot) и вставьте токен.\n" +
                    "2) Включите бота ниже.\n" +
                    "3) Напишите боту любое сообщение: он ответит вашим chat id. " +
                    "Впишите его в поле ниже, после этого бот слушается только вас."
            )
            OutlinedTextField(
                value = tgToken,
                onValueChange = { tgToken = it; Repo.tgToken = it.trim() },
                label = { Text("Токен бота") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tgChat,
                onValueChange = { tgChat = it; Repo.tgChat = it.trim() },
                label = { Text("Ваш chat id") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Бот включён", fontWeight = FontWeight.Medium)
                Switch(
                    checked = tgOn,
                    onCheckedChange = { on ->
                        val svc = Intent(ctx, TelegramService::class.java)
                        if (on && Repo.tgToken.isBlank()) {
                            toast(ctx, "Сначала вставьте токен бота")
                        } else {
                            tgOn = on
                            Repo.tgEnabled = on
                            if (on) ContextCompat.startForegroundService(ctx, svc)
                            else ctx.stopService(svc)
                        }
                    }
                )
            }
            Text(
                "Телефон с SIM должен быть включён и иметь интернет. " +
                    "Отключите для приложения экономию заряда, иначе Android может остановить бота. " +
                    "Команды: ▶ Запуск, ⏹ Стоп, 📊 Статус, /temp 65, /boost 90 30.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Section("Поддержать разработку") {
            Text("Если приложение оказалось полезным, можно перевести любую сумму по СБП.")
            Text(
                "+7 913 281-41-35",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Откройте приложение своего банка → «Переводы» → «По номеру телефона», вставьте номер и укажите сумму сами.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Номер для перевода", "+79132814135"))
                    toast(ctx, "Номер скопирован")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("☕ Скопировать номер") }
        }

        Section("Коды неисправностей") {
            ErrorCodes.all.forEach { (c, title, advice) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$c — $title", fontWeight = FontWeight.SemiBold)
                    Text(advice, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

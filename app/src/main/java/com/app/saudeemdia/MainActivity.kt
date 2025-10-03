@file:OptIn(ExperimentalMaterial3Api::class)

package com.app.saudeemdia

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.saudeemdia.ui.theme.SaudeEmDiaTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            SaudeEmDiaTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ReminderScreen(
                        onSchedule = { triggerAtMillis, title, message ->
                            val delay = triggerAtMillis - System.currentTimeMillis()
                            ReminderScheduler.schedule(
                                context = this,
                                delayMillis = delay,
                                title = title,
                                message = message
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderScreen(
    onSchedule: (triggerAtMillis: Long, title: String, message: String) -> Unit
) {
    // Estados de data/hora
    val now = remember { Calendar.getInstance() }
    var pickedYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var pickedMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) } // 0..11
    var pickedDay by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    var pickedHour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var pickedMinute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }

    // Diálogos
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Campos
    var title by remember { mutableStateOf("Lembrete de Saúde") }
    var message by remember { mutableStateOf("Exame/check-up preventivo") }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Labels
    val dateLabel = remember(pickedYear, pickedMonth, pickedDay) {
        "%02d/%02d/%04d".format(pickedDay, pickedMonth + 1, pickedYear)
    }
    val timeLabel = remember(pickedHour, pickedMinute) {
        "%02d:%02d".format(pickedHour, pickedMinute) // 24h; se quiser 12h, me avise
    }
    val fmtFull = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Saúde em Dia") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Agende um lembrete com data e hora", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = message, onValueChange = { message = it },
                label = { Text("Mensagem") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { showDatePicker = true }) { Text("Data: $dateLabel") }
                Button(onClick = { showTimePicker = true }) { Text("Hora: $timeLabel") }
            }

            Button(
                onClick = {
                    val cal = GregorianCalendar(pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute)
                    var whenMillis = cal.timeInMillis
                    val nowMillis = System.currentTimeMillis()
                    if (whenMillis <= nowMillis) {
                        // evita agendar no passado: empurra +1 min
                        whenMillis = nowMillis + 60_000L
                    }

                    val finalTitle = title.ifBlank { "Lembrete de Saúde" }
                    val finalMsg = message.ifBlank { "Hora de cuidar da prevenção." }

                    onSchedule(whenMillis, finalTitle, finalMsg)

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Lembrete agendado para ${fmtFull.format(java.util.Date(whenMillis))}"
                        )
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Agendar Lembrete")
            }
        }
    }

    // DatePickerDialog
    if (showDatePicker) {
        val dateState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        pickedYear = cal.get(Calendar.YEAR)
                        pickedMonth = cal.get(Calendar.MONTH)
                        pickedDay = cal.get(Calendar.DAY_OF_MONTH)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    // TimePickerDialog
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = pickedHour,
            initialMinute = pickedMinute,
            is24Hour = true // mude para false se quiser 12h (AM/PM)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedHour = timeState.hour
                    pickedMinute = timeState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            },
            title = { Text("Escolher Hora") },
            text = { TimePicker(state = timeState) }
        )
    }
}

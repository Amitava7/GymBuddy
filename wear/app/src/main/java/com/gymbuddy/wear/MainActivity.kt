package com.gymbuddy.wear

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val Primary = Color(0xFF3DC2EC)
private val Accent = Color(0xFFCDFE00)

private data class LogEntry(val incoming: Boolean, val payload: Payload)

class MainActivity : ComponentActivity() {
    private lateinit var phone: PhoneConnection
    private var listener: MessageClient.OnMessageReceivedListener? = null

    private var phoneName by mutableStateOf<String?>(null)
    private var error by mutableStateOf<String?>(null)
    private var setsDone by mutableIntStateOf(0)
    private val log = mutableStateListOf<LogEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the app in the foreground while testing so messages keep flowing.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        phone = PhoneConnection(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    refreshPhone()
                    delay(5_000)
                }
            }
        }

        setContent { WatchApp() }
    }

    override fun onResume() {
        super.onResume()
        listener = phone.addMessageListener(::onMessage)
    }

    override fun onPause() {
        listener?.let(phone::removeMessageListener)
        listener = null
        super.onPause()
    }

    private suspend fun refreshPhone() {
        phoneName = try {
            phone.phoneNodes().firstOrNull()?.displayName
        } catch (e: Exception) {
            error = e.message
            null
        }
    }

    private fun onMessage(payload: Payload) {
        addLog(LogEntry(incoming = true, payload = payload))
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        if (payload.type == "ping") send(Payload(type = "pong"))
    }

    private fun send(payload: Payload) {
        lifecycleScope.launch {
            try {
                if (phone.send(payload) == 0) {
                    error = "Phone app not reachable"
                } else {
                    error = null
                    addLog(LogEntry(incoming = false, payload = payload))
                }
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    private fun addLog(entry: LogEntry) {
        log.add(0, entry)
        if (log.size > 20) log.removeAt(log.lastIndex)
    }

    @Composable
    private fun WatchApp() {
        MaterialTheme {
            Scaffold(timeText = { TimeText() }) {
                ScalingLazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            text = phoneName?.let { "Phone: $it" } ?: "No phone app",
                            color = if (phoneName != null) Primary else Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                    error?.let { message ->
                        item { Text(message, color = Color(0xFFEF4444), fontSize = 11.sp, textAlign = TextAlign.Center) }
                    }
                    item {
                        Chip(
                            onClick = { send(Payload(type = "ping")) },
                            label = { Text("Ping phone") },
                            colors = ChipDefaults.primaryChipColors(backgroundColor = Primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Chip(
                            onClick = {
                                setsDone++
                                send(Payload(type = "set_done", text = "#$setsDone"))
                            },
                            label = { Text("Set done ($setsDone)") },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(log) { entry -> LogRow(entry) }
                }
            }
        }
    }

    @Composable
    private fun LogRow(entry: LogEntry) {
        val p = entry.payload
        val label = when (p.type) {
            "ping" -> "Ping"
            "pong" -> "Pong"
            "set_done" -> "Set done ${p.text.orEmpty()}"
            else -> p.text.orEmpty()
        }
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(p.ts))
        Text(
            text = "${if (entry.incoming) "↓" else "↑"} $label · $time",
            color = if (entry.incoming) Accent else Primary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

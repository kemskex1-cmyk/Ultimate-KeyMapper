package com.ultimatekeymapper

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val inputLog = mutableStateListOf<String>()
    private lateinit var store: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = getSharedPreferences("ukm_profiles", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                KeyMapperApp(
                    inputLog = inputLog,
                    savedLayout = store.getString("layout", "[]") ?: "[]",
                    onSaveLayout = { store.edit().putString("layout", it).apply() },
                    onDevices = { connectedInputDevices() }
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::store.isInitialized) {
            val action = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP"
            addLog("Key $action: ${KeyEvent.keyCodeToString(event.keyCode)}")
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (::store.isInitialized && (event.source and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE) {
            if (event.action == MotionEvent.ACTION_SCROLL) {
                addLog("Mouse wheel: ${event.getAxisValue(MotionEvent.AXIS_VSCROLL)}")
            } else if (event.action == MotionEvent.ACTION_HOVER_MOVE || event.action == MotionEvent.ACTION_MOVE) {
                addLog("Mouse move: x=${event.x.roundToInt()}, y=${event.y.roundToInt()}")
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun addLog(message: String) {
        runOnUiThread {
            inputLog.add(0, message)
            while (inputLog.size > 30) inputLog.removeAt(inputLog.lastIndex)
        }
    }

    private fun connectedInputDevices(): List<String> = InputDevice.getDeviceIds().mapNotNull { id ->
        val device = InputDevice.getDevice(id) ?: return@mapNotNull null
        val type = when {
            (device.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD -> "Keyboard"
            (device.sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE -> "Mouse"
            (device.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD -> "Gamepad"
            else -> "Input device"
        }
        "$type • ${device.name} • ID $id"
    }
}

private data class Mapping(val key: String, val x: Float, val y: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyMapperApp(
    inputLog: List<String>,
    savedLayout: String,
    onSaveLayout: (String) -> Unit,
    onDevices: () -> List<String>
) {
    var tab by remember { mutableStateOf(0) }
    var mappings by remember {
        mutableStateOf(runCatching {
            val array = JSONArray(savedLayout)
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                Mapping(item.optString("key", "W"), item.optDouble("x", .5).toFloat(), item.optDouble("y", .5).toFloat())
            }
        }.getOrDefault(listOf(Mapping("W", .5f, .5f))))
    }
    var devices by remember { mutableStateOf(onDevices()) }
    var newKey by remember { mutableStateOf("W") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ULTIMATE KEYMAPPER") })
        },
        bottomBar = {
            NavigationBar {
                listOf("Dashboard", "Editor", "Input").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index; devices = onDevices() },
                        icon = { Text(listOf("⌂", "⌖", "⌨")[index]) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(Color(0xFF10131A)).padding(padding).padding(16.dp)) {
            when (tab) {
                0 -> {
                    Text("Gaming control center", style = MaterialTheme.typography.headlineSmall)
                    Text("Android 11+ • Local-first • Build 0.1.0", color = Color.LightGray)
                    Spacer(Modifier.height(18.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Connected input devices", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            if (devices.isEmpty()) Text("No external input devices detected yet.")
                            devices.forEach { Text("• $it") }
                            Button(onClick = { devices = onDevices() }) { Text("Refresh devices") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Project status", style = MaterialTheme.typography.titleMedium)
                            Text("Input diagnostics and local mapping editor are available.")
                            Text("Cross-app game touch injection is not enabled in this foundation.")
                        }
                    }
                }
                1 -> {
                    Text("Mapping editor", style = MaterialTheme.typography.headlineSmall)
                    Text("Drag a control to position it. Enter a key, then save.")
                    Spacer(Modifier.height(10.dp))
                    BoxWithConstraints(
                        Modifier.fillMaxWidth().weight(1f).background(Color(0xFF202938), RoundedCornerShape(16.dp))
                    ) {
                        val widthPx = constraints.maxWidth.toFloat()
                        val heightPx = constraints.maxHeight.toFloat()
                        mappings.forEachIndexed { index, mapping ->
                            var offsetX by remember(mapping, index) { mutableFloatStateOf(mapping.x * widthPx) }
                            var offsetY by remember(mapping, index) { mutableFloatStateOf(mapping.y * heightPx) }
                            Box(
                                Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                    .pointerInput(index) {
                                        detectDragGestures { change, drag ->
                                            change.consume()
                                            offsetX = (offsetX + drag.x).coerceIn(0f, widthPx - 64f)
                                            offsetY = (offsetY + drag.y).coerceIn(0f, heightPx - 64f)
                                            mappings = mappings.toMutableList().also {
                                                it[index] = mapping.copy(
                                                    x = (offsetX / widthPx).coerceIn(0f, 1f),
                                                    y = (offsetY / heightPx).coerceIn(0f, 1f)
                                                )
                                            }
                                        }
                                    }
                                    .background(Color(0xFF00BFA5), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            ) { Text(mapping.key, color = Color.Black) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = newKey,
                            onValueChange = { newKey = it.take(12) },
                            modifier = Modifier.weight(1f).background(Color(0xFF263244), RoundedCornerShape(8.dp)).padding(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            decorationBox = { inner -> if (newKey.isEmpty()) Text("Key label", color = Color.Gray); inner() }
                        )
                        Button(onClick = { if (newKey.isNotBlank()) mappings = mappings + Mapping(newKey, .4f, .4f) }) {
                            Text("Add")
                        }
                    }
                    Row {
                        Button(onClick = {
                            val array = JSONArray()
                            mappings.forEach { m -> array.put(JSONObject().put("key", m.key).put("x", m.x).put("y", m.y)) }
                            onSaveLayout(array.toString())
                        }) { Text("Save layout") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { mappings = listOf(Mapping("W", .5f, .5f)) }) { Text("Reset") }
                    }
                }
                else -> {
                    Text("Live input diagnostics", style = MaterialTheme.typography.headlineSmall)
                    Text("This screen shows input events delivered to the app while it is focused.")
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(inputLog) { line ->
                            Text(line, Modifier.fillMaxWidth().background(Color(0xFF202938), RoundedCornerShape(8.dp)).padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}

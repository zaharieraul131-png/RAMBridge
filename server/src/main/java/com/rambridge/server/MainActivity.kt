package com.rambridge.server

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rambridge.shared.MemoryPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom

private val Bg = Color(0xFF080D16)
private val Panel = Color(0xFF111C2B)
private val Cyan = Color(0xFF42C8FF)
private val Green = Color(0xFF4BE0A0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val code = (100000..999999).random(SecureRandom())
        setContent { ServerScreen(code.toString()) }
    }
}

@Composable
private fun ServerScreen(pairingCode: String) {
    var status by remember { mutableStateOf("Server stopped") }
    var running by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(MemoryPool.Stats(0, MemoryPool.DEFAULT_MAX_BYTES, 0)) }
    var ip by remember { mutableStateOf("Checking…") }
    val server = remember {
        RamBridgeServer(pairingCode, { status = it }, { stats = it })
    }

    LaunchedEffect(Unit) {
        while (true) {
            ip = findLocalIp()
            running = server.isRunning()
            delay(2000)
        }
    }
    DisposableEffect(Unit) { onDispose { server.stop() } }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, secondary = Green, background = Bg, surface = Panel
    )) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("RAMBRIDGE", color = Cyan, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("SERVER  •  REDMI 15C", color = Color.LightGray, style = MaterialTheme.typography.labelLarge)
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SERVER STATUS", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        Text(if (running) "ONLINE" else "OFFLINE", color = if (running) Green else Color(0xFFFF7373), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(status, color = Color.LightGray)
                        Button(
                            onClick = { if (running) server.stop() else server.start() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (running) "Stop server" else "Start server") }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CONNECTION DETAILS", color = Cyan, fontWeight = FontWeight.Bold)
                        InfoLine("IP address", ip)
                        InfoLine("Port", RamBridgeServer.DEFAULT_PORT.toString())
                        InfoLine("Pairing code", pairingCode)
                        Text("Connect both phones to the same Wi-Fi or hotspot. Keep this screen open for v0.1.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("REMOTE MEMORY POOL", color = Cyan, fontWeight = FontWeight.Bold)
                        InfoLine("Allocated", formatBytes(stats.usedBytes))
                        InfoLine("Pool limit", formatBytes(stats.maxBytes))
                        InfoLine("Active blocks", stats.blockCount.toString())
                        val progress = (stats.usedBytes.toFloat() / stats.maxBytes.coerceAtLeast(1)).coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                }
                Text("Prototype note: this pool belongs to the RAMBridge app process. It is not system-wide Android RAM or swap.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun findLocalIp(): String = try {
    NetworkInterface.getNetworkInterfaces().toList()
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
        ?.hostAddress ?: "Not connected to Wi-Fi"
} catch (_: Exception) { "Unavailable" }

private fun formatBytes(bytes: Long): String =
    if (bytes >= 1024L * 1024L) "%.1f MiB".format(bytes / (1024.0 * 1024.0))
    else "$bytes bytes"

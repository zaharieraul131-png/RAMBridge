package com.rambridge.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF080D16)
private val Panel = Color(0xFF111C2B)
private val Cyan = Color(0xFF42C8FF)
private val Green = Color(0xFF4BE0A0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClientScreen() }
    }
}

@Composable
private fun ClientScreen() {
    val client = remember { RamBridgeClient() }
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("192.168.1.1") }
    var port by remember { mutableStateOf("45821") }
    var code by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Not connected") }
    var blockId by remember { mutableStateOf<Int?>(null) }
    var result by remember { mutableStateOf("No remote memory block allocated.") }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { client.disconnect() } }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Green, background = Bg, surface = Panel)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("RAMBRIDGE", color = Cyan, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("CLIENT  •  MOTOROLA G54 5G", color = Color.LightGray, style = MaterialTheme.typography.labelLarge)
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("SERVER CONNECTION", color = Cyan, fontWeight = FontWeight.Bold)
                        OutlinedTextField(host, { host = it }, label = { Text("Server IP address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(port, { port = it }, label = { Text("Port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(code, { code = it }, label = { Text("Pairing code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                busy = true
                                scope.launch {
                                    val message = withContext(Dispatchers.IO) {
                                        try {
                                            client.connect(host, port.toInt(), code)
                                            "Connected to $host"
                                        } catch (e: Exception) {
                                            "Connection failed: ${e.message}"
                                        }
                                    }
                                    connected = client.isConnected()
                                    status = message
                                    busy = false
                                }
                            },
                            enabled = !busy && !connected,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "Connecting…" else "Connect") }
                        OutlinedButton(
                            onClick = {
                                client.disconnect()
                                connected = false
                                status = "Disconnected"
                                blockId = null
                            },
                            enabled = connected,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Disconnect") }
                        Text(status, color = if (connected) Green else Color.LightGray)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("REMOTE MEMORY TEST", color = Cyan, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                busy = true
                                scope.launch {
                                    val message = withContext(Dispatchers.IO) {
                                        try {
                                            val id = client.allocate(1024 * 1024)
                                            blockId = id
                                            "Allocated 1 MiB block (ID $id)."
                                        } catch (e: Exception) { "Allocation failed: ${e.message}" }
                                    }
                                    result = message
                                    busy = false
                                }
                            },
                            enabled = connected && !busy && blockId == null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Allocate 1 MiB") }
                        Button(
                            onClick = {
                                busy = true
                                scope.launch {
                                    val message = withContext(Dispatchers.IO) {
                                        try {
                                            val id = blockId ?: error("Allocate a block first.")
                                            val payload = "RAMBridge remote-memory test".toByteArray()
                                            client.write(id, 0, payload)
                                            "Wrote ${payload.size} bytes to block $id."
                                        } catch (e: Exception) { "Write failed: ${e.message}" }
                                    }
                                    result = message
                                    busy = false
                                }
                            },
                            enabled = connected && !busy && blockId != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Write test data") }
                        Button(
                            onClick = {
                                busy = true
                                scope.launch {
                                    val message = withContext(Dispatchers.IO) {
                                        try {
                                            val id = blockId ?: error("Allocate a block first.")
                                            val bytes = client.read(id, 0, "RAMBridge remote-memory test".toByteArray().size)
                                            "Read back: ${bytes.decodeToString()}"
                                        } catch (e: Exception) { "Read failed: ${e.message}" }
                                    }
                                    result = message
                                    busy = false
                                }
                            },
                            enabled = connected && !busy && blockId != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Read test data") }
                        OutlinedButton(
                            onClick = {
                                busy = true
                                scope.launch {
                                    val message = withContext(Dispatchers.IO) {
                                        try {
                                            val id = blockId ?: error("No block to release.")
                                            val ok = client.release(id)
                                            if (ok) "Released block $id." else "Block was already released."
                                        } catch (e: Exception) { "Release failed: ${e.message}" }
                                    }
                                    blockId = null
                                    result = message
                                    busy = false
                                }
                            },
                            enabled = connected && !busy && blockId != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Release block") }
                        Text(result, color = Color.LightGray)
                    }
                }
                Text("This is an application-level remote memory test, not extra Android system RAM. Use only on a trusted private network.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

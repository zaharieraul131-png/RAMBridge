package com.rambridge.server

import com.rambridge.shared.MemoryPool
import com.rambridge.shared.Protocol
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class RamBridgeServer(
    private val pairingCode: String,
    private val onStatus: (String) -> Unit,
    private val onStats: (MemoryPool.Stats) -> Unit
) {
    private val pool = MemoryPool()
    private val running = AtomicBoolean(false)
    private val clients = Executors.newCachedThreadPool()
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var acceptThread: Thread? = null

    fun start(port: Int = DEFAULT_PORT) {
        if (!running.compareAndSet(false, true)) return
        acceptThread = Thread {
            try {
                ServerSocket(port).use { socket ->
                    serverSocket = socket
                    onStatus("Listening on port $port")
                    publishStats()
                    while (running.get()) {
                        val client = socket.accept()
                        clients.execute { handleClient(client) }
                    }
                }
            } catch (e: SocketException) {
                if (running.get()) onStatus("Socket error: ${e.message}")
            } catch (e: Exception) {
                onStatus("Server error: ${e.message}")
            } finally {
                running.set(false)
                serverSocket = null
                onStatus("Server stopped")
            }
        }.apply {
            name = "RAMBridge-Accept"
            start()
        }
    }

    fun stop() {
        running.set(false)
        try { serverSocket?.close() } catch (_: Exception) {}
        onStatus("Stopping server…")
    }

    fun isRunning(): Boolean = running.get()

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            try {
                client.tcpNoDelay = true
                client.soTimeout = 30_000
                val input = DataInputStream(client.getInputStream())
                val output = DataOutputStream(client.getOutputStream())

                val command = Protocol.readRequestHeader(input)
                if (command != Protocol.HELLO) throw IOException("Pairing handshake required.")
                val supplied = input.readUTF()
                if (supplied != pairingCode) {
                    Protocol.writeResponseHeader(output, Protocol.HELLO, 1)
                    output.writeUTF("Incorrect pairing code.")
                    output.flush()
                    return
                }
                Protocol.writeResponseHeader(output, Protocol.HELLO)
                output.flush()

                while (running.get() && !client.isClosed) {
                    val cmd = try { Protocol.readRequestHeader(input) } catch (_: IOException) { break }
                    try {
                        when (cmd) {
                            Protocol.ALLOCATE -> {
                                val size = input.readInt()
                                val id = pool.allocate(size)
                                Protocol.writeResponseHeader(output, cmd)
                                output.writeInt(id)
                            }
                            Protocol.WRITE -> {
                                val id = input.readInt()
                                val offset = input.readInt()
                                val length = input.readInt()
                                if (length !in 0..Protocol.MAX_PACKET_BYTES) throw IOException("Invalid data length.")
                                val data = ByteArray(length)
                                input.readFully(data)
                                pool.write(id, offset, data)
                                Protocol.writeResponseHeader(output, cmd)
                            }
                            Protocol.READ -> {
                                val id = input.readInt()
                                val offset = input.readInt()
                                val length = input.readInt()
                                if (length !in 0..Protocol.MAX_PACKET_BYTES) throw IOException("Invalid data length.")
                                val data = pool.read(id, offset, length)
                                Protocol.writeResponseHeader(output, cmd)
                                output.writeInt(data.size)
                                output.write(data)
                            }
                            Protocol.RELEASE -> {
                                val id = input.readInt()
                                val released = pool.release(id)
                                Protocol.writeResponseHeader(output, cmd)
                                output.writeBoolean(released)
                            }
                            Protocol.STATS -> {
                                val stats = pool.stats()
                                Protocol.writeResponseHeader(output, cmd)
                                output.writeLong(stats.usedBytes)
                                output.writeLong(stats.maxBytes)
                                output.writeInt(stats.blockCount)
                            }
                            else -> throw IOException("Unknown command.")
                        }
                        output.flush()
                        publishStats()
                    } catch (e: Exception) {
                        Protocol.writeResponseHeader(output, cmd, 1)
                        output.writeUTF(e.message ?: "Request failed.")
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                onStatus("Client disconnected: ${e.message ?: "connection closed"}")
            }
        }
    }

    private fun publishStats() = onStats(pool.stats())

    companion object { const val DEFAULT_PORT = 45821 }
}

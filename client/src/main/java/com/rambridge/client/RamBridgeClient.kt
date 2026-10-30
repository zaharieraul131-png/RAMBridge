package com.rambridge.client

import com.rambridge.shared.Protocol
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class RamBridgeClient {
    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    @Synchronized
    fun connect(host: String, port: Int, pairingCode: String) {
        disconnect()
        require(pairingCode.length in 4..Protocol.TOKEN_MAX_LENGTH) { "Check the pairing code." }
        val s = Socket()
        s.tcpNoDelay = true
        s.connect(InetSocketAddress(host.trim(), port), 5000)
        s.soTimeout = 15000
        val i = DataInputStream(s.getInputStream())
        val o = DataOutputStream(s.getOutputStream())
        Protocol.writeRequestHeader(o, Protocol.HELLO)
        o.writeUTF(pairingCode)
        o.flush()
        Protocol.readResponseHeader(i, Protocol.HELLO)
        socket = s
        input = i
        output = o
    }

    @Synchronized
    fun allocate(size: Int): Int {
        val o = requireOutput()
        val i = requireInput()
        Protocol.writeRequestHeader(o, Protocol.ALLOCATE)
        o.writeInt(size)
        o.flush()
        Protocol.readResponseHeader(i, Protocol.ALLOCATE)
        return i.readInt()
    }

    @Synchronized
    fun write(id: Int, offset: Int, bytes: ByteArray) {
        require(bytes.size <= Protocol.MAX_PACKET_BYTES)
        val o = requireOutput()
        val i = requireInput()
        Protocol.writeRequestHeader(o, Protocol.WRITE)
        o.writeInt(id); o.writeInt(offset); o.writeInt(bytes.size); o.write(bytes); o.flush()
        Protocol.readResponseHeader(i, Protocol.WRITE)
    }

    @Synchronized
    fun read(id: Int, offset: Int, length: Int): ByteArray {
        require(length in 0..Protocol.MAX_PACKET_BYTES)
        val o = requireOutput()
        val i = requireInput()
        Protocol.writeRequestHeader(o, Protocol.READ)
        o.writeInt(id); o.writeInt(offset); o.writeInt(length); o.flush()
        Protocol.readResponseHeader(i, Protocol.READ)
        val actual = i.readInt()
        require(actual == length) { "Unexpected response length." }
        return ByteArray(actual).also { i.readFully(it) }
    }

    @Synchronized
    fun release(id: Int): Boolean {
        val o = requireOutput()
        val i = requireInput()
        Protocol.writeRequestHeader(o, Protocol.RELEASE)
        o.writeInt(id); o.flush()
        Protocol.readResponseHeader(i, Protocol.RELEASE)
        return i.readBoolean()
    }

    @Synchronized
    fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null; input = null; output = null
    }

    fun isConnected(): Boolean = socket?.isConnected == true && socket?.isClosed == false

    private fun requireInput() = input ?: error("Not connected.")
    private fun requireOutput() = output ?: error("Not connected.")
}

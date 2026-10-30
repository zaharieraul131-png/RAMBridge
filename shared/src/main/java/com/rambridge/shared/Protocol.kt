package com.rambridge.shared

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Small binary, length-bounded protocol for the first local-network prototype.
 *
 * Request: magic(int), version(byte), command(byte), command payload.
 * Response: magic(int), version(byte), response-command(byte), status(byte), payload.
 */
object Protocol {
    const val MAGIC = 0x52424D31 // "RBM1"
    const val VERSION = 1
    const val HELLO = 1
    const val ALLOCATE = 2
    const val WRITE = 3
    const val READ = 4
    const val RELEASE = 5
    const val STATS = 6
    const val MAX_PACKET_BYTES = 16 * 1024 * 1024
    const val TOKEN_MAX_LENGTH = 64

    fun writeRequestHeader(out: DataOutputStream, command: Int) {
        out.writeInt(MAGIC)
        out.writeByte(VERSION)
        out.writeByte(command)
        out.flush()
    }

    fun readRequestHeader(input: DataInputStream): Int {
        val magic = input.readInt()
        val version = input.readUnsignedByte()
        val command = input.readUnsignedByte()
        if (magic != MAGIC || version != VERSION) throw IOException("Unsupported RAMBridge protocol.")
        return command
    }

    fun writeResponseHeader(out: DataOutputStream, command: Int, status: Int = 0) {
        out.writeInt(MAGIC)
        out.writeByte(VERSION)
        out.writeByte(command or 0x80)
        out.writeByte(status)
    }

    fun readResponseHeader(input: DataInputStream, expectedCommand: Int) {
        val magic = input.readInt()
        val version = input.readUnsignedByte()
        val command = input.readUnsignedByte()
        val status = input.readUnsignedByte()
        if (magic != MAGIC || version != VERSION || command != (expectedCommand or 0x80)) {
            throw IOException("Invalid RAMBridge response.")
        }
        if (status != 0) {
            val message = input.readUTF()
            throw IOException(message.ifBlank { "Server rejected the request." })
        }
    }
}

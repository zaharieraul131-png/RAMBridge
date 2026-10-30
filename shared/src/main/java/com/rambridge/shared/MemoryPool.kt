package com.rambridge.shared

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Bounded in-process memory pool. The byte arrays live in the server app's heap;
 * they are not registered as Android system RAM or kernel swap.
 */
class MemoryPool(private val maxBytes: Long = DEFAULT_MAX_BYTES) {
    data class Stats(val usedBytes: Long, val maxBytes: Long, val blockCount: Int)

    private val nextId = AtomicInteger(1)
    private val blocks = ConcurrentHashMap<Int, ByteArray>()
    @Volatile private var used = 0L

    @Synchronized
    fun allocate(size: Int): Int {
        require(size in 1..MAX_SINGLE_ALLOCATION) { "Block size must be between 1 byte and 16 MiB." }
        if (used + size > maxBytes) throw IllegalStateException("Memory pool limit reached.")
        val id = nextId.getAndIncrement()
        blocks[id] = ByteArray(size)
        used += size
        return id
    }

    fun write(id: Int, offset: Int, data: ByteArray) {
        val block = blocks[id] ?: throw NoSuchElementException("Unknown block ID.")
        require(offset >= 0 && offset.toLong() + data.size <= block.size) { "Write is outside the block." }
        synchronized(block) { data.copyInto(block, offset) }
    }

    fun read(id: Int, offset: Int, length: Int): ByteArray {
        val block = blocks[id] ?: throw NoSuchElementException("Unknown block ID.")
        require(offset >= 0 && length >= 0 && offset.toLong() + length <= block.size) {
            "Read is outside the block."
        }
        return synchronized(block) { block.copyOfRange(offset, offset + length) }
    }

    @Synchronized
    fun release(id: Int): Boolean {
        val removed = blocks.remove(id) ?: return false
        used -= removed.size
        return true
    }

    fun stats(): Stats = Stats(used, maxBytes, blocks.size)

    companion object {
        const val DEFAULT_MAX_BYTES = 64L * 1024L * 1024L
        const val MAX_SINGLE_ALLOCATION = 16 * 1024 * 1024
    }
}

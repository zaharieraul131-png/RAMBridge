# Architecture

RAMBridge v0.1 contains two Android applications and a shared Android library.

- `:server` hosts a TCP socket and a bounded `MemoryPool`.
- `:client` connects to the server and sends memory operations.
- `:shared` defines the pool and binary protocol.

The server pool defaults to 64 MiB. It is an ordinary collection of byte arrays in the server process heap. It is not Linux swap, zRAM, or system RAM.

## Protocol v1

Every request starts with:
- 4-byte magic: `RBM1`
- 1-byte protocol version
- 1-byte command

Responses add a status byte after the response command. Error responses contain a UTF string.

Commands:
1. HELLO: UTF pairing code
2. ALLOCATE: 4-byte block size; response is 4-byte block ID
3. WRITE: block ID, offset, length, then bytes
4. READ: block ID, offset, length; response length then bytes
5. RELEASE: block ID; response boolean
6. STATS: used bytes, maximum bytes, block count

This is an early prototype protocol. Do not expose the server to the public internet.

# Protocol notes

The first prototype uses a persistent TCP connection and a compact binary request header. The client authenticates once per connection using the pairing code displayed by the server.

The server validates block bounds and packet lengths. Each block is limited to 16 MiB; the total pool defaults to 64 MiB.

Future protocol versions should add request IDs, TLS, explicit protocol negotiation, timeouts per operation, and robust handling of concurrent clients.

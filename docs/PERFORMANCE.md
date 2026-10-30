# Performance plan

Measure before optimizing:
- connection setup time
- round-trip latency for small reads/writes
- throughput for 64 KiB, 1 MiB, and 8 MiB payloads
- CPU and memory overhead on both phones
- behavior during Wi-Fi signal changes

Potential optimizations include batching, larger reusable buffers, reduced copying, and optional compression. Compression should only be enabled when benchmarks show a net benefit.

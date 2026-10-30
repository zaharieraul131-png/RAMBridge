# RAMBridge

RAMBridge is an experimental Android-to-Android remote memory service.

- **Server app:** runs on the Redmi 15C and owns a bounded in-process memory pool.
- **Client app:** runs on the Motorola G54 5G and can connect, allocate blocks, write/read test data, and release blocks.
- **Shared module:** defines the binary TCP protocol and memory-pool implementation.

## Important limitation

RAMBridge v0.1 is an application-level remote memory service. It does **not** add physical RAM to Android, does not change the kernel page allocator, and does not make arbitrary apps or games automatically use the Redmi's memory. Apps must explicitly communicate with RAMBridge. System-wide remote paging would require substantial OS/kernel work and may not be feasible on stock, unrooted devices.

## Build

Recommended build environment:
- JDK 17
- Android SDK Platform 36
- Android Gradle Plugin 8.13.2
- Gradle 8.13

Open this folder as an Android Gradle project in Android Studio, or use a compatible Gradle 8.13 installation:

```bash
gradle :server:assembleDebug :client:assembleDebug
```

If your environment only has a newer Gradle release, use a Gradle 8.13 wrapper/distribution rather than assuming every Android Gradle Plugin supports Gradle 9.x.

APK outputs:
- `server/build/outputs/apk/debug/server-debug.apk`
- `client/build/outputs/apk/debug/client-debug.apk`

## First test

1. Build and install both APKs.
2. Connect both phones to the same Wi-Fi network or connect the Motorola to the Redmi's hotspot.
3. Open RAMBridge Server on the Redmi and tap **Start server**.
4. Note the IP address and port shown by the server.
5. Open RAMBridge Client on the Motorola, enter that IP and port, and tap **Connect**.
6. Use **Allocate 1 MiB**, **Write test data**, **Read test data**, and **Release block**.

The initial pairing code is shown in the server app. Enter it in the client before connecting. Treat it as a local-network prototype credential, not production-grade security.

## Safety and performance notes

- The default memory-pool cap is 64 MiB. It is deliberately conservative; do not increase it aggressively.
- Allocated blocks consume the server app's process heap and can cause memory pressure.
- Keep both apps in the foreground for this prototype. Android may stop background processes.
- Wi-Fi latency and copying overhead mean remote memory is much slower than local RAM.
- The protocol is a prototype and does not yet provide TLS encryption. Use it only on a trusted private network.

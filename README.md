# MeshVerse 🌐📱
## Offline-First Decentralized Super App for Android

A revolutionary Android application combining WhatsApp, Telegram, Discord, Instagram, Reddit, X, and more into a single offline-capable mesh-networked super app. MeshVerse enables secure peer-to-peer communication, decentralized social networking, and collaborative features without requiring internet connectivity.

### 🎯 Core Features

#### 🔐 Communication
- **End-to-End Encrypted Messaging** - Signal Protocol with Curve25519, AES-256-GCM
- **Walkie-Talkie Push-to-Talk** - Ultra-low latency voice with hardware button integration
- **Voice Calls & Audio Rooms** - Local VoIP over Wi-Fi Direct with Opus codec
- **Mesh Relay Messaging** - Multi-hop routing (A→B→C communication)
- **Store-and-Forward Delivery** - Automatic retry and delayed synchronization

#### 🌍 Networking
- **Hybrid Mesh Architecture** - Bluetooth, BLE, Wi-Fi Direct, Nearby Connections, NFC, GPS
- **Dynamic Routing** - Adaptive path selection with peer heartbeat monitoring
- **Mesh Relay Optimization** - Multi-hop relay with reputation systems
- **Internet Gateway Mode** - Secure relaying when internet available
- **Delay-Tolerant Networking** - Opportunistic synchronization

#### 📱 Social Features
- **Decentralized Feeds** - Instagram/Reddit/X-style local-only social media
- **Offline Groups & Communities** - Discord/Telegram-inspired local channels
- **Live Audio Broadcasting** - Community radio, DJ streaming, podcasts
- **Polling & Voting** - Synchronized polls across mesh nodes
- **Stories, Reels & Posts** - Media propagation across peer nodes

#### 🗺️ Maps & Location
- **Offline OpenStreetMap** - Full GPS navigation without internet
- **Live Location Sharing** - Encrypted peer-to-peer GPS synchronization
- **Local Discovery** - Hospitals, fuel, charging stations, shelters
- **Community Reports** - "Road blocked", "Food available", "Medical help"
- **Emergency Overlays** - Disaster communication and SOS broadcasting

#### 🤖 AI & Intelligence
- **Offline LLM Models** - Gemma, Phi, TinyLlama via llama.cpp
- **AI Assistant** - Translation, OCR, coding help, voice commands
- **Document Scanning** - ML Kit OCR with object/plant/food recognition
- **Background Inference** - Vector memory, lightweight RAG system
- **Voice Commands** - "Send SOS", "Find hospital", "Start walkie-talkie"

#### 🎮 Mini Apps & Commerce
- **Mini App Ecosystem** - WeChat-style sandboxed applications
- **Local APIs & Plugins** - Developer SDK with permission system
- **Mini App Marketplace** - Restaurant booking, taxi, shopping, food delivery
- **Campus Marketplace** - Hyperlocal commerce and community boards
- **Offline Payments** - QR transfers, NFC, blockchain-style receipts

#### 📁 Media & Sharing
- **Nearby File Sharing** - Fast transfer over Wi-Fi Direct
- **Peer-Assisted Media** - Video, music, PDFs, APKs across mesh
- **LAN Streaming** - Campus TV, local radio broadcasting
- **Resumable Downloads** - Chunk-based transfer with retry
- **Local Media Caching** - CDN-style propagation

#### 🎮 Gaming
- **LAN Multiplayer** - Chess, Ludo, quizzes, arcade games
- **Local Leaderboards** - Offline tournament systems
- **Peer-to-Peer Sync** - Real-time game state synchronization
- **Room Discovery** - Automatic game session discovery

### 🏗️ Architecture

```
MeshVerse/
├── app/                          # Main application module
│   ├── src/main/
│   │   ├── kotlin/
│   │   │   ├── com/meshverse/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   ├── components/
│   │   │   │   │   └── theme/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   ├── remote/
│   │   │   │   │   └── repository/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── usecase/
│   │   │   │   │   ├── model/
│   │   │   │   │   └── repository/
│   │   │   │   ├── presentation/
│   │   │   │   │   └── viewmodel/
│   │   │   │   └── di/
│   │   │   │       └── modules/
│   │   │   └── services/
│   │   │       ├── mesh/
│   │   │       ├── messaging/
│   │   │       ├── encryption/
│   │   │       ├── networking/
│   │   │       ├── audio/
│   │   │       └── location/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── features/
│   ├── messaging/
│   ├── social/
│   ├── maps/
│   ├── ai/
│   ├── walkie-talkie/
│   └── payments/
├── sdk/
│   ├── mesh-sdk/
│   ├── mini-app-sdk/
│   └── plugin-framework/
├── build-scripts/
│   ├── build-apk.sh
│   ├── setup-dev-env.sh
│   └── mesh-simulator.py
├── docs/
│   ├── ARCHITECTURE.md
│   ├── MESH_NETWORKING.md
│   ├── API.md
│   └── diagrams/
└── settings.gradle.kts
```

### 🚀 Quick Start

#### Prerequisites
- Android Studio Hedgehog or later
- Kotlin 1.9+
- Android SDK 33 (API 33) minimum
- Java 17+
- Python 3.8+ (for mesh simulator)

#### Setup Development Environment
```bash
chmod +x build-scripts/setup-dev-env.sh
./build-scripts/setup-dev-env.sh
```

#### Build APK
```bash
chmod +x build-scripts/build-apk.sh
./build-scripts/build-apk.sh
```

#### Run on Emulator
```bash
# Start emulator with Bluetooth support
emulator -avd Pixel_6_API_33 -feature-emulated-performance -qemu

# Install APK
adb install -r app/build/outputs/apk/debug/meshverse-debug.apk

# Launch app
adb shell am start -n com.meshverse.app/.MainActivity
```

### 📊 Key Technologies

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **UI** | Jetpack Compose | Modern reactive UI with animations |
| **Messaging** | Signal Protocol, Curve25519, AES-256-GCM | E2E encryption |
| **Networking** | Bluetooth, BLE, Wi-Fi Direct, Nearby Connections | Mesh transport |
| **Storage** | Room Database + SQLCipher | Encrypted local storage |
| **AI** | llama.cpp, TensorFlow Lite, Gemma/Phi | Offline AI inference |
| **Audio** | WebRTC, Opus, ExoPlayer | Voice/audio streaming |
| **Maps** | OpenStreetMap, Mapbox SDK | Offline navigation |
| **Dependency Injection** | Hilt | Modular architecture |
| **Async** | Kotlin Coroutines, WorkManager | Background tasks |
| **Testing** | JUnit, Espresso, MockK | Quality assurance |

### 🔒 Security Features

- ✅ End-to-end encryption (Signal Protocol)
- ✅ Forward secrecy with rotating keys
- ✅ Encrypted local database (SQLCipher)
- ✅ Anonymous temporary identities
- ✅ Metadata minimization
- ✅ Secure peer authentication
- ✅ Encrypted mesh packets
- ✅ Self-destructing messages
- ✅ Hidden chats and panic mode
- ✅ Anti-tracking systems

### 📈 Performance Optimization

- **Battery Optimization**: Adaptive scanning, low-power BLE, background scheduling
- **Memory Efficiency**: Lazy loading, efficient caching, compressed sync packets
- **Low-End Device Support**: Android 7+, 2GB RAM, graceful API fallbacks
- **Network Optimization**: Packet compression, deduplication, adaptive bitrate
- **UI Performance**: Jetpack Compose, reactive updates, smooth animations

### 🧪 Testing & Debugging

#### Run Unit Tests
```bash
./gradlew test
```

#### Run UI Tests
```bash
./gradlew connectedAndroidTest
```

#### Mesh Network Simulator
```bash
python build-scripts/mesh-simulator.py --nodes 5 --duration 120
```

#### Enable Debug Logging
```bash
adb shell setprop log.tag.MeshVerse DEBUG
adb logcat MeshVerse:V *:S
```

### 📚 Documentation

- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System design and component overview
- **[MESH_NETWORKING.md](docs/MESH_NETWORKING.md)** - Mesh routing, relay logic, synchronization
- **[API.md](docs/API.md)** - Complete API reference and usage examples
- **[ENCRYPTION.md](docs/ENCRYPTION.md)** - Signal Protocol implementation details
- **[MINI_APP_SDK.md](docs/MINI_APP_SDK.md)** - Mini app development guide

### 🎨 UI/UX Design

- Cyberpunk-inspired glassmorphism interfaces
- AMOLED dark mode with vibrant accents
- Smooth gesture-based navigation
- Animated mesh network visualizers
- Real-time sync indicators
- Beautiful walkie-talkie activation UI
- Modern social feed layouts

### 🔄 Deployment Pipeline

#### Debug Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/meshverse-debug.apk
```

#### Release Build
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/meshverse-release.apk
```

#### App Bundle
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### 📋 Roadmap

#### Phase 1 (Current)
- ✅ Core messaging with encryption
- ✅ Bluetooth/BLE mesh discovery
- ✅ Basic walkie-talkie
- ✅ Offline maps
- ✅ Local AI assistant
- ✅ Storage and sync

#### Phase 2
- 🔄 Live audio broadcasting
- 🔄 Social feeds with media propagation
- 🔄 Mini app ecosystem
- 🔄 Advanced routing algorithms
- 🔄 Emergency communication mode

#### Phase 3
- 📅 Large-scale mesh simulation
- 📅 Advanced payments system
- 📅 Enterprise features
- 📅 Wearable integration
- 📅 Global internet relay network

### ⚠️ Disclaimers

**Prototype Status**: MeshVerse is currently a prototype implementation. Features are actively being developed and tested.

**Payments**: Any payment system integration is prototype-only and not real banking infrastructure. Do not use for real financial transactions.

**Security**: While end-to-end encryption is implemented, please conduct security audits before production deployment.

**Battery Life**: Mesh networking and background services consume battery. Users should monitor device battery health.

### 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### 📄 License

MeshVerse is released under the MIT License. See [LICENSE](LICENSE) for details.

### 👥 Team

Built with ❤️ by a team of distributed systems engineers, Android developers, and networking specialists.

### 📞 Support

- **Documentation**: See [docs/](docs/) directory
- **Issue Tracker**: [GitHub Issues](https://github.com/2303630100089-lang/offline-social-media-/issues)
- **Discussions**: [GitHub Discussions](https://github.com/2303630100089-lang/offline-social-media-/discussions)

---

**Last Updated**: 2026-05-11  
**Current Version**: 0.1.0-alpha  
**Status**: Active Development 🚀

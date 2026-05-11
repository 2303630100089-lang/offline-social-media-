# MeshVerse Architecture 🏗️

## System Overview

```
┌─────────────────────────────────────────────────────────┐
│              MeshVerse Application Layer                │
├─────────────────────────────────────────────────────────┤
│ UI Layer (Jetpack Compose)                              │
│ ├── Chat Screens                                        │
│ ├── Social Feeds                                        │
│ ├── Maps & Location                                     │
│ ├── Audio Rooms                                         │
│ ├── Walkie-Talkie UI                                    │
│ └── Mini App Marketplace                                │
├─────────────────────────────────────────────────────────┤
│ Presentation Layer (MVVM)                               │
│ ├── ViewModels                                          │
│ ├── UI State Management                                 │
│ └── Navigation                                          │
├─────────────────────────────────────────────────────────┤
│ Domain Layer (Business Logic)                           │
│ ├── Use Cases                                           │
│ ├── Repositories (Interfaces)                           │
│ └── Models                                              │
├─────────────────────────────────────────────────────────┤
│ Data Layer (Repository Pattern)                         │
│ ├── Local (Room Database + SQLCipher)                   │
│ ├── Remote (Mesh/Network APIs)                          │
│ └── Cache                                               │
├─────────────────────────────────────────────────────────┤
│ Service Layer                                           │
│ ├── Mesh Networking Service                             │
│ ├── Encryption Service                                  │
│ ├── Audio Service                                       │
│ ├── Location Service                                    │
│ ├── AI Service                                          │
│ └── Synchronization Service                             │
├─────────────────────────────────────────────────────────┤
│ Transport Layer                                         │
│ ├── Bluetooth Classic                                   │
│ ├── Bluetooth Low Energy (BLE)                          │
│ ├── Wi-Fi Direct                                        │
│ ├── Nearby Connections API                              │
│ ├── NFC                                                 │
│ ├── Local Hotspot                                       │
│ └── Internet (Optional)                                 │
├─────────────────────────────────────────────────────────┤
│ Android System APIs                                     │
│ ├── Bluetooth Manager                                   │
│ ├── Wi-Fi Manager                                       │
│ ├── LocationManager                                     │
│ ├── NFC Manager                                         │
│ ├── Audio Manager                                       │
│ └── Foreground Services                                 │
└─────────────────────────────────────────────────────────┘
```

## Module Structure

```
MeshVerse/
├── app/                              # Main Application Module
│   ├── src/main/
│   │   ├── kotlin/com/meshverse/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MeshVerseApp.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   ├── FeedScreen.kt
│   │   │   │   │   ├── MapScreen.kt
│   │   │   │   │   ├── AudioRoomScreen.kt
│   │   │   │   │   └── ProfileScreen.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── MessageBubble.kt
│   │   │   │   │   ├── PeerCard.kt
│   │   │   │   │   ├── MeshTopology.kt
│   │   │   │   │   └── WalkieTalkieButton.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Typography.kt
│   │   │   │       └── Theme.kt
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── MessageDao.kt
│   │   │   │   │   │   ├── PeerDao.kt
│   │   │   │   │   │   └── ChatDao.kt
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── MessageEntity.kt
│   │   │   │   │   │   ├── PeerEntity.kt
│   │   │   │   │   │   └── ChatEntity.kt
│   │   │   │   │   └── MeshDatabase.kt
│   │   │   │   ├── remote/
│   │   │   │   │   ├── MeshNetworkAPI.kt
│   │   │   │   │   └── PeerDiscoveryAPI.kt
│   │   │   │   └── repository/
│   │   │   │       ├── MessageRepository.kt
│   │   │   │       ├── PeerRepository.kt
│   │   │   │       └── SyncRepository.kt
│   │   │   ├── domain/
│   │   │   │   ├── usecase/
│   │   │   │   │   ├── SendMessageUseCase.kt
│   │   │   │   │   ├── DiscoverPeersUseCase.kt
│   │   │   │   │   └── SyncDataUseCase.kt
│   │   │   │   ├── model/
│   │   │   │   │   ├── Message.kt
│   │   │   │   │   ├── Peer.kt
│   │   │   │   │   └── Chat.kt
│   │   │   │   └── repository/
│   │   │   │       ├── IMessageRepository.kt
│   │   │   │       └── IPeerRepository.kt
│   │   │   ├── presentation/
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── ChatViewModel.kt
│   │   │   │   │   ├── PeerDiscoveryViewModel.kt
│   │   │   │   │   └── MapViewModel.kt
│   │   │   │   └── state/
│   │   │   │       ├── ChatUIState.kt
│   │   │   │       └── PeerUIState.kt
│   │   │   ├── services/
│   │   │   │   ├── mesh/
│   │   │   │   │   ├── MeshNetworkService.kt
│   │   │   │   │   ├── MeshRouter.kt
│   │   │   │   │   ├── PeerDiscoveryService.kt
│   │   │   │   │   └── SynchronizationService.kt
│   │   │   │   ├── messaging/
│   │   │   │   │   ├── MessageService.kt
│   │   │   │   │   └── MessageQueueManager.kt
│   │   │   │   ├── encryption/
│   │   │   │   │   ├── EncryptionService.kt
│   │   │   │   │   ├── KeyManagementService.kt
│   │   │   │   │   └── SignalProtocolImpl.kt
│   │   │   │   ├── audio/
│   │   │   │   │   ├── AudioService.kt
│   │   │   │   │   ├── WalkieTalkieService.kt
│   │   │   │   │   └── AudioStreamManager.kt
│   │   │   │   ├── location/
│   │   │   │   │   ├── LocationService.kt
│   │   │   │   │   └── GPSSyncService.kt
│   │   │   │   ├── ai/
│   │   │   │   │   ├── AIAssistant.kt
│   │   │   │   │   └── OfflineLLMManager.kt
│   │   │   │   └── background/
│   │   │   │       ├── BackgroundSyncWorker.kt
│   │   │   │       └── MeshMaintenanceWorker.kt
│   │   │   └── di/
│   │   │       ├── AppModule.kt
│   │   │       ├── DataModule.kt
│   │   │       ├── ServiceModule.kt
│   │   │       └── NetworkModule.kt
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── features/
│   ├── messaging/                    # Messaging Feature Module
│   │   ├── src/main/kotlin/com/meshverse/messaging/
│   │   ├── build.gradle.kts
│   │   └── ...
│   ├── social/                       # Social Features Module
│   ├── maps/                         # Maps & Location Module
│   ├── ai/                           # AI & Intelligence Module
│   ├── walkie-talkie/                # Walkie-Talkie Module
│   └── payments/                     # Payment System Module
├── sdk/
│   ├── mesh-sdk/                     # Mesh Networking SDK
│   │   ├── src/main/kotlin/
│   │   ├── build.gradle.kts
│   │   └── README.md
│   ├── mini-app-sdk/                 # Mini App SDK
│   └── plugin-framework/             # Plugin Framework
├── build-scripts/
│   ├── build-apk.sh
│   ├── setup-dev-env.sh
│   ├── mesh-simulator.py
│   └── test-runner.sh
├── docs/
│   ├── ARCHITECTURE.md               # This file
│   ├── MESH_NETWORKING.md
│   ├── API.md
│   ├── ENCRYPTION.md
│   ├── diagrams/
│   │   ├── system-architecture.png
│   │   ├── mesh-topology.png
│   │   └── data-flow.png
│   └── examples/
├── build.gradle.kts                  # Root Gradle
└── settings.gradle.kts
```

## Layer Details

### 1. Presentation Layer (UI)

**Technology**: Jetpack Compose

**Responsibilities**:
- Display user interfaces
- Handle user interactions
- Update UI based on ViewModel state
- Show loading/error states

**Key Components**:
- `Screen` composables for full screens
- `Component` composables for reusable UI elements
- State management via ViewModels
- Navigation handling

### 2. Presentation Layer (ViewModel)

**Technology**: AndroidX ViewModel + StateFlow

**Responsibilities**:
- Manage UI state
- Handle user actions
- Communicate with domain layer
- Survive configuration changes

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageUseCase: SendMessageUseCase,
    private val syncUseCase: SyncDataUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ChatUIState>(Loading)
    val uiState: StateFlow<ChatUIState> = _uiState.asStateFlow()
    
    fun sendMessage(message: String) {
        viewModelScope.launch {
            _uiState.value = Sending
            messageUseCase(message).collect { result ->
                _uiState.value = when (result) {
                    is Success -> Success(result.data)
                    is Error -> Error(result.exception)
                }
            }
        }
    }
}
```

### 3. Domain Layer

**Technology**: Pure Kotlin, no Android dependencies

**Responsibilities**:
- Implement business logic
- Define use cases
- Interface with repositories
- Be framework-agnostic

**Use Case Example**:

```kotlin
class SendMessageUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val encryptionService: EncryptionService
) {
    suspend operator fun invoke(message: Message): Flow<Result<Message>> = flow {
        // Encrypt message
        val encrypted = encryptionService.encrypt(message)
        
        // Send via mesh
        val result = messageRepository.saveAndSync(encrypted)
        
        emit(result)
    }
}
```

### 4. Data Layer

**Technologies**:
- **Local**: Room Database + SQLCipher
- **Remote**: Mesh Network APIs
- **Cache**: In-memory and disk caching

**Responsibilities**:
- Abstract data sources
- Implement repositories
- Handle caching
- Manage data synchronization

**Repository Pattern**:

```kotlin
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val meshAPI: MeshNetworkAPI,
    private val encryptionService: EncryptionService
) : IMessageRepository {
    
    override suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            // Save locally
            val entity = message.toEntity()
            messageDao.insert(entity)
            
            // Send via mesh
            val result = meshAPI.sendMessage(message)
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 5. Service Layer

**Core Services**:

#### MeshNetworkService
Manages all mesh networking:
- Peer discovery
- Connection management
- Packet routing
- Relay management

#### EncryptionService
Handles all cryptographic operations:
- Key exchange (Curve25519)
- Message encryption (AES-256-GCM)
- Session management
- Forward secrecy

#### AudioService
Manages audio operations:
- Walkie-talkie push-to-talk
- Voice calls
- Audio broadcasting
- Audio streaming

#### LocationService
Handles GPS and location:
- GPS tracking
- Location sharing
- Location synchronization
- Peer location discovery

#### AIService
Manages offline AI:
- LLM inference
- OCR processing
- Translation
- Voice commands

### 6. Transport Layer

**Networking Stack**:

```
Application Data
     |
     v
┌─────────────────────────────────┐
│  Packet Framing & Compression   │
└─────────────────────────────────┘
     |
     v
┌─────────────────────────────────┐
│    Encryption (AES-256-GCM)     │
└─────────────────────────────────┘
     |
     v
┌─────────────────────────────────┐
│   Mesh Routing & Relay Logic    │
└─────────────────────────────────┘
     |
     v
┌─────────────────────────────────┐
│  Transport Layer Selection      │
├─────────────────────────────────┤
│ ├─ Bluetooth Classic            │
│ ├─ Bluetooth Low Energy         │
│ ├─ Wi-Fi Direct                 │
│ ├─ Nearby Connections API       │
│ ├─ NFC                          │
│ └─ Internet Relay (Optional)    │
└─────────────────────────────────┘
```

## Data Flow Patterns

### Messaging Flow

```
User Input (Chat Screen)
    |
    v
ChatViewModel.sendMessage()
    |
    v
SendMessageUseCase
    |
    v
MessageRepository
    ├─> Save to Room DB
    ├─> Encrypt message
    └─> Send via MeshNetworkService
           |
           v
        MeshRouter (Find path)
           |
           v
        Transport Layer (Bluetooth/BLE/Wi-Fi Direct)
           |
           v
        Recipient Device
```

### Peer Discovery Flow

```
PeerDiscoveryViewModel
    |
    v
DiscoverPeersUseCase
    |
    v
PeerRepository
    |
    v
PeerDiscoveryService
    ├─> Start Bluetooth scanning
    ├─> Start BLE scanning
    ├─> Start Wi-Fi Direct discovery
    └─> Start Nearby Connections scanning
           |
           v
        Collect peer advertisements
           |
           v
        Authenticate & establish connection
           |
           v
        Save to Room DB
           |
           v
        Update UI State
```

### Synchronization Flow

```
Background Sync (WorkManager)
    |
    v
SynchronizationService
    |
    v
Find nearby peers
    |
    v
Exchange local database deltas
    |
    v
Resolve conflicts
    |
    v
Apply updates to local DB
    |
    v
Notify UI to refresh
```

## Dependency Injection (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideMeshNetworkService(
        context: Context
    ): MeshNetworkService = MeshNetworkService(context)
    
    @Provides
    @Singleton
    fun provideEncryptionService(): EncryptionService =
        EncryptionServiceImpl()
    
    @Provides
    @Singleton
    fun provideMeshRouter(): MeshRouter = MeshRouter()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideMeshDatabase(context: Context): MeshDatabase =
        Room.databaseBuilder(
            context,
            MeshDatabase::class.java,
            "meshverse.db"
        )
        .addMigrations(*MeshDatabase.MIGRATIONS)
        .build()
    
    @Provides
    fun provideMessageDao(db: MeshDatabase): MessageDao =
        db.messageDao()
}
```

## Thread Safety & Coroutines

- **Main Thread**: UI updates only
- **IO Thread**: Database and network operations
- **Default Thread**: Heavy computations
- **Unconfined**: Internal state management

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    // Database operation
    val messages = messageDao.getAllMessages()
    
    withContext(Dispatchers.Main) {
        // Update UI
        _uiState.value = Success(messages)
    }
}
```

## Lifecycle & Memory Management

- **Fragment Lifecycle**: Bind to fragment scope
- **Activity Lifecycle**: Bind to activity scope
- **Application Lifecycle**: Singleton services
- **ViewModel Cleanup**: Clear resources in onCleared()

## Performance Optimization

1. **Lazy Loading**: Load data on demand
2. **Pagination**: Load messages in chunks
3. **Caching**: Cache frequently accessed data
4. **Indexing**: Database indexes for fast queries
5. **Compression**: Compress mesh packets
6. **Deduplication**: Avoid sending duplicate packets

## Error Handling

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

## Testing Strategy

- **Unit Tests**: Pure logic without Android dependencies
- **Integration Tests**: Database + repository tests
- **UI Tests**: Compose screen tests
- **End-to-End Tests**: Complete feature workflows

---

**Last Updated**: 2026-05-11
**Architecture Version**: 1.0

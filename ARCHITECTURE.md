# EyesPie Architecture

This document describes the architectural patterns, design decisions, and technical implementation of the EyesPie application.

## Table of Contents

- [Overview](#overview)
- [Architectural Patterns](#architectural-patterns)
- [Module Structure](#module-structure)
- [Data Flow](#data-flow)
- [Platform Abstraction](#platform-abstraction)
- [Dependency Injection](#dependency-injection)
- [Navigation](#navigation)
- [State Management](#state-management)
- [Machine Learning Pipeline](#machine-learning-pipeline)
- [Offline Support](#offline-support)
- [Real-time Features](#real-time-features)
- [Security](#security)
- [Performance Considerations](#performance-considerations)

## Overview

EyesPie follows **Clean Architecture** principles with feature-based module organization. The architecture emphasizes:

- **Separation of Concerns**: Clear boundaries between UI, business logic, and data
- **Testability**: Dependency injection and interface-based design
- **Platform Independence**: Shared code with platform-specific implementations
- **Offline-First**: Network-first with cache fallback strategy

### Architectural Layers

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Screen    │  │ ScreenModel │  │ Environment │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────┤
│                    Domain Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Entities   │  │  UseCases   │  │  Services   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────┤
│                     Data Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Repositories│  │   Sources   │  │   Models    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

## Architectural Patterns

### Flux Architecture

EyesPie implements a unidirectional data flow pattern inspired by Flux:

```
Action → Dispatcher → Store → View
  ↑                           │
  └───────────────────────────┘
```

**Components:**
- **Action**: User interactions or system events
- **Dispatcher**: Routes actions to appropriate stores
- **Store**: Manages state and business logic
- **View**: Renders UI based on state

### Repository Pattern

Data access is abstracted through repositories:

```kotlin
// Domain interface
interface GameRepository {
    suspend fun getGames(): List<Game>
    suspend fun createGame(game: Game): Game
    suspend fun updateGame(game: Game): Game
}

// Data implementation
class GameDataRepository(
    private val remoteSource: GameRemoteSource,
    private val localSource: GamesLocalSource
) : GameRepository {
    // Network-first, cache fallback implementation
}
```

### Use Case Pattern

Business logic is encapsulated in use cases:

```kotlin
class CreateGameUseCase(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(params: CreateGameParams): Game {
        // Business logic
        // Validation
        // Repository calls
        return game
    }
}
```

### Screen/ScreenModel Pattern

UI is organized into screens with dedicated state management:

```kotlin
// Screen definition
class GameScreen : Screen {
    @Composable
    override fun Content() {
        val model = getScreenModel<GameScreenModel>()
        val state by model.state.collectAsState()
        
        GameContent(
            state = state,
            onAction = model::onAction
        )
    }
}

// ScreenModel (state management)
class GameScreenModel : ScreenModel {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()
    
    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.LoadGames -> loadGames()
            is GameAction.CreateGame -> createGame(action.params)
        }
    }
}
```

## Module Structure

### Bluebell Framework (`bluebell/`)

The foundation framework providing:

```
bluebell/
├── src/commonMain/kotlin/com/micrantha/bluebell/
│   ├── app/                    # Application utilities
│   │   ├── navi/              # Navigation helpers
│   │   ├── Errors.kt          # Error handling
│   │   └── Scaffolding.kt     # App scaffolding
│   ├── arch/                   # Architecture components
│   │   ├── Dispatcher.kt      # Action dispatcher
│   │   ├── Effect.kt          # Side effects
│   │   ├── Reducer.kt         # State reduction
│   │   ├── State.kt           # State management
│   │   └── Store.kt           # Store implementation
│   ├── data/                   # Data utilities
│   │   ├── MemoryStore.kt     # In-memory storage
│   │   └── DownloadState.kt   # Download state management
│   ├── domain/                 # Domain utilities
│   │   ├── StateMap.kt        # State mapping
│   │   ├── ReactiveMap.kt     # Reactive collections
│   │   └── security/          # Security utilities
│   ├── flux/                   # Flux implementation
│   │   ├── Flux.kt            # Core Flux
│   │   ├── FluxDispatcher.kt  # Flux dispatcher
│   │   ├── FluxEffects.kt     # Flux effects
│   │   └── FluxStore.kt       # Flux store
│   ├── i18n/                   # Internationalization
│   │   ├── entity/            # Localization entities
│   │   └── repository/        # Localization repositories
│   ├── observability/          # Monitoring and analytics
│   │   ├── domain/            # Observability domains
│   │   ├── entity/            # Event entities
│   │   ├── repository/        # Event repositories
│   │   └── usecase/           # Observability use cases
│   ├── platform/               # Platform abstractions
│   │   ├── FileSystem.kt      # File system interface
│   │   ├── NetworkMonitor.kt  # Network monitoring
│   │   ├── GenAI.kt           # AI model interface
│   │   └── Image.kt           # Image processing
│   └── ui/                     # UI components
│       ├── components/         # Reusable components
│       ├── model/             # UI models
│       ├── screen/            # Screen utilities
│       └── theme/             # Theming
```

### EyesPie App (`eyespie/`)

The main application module:

```
eyespie/
├── src/commonMain/kotlin/com/micrantha/eyespie/
│   ├── app/                    # App entry point
│   │   ├── App.kt            # Main app composable
│   │   ├── Module.kt         # DI module
│   │   ├── Theme.kt          # App theme
│   │   └── ui/               # Main UI
│   ├── config/                 # Configuration
│   │   └── AppConfigDelegate.kt
│   ├── core/                   # Core infrastructure
│   │   ├── data/             # Core data layer
│   │   │   ├── account/      # Account management
│   │   │   ├── ai/           # AI services
│   │   │   ├── client/       # HTTP clients
│   │   │   ├── db/           # Database
│   │   │   ├── observability/# Monitoring
│   │   │   ├── storage/      # File storage
│   │   │   └── system/       # System services
│   │   └── ui/               # Core UI components
│   ├── domain/                 # Domain layer
│   │   ├── entities/          # Business entities
│   │   ├── logic/             # Business logic
│   │   ├── repository/        # Repository interfaces
│   │   └── usecase/           # Domain use cases
│   ├── features/               # Feature modules
│   │   ├── dashboard/         # Home screen
│   │   ├── game/              # Game management
│   │   ├── guess/             # Guessing gameplay
│   │   ├── login/             # Authentication
│   │   ├── onboarding/        # First-run experience
│   │   ├── players/           # Player management
│   │   ├── register/          # User registration
│   │   ├── scan/              # Camera and ML
│   │   └── things/            # Things management
│   └── platform/               # Platform-specific code
│       └── scan/              # Camera implementations
```

## Data Flow

### Request Flow

```
User Action → Screen → ScreenModel → UseCase → Repository → Source
                                                              ↓
UI Update ← State ← ScreenModel ← UseCase ← Repository ← Source
```

### Example: Creating a Game

1. **User Interaction**: User fills form and taps "Create"
2. **Screen**: `GameCreateScreen` captures action
3. **ScreenModel**: `GameCreateScreenModel` processes action
4. **UseCase**: `CreateGameUseCase` validates and orchestrates
5. **Repository**: `GameDataRepository` coordinates sources
6. **Source**: `GameRemoteSource` sends to Supabase
7. **State Update**: New game state flows back to UI

### Data Transformation

```kotlin
// Network response
data class GameResponse(
    val id: String,
    val name: String,
    val created_at: String
)

// Domain entity
data class Game(
    val id: GameId,
    val name: String,
    val createdAt: LocalDateTime
)

// Mapper
fun GameResponse.toDomain(): Game = Game(
    id = GameId(id),
    name = name,
    createdAt = created_at.toLocalDateTime()
)
```

## Platform Abstraction

### Interface-Based Design

Platform-specific code is abstracted through interfaces:

```kotlin
// Common interface
interface CameraCapture {
    suspend fun captureImage(): Image?
    fun startPreview()
    fun stopPreview()
}

// Android implementation
class AndroidCameraCapture(
    private val context: Context
) : CameraCapture {
    // Android-specific camera implementation
}

// iOS implementation
class IOSCameraCapture(
    private val device: AVCaptureDevice
) : CameraCapture {
    // iOS-specific camera implementation
}
```

### Expect/Actual Pattern

Kotlin Multiplatform uses expect/actual for platform-specific code:

```kotlin
// Common module
expect class PlatformImage {
    fun toByteArray(): ByteArray
}

// Android module
actual class PlatformImage(
    private val bitmap: Bitmap
) {
    actual fun toByteArray(): ByteArray {
        // Android-specific implementation
    }
}

// iOS module
actual class PlatformImage(
    private val uiImage: UIImage
) {
    actual fun toByteArray(): ByteArray {
        // iOS-specific implementation
    }
}
```

## Dependency Injection

### Kodein DI Setup

Dependencies are organized into modules:

```kotlin
// Core module
val coreModule = DI.Module("core") {
    bindSingleton { HttpClient() }
    bindSingleton { SupaClient(instance()) }
    bindSingleton { DatabaseDriverFactory(instance()) }
}

// Feature module
val gameModule = DI.Module("game") {
    bindSingleton { GameDataRepository(instance(), instance()) }
    bindFactory { GameCreateScreenModel(instance()) }
}

// App module
val appModule = DI.Module("app") {
    import(coreModule)
    import(gameModule)
    // ... other modules
}
```

### Dependency Graph

```
App
├── Bluebell Framework
│   ├── UI Components
│   ├── Navigation
│   └── Platform Abstractions
├── Core Module
│   ├── HTTP Clients
│   ├── Database
│   └── Storage
├── Domain Module
│   ├── Entities
│   ├── Use Cases
│   └── Repository Interfaces
└── Feature Modules
    ├── Dashboard
    ├── Game
    ├── Scan
    └── ...
```

## Navigation

### Voyager Navigation

EyesPie uses Voyager for type-safe navigation:

```kotlin
// Screen definition
class GameListScreen : Screen {
    @Composable
    override fun Content() {
        // Screen content
    }
}

// Navigation
class GameEnvironment(
    private val navigate: ScreenContext
) {
    fun openGame(gameId: GameId) {
        navigate<GameDetailScreen> {
            arg("gameId", gameId.value)
        }
    }
}
```

### Navigation Patterns

- **Tab Navigation**: Dashboard tabs (Friends, Nearby, Scan)
- **Stack Navigation**: Screen-to-screen navigation
- **Modal Navigation**: Dialogs and bottom sheets
- **Deep Linking**: URL-based navigation

## State Management

### Contract Pattern

Each screen defines a contract:

```kotlin
data class GameListContract(
    val state: GameListUiState,
    val actions: List<GameListAction>,
    val effects: Flow<GameListEffect>
)

// UI State
data class GameListUiState(
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Actions
sealed class GameListAction {
    object LoadGames : GameListAction()
    data class GameClicked(val gameId: GameId) : GameListAction()
}

// Effects
sealed class GameListEffect {
    data class NavigateToGame(val gameId: GameId) : GameListEffect()
    data class ShowError(val message: String) : GameListEffect()
}
```

### State Flow

```kotlin
class GameListScreenModel : ScreenModel {
    private val _state = MutableStateFlow(GameListUiState())
    val state: StateFlow<GameListUiState> = _state.asStateFlow()
    
    private val _effects = Channel<GameListEffect>()
    val effects: Flow<GameListEffect> = _effects.receiveAsFlow()
    
    fun onAction(action: GameListAction) {
        when (action) {
            is GameListAction.LoadGames -> loadGames()
            is GameListAction.GameClicked -> {
                _effects.trySend(GameListEffect.NavigateToGame(action.gameId))
            }
        }
    }
    
    private fun loadGames() {
        _state.update { it.copy(isLoading = true) }
        // Load games...
        _state.update { it.copy(isLoading = false, games = games) }
    }
}
```

## Machine Learning Pipeline

### ML Architecture

```
Camera Input → Preprocessing → Model Inference → Post-processing → Result
     ↓              ↓              ↓              ↓            ↓
  Image        Resize/Crop    TensorFlow     Parse Output   Domain
  Capture      Normalize      Lite/MediaPipe  Filter        Entities
```

### Model Types

1. **Image Classification** (EfficientNet)
   - Object recognition
   - Scene understanding

2. **Object Detection**
   - Bounding box detection
   - Object localization

3. **Image Segmentation** (DeepLab)
   - Pixel-level classification
   - Semantic segmentation

4. **Image Embeddings** (MobileNet)
   - Feature extraction
   - Similarity matching

5. **Style Transfer**
   - Artistic effects
   - Image transformation

### Pipeline Implementation

```kotlin
// ML Pipeline interface
interface ImageAnalyzer {
    suspend fun analyze(image: PlatformImage): AnalysisResult
}

// Analysis result
data class AnalysisResult(
    val labels: List<Label>,
    val embeddings: List<Float>,
    val segmentation: SegmentationMap?,
    val objects: List<DetectedObject>
)

// Pipeline implementation
class TensorFlowAnalyzer(
    private val classifier: ImageClassifier,
    private val detector: ObjectDetector,
    private val embedder: ImageEmbedder
) : ImageAnalyzer {
    override suspend fun analyze(image: PlatformImage): AnalysisResult {
        val preprocessed = preprocess(image)
        val labels = classifier.classify(preprocessed)
        val objects = detector.detect(preprocessed)
        val embeddings = embedder.embed(preprocessed)
        
        return AnalysisResult(labels, embeddings, null, objects)
    }
}
```

## Offline Support

### Offline-First Strategy

1. **Network First**: Try network request
2. **Cache Fallback**: Use cached data if network fails
3. **Queue Mutations**: Store writes for later sync
4. **Background Sync**: Sync when network available

### Implementation

```kotlin
class GameRepository(
    private val remoteSource: GameRemoteSource,
    private val localSource: GamesLocalSource,
    private val syncQueue: SyncQueue
) {
    suspend fun getGames(): List<Game> {
        return try {
            // Network first
            val games = remoteSource.getGames()
            localSource.cacheGames(games)
            games
        } catch (e: Exception) {
            // Cache fallback
            localSource.getCachedGames()
        }
    }
    
    suspend fun createGame(game: Game): Game {
        return try {
            // Try network
            val created = remoteSource.createGame(game)
            localSource.cacheGame(created)
            created
        } catch (e: Exception) {
            // Queue for later sync
            syncQueue.enqueue(SyncOperation.CreateGame(game))
            game
        }
    }
}
```

### Sync Queue

```kotlin
class SyncQueue(
    private val database: Database
) {
    suspend fun enqueue(operation: SyncOperation) {
        database.syncOperations.insert(operation.toEntity())
    }
    
    suspend fun syncPending() {
        val pending = database.syncOperations.getAllPending()
        pending.forEach { operation ->
            try {
                executeOperation(operation)
                database.syncOperations.markSynced(operation.id)
            } catch (e: Exception) {
                // Retry later
            }
        }
    }
}
```

## Real-time Features

### WebSocket Implementation

```kotlin
class RealtimeClient(
    private val supabaseClient: SupabaseClient
) {
    fun observeGames(): Flow<GameEvent> = callbackFlow {
        val channel = supabaseClient.realtime.channel("games")
        
        channel.subscribe { event ->
            when (event) {
                is GameInserted -> send(GameEvent.Created(event.game))
                is GameUpdated -> send(GameEvent.Updated(event.game))
                is GameDeleted -> send(GameEvent.Deleted(event.gameId))
            }
        }
        
        awaitClose { channel.unsubscribe() }
    }
}
```

### Real-time Events

```kotlin
sealed class GameEvent {
    data class Created(val game: Game) : GameEvent()
    data class Updated(val game: Game) : GameEvent()
    data class Deleted(val gameId: GameId) : GameEvent()
    data class PlayerJoined(val gameId: GameId, val player: Player) : GameEvent()
    data class PlayerLeft(val gameId: GameId, val playerId: PlayerId) : GameEvent()
}
```

## Security

### Authentication

```kotlin
class AuthService(
    private val supabaseClient: SupaClient
) {
    suspend fun signIn(email: String, password: String): Session {
        return supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }
    
    suspend fun signUp(email: String, password: String): Session {
        return supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }
}
```

### Data Security

- **Encryption at Rest**: Supabase handles database encryption
- **Encryption in Transit**: HTTPS for all API calls
- **Row Level Security**: Database policies for data access
- **Token Management**: Secure token storage on device

### Secure Storage

```kotlin
expect class SecureStorage {
    suspend fun store(key: String, value: String)
    suspend fun retrieve(key: String): String?
    suspend fun delete(key: String)
}
```

## Performance Considerations

### Image Optimization

- **Compression**: Use appropriate image formats
- **Resizing**: Resize before ML processing
- **Caching**: Cache processed images
- **Lazy Loading**: Load images on demand

### Memory Management

- **Object Pooling**: Reuse ML model instances
- **Lazy Initialization**: Initialize components on demand
- **Cleanup**: Release resources when not needed

### Network Optimization

- **Pagination**: Load data in chunks
- **Compression**: Use gzip for API responses
- **Caching**: HTTP caching headers
- **Connection Pooling**: Reuse HTTP connections

### Battery Optimization

- **Background Limits**: Minimize background work
- **Batch Operations**: Group similar operations
- **Smart Scheduling**: Use WorkManager for deferred tasks

## Testing Strategy

### Test Pyramid

```
        ┌─────────────┐
        │     E2E     │  Few
        ├─────────────┤
        │ Integration │  Some
        ├─────────────┤
        │    Unit     │  Many
        └─────────────┘
```

### Test Types

1. **Unit Tests**: Business logic, use cases
2. **Integration Tests**: Repositories, data sources
3. **UI Tests**: Compose components
4. **E2E Tests**: Full user flows

### Test Coverage Goals

- **Unit Tests**: 80%+ coverage
- **Integration Tests**: 70%+ coverage
- **Critical Paths**: 100% coverage

## Monitoring and Observability

### Analytics Events

```kotlin
sealed class AnalyticsEvent {
    data class UserAction(val action: String, val params: Map<String, Any>) : AnalyticsEvent()
    data class ScreenView(val screen: String) : AnalyticsEvent()
    data class Error(val error: String, val stackTrace: String) : AnalyticsEvent()
}
```

### Telemetry

- **Performance Metrics**: App startup, screen load times
- **Usage Analytics**: Feature adoption, user flows
- **Error Tracking**: Crash reports, error rates
- **Business Metrics**: Games created, matches made

## Future Considerations

### Scalability

- **Modular Architecture**: Easy to add new features
- **Microservices**: Backend can be split if needed
- **CDN**: Static asset delivery

### Extensibility

- **Plugin System**: Allow third-party extensions
- **API Versioning**: Support multiple API versions
- **Feature Flags**: Gradual feature rollout

### Maintainability

- **Documentation**: Keep docs updated
- **Code Review**: Enforce quality standards
- **Automated Testing**: CI/CD integration

---

This architecture is designed to scale with the project's growth while maintaining code quality and developer productivity.
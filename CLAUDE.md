# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EyesPie is a Kotlin Multiplatform mobile game implementing "I Spy" with machine vision. Players create visual challenges using AI-generated clues and image obfuscation, while others guess using photo matching with embeddings.

Key technologies:
- Kotlin Multiplatform with Compose Multiplatform
- TensorFlow Lite & MediaPipe for ML models
- Supabase backend with GraphQL
- Kodein DI for dependency injection
- Voyager for navigation

## Build & Development Commands

### Building
```bash
# Build debug versions for both platforms
./gradlew build
fastlane build_debug

# Build release versions
fastlane build_release

# Build Android only
./gradlew assembleDebug
./gradlew assembleRelease

# Build iOS (requires macOS)
fastlane ios build_debug
fastlane ios build_release
```

### Testing
```bash
# Run all tests
fastlane test
./gradlew test

# Run specific tests
./gradlew testDebugUnitTest
```

### Deployment
```bash
# Deploy to development/staging environments
fastlane distribute_development
fastlane distribute_staging

# Deploy to production
fastlane distribute_production
```

### Environment Setup
Copy `env.example` to `.env.local` and configure:
- `SUPABASE_URL`: Supabase project URL
- `SUPABASE_KEY`: Supabase anon key  
- `HUGGING_FACE_TOKEN`: For AI model access
- `LOGIN_EMAIL`/`LOGIN_PASSWORD`: Development login credentials

## Architecture

### Module Structure
The app follows Clean Architecture with feature-based modules:

- **Bluebell Framework** (`com.micrantha.bluebell`): Shared architecture foundation
  - State management via Flux pattern (Store/Reducer/Effect)
  - UI components and theming
  - Platform abstractions (networking, file system, notifications)

- **Core** (`com.micrantha.eyespie.core`): Shared app infrastructure
  - Data repositories and mapping
  - HTTP client configuration
  - Location and realtime services

- **Domain** (`com.micrantha.eyespie.domain`): Business logic
  - Entities: Game, Thing, Player, Location, Clues
  - Game logic and distance calculations
  - Repository interfaces

- **Features** (feature-based packages):
  - `dashboard`: Home screen with friends/nearby/scan tabs
  - `game`: Game creation, listing, and details
  - `scan`: Camera capture with ML analysis
  - `login`/`register`: Authentication flows
  - `players`: Player management
  - `guess`: Photo guessing gameplay

### Key Patterns
- **Screen/ScreenModel**: UI layer using Voyager navigation with ScreenModels for state
- **Contract classes**: Define UI state, actions, and effects for each screen
- **Repository pattern**: Data layer abstraction with local/remote sources
- **Dependency Injection**: Kodein DI modules throughout the app
- **ML Pipeline**: Camera → Analyzer → TensorFlow/MediaPipe models → Results

### Platform-Specific Code
- Android: `src/androidMain` - Camera implementations, ML model integration
- iOS: `src/iosMain` - Native camera and ML model bindings via CocoaPods
- Common: `src/commonMain` - Shared business logic and UI

## ML Models & Assets
The app downloads and bundles various TensorFlow Lite models:
- Image classification (EfficientNet)
- Object detection
- Image segmentation (DeepLab)
- Image embeddings (MobileNet)
- Style transfer models

Models are configured in `composeApp/build.gradle.kts` under the `bluebell.assets.downloads` block.
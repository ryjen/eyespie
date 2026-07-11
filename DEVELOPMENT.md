# Development Guide

This guide covers setting up the EyesPie development environment, workflows, and best practices.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [IDE Configuration](#ide-configuration)
- [Build System](#build-system)
- [Development Workflow](#development-workflow)
- [Debugging](#debugging)
- [Performance Profiling](#performance-profiling)
- [Common Issues](#common-issues)
- [Tips and Tricks](#tips-and-tricks)

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| JDK | 17+ | Kotlin compilation |
| Android Studio | 2023.2+ | Android development |
| Xcode | 14.0+ | iOS development |
| Git | 2.40+ | Version control |
| Node.js | 18+ | Build tools |

### Optional Tools

| Software | Purpose |
|----------|---------|
| Fastlane | CI/CD automation |
| CocoaPods | iOS dependency management |
| Homebrew | Package management (macOS) |
| direnv | Environment variable management |

## Environment Setup

### 1. Install JDK 17+

**macOS (Homebrew):**
```bash
brew install openjdk@17
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

**Windows:**
Download from [Adoptium](https://adoptium.net/)

### 2. Install Android Studio

1. Download from [developer.android.com](https://developer.android.com/studio)
2. Install with default settings
3. Launch and complete initial setup
4. Install Kotlin Multiplatform plugin:
   - Settings → Plugins → Marketplace
   - Search "Kotlin Multiplatform"
   - Install and restart

### 3. Install Xcode (macOS only)

1. Download from Mac App Store
2. Launch Xcode and accept license
3. Install additional components

### 4. Install CocoaPods (macOS only)

```bash
sudo gem install cocoapods
```

### 5. Clone and Setup

```bash
# Clone repository
git clone https://github.com/hackelia-micrantha/eyespie.git
cd eyespie

# Copy environment template
cp env.example .env.local

# Edit .env.local with your credentials
# Use your preferred editor

# Build the project
./gradlew build
```

### 6. iOS Setup (macOS only)

```bash
cd iosApp
pod install
open iosApp.xcworkspace
```

## IDE Configuration

### Android Studio

#### Kotlin Multiplatform Plugin

1. Install plugin (see above)
2. Enable experimental features:
   - Settings → Experimental
   - Enable "Compose Multiplatform"
   - Enable "Kotlin Multiplatform"

#### Code Style

1. Settings → Editor → Code Style → Kotlin
2. Set tab size to 4
3. Enable "Continuation indent"
4. Import project code style if available

#### Run Configurations

1. Run → Edit Configurations
2. Add Android App configuration
3. Select module: `eyespie.app`
4. Set default device

#### Gradle Settings

1. Settings → Build → Gradle
2. Enable "Configure build scripts automatically"
3. Set Gradle JVM to JDK 17

### Xcode (iOS)

#### Workspace Setup

1. Open `iosApp/iosApp.xcworkspace`
2. Select project in navigator
3. Verify signing team
4. Update bundle identifier if needed

#### Build Settings

1. Select your target
2. Build Settings tab
3. Verify:
   - Swift Language Version: 5.0
   - iOS Deployment Target: 15.0+

## Build System

### Gradle Commands

```bash
# Full build
./gradlew build

# Clean build
./gradlew clean build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all tests
./gradlew test

# Run unit tests only
./gradlew testDebugUnitTest

# Check code style
./gradlew ktlintCheck

# Format code
./gradlew ktlintFormat
```

### Fastlane Commands

```bash
# Install dependencies
bundle install

# Build debug (both platforms)
fastlane build_debug

# Build release (both platforms)
fastlane build_release

# Run tests
fastlane test

# Deploy to development
fastlane distribute_development

# Deploy to staging
fastlane distribute_staging

# Deploy to production
fastlane distribute_production
```

### Build Variants

#### Android

- **debug**: Development build with debugging enabled
- **release**: Production build with optimizations
- **androidTest**: Instrumentation tests

#### iOS

- **Debug**: Development build with debugging
- **Release**: Production build with optimizations
- **AdHoc**: Testing distribution

## Development Workflow

### Daily Workflow

1. **Update main branch**:
   ```bash
   git checkout develop
   git pull upstream develop
   ```

2. **Create feature branch**:
   ```bash
   git checkout -b feature/your-feature
   ```

3. **Make changes**:
   - Write code
   - Add tests
   - Update documentation if needed

4. **Test locally**:
   ```bash
   ./gradlew test
   fastlane test
   ```

5. **Commit changes**:
   ```bash
   git add .
   git commit -m "feat(scope): description"
   ```

6. **Push and create PR**:
   ```bash
   git push origin feature/your-feature
   ```

### Feature Development Process

1. **Planning**:
   - Review requirements
   - Break down into tasks
   - Estimate effort

2. **Implementation**:
   - Start with tests (TDD recommended)
   - Implement feature
   - Add documentation

3. **Testing**:
   - Unit tests
   - Integration tests
   - Manual testing

4. **Review**:
   - Self-review code
   - Address review feedback
   - Final testing

5. **Merge**:
   - Squash and merge to develop
   - Delete feature branch

### Hotfix Process

1. **Create hotfix branch**:
   ```bash
   git checkout main
   git checkout -b hotfix/issue-description
   ```

2. **Fix the issue**:
   - Minimal changes only
   - Add regression test

3. **Test thoroughly**:
   ```bash
   ./gradlew test
   fastlane test
   ```

4. **Merge to main and develop**:
   ```bash
   git checkout main
   git merge hotfix/issue-description
   git checkout develop
   git merge hotfix/issue-description
   ```

## Debugging

### Android Debugging

#### Using Android Studio

1. Set breakpoints in code
2. Click Debug button or press `Shift+F9`
3. Use Debug tool window:
   - Variables pane
   - Evaluate expressions
   - Step through code

#### Logcat

```bash
# View logs
adb logcat

# Filter by tag
adb logcat -s EyesPie

# Clear logs
adb logcat -c
```

#### Common Debug Commands

```bash
# Install debug APK
adb install -r eyespie/build/outputs/apk/debug/eyespie-debug.apk

# Launch app
adb shell am start -n com.micrantha.eyespie/.MainActivity

# View database
adb shell run-as com.micrantha.eyespie ls databases/
```

### iOS Debugging

#### Using Xcode

1. Select scheme in toolbar
2. Click Debug button or press `Cmd+U`
3. Use Debug navigator:
   - View threads
   - Inspect variables
   - Step through code

#### Console

```bash
# View device logs
xcrun devicectl device info log show --device <device-id>
```

### Kotlin Multiplatform Debugging

#### Common Code

1. Use `println()` for quick debugging
2. Use `Log.d()` from platform logger
3. Set breakpoints in common code (works on both platforms)

#### Platform Code

1. Use platform-specific debuggers
2. Check platform logs separately
3. Test on both platforms

## Performance Profiling

### Android Profiler

1. Open Android Studio
2. View → Tool Windows → Profiler
3. Select process
4. Analyze:
   - **CPU**: Thread activity, method tracing
   - **Memory**: Allocations, GC events
   - **Network**: Request timing
   - **Energy**: Battery usage

### Instruments (iOS)

1. Open Xcode
2. Product → Profile
3. Select template:
   - Time Profiler: CPU usage
   - Allocations: Memory usage
   - Network: Network activity
   - Energy Log: Battery usage

### Common Performance Issues

#### Memory Leaks

```kotlin
// Check for leaked resources
class ExampleScreenModel : ScreenModel {
    init {
        // Ensure cleanup in onDispose
        DisposableEffect(Unit) {
            onDispose {
                // Cleanup resources
            }
        }
    }
}
```

#### Slow UI Rendering

```kotlin
// Avoid expensive operations in composition
@Composable
fun ExpensiveComponent(data: List<Item>) {
    // Use remember for expensive calculations
    val processedData = remember(data) {
        data.map { processExpensive(it) }
    }
    
    LazyColumn {
        items(processedData) { item ->
            ItemRow(item)
        }
    }
}
```

#### Network Optimization

```kotlin
// Use pagination
class GameListScreenModel : ScreenModel {
    fun loadMore() {
        if (isLoading || !hasMore) return
        // Load next page
    }
}
```

## Common Issues

### Build Failures

#### "Could not resolve dependencies"

```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches

# Rebuild
./gradlew build
```

#### "SDK not found"

1. Open Android Studio
2. Settings → SDK Manager
3. Install required SDK versions
4. Sync project

#### "CocoaPods not installed"

```bash
# Install CocoaPods
sudo gem install cocoapods

# Install pods
cd iosApp
pod install
```

### Runtime Issues

#### "Supabase connection failed"

1. Check `.env.local` credentials
2. Verify network connection
3. Check Supabase project status

#### "ML model not found"

1. Ensure models are downloaded
2. Check onboarding flow
3. Verify model assets in `bluebellAssets/`

#### "Camera permission denied"

1. Check device settings
2. Verify manifest permissions
3. Request permissions at runtime

### Development Issues

#### "Hot reload not working"

```bash
# Restart Gradle daemon
./gradlew --stop

# Invalidate caches
# File → Invalidate Caches → Invalidate and Restart
```

#### "IDE not recognizing Kotlin Multiplatform"

1. File → Sync Project with Gradle Files
2. File → Invalidate Caches → Invalidate and Restart
3. Reinstall Kotlin Multiplatform plugin

## Tips and Tricks

### Productivity Shortcuts

#### Android Studio

- `Cmd/Ctrl + Shift + A`: Find Action
- `Cmd/Ctrl + E`: Recent Files
- `Cmd/Ctrl + Shift + E`: Recently Changed Files
- `Cmd/Ctrl + F12`: File Structure
- `Shift twice`: Search Everywhere

#### Xcode

- `Cmd/Ctrl + Shift + O`: Open Quickly
- `Cmd/Ctrl + Shift + F`: Find in Workspace
- `Cmd/Ctrl + /`: Comment/Uncomment
- `Cmd/Ctrl + Option + /`: Documentation Comment

### Useful Commands

```bash
# Find files by name
find . -name "*.kt" -type f

# Search for text
grep -r "TODO" --include="*.kt" .

# Count lines of code
find . -name "*.kt" -exec cat {} + | wc -l

# Check git status
git status

# View git log
git log --oneline --graph --all
```

### Kotlin Multiplatform Tips

1. **Use expect/actual wisely**: Keep platform code minimal
2. **Test on both platforms**: Don't assume behavior
3. **Use common tests**: Write tests in `commonTest`
4. **Leverage Compose Multiplatform**: Share UI code

### Compose Tips

1. **Use derivedStateOf**: For expensive state calculations
2. **Remember expensive objects**: Avoid recomposition overhead
3. **Use key() in lists**: Help Compose identify items
4. **Minimize side effects**: Use LaunchedEffect wisely

### Performance Tips

1. **Profile early**: Don't wait for production
2. **Use lazy lists**: LazyColumn/LazyRow for large lists
3. **Avoid heavy composition**: Move to side effects
4. **Cache network responses**: Use repository pattern

## Resources

### Documentation

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Supabase Kotlin](https://supabase.com/docs/reference/kotlin/introduction)
- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [MediaPipe](https://developers.google.com/mediapipe)

### Community

- [Kotlin Slack](https://kotlinlang.org/docs/community.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Supabase Discord](https://supabase.com/discord)

### Learning Resources

- [Kotlin Multiplatform Samples](https://github.com/JetBrains/kotlin/tree/main/templates)
- [Compose Multiplatform Samples](https://github.com/JetBrains/compose-multiplatform)
- [Supabase Examples](https://github.com/supabase/supabase/tree/master/examples)

## Getting Help

### When Stuck

1. Check this documentation
2. Search existing issues
3. Ask in discussions
4. Create minimal reproduction
5. Ask for help with context

### Useful Links

- [GitHub Issues](https://github.com/hackelia-micrantha/eyespie/issues)
- [GitHub Discussions](https://github.com/hackelia-micrantha/eyespie/discussions)
- [Project Board](https://github.com/orgs/hackelia-micrantha/projects/3)

---

Happy coding! 🚀
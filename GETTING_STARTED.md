# Getting Started with EyesPie

This guide will help you set up EyesPie for development and testing.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Configuration](#configuration)
- [First Run](#first-run)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required

| Software | Version | Notes |
|----------|---------|-------|
| JDK | 17+ | OpenJDK or Oracle JDK |
| Git | 2.40+ | Version control |
| Node.js | 18+ | Build tools (optional) |

### For Android Development

| Software | Version | Notes |
|----------|---------|-------|
| Android Studio | 2023.2+ | With Kotlin Multiplatform plugin |
| Android SDK | API 24+ | Minimum supported version |
| Android Build Tools | 34.0.0 | Latest stable |

### For iOS Development (macOS only)

| Software | Version | Notes |
|----------|---------|-------|
| Xcode | 14.0+ | iOS development |
| CocoaPods | 1.12+ | Dependency management |
| iOS Simulator | 15.0+ | For testing |

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/hackelia-micrantha/eyespie.git
cd eyespie
```

### 2. Set Up Environment

```bash
# Copy environment template
cp env.example .env.local

# Edit with your credentials
# Use your preferred editor:
nano .env.local
# or
vim .env.local
```

### 3. Build and Run

**Android:**
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

**iOS (macOS only):**
```bash
# Install dependencies
cd iosApp
pod install

# Open in Xcode
open iosApp.xcworkspace
```

## Detailed Setup

### Step 1: Install JDK 17+

#### macOS (Homebrew)

```bash
# Install Homebrew if not present
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install JDK
brew install openjdk@17

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Ubuntu/Debian

```bash
# Update package list
sudo apt update

# Install JDK
sudo apt install openjdk-17-jdk

# Verify installation
java -version
```

#### Windows

1. Download from [Adoptium](https://adoptium.net/)
2. Run installer
3. Verify installation:
   ```cmd
   java -version
   ```

### Step 2: Install Android Studio

1. Download from [developer.android.com](https://developer.android.com/studio)
2. Run installer
3. Complete initial setup wizard
4. Install Kotlin Multiplatform plugin:
   - Settings → Plugins → Marketplace
   - Search "Kotlin Multiplatform"
   - Install and restart Android Studio

### Step 3: Configure Android SDK

1. Open Android Studio
2. Settings → Languages & Frameworks → Android SDK
3. Install:
   - Android 14 (API 34)
   - Android 13 (API 33)
   - Intel/ARM System Images
   - SDK Build-Tools 34.0.0

### Step 4: Set Up iOS Development (macOS only)

#### Install CocoaPods

```bash
sudo gem install cocoapods
```

#### Verify Xcode

```bash
# Check Xcode version
xcodebuild -version

# Accept license
sudo xcodebuild -license accept
```

### Step 5: Configure Environment Variables

Edit `.env.local`:

```env
# Required: Supabase credentials
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-anon-or-service-role-key

# Optional: AI model access
HUGGING_FACE_TOKEN=your-token

# Optional: Development defaults
LOGIN_EMAIL=eyespie@micrantha.test
LOGIN_PASSWORD=P@ssw0rd!

# Optional: Match configuration
MATCH_THRESHOLD=0.5
MATCH_COUNT=5
```

### Step 6: Build the Project

```bash
# Make Gradle wrapper executable (if needed)
chmod +x gradlew

# Build all modules
./gradlew build

# Or build just debug
./gradlew assembleDebug
```

## Configuration

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SUPABASE_URL` | Yes | Your Supabase project URL |
| `SUPABASE_KEY` | Yes | Supabase anon or service role key |
| `HUGGING_FACE_TOKEN` | No | For AI model downloads |
| `LOGIN_EMAIL` | No | Default dev email |
| `LOGIN_PASSWORD` | No | Default dev password |
| `MATCH_THRESHOLD` | No | Image match threshold (0-1) |
| `MATCH_COUNT` | No | Number of matches to return |

### Supabase Setup

1. Create account at [supabase.com](https://supabase.com)
2. Create new project
3. Get project URL and anon key from Settings → API
4. Run migrations from `supabase/migrations/`

### Getting Supabase Credentials

1. Go to [Supabase Dashboard](https://app.supabase.com)
2. Select your project
3. Go to Settings → API
4. Copy:
   - Project URL
   - anon public key

### Getting Hugging Face Token

1. Create account at [huggingface.co](https://huggingface.co)
2. Go to Settings → Access Tokens
3. Create new token
4. Copy token

## First Run

### Android

1. Connect Android device or start emulator
2. Run from Android Studio or:
   ```bash
   ./gradlew installDebug
   ```
3. Launch "EyesPie" app
4. Follow onboarding

### iOS

1. Open `iosApp/iosApp.xcworkspace` in Xcode
2. Select simulator or device
3. Click Run (▶️)
4. Follow onboarding

## Troubleshooting

### Build Issues

#### "Could not resolve dependencies"

```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches

# Rebuild
./gradlew clean build
```

#### "SDK not found"

1. Open Android Studio
2. Settings → SDK Manager
3. Install required SDK versions
4. Sync project

#### "CocoaPods not installed"

```bash
sudo gem install cocoapods
cd iosApp
pod install
```

### Runtime Issues

#### "Supabase connection failed"

1. Check `.env.local` credentials
2. Verify network connection
3. Check Supabase project status
4. Ensure API is enabled

#### "Camera permission denied"

1. Check device settings
2. Grant camera permission
3. Restart app

#### "ML model not found"

1. Complete onboarding flow
2. Wait for model download
3. Check network connection

### IDE Issues

#### Android Studio not recognizing Kotlin

1. File → Sync Project with Gradle Files
2. File → Invalidate Caches → Invalidate and Restart

#### Xcode not building

1. Clean build folder: Product → Clean Build Folder
2. Delete derived data: Product → Delete Derived Data
3. Reinstall pods: `cd iosApp && pod install`

### Performance Issues

#### Slow builds

```bash
# Enable Gradle daemon
echo "org.gradle.daemon=true" >> gradle.properties

# Increase memory
echo "org.gradle.jvmargs=-Xmx4096m" >> gradle.properties
```

#### App crashes on startup

1. Check logcat (Android) or Console (iOS)
2. Verify all permissions granted
3. Clear app data and retry

## Next Steps

### Learn the Codebase

1. Read [ARCHITECTURE.md](ARCHITECTURE.md)
2. Explore the code structure
3. Review existing features

### Start Contributing

1. Read [CONTRIBUTING.md](CONTRIBUTING.md)
2. Find a good first issue
3. Submit your first PR

### Get Help

1. Check [DEVELOPMENT.md](DEVELOPMENT.md)
2. Search existing issues
3. Create a discussion

## Resources

### Documentation

- [README](README.md) — Project overview
- [ARCHITECTURE](ARCHITECTURE.md) — Technical architecture
- [DEVELOPMENT](DEVELOPMENT.md) — Development guide
- [CONTRIBUTING](CONTRIBUTING.md) — How to contribute

### External Links

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Supabase Docs](https://supabase.com/docs)
- [Android Studio](https://developer.android.com/studio)
- [Xcode](https://developer.apple.com/xcode/)

### Community

- [GitHub Discussions](https://github.com/hackelia-micrantha/eyespie/discussions)
- [GitHub Issues](https://github.com/hackelia-micrantha/eyespie/issues)

---

*Last updated: December 2024*
# Repository Guidelines

## Project Structure & Module Organization
- `eyespie/`: Kotlin Multiplatform app module (Compose UI, shared logic, platform glue).
  - Shared code: `eyespie/src/commonMain`
  - Android-specific: `eyespie/src/androidMain`
  - iOS-specific: `eyespie/src/iosMain`
  - Tests: `eyespie/src/commonTest`
  - Assets: `eyespie/bluebellAssets`
- `bluebell/`: Shared framework code used by the app.
- `iosApp/`: iOS wrapper app and CocoaPods integration.
- `supabase/`: Database schema and migrations.
- `fastlane/`: Build, test, and distribution automation.
- `buildSrc/`: Gradle convention plugins and build tooling.

## Build, Test, and Development Commands
Use Gradle for local builds and Fastlane for CI-style workflows:

```bash
# Build debug (all platforms)
./gradlew build

# Android builds
./gradlew assembleDebug
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew testDebugUnitTest

# Fastlane pipelines
fastlane build_debug
fastlane build_release
fastlane test
```

## Coding Style & Naming Conventions
- Language: Kotlin (Multiplatform + Compose). Use 4-space indentation and keep Kotlin DSL style in `*.gradle.kts`.
- Naming: `PascalCase` for types, `camelCase` for functions/vars, `UPPER_SNAKE_CASE` for constants.
- No repo-wide formatter detected; match the local style in the file you edit.

## Testing Guidelines
- Framework: Kotlin `kotlin("test")` in `eyespie/src/commonTest`.
- Naming: use `*Test.kt` (e.g., `LabelDataRepositoryTest.kt`).
- Run tests with `./gradlew test` or `./gradlew testDebugUnitTest` for Android unit tests.

## Commit & Pull Request Guidelines
- Commit messages follow a Conventional Commits style: `type(scope): summary` or `type: summary` (e.g., `feat(scan): modularize scan feature`).
- PRs should include a concise summary, testing notes (commands + results), and screenshots for UI changes.

## Configuration & Secrets
- Copy `env.example` to `.env.local` and set required keys (e.g., Supabase credentials). Keep secrets out of version control.

## Common Anti-patterns to Avoid
- **Nested Scrolling**: Never nest a `LazyColumn` inside a scrollable `Column`. Use `Modifier.weight(1f)` on the container or a single `LazyColumn` with multiple `item {}` blocks.
- **Manual Navigation**: Prefer `context.navigate<Screen>()` (with type safety and DI) over `context.router.navigate(Screen())`.

## Navigation Strategy
- Side-effects in `Environment` classes should use `ScreenContext.navigate<T>()` to resolve screens via DI.
- Use `Router.Options.Replace` for non-linear flows (like moving from capture to edit).

## Offline Support Strategy
- Use `okio` for persistent file-based caching in `commonMain`.
- Data repositories should follow the "Network first, Cache fallback" pattern for remote areas.

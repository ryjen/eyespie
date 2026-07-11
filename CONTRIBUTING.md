# Contributing to EyesPie

Thank you for your interest in contributing to EyesPie! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Style Guidelines](#code-style-guidelines)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)
- [Architecture Decisions](#architecture-decisions)
- [Community](#community)

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive experience for everyone. We expect all participants to:

- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior

- Trolling, insulting/derogatory comments, and personal attacks
- Public or private harassment
- Publishing others' private information without explicit permission
- Other conduct which could reasonably be considered inappropriate in a professional setting

## Getting Started

### Prerequisites

Before contributing, ensure you have:

1. **JDK 17+** installed
2. **Android Studio** with Kotlin Multiplatform plugin
3. **Xcode 14+** (for iOS development)
4. **Git** configured with your name and email
5. **Supabase account** for backend testing

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/eyespie.git
   cd eyespie
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/hackelia-micrantha/eyespie.git
   ```

### Environment Setup

1. Copy environment template:
   ```bash
   cp env.example .env.local
   ```

2. Configure your credentials in `.env.local`

3. Install dependencies:
   ```bash
   ./gradlew build
   ```

4. For iOS development:
   ```bash
   cd iosApp
   pod install
   ```

## Development Workflow

### Branch Strategy

- **main**: Production-ready code
- **develop**: Integration branch for features
- **feature/***: New features
- **bugfix/***: Bug fixes
- **hotfix/***: Critical production fixes

### Creating a Branch

```bash
# Update main
git checkout main
git pull upstream main

# Create feature branch
git checkout -b feature/your-feature-name

# Or bugfix branch
git checkout -b bugfix/issue-number-description
```

### Commit Messages

Follow Conventional Commits format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, missing semi-colons, etc.)
- `refactor`: Code refactoring without functionality changes
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Build process or auxiliary tool changes

**Examples:**
```bash
git commit -m "feat(scan): add image embedding generation"
git commit -m "fix(game): resolve distance calculation edge case"
git commit -m "docs(readme): update installation instructions"
```

### Keeping Your Fork Updated

```bash
git fetch upstream
git checkout develop
git merge upstream/develop
git push origin develop
```

## Code Style Guidelines

### Kotlin Style

- **Indentation**: 4 spaces (no tabs)
- **Line Length**: 120 characters maximum
- **Naming Conventions**:
  - Classes/Types: `PascalCase`
  - Functions/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### File Organization

```kotlin
// Package declaration
package com.micrantha.eyespie.feature.example

// Imports (grouped and sorted)
import com.micrantha.bluebell.ui.screen.Screen
import com.micrantha.eyespie.domain.entities.Game
import org.kodein.di.DI
import org.kodein.di.instance

// Class/function declaration
class ExampleScreen : Screen {
    // Properties
    
    // Companion objects
    
    // Init blocks
    
    // Public methods
    
    // Private methods
}
```

### Compose Guidelines

```kotlin
// Screen composable
@Composable
fun ExampleScreen(
    state: ExampleUiState,
    onAction: (ExampleAction) -> Unit
) {
    // Use descriptive parameter names
    // Keep composables focused and small
    // Use derivedStateOf for computed values
}

// Component composable
@Composable
fun ExampleComponent(
    modifier: Modifier = Modifier,
    data: ExampleData
) {
    // Always include modifier parameter
    // Use semantic naming
}
```

### Architecture Patterns

- **Screen**: UI entry point with Voyager navigation
- **ScreenModel**: State management (replaces ViewModel)
- **Contract**: Defines UI state, actions, and effects
- **Environment**: Side effects and navigation logic
- **UseCase**: Business logic encapsulation
- **Repository**: Data layer abstraction

Example structure:
```kotlin
// Contract
data class ExampleContract(
    val state: ExampleUiState,
    val actions: List<ExampleAction>,
    val effects: Flow<ExampleEffect>
)

// ScreenModel
class ExampleScreenModel : ScreenModel {
    // State management
}

// Environment
class ExampleEnvironment(
    private val navigate: ScreenContext
) {
    // Side effects
}
```

## Testing Requirements

### Test Types

1. **Unit Tests**: Business logic and use cases
2. **Integration Tests**: Repository and data layer
3. **UI Tests**: Compose components and screens

### Writing Tests

```kotlin
// Unit test example
class ExampleUseCaseTest {
    private val useCase = ExampleUseCase()
    
    @Test
    fun `should return expected result when given valid input`() {
        // Given (Arrange)
        val input = "test input"
        
        // When (Act)
        val result = useCase.execute(input)
        
        // Then (Assert)
        assertEquals("expected output", result)
    }
    
    @Test
    fun `should throw exception when given invalid input`() {
        // Given
        val invalidInput = ""
        
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            useCase.execute(invalidInput)
        }
    }
}
```

### Test Coverage

- Maintain minimum 80% code coverage for new code
- Test edge cases and error conditions
- Use descriptive test names that explain the scenario

### Running Tests

```bash
# All tests
./gradlew test

# Android unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "com.micrantha.eyespie.features.example.ExampleUseCaseTest"
```

## Pull Request Process

### Before Submitting

1. **Update your branch**:
   ```bash
   git fetch upstream
   git rebase upstream/develop
   ```

2. **Run tests**:
   ```bash
   ./gradlew test
   ```

3. **Check code style**:
   ```bash
   ./gradlew ktlintCheck
   ```

4. **Build the project**:
   ```bash
   ./gradlew build
   ```

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] Tests added and passing
- [ ] No breaking changes (or documented)
```

### PR Guidelines

1. **Keep PRs focused**: One feature or fix per PR
2. **Write descriptive titles**: Clear, concise description of changes
3. **Include context**: Explain why changes were made
4. **Add screenshots**: For UI changes
5. **Link issues**: Reference related issues
6. **Respond to feedback**: Address review comments promptly

### Review Process

1. **Automated checks**: CI must pass
2. **Code review**: At least one approval required
3. **Testing**: Verify on both platforms if applicable
4. **Merge**: Squash and merge to develop

## Issue Guidelines

### Bug Reports

Use the bug report template:

```markdown
**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

**Expected behavior**
What you expected to happen.

**Screenshots**
If applicable, add screenshots.

**Environment:**
- Device: [e.g., Pixel 7, iPhone 14]
- OS: [e.g., Android 14, iOS 17]
- App Version: [e.g., 1.0.0]
```

### Feature Requests

```markdown
**Is your feature request related to a problem?**
A clear description of the problem.

**Describe the solution you'd like**
What you want to happen.

**Describe alternatives you've considered**
Other solutions you've thought about.

**Additional context**
Any other context or screenshots.
```

### Good First Issues

Look for issues labeled:
- `good first issue`
- `help wanted`
- `documentation`

## Architecture Decisions

### When to Add New Dependencies

1. Check if existing dependencies can solve the problem
2. Consider maintenance and community support
3. Evaluate size and performance impact
4. Discuss in issue before implementing

### When to Refactor

- Code duplication
- Complex logic that's hard to test
- Performance bottlenecks
- Poor separation of concerns

### Design Patterns

Follow existing patterns in the codebase:
- **Flux Architecture**: For state management
- **Repository Pattern**: For data access
- **Use Case Pattern**: For business logic
- **Dependency Injection**: For loose coupling

## Community

### Communication Channels

- **GitHub Discussions**: For questions and ideas
- **GitHub Issues**: For bugs and feature requests
- **Pull Requests**: For contributions

### Getting Help

1. Check existing documentation
2. Search existing issues and discussions
3. Create a new discussion for questions
4. Open an issue for bugs

### Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- Git commit history

## License

By contributing to EyesPie, you agree that your contributions will be licensed under the GPLv3 License.

## Questions?

If you have questions about contributing, please open a discussion or reach out to the maintainers.

---

Thank you for contributing to EyesPie! 🎉
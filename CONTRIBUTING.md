# Contributing to MeshVerse 🤝

Thank you for your interest in contributing to MeshVerse! We're building a revolutionary offline-first decentralized super app, and we'd love your help.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)
- [Community](#community)

## 📜 Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inspiring community for all. We pledge to:

- Be respectful and inclusive
- Welcome people of all backgrounds and experience levels
- Focus on what is best for the community
- Show empathy towards other community members
- Give credit where it's due

### Expected Behavior

- Use welcoming and inclusive language
- Be respectful of differing opinions and experiences
- Accept constructive criticism gracefully
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior

- Harassment, intimidation, or discrimination
- Insulting or derogatory comments
- Trolling or deliberate disruption
- Personal attacks
- Unwelcome sexual attention or advances
- Posting private information without consent

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Android Studio**: Hedgehog (2023.1.1) or later
- **Java**: JDK 17 or higher
- **Kotlin**: 1.9.0 or higher
- **Android SDK**: API level 33 (Android 13) or higher
- **Gradle**: 8.0 or higher (included with Android Studio)
- **Git**: Latest version
- **Python**: 3.8+ (for mesh simulator scripts)

### Fork and Clone

1. Fork the repository by clicking the "Fork" button on GitHub
2. Clone your fork to your local machine:

```bash
git clone https://github.com/YOUR_USERNAME/offline-social-media-.git
cd offline-social-media-
```

3. Add upstream remote to sync with main repository:

```bash
git remote add upstream https://github.com/2303630100089-lang/offline-social-media-.git
git fetch upstream
git branch --set-upstream-to=upstream/main main
```

## 🛠️ Development Setup

### Step 1: Install Dependencies

```bash
chmod +x build-scripts/setup-dev-env.sh
./build-scripts/setup-dev-env.sh
```

This script will:
- Verify Android Studio installation
- Check Java version
- Download required Android SDKs
- Install Kotlin compiler
- Setup Gradle
- Clone submodules

### Step 2: Open in Android Studio

1. Open Android Studio
2. Select "Open" and navigate to the project directory
3. Wait for Gradle sync to complete
4. If you see Gradle errors, go to `File > Invalidate Caches` and restart

### Step 3: Build the Project

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Step 4: Create Emulator

```bash
# Create AVD for testing
emulator -avd Pixel_6_API_33 -feature-emulated-performance -qemu

# Install APK
adb install -r app/build/outputs/apk/debug/meshverse-debug.apk

# Launch app
adb shell am start -n com.meshverse.app/.MainActivity
```

## 📝 How to Contribute

### Pick an Issue

1. Go to [GitHub Issues](https://github.com/2303630100089-lang/offline-social-media-/issues)
2. Look for issues labeled:
   - `good first issue` - Perfect for newcomers
   - `help wanted` - Contributors needed
   - `feature request` - New functionality
   - `bug` - Bug fixes

3. Comment on the issue: "I'd like to work on this"
4. Wait for maintainer approval

### Work on Features

Contribute to these major areas:

#### 🔐 Messaging & Encryption
- End-to-end encrypted messaging
- Signal Protocol implementation
- Message synchronization
- Walkie-talkie features

#### 🌍 Networking
- Bluetooth/BLE mesh networking
- Wi-Fi Direct communication
- Mesh routing algorithms
- Relay optimization

#### 📱 Social Features
- Decentralized feeds
- Comments and reactions
- Community channels
- Polling systems

#### 🗺️ Maps & Location
- Offline OpenStreetMap integration
- GPS synchronization
- Location sharing
- Emergency overlays

#### 🤖 AI & Intelligence
- Offline LLM integration
- AI assistant
- OCR and vision features
- Voice commands

#### 🎮 Mini Apps
- Mini app SDK
- Developer tools
- Marketplace system
- Plugin framework

#### 📹 Media & Sharing
- File sharing systems
- Media propagation
- Compression algorithms
- Resumable transfers

#### 🎮 Gaming
- Multiplayer games
- Leaderboards
- Tournament systems

## 🔀 Pull Request Process

### Before Creating a PR

1. Create a feature branch:

```bash
git checkout -b feature/your-feature-name
```

2. Make your changes and commit:

```bash
git add .
git commit -m "feat: add amazing feature"
```

3. Push to your fork:

```bash
git push origin feature/your-feature-name
```

### Creating a Pull Request

1. Go to GitHub and click "New Pull Request"
2. Select your branch and fill out the PR template:

```markdown
## Description
Briefly describe what this PR does.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issues
Closes #123

## How to Test
Steps to verify the changes:
1. ...
2. ...

## Checklist
- [ ] Code follows style guidelines
- [ ] I have performed a self-review
- [ ] I have commented complex areas
- [ ] I have updated documentation
- [ ] I have added tests
- [ ] Tests pass locally
- [ ] No new warnings generated
```

### PR Review Process

1. Maintainers will review your code
2. Make requested changes in new commits
3. Keep PR focused on one feature/fix
4. Ensure all CI checks pass
5. Once approved, your PR will be merged

## 📐 Coding Standards

### Kotlin Style Guide

We follow the [Kotlin Official Style Guide](https://kotlinlang.org/docs/coding-conventions.html).

#### Naming Conventions

```kotlin
// Classes: PascalCase
class MeshNetworkManager { }

// Functions: camelCase
fun startMeshDiscovery() { }

// Constants: UPPER_SNAKE_CASE
const val MAX_RETRY_COUNT = 5

// Variables: camelCase
var deviceId = ""

// Private properties: _camelCase or camelCase with private modifier
private val encryptionKey = ""
```

#### Code Organization

```kotlin
class MyClass {
    // Companion object
    companion object {
        const val TAG = "MyClass"
    }
    
    // Properties
    private val property1: String = ""
    
    // Lifecycle
    init { }
    
    // Public methods
    fun publicMethod() { }
    
    // Private methods
    private fun privateMethod() { }
}
```

#### Comments

```kotlin
// Document complex routing logic
/**
 * Performs multi-hop mesh routing to find optimal path.
 * Uses Dijkstra's algorithm with peer reputation weights.
 * 
 * @param targetDeviceId The destination device
 * @param hopLimit Maximum number of relay hops
 * @return Optimal route path or null if unreachable
 */
private fun findOptimalRoute(targetDeviceId: String, hopLimit: Int): List<String>? {
    // Implementation...
}
```

### Architecture

Follow MVVM + Clean Architecture:

```
feature/
├── data/
│   ├── local/          # Room Database
│   ├── remote/         # Network/Mesh APIs
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Domain models
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Business logic
├── presentation/
│   ├── screen/         # Compose screens
│   ├── component/      # Reusable components
│   └── viewmodel/      # ViewModels
└── di/                 # Hilt modules
```

### Hilt Dependency Injection

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val encryptionService: EncryptionService
) : ViewModel() {
    // Implementation...
}
```

## 🧪 Testing Guidelines

### Unit Tests

Create tests in `src/test/kotlin/`:

```kotlin
class MeshRouterTest {
    
    private lateinit var meshRouter: MeshRouter
    
    @Before
    fun setup() {
        meshRouter = MeshRouter()
    }
    
    @Test
    fun `should find optimal route between three nodes`() {
        // Arrange
        val path = meshRouter.findRoute("A", "C")
        
        // Act & Assert
        assertEquals(listOf("A", "B", "C"), path)
    }
}
```

### Instrumented Tests

Create tests in `src/androidTest/kotlin/`:

```kotlin
@RunWith(AndroidJUnit4::class)
class MessagingIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun sendMessageSuccessfully() {
        composeTestRule.setContent {
            // Test UI...
        }
    }
}
```

### Run Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# With coverage
./gradlew testDebugUnitTestCoverage
```

## 📚 Documentation

Always document your code:

1. **KDoc for public APIs**:

```kotlin
/**
 * Encrypts a message using Signal Protocol.
 *
 * @param plaintext The message to encrypt
 * @param recipientId The recipient's unique identifier
 * @return Encrypted message bytes
 * @throws EncryptionException if encryption fails
 */
fun encryptMessage(plaintext: String, recipientId: String): ByteArray { }
```

2. **Update README.md** if adding major features
3. **Update ARCHITECTURE.md** if changing structure
4. **Add inline comments** for complex algorithms

## 💬 Commit Message Guidelines

Use conventional commits format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only
- `style`: Code style changes (formatting, missing semicolons, etc)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Build, CI, dependencies

### Examples

```
feat(messaging): implement Signal Protocol encryption

Implement full Signal Protocol support with:
- Curve25519 key exchange
- AES-256-GCM encryption
- Forward secrecy
- Session management

Closes #123

feat(mesh): add dynamic routing algorithm
fix(bluetooth): handle disconnection gracefully
docs: update mesh networking architecture
test: add encryption unit tests
perf(sync): optimize database synchronization
```

## 🐛 Reporting Bugs

Found a bug? Please report it!

1. Go to [Issues](https://github.com/2303630100089-lang/offline-social-media-/issues)
2. Click "New Issue" and select "Bug Report"
3. Provide:
   - Clear description of the bug
   - Steps to reproduce
   - Expected behavior
   - Actual behavior
   - Device info (Android version, device model)
   - Logs/screenshots

### Bug Report Template

```markdown
## Description
A clear description of what the bug is.

## To Reproduce
1. Go to...
2. Click on...
3. See error...

## Expected Behavior
What should happen

## Actual Behavior
What actually happens

## Environment
- Android Version: 13
- Device: Pixel 6
- App Version: 0.1.0

## Logs
```
adb logcat output here
```

## Screenshots
[If applicable]
```

## 💡 Feature Requests

Want a new feature?

1. Go to [Issues](https://github.com/2303630100089-lang/offline-social-media-/issues)
2. Click "New Issue" and select "Feature Request"
3. Describe:
   - The feature you want
   - Why you need it
   - How it should work
   - Any related features or examples

### Feature Request Template

```markdown
## Description
Clear description of the feature.

## Motivation
Why would this feature be useful?

## Proposed Solution
How should it work?

## Alternative Solutions
Other ways to solve this?

## Additional Context
Any other relevant information?
```

## 👥 Community

### Stay Connected

- **GitHub Issues**: Ask questions and discuss features
- **GitHub Discussions**: Community chat and ideas
- **Pull Requests**: Share your code and collaborate
- **Wiki**: Community knowledge base

### Recognition

Contributors will be:
- Listed in [CONTRIBUTORS.md](CONTRIBUTORS.md)
- Thanked in release notes
- Recognized in README.md

## 📚 Additional Resources

### Documentation
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - System design
- [MESH_NETWORKING.md](docs/MESH_NETWORKING.md) - Networking details
- [API.md](docs/API.md) - API reference
- [ENCRYPTION.md](docs/ENCRYPTION.md) - Security details

### Learning Resources
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-overview.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [Mesh Networking Concepts](https://en.wikipedia.org/wiki/Mesh_networking)
- [Signal Protocol](https://signal.org/docs/)

### Tools
- [Android Studio](https://developer.android.com/studio)
- [Android Profiler](https://developer.android.com/studio/profile)
- [Network Monitor](https://developer.android.com/tools/logcat)

## ❓ Questions?

Don't hesitate to ask:

1. Open a [Discussion](https://github.com/2303630100089-lang/offline-social-media-/discussions)
2. Join our community chat
3. Comment on related issues

## 🙏 Thank You!

Thank you for contributing to MeshVerse! Your effort helps build a better, more open, and decentralized internet for everyone.

Happy coding! 🚀

---

**Last Updated**: 2026-05-11
**Version**: 1.0

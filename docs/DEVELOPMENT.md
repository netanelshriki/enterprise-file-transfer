# SecureTransfer Development Guide

This guide covers setting up the development environment and contributing to SecureTransfer.

## 🛠️ Development Environment Setup

### Prerequisites

#### Required Software
- **Java 21 LTS** (OpenJDK or Oracle)
- **Maven 3.8+** (for desktop development)
- **Android SDK** with Build Tools 34+ (for Android development)
- **Git** for version control
- **IDE**: IntelliJ IDEA, Eclipse, or Android Studio

#### Optional Tools
- **Docker** (for server development)
- **Node.js** (for documentation site)
- **Postman** (for API testing)

### Installation Guide

#### Java 21 LTS
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# macOS (Homebrew)
brew install openjdk@21

# Windows (Chocolatey)
choco install openjdk21

# Verify installation
java -version
javac -version
```

#### Maven
```bash
# Ubuntu/Debian
sudo apt install maven

# macOS (Homebrew)
brew install maven

# Windows (Chocolatey)
choco install maven

# Verify installation
mvn -version
```

#### Android SDK
1. Download Android Studio from https://developer.android.com/studio
2. Install Android Studio and follow setup wizard
3. Install SDK Build Tools 34.0.0+
4. Set environment variables:
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

## 🏗️ Project Setup

### Clone Repository
```bash
git clone https://github.com/netanelshriki/file-transfer-app.git
cd file-transfer-app
```

### Build All Components
```bash
# Quick build (all platforms)
./scripts/build-all.sh        # Unix/Linux/Mac
scripts\build-all.bat         # Windows

# Individual components
./scripts/build-desktop.sh linux-deb
./scripts/build-android.sh
```

### IDE Configuration

#### IntelliJ IDEA
1. Open project root directory
2. Import as Maven project (for desktop module)
3. Configure Project SDK to Java 21
4. Install plugins:
   - JavaFX Scene Builder
   - Android Support (if developing Android)

#### Eclipse
1. Import → Existing Maven Projects
2. Select desktop directory
3. Configure Java Build Path to use Java 21
4. Install e(fx)clipse plugin for JavaFX

#### Android Studio
1. Open android directory as project
2. Sync Gradle files
3. Configure SDK path in local.properties

## 🧪 Development Workflow

### Running Applications

#### Desktop Application
```bash
cd desktop

# Development mode (hot reload)
mvn javafx:run

# With specific JVM args
mvn javafx:run -Djavafx.args="--enable-preview"

# Debug mode
mvn javafx:run -Djavafx.args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

#### Android Application
```bash
cd android

# Install debug APK
./gradlew installDebug

# Run on connected device
./gradlew installDebug
adb shell am start -n com.securetransfer.android/.MainActivity

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

#### Signaling Server
```bash
cd server

# Development mode (auto-restart)
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=development

# Debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Testing

#### Unit Tests
```bash
# Desktop tests
cd desktop && mvn test

# Android tests
cd android && ./gradlew test

# Server tests
cd server && mvn test

# All tests
./scripts/test-all.sh
```

#### Integration Tests
```bash
# Desktop integration tests
cd desktop && mvn verify -Pintegration-tests

# Android instrumentation tests
cd android && ./gradlew connectedAndroidTest

# Server integration tests
cd server && mvn verify -Pintegration-tests
```

#### Manual Testing
```bash
# Start local signaling server
cd server && mvn spring-boot:run

# Start desktop app in debug mode
cd desktop && mvn javafx:run -Djavafx.args="-Dserver.url=http://localhost:8080"

# Install Android debug APK
cd android && ./gradlew installDebug
```

## 🏛️ Architecture Overview

### Project Structure
```
file-transfer-app/
├── desktop/                    # JavaFX desktop application
│   ├── src/main/java/         # Java source code
│   │   └── com/securetransfer/
│   │       ├── ui/            # JavaFX controllers and views
│   │       ├── network/       # Network communication
│   │       ├── security/      # Encryption and security
│   │       └── storage/       # Local data storage
│   ├── src/main/resources/    # FXML files, CSS, images
│   └── src/test/java/         # Unit tests
├── android/                   # Android application
│   ├── src/main/java/         # Java/Kotlin source code
│   │   └── com/securetransfer/android/
│   │       ├── ui/            # Activities and fragments
│   │       ├── network/       # Network layer
│   │       ├── security/      # Security implementation
│   │       └── storage/       # Room database
│   ├── src/main/res/          # Android resources
│   └── src/test/java/         # Unit tests
├── server/                    # Spring Boot signaling server
│   ├── src/main/java/         # Java source code
│   │   └── com/securetransfer/server/
│   │       ├── websocket/     # WebSocket handlers
│   │       ├── api/           # REST API controllers
│   │       └── service/       # Business logic
│   └── src/test/java/         # Unit tests
└── shared/                    # Shared utilities (if needed)
```

### Key Components

#### Network Layer
- **Local Discovery**: mDNS for local network device discovery
- **Internet Discovery**: WebSocket signaling server
- **Data Transfer**: Direct TCP (local) or WebRTC (internet)
- **Security**: End-to-end AES-256 encryption

#### UI Layer
- **Desktop**: JavaFX with FXML and CSS styling
- **Android**: Material Design 3 with Jetpack Compose
- **Responsive**: Adaptive layouts for different screen sizes

#### Storage Layer
- **Desktop**: H2 embedded database
- **Android**: Room database with SQLite
- **Server**: In-memory storage (stateless)

## 🎨 UI Development

### Desktop (JavaFX)

#### FXML Structure
```xml
<!-- MainWindow.fxml -->
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<BorderPane xmlns="http://javafx.com/javafx/11.0.1" 
            xmlns:fx="http://javafx.com/fxml/1" 
            fx:controller="com.securetransfer.ui.MainController">
    <top>
        <ToolBar fx:id="toolbar">
            <Button fx:id="settingsButton" text="Settings" />
        </ToolBar>
    </top>
    <center>
        <GridPane fx:id="deviceGrid" />
    </center>
    <bottom>
        <VBox fx:id="transferQueue" />
    </bottom>
</BorderPane>
```

#### CSS Styling
```css
/* styles.css */
.root {
    -fx-font-family: "Segoe UI", "San Francisco", "Ubuntu", sans-serif;
    -fx-font-size: 14px;
}

.device-card {
    -fx-background-color: #ffffff;
    -fx-background-radius: 8px;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);
    -fx-padding: 16px;
}

.device-card:hover {
    -fx-background-color: #f5f5f5;
    -fx-cursor: hand;
}

/* Dark theme */
.root.dark {
    -fx-base: #2b2b2b;
    -fx-background: #383838;
    -fx-control-inner-background: #2b2b2b;
}
```

#### Controller Pattern
```java
@FXML
public class MainController implements Initializable {
    @FXML private GridPane deviceGrid;
    @FXML private VBox transferQueue;
    
    private DeviceDiscoveryService discoveryService;
    private TransferManager transferManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDeviceGrid();
        startDeviceDiscovery();
    }
    
    @FXML
    private void handleSettingsAction(ActionEvent event) {
        // Handle settings button click
    }
}
```

### Android (Material Design 3)

#### Activity Structure
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var viewModel: MainViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device ->
            viewModel.selectDevice(device)
        }
        binding.deviceRecyclerView.adapter = deviceAdapter
    }
}
```

#### Jetpack Compose UI
```kotlin
@Composable
fun DeviceCard(
    device: Device,
    onDeviceClick: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDeviceClick(device) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = device.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

## 🔧 Configuration

### Development Configuration

#### Desktop (application.properties)
```properties
# Development settings
app.debug=true
app.log.level=DEBUG
app.server.url=http://localhost:8080
app.discovery.local.enabled=true
app.discovery.internet.enabled=true
app.transfer.chunk.size=65536
app.ui.theme=system
```

#### Android (build.gradle debug)
```gradle
android {
    buildTypes {
        debug {
            debuggable true
            minifyEnabled false
            buildConfigField "String", "SERVER_URL", "\"http://10.0.2.2:8080\""
            buildConfigField "boolean", "DEBUG_MODE", "true"
        }
    }
}
```

#### Server (application-development.yml)
```yaml
server:
  port: 8080
  
spring:
  profiles:
    active: development
    
logging:
  level:
    com.securetransfer: DEBUG
    org.springframework.web.socket: DEBUG
    
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

### Environment Variables
```bash
# Development environment
export ST_DEBUG=true
export ST_SERVER_URL=http://localhost:8080
export ST_LOG_LEVEL=DEBUG
export ANDROID_HOME=/path/to/android/sdk
export JAVA_HOME=/path/to/java21
```

## 🐛 Debugging

### Desktop Application
```bash
# Debug with IDE
mvn javafx:run -Djavafx.args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

# Enable JavaFX debug logging
mvn javafx:run -Djavafx.args="-Dprism.verbose=true -Djavafx.verbose=true"

# Memory debugging
mvn javafx:run -Djavafx.args="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
```

### Android Application
```bash
# Debug with Android Studio
./gradlew installDebug
# Then attach debugger in Android Studio

# ADB debugging
adb logcat | grep SecureTransfer

# Network debugging
adb shell setprop log.tag.OkHttp DEBUG
```

### Server Application
```bash
# Debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

# Enable WebSocket debugging
mvn spring-boot:run -Dlogging.level.org.springframework.web.socket=DEBUG
```

## 📊 Performance Profiling

### JVM Profiling
```bash
# Enable JFR (Java Flight Recorder)
java -XX:+FlightRecorder 
     -XX:StartFlightRecording=duration=60s,filename=profile.jfr 
     -jar target/app.jar

# Analyze with JProfiler or VisualVM
jvisualvm --jdkhome $JAVA_HOME
```

### Android Profiling
```bash
# CPU profiling
./gradlew :android:assembleDebug
# Use Android Studio Profiler

# Memory profiling
adb shell dumpsys meminfo com.securetransfer.android
```

## 🔒 Security Testing

### Static Analysis
```bash
# OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check

# SpotBugs
mvn com.github.spotbugs:spotbugs-maven-plugin:check

# Android Lint
./gradlew lint
```

### Dynamic Testing
```bash
# Network security testing
nmap -sS -O target_ip

# SSL/TLS testing
sslscan your-domain.com
testssl.sh your-domain.com
```

## 📝 Code Style and Standards

### Java Code Style
- Use 4 spaces for indentation
- Line length: 120 characters
- Follow Oracle Java naming conventions
- Use meaningful variable names
- Add JavaDoc for public APIs

### Formatting Configuration
```xml
<!-- .editorconfig -->
root = true

[*.java]
indent_style = space
indent_size = 4
end_of_line = lf
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true
max_line_length = 120
```

### Git Hooks
```bash
# Pre-commit hook
#!/bin/sh
# Run tests before commit
./scripts/test-all.sh
if [ $? -ne 0 ]; then
    echo "Tests failed. Commit aborted."
    exit 1
fi
```

## 🚀 Release Process

### Version Management
```bash
# Update version in all pom.xml files
mvn versions:set -DnewVersion=1.1.0

# Update Android version
# Edit android/build.gradle:
# versionCode 2
# versionName "1.1.0"

# Commit version changes
git add -A
git commit -m "Bump version to 1.1.0"
git tag -a v1.1.0 -m "Release version 1.1.0"
```

### Build Release
```bash
# Build all platforms
./scripts/build-all.sh

# Sign Android APK (production)
cd android
./gradlew assembleRelease
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore release-key.keystore \
  build/outputs/apk/release/android-release-unsigned.apk \
  alias_name
```

### Automated Release (GitHub Actions)
```bash
# Push tag to trigger release
git push origin v1.1.0

# GitHub Actions will:
# 1. Run all tests
# 2. Build all platforms
# 3. Create GitHub release
# 4. Upload artifacts
```

This development guide provides comprehensive information for setting up and contributing to SecureTransfer. Follow these guidelines to ensure consistent, high-quality code across all platforms.

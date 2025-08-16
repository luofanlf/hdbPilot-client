# HDB Pilot Client

## Prerequisites

- **Android Studio** (latest version recommended)
- **JDK 11** or higher
- **Android SDK** with API level 29+ (Android 10.0)
- **Gradle 8.11.1**

## How to Run Locally

### 1. Clone the Repository
```bash
git clone <your-repository-url>
cd hdbPilot_client
```

### 2. Open in Android Studio
- Launch Android Studio
- Select "Open an existing project"
- Navigate to the `hdbPilot_client` folder and open it

### 3. Sync Project
- Wait for Android Studio to sync the project with Gradle
- If prompted, update Gradle wrapper to version 8.11.1
- Ensure all dependencies are downloaded

### 4. Configure Android Device/Emulator
- Connect a physical Android device via USB (API level 29+)
- Or create an Android Virtual Device (AVD) with API level 29+
- Enable USB debugging on your device if using physical device

### 5. Build and Run
- Click the "Run" button (green play icon) in Android Studio
- Select your target device
- Wait for the app to build and install
- The app will launch automatically on your device

### 6. Alternative: Command Line
```bash
# Build the project
./gradlew build

# Install and run on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Troubleshooting

- **Gradle sync issues**: Try "File > Invalidate Caches and Restart"
- **Build errors**: Ensure JDK 11 is set in Project Structure
- **Device not detected**: Check USB debugging and device drivers
- **Memory issues**: Increase Gradle memory in `gradle.properties`

## Project Structure

- `app/src/main/java/com/iss/` - Main application code
- `app/src/main/res/` - Resources (layouts, drawables, etc.)
- `app/build.gradle.kts` - App-level build configuration
- `build.gradle.kts` - Project-level build configuration 
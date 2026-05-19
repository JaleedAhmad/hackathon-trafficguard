# TrafficGuard Android Client

The Android client is the mobile interface for **TrafficGuard**, a real-time, crowd-sourced traffic crisis mitigation and intelligence system. It provides users with live maps, step-by-step incident reporting wizard with AI preprocessing feedback, automated rerouting alerts, a low-distraction Driving Mode, and emergency SOS dispatch.

## Tech Stack & Architecture

- **UI Framework**: Modern Jetpack Compose (Declarative UI) with Material 3 styling.
- **Architecture Pattern**: MVVM (Model-View-ViewModel) + clean Repository/Data layer pattern.
- **State Management**: Reactive flows with Kotlin `StateFlow` and Compose state lifting.
- **Navigation**: Jetpack Navigation 3 for declarative navigation and backstack state control.
- **Build System**: Gradle Kotlin DSL (`.gradle.kts`).

---

## Folder Structure

```
android/app/src/main/java/com/trafficguard/app/
├── MainActivity.kt               # Application entry point & window orchestration
├── Navigation.kt                 # Declarative Navigation 3 graph and shared ViewModels
├── NavigationKeys.kt             # Navigation keys and screen route entities
├── auth/                         # Authenticaton/anonymous token repositories
├── data/                         # Repositories (Location, Report, Emergency, Profile, etc.)
├── theme/                        # HSL curated themes, colors, and typography tokens
└── ui/                           # UI Screens and view models
    ├── auth/                     # Sign-in, Sign-up, Forgot Password UI flow
    ├── community/                # Alerts feed, Incident discussions, Contributor profile
    ├── components/               # Custom reusable Compose UI widgets (TopBars, buttons)
    ├── drivingmode/              # Driving HUD and Voice guidance overlays
    ├── emergency/                # SOS dashboard, multi-tap trigger, and emergency routing maps
    ├── home/                     # Interactive dashboard, central map, and reporting launcher
    ├── mapnavigation/            # Rerouting suggestions, ETA overlays, and simulation layers
    ├── onboarding/               # Onboarding walkthrough wizard
    ├── permissions/              # Location & notification runtime permission check screen
    ├── profile/                  # User profile and settings dashboard hubs
    ├── report/                   # Multi-agent simulation processing & reporting wizards
    ├── showcase/                 # Telemetry UI showcase
    └── splash/                   # App bootloader and initialization sequence
```

---

## Core Features

1. **Active Map Launcher (`ui/home`)**
   * Displays an interactive map of the local area. Resolves live coordinates and shares session state globally with emergency and routing layers.

2. **AI Preprocessing Wizard (`ui/report`)**
   * Multi-step reporting flow that guides the user to attach location details and descriptions.
   * Feeds inputs directly through backend agents (Ingestion, Trust, Situation Planning, Execution) with real-time progress phases, comprehensive validation, and a self-recovery error state interface.

3. **Low-Distraction Driving Mode (`ui/drivingmode`)**
   * Large typography HUD displaying alternate routing alerts, safety metrics, and automated voice guidance status flags.

4. **Crisis SOS Dispatch (`ui/emergency`)**
   * Triple-tap safety mechanism to prevent false triggers. 
   * Geographically resolves the user's nearest safe zones (police stations, shelter points, medical units) within a localized radius (~600m to 900m) and broadcasts coordinates to nearby subscribers.

---

## Getting Started

### Prerequisites
* **Java SDK**: Version 17 or higher.
* **Android SDK**: API level 26 minimum, API level 34 target.
* **Backend**: Make sure the Python FastAPI backend is running locally or configured in `.env`.

### Environment Configuration
The app retrieves the base server endpoint dynamically. Verify that the correct URL is mapped inside `android/local.properties` or `gradle.properties`:
```properties
VITE_BACKEND_URL=http://<YOUR_LOCAL_IP>:8000
```

### Build & Run Commands

Follow these steps from the `android/` directory to forward ports, compile, and run the app on your connected device or emulator:

1. **Forward Backend API Port**:
   Enable network communication with your local FastAPI backend server (running on port `8000`):
   ```bash
   adb reverse tcp:8000 tcp:8000
   ```

2. **Build Debug APK**:
   Generate the debug APK package:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install and Launch the App**:
   Deploy the compiled APK package directly to your connected device/emulator and launch the main activity:
   ```bash
   android run --apks=app/build/outputs/apk/debug/app-debug.apk
   ```

---

### Code Compilation Check
To check for syntax correctness and Kotlin compiler errors without assembling the APK package:
```bash
./gradlew compileDebugKotlin
```

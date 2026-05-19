# TrafficGuard AI 🛡️
### Community-Powered Urban Crisis Intelligence & Smart Mobility Response

TrafficGuard AI is an advanced, AI-driven urban crisis intelligence platform designed for Pakistani cities to detect, analyze, simulate, and mitigate severe urban mobility disruptions like flash flooding, infrastructure failures, and major traffic blockages. Built for the Google Antigravity Hackathon under **Challenge 3: CIRO**.

---

## 🤖 System Architecture & 4-Agent Pipeline

The core intelligence layer of TrafficGuard AI relies on a highly coordinated 4-agent orchestration engine powered by Gemini 2.5 models embedded into the core asynchronous REST endpoint pipeline (`POST /report`).

### 🧠 Deep-Dive Agent Workflow & Capabilities
1. **Agent 1: Ingestion Agent (Gemini 2.5 Flash)**
   * **Purpose:** Acts as the entry-point gatekeeper for raw community telemetry. 
   * **Capabilities:** Ingests unstructured community reports, performs automatic language detection, and leverages Gemini 2.5 Flash to seamlessly translate Urdu or Roman Urdu signals into clear English standard hazard categories without breaking semantic context.

2. **Agent 2: Trust & Detection Agent (Gemini 2.5 Pro)**
   * **Purpose:** Implements validation mechanics to filter out false data and adversarial noise.
   * **Capabilities:** Cross-references incoming user signals against simulated external weather and maps telemetry. It applies spatial clustering to build dynamic trust scores. If an anomaly or contradiction is discovered, it routes details to an internal contradictions array, officially determining if a crisis state is `ACTIVE` or rejected.

3. **Agent 3: Situation Planning Agent (Gemini 2.5 Pro)**
   * **Purpose:** Processes situational context and executes impact mapping.
   * **Capabilities:** Evaluates localized infrastructure impact by calculating the active incident radius and real-time travel delay metrics (ETA lag). Leverages LLM reasoning to output exactly three human-readable, prioritized operational tasks (e.g., *Immediate Rerouting*, *Emergency Dispatch*, *Local Push Notifications*).

4. **Agent 4: Execution Agent (Gemini 2.5 Pro)**
   * **Purpose:** Drives the predictive simulation framework and matches live state targets.
   * **Capabilities:** Models downstream traffic patterns post-mitigation, auto-translates outbound push alerts back into localized Urdu string arrays for nearby field devices, automatically flags emergency response tickets, executes transactional state updates to Firebase, and bundles the complete execution process into an auditable `AgentTrace` object.

---

## 🛠️ Complete Technology Stack

| Layer | Technology | Operational Purpose / Implementation Details |
| :--- | :--- | :--- |
| **Mobile Client** | Native Android (Kotlin) | Implements an modern UI framework using declarative Jetpack Compose with Material 3 styling tokens. |
| **Architecture** | MVVM Pattern | Isolates business logic from UI elements using Kotlin `StateFlow` streams paired with a clean repository data layer. |
| **UI Design Pipeline** | Google Stitch → Antigravity | Seamless design-to-component pipeline converting high-fidelity visual layouts directly into design system tokens. |
| **Backend Core** | FastAPI (Python 3.12-slim) | High-performance asynchronous REST hub hosting the concurrent multi-agent processing routines. |
| **AI Models** | Gemini 2.5 Pro & Flash | Structured dual-model architecture balancing rapid language normalization with deep analytical reasoning. |
| **Realtime Sync** | Firebase Realtime DB | Instant telemetry streaming engine designed to push immediate live marker state switches to client map layers. |
| **Persistent Storage**| Google Cloud Firestore | Handled natively via the `google-cloud-firestore` engine explicitly targeting the `hackathon-trafficguard` workspace. |
| **Mapping Canvas** | Google Maps SDK for Android | Custom dark-themed layout tracking incident pins, localized radius circles, and optimized routing polylines. |
| **Container Engine** | Docker / Google Cloud Run | Fully containerized environment exposing high-throughput production ports on fallback port `8080`. |
| **Secure Tunneling** | Live Ngrok Integration | Embedded background `pyngrok` loop forwarding stable, public HTTPS gateway links to mobile front-end environments. |

---

## 📁 Native Android Project Structure (Jetpack Compose)

The mobile client workspace follows strict clean architecture separation rules inside the target package tree:

```text
android/app/src/main/java/com/trafficguard/app/
├── MainActivity.kt               # Application entry point & window window orchestration
├── Navigation.kt                 # Declarative Navigation 3 graph and shared ViewModels
├── NavigationKeys.kt             # Navigation route keys and screen argument models
├── auth/                         # Anonymous authentication session management repositories
├── data/                         # Core Repositories (Location, Report, Emergency, Profile)
├── theme/                        # Design system curation (HSL color spaces, vector typography)
└── ui/                           # UI Screens and isolated screen state models
    ├── auth/                     # Anonymous sign-in and localized initialization screens
    ├── community/                # Context-aware local incident discussions and regional feeds
    ├── components/               # Reusable atomic widgets (TopBars, custom error banners, buttons)
    ├── drivingmode/              # High-visibility HUD with massive typography and voice instruction flags
    ├── emergency/                # SOS dashboard, multi-tap safety triggers, and safe-zone radar charts
    ├── home/                     # Central map interface tracking active crises and report triggers
    ├── mapnavigation/            # Rerouting suggestion matrix sheets and before/after layout views
    ├── onboarding/               # Onboarding walkthrough wizard
    ├── permissions/              # Runtime permission control gates for precise location access
    ├── profile/                  # Contributor score metrics and historical activity logs
    ├── report/                   # Progress-tracked wizard with step-by-step multi-agent feedback indicators
    ├── showcase/                 # Live developer/judge telemetry trace inspect screens
    └── splash/                   # Verification sequence, bootloader, and configuration checker

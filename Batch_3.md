# Batch 3: Google Maps Integration, Real-Time Navigation & Alerts Dashboard

## # Batch Overview

### - Purpose
Implement the core visual mapping engine, real-time location tracker, and hazard navigation dashboard. This includes embedding Google Maps SDK, rendering real-time incident marker overlays, displaying dynamic voice-assisted driving routes, and showing AI-generated reroute advice on detection of hazard ahead events.

### - Features
- **Interactive Google Maps Canvas**: Custom Dark/Light maps styled dynamically with marker clustering.
- **Dynamic Heatmaps & Overlays**: Heatmap layers depicting traffic congestion and blue semi-transparent polygons displaying flooded zones.
- **Location-Aware Alerts System**: Location monitoring that triggers visual alert banners when entering proximity of hazards.
- **AI Route Plan Recommendations**: Present live AI recommendations offering faster alternatives on ETA drops.
- **Integrated Driving Mode**: Dashboard layout designed with safety in mind, featuring Voice Navigation prompts and large UI controls.

### - User flow
1. **Dashboard Home**: User views their live location, current traffic, and nearby active alerts.
2. **Set Destination**: Search for destination, display route alternatives, select one, and enter Navigation Active state.
3. **Driving Progress**: Real-time position moves on map. Voice prompt coordinates driving details.
4. **Hazard Event Triggers**: Proximity alert triggers. Map draws Flood overlay. AI Reroute Recommendation popup appears.
5. **Interactive Actions**: User accepts alternative via the Reroute Modal, routes transition instantly, and system updates the ETA.

### - Firebase services required
- **Firebase Firestore**: Real-time listeners streaming active incident models filtered by geospatial coordinates.
- **Firebase Cloud Messaging (FCM)**: Remote notifications triggering instant hazard state redraws.

### - APIs required
- **Google Maps SDK for Android** (for map rendering).
- **Google Directions API** & **Distance Matrix API** (routing & alternative paths computation).
- **Android FusedLocationProviderClient** (foreground and background location).
- **Android Text-To-Speech (TTS) Engine** (voice navigation).

### - Reusable components
- `GoogleMapsView` (Dynamic wrapper managing map configurations, overlays, and styles)
- `AlertBannerCard` (Animated notifications displayed at the top of the map)
- `RerouteRecommendationModal` (Clean AI layout showing alternative paths and comparative stats)
- `DrivingModeOverlay` (Minimal UI controls with high contrast buttons)
- `IncidentBottomSheet` (Expansible detailed sheet for live incidents)
- `ActiveRouteCard` (Dynamic info showing ETA, remaining distance, and navigation guidance)

### - Navigation routes
- `Route.Home` (Main Dashboard view)
- `Route.MapNavigation` (Full navigation screen with options for: `idle`, `active`, `rerouting`)
- `Route.DrivingMode` (Safe voice-prompts landscape interface)

### - State management requirements
- `MapState`: Live coordinates, active markers list, flooding polygons, congestion heatmaps, active navigation route details.
- `LocationState`: Speed, precision details, historical path tracking.
- `NavigationUiState`: Active guidance state (`IDLE`, `ACTIVE_ROUTING`, `PROXIMITY_HAZARD`, `REROUTING`).

### - Offline support requirements
- **Room Database** caches nearby incidents.
- Map tiles cached automatically by Google Play Services.
- Directions fallback utilizes offline straight-line routes if network connection drops.

---

## # Screens Breakdown

### 1. Home Dashboard & Map with Nearby Alerts
- **Screen Purpose**: Central viewport showing live location, active warnings, and entry points for navigation.
- **Required ViewModel**: `HomeViewModel` (manages Firestore subscriptions and user telemetry updates).
- **UI states**:
  - `HomeUiState(userLocation: LatLng?, nearbyIncidents: List<Incident>, activeAlertCount: Int, isLoading: Boolean)`
- **Compose components needed**:
  - `GoogleMap` container.
  - Floating action buttons (FABs) for reporting, current location centring.
  - Horizontally scrolling list of nearby alerts (`AlertBannerCard` list).
- **API/Firebase calls**:
  - Firestore live database listener: `/incidents` (geo-indexed).
- **Validation rules**: None.
- **Navigation actions**: 
  - `Search` -> `Route.MapNavigation`
  - `Report` -> `Route.Report` (Batch 4)
- **Error handling**: Displays offline banner if connection fails, rendering local cached database entries.
- **Loading handling**: Map shimmer overlay.
- **Accessibility requirements**: High contrast marker icons, descriptive labels on FAB controls.
- **Animations/transitions**: Alert cards enter with dynamic slide-in from top, marker icons scale up on active rendering.
- **Dark mode considerations**: Customized Dark Map style utilizing slate-blue hues `#0F172A` as showcased in the design guidelines.
- **Reusable widgets extracted from this screen**: `AlertBannerCard`.

### 2. Traffic Heatmap & Flood Hazard Overlays
- **Screen Purpose**: Toggleable visual layers overlaying hazard distributions on map.
- **Required ViewModel**: `MapOverlayViewModel`.
- **UI states**:
  - `OverlayUiState(showHeatmap: Boolean, showFloodPolygons: Boolean, activePolygons: List<PolygonData>)`
- **Compose components needed**:
  - Overlay controllers.
  - Google Map Polygon and TileOverlay components.
- **API/Firebase calls**: Streamed geographic polygons representing flood zones.
- **Validation rules**: None.
- **Navigation actions**: None.
- **Error handling**: Overlay loading fails gracefully without affecting primary map rendering.
- **Loading handling**: Soft fade-in on polygon overlays.
- **Accessibility requirements**: High-contrast outline colors (e.g. bold blue borders for flood zones) to ensure color-blind accessibility.
- **Animations/transitions**: Smooth animation on polygon opacity.
- **Dark mode considerations**: Semitransparent neon-blue overlays `#3B82F6` for floods and bright red overlays `#EF4444` for traffic heatmaps.
- **Reusable widgets extracted from this screen**: `FloodOverlay`, `HeatmapToggle`.

### 3. Navigation State Views (Idle, Active Route, Rerouting, Route Alternatives)
- **Screen Purpose**: Guides users from routing setup to active driving and emergency adjustments.
- **Required ViewModel**: `NavigationViewModel` (coordinates route computations and state updates).
- **UI states**:
  - `NavigationState(activeRoute: RoutePath?, alternateRoutes: List<RoutePath>, currentGuidance: GuidanceStep?, currentNavState: NavState)`
- **Compose components needed**:
  - Route planning controls.
  - Map path polyline drawer (colored green for standard, red for hazard segments).
  - Floating `ActiveRouteCard` (ETA, remaining distance).
- **API/Firebase calls**:
  - Google Directions API queries.
- **Validation rules**: Destination coordinates must be valid.
- **Navigation actions**: Directs back to `Route.Home` on trip completion.
- **Error handling**: Graceful recovery if Directions API hits rate limit, falling back to cached routes.
- **Loading handling**: Segmented progress shimmer inside navigation route summary blocks.
- **Accessibility requirements**: Support for large scale text on ETA displays.
- **Animations/transitions**: Slide-up transition on route alternative drawers.
- **Dark mode considerations**: Route polyline paths match midnight mode navigation contrasts.
- **Reusable widgets extracted from this screen**: `ActiveRouteCard`, `RouteAlternativeItem`.

### 4. AI Reroute Recommendation & Emergency Reroute Modal
- **Screen Purpose**: Automatically notify driving users of hazards ahead, prompting alternative path choices.
- **Required ViewModel**: `AiRerouteViewModel`.
- **UI states**:
  - `AiRerouteUiState(hazardDetected: Incident?, proposedRoute: RoutePath?, timeSavedMinutes: Int, showModal: Boolean)`
- **Compose components needed**:
  - Custom full-width `RerouteRecommendationModal` with comparative path cards.
  - Quick action buttons (`Accept Detour`, `Ignore`).
- **API/Firebase calls**:
  - AI planning engine query via backend endpoint.
- **Validation rules**: None.
- **Navigation actions**: Instantly triggers transition to active rerouting path on acceptance.
- **Error handling**: Displays standard alert if reroute query fails.
- **Loading handling**: Spinner inside modal during detour calculation.
- **Accessibility requirements**: Large button touch targets (>48dp) for safe interactions during driving.
- **Animations/transitions**: Slide-down entry banner with pulsing emergency border.
- **Dark mode considerations**: Sleek translucent glassmorphism backdrop overlays.
- **Reusable widgets extracted from this screen**: `RerouteRecommendationModal`.

### 5. Voice Navigation & Driving Mode UI
- **Screen Purpose**: Distraction-free navigation viewport prioritizing auditory guidance.
- **Required ViewModel**: `VoiceGuidanceViewModel`.
- **UI states**:
  - `VoiceUiState(currentPromptText: String, isMuted: Boolean)`
- **Compose components needed**:
  - Clean landscape-optimized container.
  - Oversized navigation arrows indicating upcoming moves.
  - Large mute/unmute visual toggles.
- **API/Firebase calls**: None.
- **Validation rules**: None.
- **Navigation actions**: Option to exit driving mode back to standard UI.
- **Error handling**: Standard visual fallback if the Text-To-Speech engine fails to initialize.
- **Loading handling**: None.
- **Accessibility requirements**: High contrast text sizing, spoken navigation voice cues.
- **Animations/transitions**: Visual pulsing ring synced to voice prompt triggers.
- **Dark mode considerations**: Deep dark backgrounds (`#05070F`) to reduce driver screen glare at night.
- **Reusable widgets extracted from this screen**: `VoiceIndicatorButton`, `LargeGuidanceArrow`.

---

## # Development Todo List

### 🛠 UI Tasks
- [ ] Integrate Google Maps Compose library (`com.google.maps.android:maps-compose`).
- [ ] Implement map layout rendering with dynamic theme styles.
- [ ] Create custom Marker layouts for incidents (using Lucide icons).
- [ ] Build the dynamic expanding `IncidentBottomSheet` component.
- [ ] Design the floating `ActiveRouteCard` containing progress gauges.
- [ ] Construct the landscape layout for distraction-free `DrivingModeScreen`.
- [ ] Create the visual `RerouteRecommendationModal` displaying path comparison stats.

### 🛠 ViewModel Tasks
- [ ] Implement `HomeViewModel` containing live Firestore location-bounded queries.
- [ ] Write `NavigationViewModel` coordinating Google Directions API integration.
- [ ] Implement `AiRerouteViewModel` managing detour evaluation events.
- [ ] Design `VoiceGuidanceViewModel` driving TTS integrations.

### 🛠 Repository Tasks
- [ ] Implement `LocationRepository` supporting GPS tracking logic.
- [ ] Design `NavigationRepository` to interface route calculations.
- [ ] Build local database cache entities for saving incident profiles locally (Room).

### 🛠 Firebase Tasks
- [ ] Setup FCM listeners inside a persistent Background Service.
- [ ] Configure location telemetry writes to Firestore under `/user_telemetry` collection.

### 🛠 Navigation Tasks
- [ ] Configure route mappings to transit from Home to Navigation flows.
- [ ] Implement deep-link intents mapping notification clicks directly to hazard map locations.

### 🛠 Data Model Tasks
- [ ] Define the `RoutePath`, `GuidanceStep`, and `Incident` domain entities.
- [ ] Create Room DB `IncidentEntity` schemas.

### 🛠 State Handling Tasks
- [ ] Expose real-time location stream flows.
- [ ] Build coordinate bounds calculators to limit map data loading to visible viewports.

### 🛠 Testing Tasks
- [ ] Write unit tests for Google Maps styling and JSON configuration loaders.
- [ ] Implement mock location provider integration tests.
- [ ] Write unit test validating route progress math (ETA calculations).

---

## # Deliverables
- Fully styled Google Maps Canvas integrated inside the Compose layout.
- Real-time location tracker service streaming user coordinates.
- Proximity monitoring engine triggering hazard proximity alerts on map.
- Functional route plotting and navigation views, including alternative paths.
- Working voice navigation prompt triggers using TTS.
- Visual overlays mapping flooded zones and traffic density heatmaps.

# Batch 6: Emergency Systems, SOS Controls, Profiles & Preference Settings

## # Batch Overview

### - Purpose
Complete the application by integrating high-priority emergency functions, offline safety protocols, customized system parameters, preferences persistence, profile management, and comprehensive user statistics. This includes implementing an instant SOS button, emergency hospital/shelter routing, accessibility customization settings, and data saving optimization.

### - Features
- **One-Tap SOS Broadcast**: Instant trigger alerting contacts and broadcasting location data to active emergency channels.
- **Dynamic Emergency Routing**: Fast-path navigation routing immediately directing users to nearest hospitals or shelters in crisis situations.
- **Offline Emergency Mode**: Local map routing and safety advice accessible entirely offline during total network grid failures.
- **Comprehensive Settings Hub**: Unified management of privacy parameters, notification alerts, language settings, and custom dark modes.
- **Accessibility Engine**: Font adjustments, spoken alerts triggers, and color accessibility modes suited for all users.
- **Data Saver Mode**: Restrict map downloads and high-bandwidth media syncs over cellular data.

### - User flow
1. **Crisis Trigger**: User hits the prominent SOS button from any screen.
2. **SOS Countdown**: A 3-second abort window displays. If uninterrupted, the app broadcasts coordinates, sends alerts to emergency contacts, and activates Emergency Mode.
3. **Emergency Action Dashboard**: Fast routes show up for nearest shelters and hospitals, complete with offline navigation assistance.
4. **Tune App Preferences**: Go to the Profile Screen, check statistics, and open Settings to toggle Data Saver or custom Accessibility configurations.

### - Firebase services required
- **Firebase Firestore**: Storing user preferences, safety configurations, emergency contact details, and emergency logs.
- **Firebase Cloud Messaging (FCM)**: Immediate emergency dispatch messages.

### - APIs required
- **Android TelephonyManager API** (emergency SMS fallback).
- **Google Places API** (fast lookup of hospitals/shelters).
- **Android System Accessibility Services** (auditory alerts).

### - Reusable components
- `SosCountdownButton` (Large circular button showing countdown progress)
- `EmergencyRouteCard` (Compact card displaying fast routes to emergency centers)
- `SettingsToggleRow` (Standard settings row with switches or navigation guides)
- `StatDisplayCard` (Grid cards detailing user metrics, e.g. "Reports Sent", "Help Count")
- `AccessibilitySelector` (Segmented layout adjustments for fonts and color settings)

### - Navigation routes
- `Route.SosDashboard` (SOS circular button and dispatch overlay)
- `Route.EmergencyMode` (Dedicated dashboard for active crisis states)
- `Route.OfflineEmergency` (Offline guides and saved route configurations)
- `Route.EmergencyRouting` (Hospital and shelter directions canvas)
- `Route.Profile` (User statistics and achievements list)
- `Route.Settings` (General settings hub routing to subsections)
- `Route.PrivacySettings` (Data storage controls)
- `Route.NotificationSettings` (Alert preference checkboxes)
- `Route.DataSaver` (Bandwidth optimization preferences)
- `Route.DarkModeSettings` (Theme selections)
- `Route.AccessibilitySettings` (Visual parameters setup)

### - State management requirements
- `SosState`: Countdown seconds, emergency dispatch state, active emergency contacts, location streaming references.
- `SettingsUiState`: Notification channels toggles, theme configurations, accessibility text size, data saver state.
- `ProfileStatsUiState`: Dynamic user statistics, verification levels, completed paths count.

### - Offline support requirements
- **Critical Offline Safety Pack**: Local guides and simplified offline coordinates for regional shelters/hospitals stored in Room.
- SMS fallback triggers location dispatch even when mobile internet data is entirely unavailable.

---

## # Screens Breakdown

### 1. SOS & Emergency Active Dashboard
- **Screen Purpose**: High-priority view triggers instant broadcast and handles active crisis routing.
- **Required ViewModel**: `SosViewModel` (handles GPS broadcasts, SMS triggers, and countdowns).
- **UI states**:
  - `SosUiState(countdownValue: Int, isBroadcasting: Boolean, contactsAlerted: Boolean, emergencyActive: Boolean)`
- **Compose components needed**:
  - Oversized pulsing circular SOS button.
  - Abort action buttons.
  - List of primary emergency contacts.
- **API/Firebase calls**:
  - Real-time location stream published to `/emergencies` collection.
- **Validation rules**: Contacts lists must contain at least one valid phone number to trigger SMS fallbacks.
- **Navigation actions**:
  - `SOS Complete` -> `Route.EmergencyMode`
- **Error handling**: Switches immediately to SMS broadcast logic if mobile internet upload fails.
- **Loading handling**: Countdown ring overlay.
- **Accessibility requirements**: Prominent voice narration on countdown, huge button click bounds.
- **Animations/transitions**: Pulsing high-contrast rings behind the central SOS button.
- **Dark mode considerations**: Pure black backdrop `#000000` contrasting against neon-red triggers `#EF4444`.
- **Reusable widgets extracted from this screen**: `SosCountdownButton`.

### 2. Emergency Shelter & Hospital Routing
- **Screen Purpose**: Actionable map guiding users directly to closest safe zones.
- **Required ViewModel**: `EmergencyRoutingViewModel`.
- **UI states**:
  - `EmergencyRouteUiState(closestHospitals: List<EmergencyCenter>, selectedCenter: EmergencyCenter?, route: RoutePath?, isLoading: Boolean)`
- **Compose components needed**:
  - Map view highlighting safe shelter overlays.
  - Fast-action directions cards (`EmergencyRouteCard`).
- **API/Firebase calls**:
  - Places API calls querying nearest hospital centers.
- **Validation rules**: None.
- **Navigation actions**: Directs back to standard viewports on cancellation.
- **Error handling**: Displays saved offline hospital coordinates if network goes down.
- **Loading handling**: Progressive shimmers.
- **Accessibility requirements**: Explicit audible directions prompts, color blind-friendly path vectors.
- **Animations/transitions**: High-priority route paths blink softly.
- **Dark mode considerations**: Dark map configuration highlighting neon-emerald hospital pins.
- **Reusable widgets extracted from this screen**: `EmergencyRouteCard`.

### 3. Profile & Contributor Statistics Screen
- **Screen Purpose**: Summarize user impact, show badges, and link to settings modules.
- **Required ViewModel**: `ProfileViewModel`.
- **UI states**:
  - `ProfileUiState(userStats: ContributorStats, badgeList: List<Badge>, username: String, profilePic: String)`
- **Compose components needed**:
  - Contributor status summaries (`StatDisplayCard`).
  - Scrollable grid displaying earned hazard reporter badges.
  - Action options list linking to settings.
- **API/Firebase calls**:
  - Firestore query fetching user contribution summaries.
- **Validation rules**: None.
- **Navigation actions**: 
  - `Settings` -> `Route.Settings`
- **Error handling**: Displays static offline profile info if Firestore query fails.
- **Loading handling**: Skeleton shimmers.
- **Accessibility requirements**: Text descriptions for all graphic badge assets.
- **Animations/transitions**: Stat count items slide in.
- **Dark mode considerations**: Smooth card color schemes blending with profile headers.
- **Reusable widgets extracted from this screen**: `StatDisplayCard`.

### 4. Settings Hub (Privacy, Notifications, Data Saver, Accessibility)
- **Screen Purpose**: Configure application preferences.
- **Required ViewModel**: `SettingsViewModel` (coordinates DataStore changes and updates configurations).
- **UI states**:
  - `SettingsState(theme: ThemeType, dataSaverEnabled: Boolean, alertsEnabled: Boolean, fontSizeFactor: Float)`
- **Compose components needed**:
  - Nested settings categories rows.
  - Quick-toggle switch cards.
  - Text-scale sliders.
- **API/Firebase calls**: Updates topic notification registrations dynamically.
- **Validation rules**: Font scaling slider limited to factors between `0.8` and `1.4`.
- **Navigation actions**: Configures sub-settings views transitions.
- **Error handling**: Catches preference writing issues, showing helpful error dialogues.
- **Loading handling**: None.
- **Accessibility requirements**: Fully supports screen font changes, screen reader-friendly switch states.
- **Animations/transitions**: Settings menus list transitions.
- **Dark mode considerations**: Consistent midnight gray elements matching M3 Dark parameters.
- **Reusable widgets extracted from this screen**: `SettingsToggleRow`.

---

## # Development Todo List

### 🛠 UI Tasks
- [ ] Build the pulsing circular `SosScreen` layout.
- [ ] Implement `EmergencyModeDashboard` view.
- [ ] Design custom high-priority maps for `EmergencyRoutingScreen`.
- [ ] Create `ProfileScreen` layout displaying contributor statistics.
- [ ] Implement paginated settings menu rows inside `SettingsHubScreen`.
- [ ] Design the dynamic `AccessibilitySettingsScreen` text-scaler slider.

### 🛠 ViewModel Tasks
- [ ] Implement `SosViewModel` managing emergency broadcast loops and SMS fallbacks.
- [ ] Write `EmergencyRoutingViewModel` managing high-priority Places API requests.
- [ ] Implement `ProfileViewModel` fetching contribution statistics.
- [ ] Design `SettingsViewModel` executing DataStore writes on preference updates.

### 🛠 Repository Tasks
- [ ] Design `EmergencyRepository` contract handling emergency triggers.
- [ ] Create local database entities mapping offline regional shelters.
- [ ] Write logic toggling network usage thresholds inside API interceptor tools.

### 🛠 Firebase Tasks
- [ ] Setup FCM topic registration controllers managing notification channel subscriptions.
- [ ] Configure Firestore indexing on `/emergencies` datasets.

### 🛠 Navigation Tasks
- [ ] Configure routes linking settings lists to respective subviews.
- [ ] Implement global action overrides immediately opening SOS layout from any screen.

### 🛠 Data Model Tasks
- [ ] Model `ContributorStats`, `Badge`, and `EmergencyCenter` domain structures.
- [ ] Create Room `ShelterEntity` database schemas.

### 🛠 State Handling Tasks
- [ ] Build system managers listening to GPS coordinate streams during crisis modes.
- [ ] Expose global flows mapping active data saver states.

### 🛠 Testing Tasks
- [ ] Write unit tests verifying SOS SMS fallback conditions on network loss.
- [ ] Implement UI layout verification tests for landscape emergency routing viewports.
- [ ] Write integration test validating DataStore preferences persistent saves.

---

## # Deliverables
- Fully completed SOS broadcast layout complete with countdown rings.
- Integrated emergency SMS location broadcast fallback triggers.
- Functional emergency shelter routing map.
- User profile dashboards detailing hazard contribution metrics.
- Complete settings screens managing Dark theme, local languages, and notifications.
- Accessibility tools adjusting custom font scale variables dynamically.
- Bandwidth saver settings restricting downloads over mobile connections.

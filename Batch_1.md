# Batch 1: Project Foundations & Design System

## # Batch Overview

### - Purpose
Establish the fundamental base of the application. This includes creating the design system (theme, colors, typography, shapes), setting up clean MVVM/MVI architecture structures, establishing dynamic/dark/light themes, configuring dependency injection (Hilt), setting up routing structure (Navigation Compose), and initiating Firebase core configuration. 

### - Features
- **Dynamic Theming Engine**: Support for light, dark, and dynamic (Material You Android 12+) color palettes.
- **Shared Typography & Component Library**: Reusable buttons, inputs, headers, and custom icons customized with the "Outfit" Google Font.
- **Unified Base Architecture**: Base classes for ViewModels, custom StateFlow event/state wrappers, standard result patterns, and structural state classes (`Loading`, `Success`, `Error`).
- **Core Navigation Setup**: Entry-level navigation graph including splash screen and elements showcase.
- **Firebase Core Prep**: Initialization configuration and base dependency injection hooks for auth, database, analytics, and crashlytics.
- **Error/Empty Showcase Screen**: Standardized visual guides for non-ideal app flows.
- **App Icon Concepts**: Adaptive app icon templates and launch icons.

### - User flow
1. **App Launch**: User opens the app.
2. **Splash Screen**: The splash screen is shown with a fade-in scale animation, verifying the Firebase and initial theme configurations.
3. **Redirection (Local Test Flow)**: While in early-access development, user is directed to the UI Elements Showcase to audit the design system elements.

### - Firebase services required
- **Firebase Core**: Dynamic installation and startup logs.
- **Firebase Crashlytics & Analytics**: Base integration to trace early startup anomalies.

### - APIs required
- None (Local foundations & design system assets only).

### - Reusable components
- `AppButton` (Primary Solid, Secondary Outlined, Icon-Only, Danger variant)
- `AppTextField` (Dynamic input decoration, error support, interactive states)
- `AppTopBar` (Consistent action bar, back navigation hook)
- `SkeletonShimmer` (Generic animated skeleton gradient block)
- `ErrorMessage` (Unified visual card with retry triggers)
- `EmptyStateView` (Generic asset slot + descriptive text and actionable buttons)

### - Navigation routes
- `Route.Splash` (Initial application bootstrap)
- `Route.Showcase` (Showcase view for styling auditor)

### - State management requirements
- `ThemeState`: Dark Mode setting (`AUTO`, `ALWAYS_LIGHT`, `ALWAYS_DARK`).
- Showcase UI State: Tracks interactive showcases (e.g. simulated loader toggles).

### - Offline support requirements
- Offline preferences caching for `ThemeState` via **DataStore**.

---

## # Screens Breakdown

### 1. Foundations (Dark & Light Themes)
- **Screen Purpose**: Provide the dynamic core container representing theme context. 
- **Required ViewModel**: `MainViewModel` (manages user configuration preferences).
- **UI states**:
  - `ThemeState(isDarkMode: Boolean, useDynamicColor: Boolean)`
- **Compose components needed**:
  - `MaterialTheme` (Wrapper)
  - `AppTheme` (Dynamic custom composition provider)
  - `Surface` (Adaptive canvas backgrounds matching `--bg-deep` `#0F172A` in Dark, `#F8FAFC` in Light)
- **API/Firebase calls**: None.
- **Validation rules**: None.
- **Navigation actions**: None.
- **Error handling**: Fallback to static Material Light/Dark if system context lacks dynamic color support.
- **Loading handling**: Default window background color displayed until configuration resolves.
- **Accessibility requirements**: High color-contrast ratios conforming to WCAG AA. Supporting dynamic text sizing (`sp`).
- **Animations/transitions**: Smooth crossfade transition on theme changes.
- **Dark mode considerations**: Pure black overlays `#000000` combined with blue-gray card tones `#1E293B` to maintain the premium dark look from the HTML prototype.
- **Reusable widgets extracted from this screen**: `AppTheme` Compose theme wrapper.

### 2. Design System & UI Elements Showcase
- **Screen Purpose**: Demonstrate all components (buttons, text fields, cards, dialogs) in all states.
- **Required ViewModel**: `ShowcaseViewModel` (simulates interactive changes).
- **UI states**:
  - `ShowcaseState` (holds input values, toggle states, selected tabs)
- **Compose components needed**:
  - `LazyColumn` for showcase catalog.
  - Cards displaying inputs, text fields, progress indicators, buttons.
  - Interactive alerts.
- **API/Firebase calls**: None.
- **Validation rules**: Live validation preview on custom `AppTextField` inputs.
- **Navigation actions**: None.
- **Error handling**: Visual demonstration of input error states and message layouts.
- **Loading handling**: Demonstrations of shimmers and custom spinner overlays.
- **Accessibility requirements**: Semantic structures, custom roles for custom interactive components.
- **Animations/transitions**: Component hover and click scale responses.
- **Dark mode considerations**: Components are fully tested in both light and dark styles.
- **Reusable widgets extracted from this screen**: Core design system components (`AppButton`, `AppTextField`, etc.).

### 3. Splash Screen
- **Screen Purpose**: App bootstrap, Firebase core loading, and initial navigation routing.
- **Required ViewModel**: `SplashViewModel`.
- **UI states**:
  - `SplashState` (`Initializing`, `NavigateToOnboarding`, `NavigateToMain`)
- **Compose components needed**:
  - `Box` for central alignment.
  - `Icon` with shield motif (`logo-shield` from the HTML spec).
  - Elegant text logo (`TrafficGuard AI`).
- **API/Firebase calls**: Firebase initial logging handshake.
- **Validation rules**: Verifies DataStore configs.
- **Navigation actions**: Navigates to `Route.Showcase` or onboarding flow.
- **Error handling**: Fallback route if startup throws exceptions.
- **Loading handling**: Circular custom progress element hidden unless initialization takes longer than 2 seconds.
- **Accessibility requirements**: Skip button description for fast screen updates, appropriate `contentDescription` on the brand icon.
- **Animations/transitions**: Entrance scaling for the logo shield (`scaleIn` with `Spring.DampingRatioMediumBouncy`) followed by slideUp transition.
- **Dark mode considerations**: Strict brand colors matching the midnight background `#0F172A`.
- **Reusable widgets extracted from this screen**: Brand Logo component.

### 4. Empty & Error States Screen
- **Screen Purpose**: Establish consistent user layouts for network errors, database failures, and empty alert states.
- **Required ViewModel**: `ErrorShowcaseViewModel` (simulates error responses).
- **UI states**:
  - `ErrorState` (`NoNetwork`, `ServerDown`, `NoRecordsFound`)
- **Compose components needed**:
  - `EmptyStateView` & `ErrorMessage`.
  - Action buttons (`Try Again`).
- **API/Firebase calls**: None.
- **Validation rules**: None.
- **Navigation actions**: Triggers custom callback events.
- **Error handling**: Custom UI handling.
- **Loading handling**: Simulates retry progress indicators.
- **Accessibility requirements**: Explicit text descriptors read by screen readers.
- **Animations/transitions**: Delicate bounce animation on warning icon, fade-in for retry buttons.
- **Dark mode considerations**: Vibrant neon warnings (e.g. `--accent-red` `#EF4444` and `--accent-orange` `#F59E0B`) styled to pop in dark layouts.
- **Reusable widgets extracted from this screen**: `EmptyStateView`, `ErrorStateView`.

---

## # Development Todo List

### 🛠 UI Tasks
- [ ] Create core theme packages (`core/ui/theme`) containing `Color.kt`, `Type.kt`, `Theme.kt`, `Shape.kt`.
- [ ] Setup `Outfit` Google font provider.
- [ ] Implement `AppButton` in multiple variations (Solid, Outlined, Text, Danger).
- [ ] Implement `AppTextField` with interactive placeholder/error states.
- [ ] Build the `AppScaffold` component utilizing insets management (`systemBarsPadding()`).
- [ ] Implement animated `SkeletonShimmer`.
- [ ] Construct custom `Splash` canvas logo and scaling animation.
- [ ] Create the visual Design System showcase viewport under `core/ui/showcase`.

### 🛠 ViewModel Tasks
- [ ] Define generic `BaseViewModel<State, Event, Effect>` template.
- [ ] Create `MainViewModel` targeting theme configuration updates.
- [ ] Implement `SplashViewModel` utilizing dynamic startup delays.
- [ ] Implement `ShowcaseViewModel` for visual element auditing.

### 🛠 Repository Tasks
- [ ] Implement `PreferencesRepository` contract in the domain layer.
- [ ] Implement local `PreferencesRepositoryImpl` utilizing DataStore in the data layer.

### 🛠 Firebase Tasks
- [ ] Integrate Firebase BOM versioning inside Version Catalog.
- [ ] Apply Gradle Google Services plugin.
- [ ] Configure `google-services.json` inside raw resources structure.
- [ ] Verify base Analytics tracking for early boot events.

### 🛠 Navigation Tasks
- [ ] Setup central `RootNavGraph` using Navigation Compose.
- [ ] Define core destination routes.
- [ ] Integrate Hilt-Compose navigation dependencies inside the Gradle build definitions.

### 🛠 Data Model Tasks
- [ ] Model `ThemePreference` domain schemas.
- [ ] Define standard operational `Result<out T>` packaging structure.

### 🛠 State Handling Tasks
- [ ] Construct base unidirectional state flows (MVI/MVVM pattern hooks).
- [ ] Standardize event communication flows (using Channels to expose one-off UI effects).

### 🛠 Testing Tasks
- [ ] Write unit tests for `SplashViewModel` state transition timing.
- [ ] Develop UI Screenshot / Compose Layout tests for `AppButton` states.
- [ ] Create integration tests for DataStore preference saving.

---

## # Deliverables
- A working project scaffolding build compilation without errors.
- Fully configured Version Catalog (`libs.versions.toml`) and `build.gradle.kts` files.
- The base `AppTheme` engine supporting dynamic theme swaps.
- Dynamic Splash screen complete with logo entrance scaling.
- Fully functional Showcase screen rendering all typography, colors, components, and skeleton shimmers in both Dark and Light modes.

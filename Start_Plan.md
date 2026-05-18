```text
I have already downloaded all UI screens of my Android application as HTML exports inside @contextScopeItemMention.

Your task is to act as a Senior Android Architect and create a COMPLETE PRODUCTION-GRADE IMPLEMENTATION PLAN for converting these designs into a fully functional Android application using:

- Kotlin
- Jetpack Compose
- MVVM + Clean Architecture
- Firebase
- Android Jetpack libraries
- Material 3
- Navigation Compose
- Hilt Dependency Injection
- Coroutines + Flow
- Retrofit + OkHttp
- Google Maps SDK
- Firebase Authentication
- Firebase Firestore
- Firebase Cloud Messaging (FCM)
- Firebase Storage
- Firebase Analytics
- Firebase Crashlytics
- WorkManager
- Room Database
- DataStore Preferences

This is an ANDROID-ONLY PROJECT.
DO NOT generate React Native, Flutter, Web, or iOS plans.

The application is an AI-powered traffic, hazard, and emergency response system.

--------------------------------------------------
PROJECT REQUIREMENTS
--------------------------------------------------

Create a FULL IMPLEMENTATION ROADMAP with:

1. Project architecture
2. Module structure
3. Folder structure
4. Gradle dependency planning
5. Firebase integration strategy
6. Offline-first architecture
7. State management strategy
8. Navigation graph structure
9. UI implementation strategy
10. Reusable component strategy
11. Backend integration strategy
12. Security strategy
13. Scalability considerations
14. Performance optimization plan
15. Testing strategy
16. CI/CD recommendations
17. Android permissions planning
18. Maps & location integration plan
19. Notification system plan
20. AI integration placeholders
21. Production release checklist

--------------------------------------------------
IMPORTANT OUTPUT FORMAT
--------------------------------------------------

For EACH BATCH below generate:

# Batch Overview
- Purpose
- Features
- User flow
- Firebase services required
- APIs required
- Reusable components
- Navigation routes
- State management requirements
- Offline support requirements

# Screens Breakdown
For EVERY screen provide:
- Screen purpose
- Required ViewModel
- UI states
- Compose components needed
- API/Firebase calls
- Validation rules
- Navigation actions
- Error handling
- Loading handling
- Accessibility requirements
- Animations/transitions
- Dark mode considerations
- Reusable widgets extracted from this screen

# Development Todo List
Create detailed implementation tasks:
- UI tasks
- ViewModel tasks
- Repository tasks
- Firebase tasks
- Navigation tasks
- Data model tasks
- State handling tasks
- Testing tasks

# Deliverables
Define what must be completed before moving to next batch.

--------------------------------------------------
GLOBAL REQUIREMENTS
--------------------------------------------------

Use:
- Kotlin DSL (build.gradle.kts)
- Version Catalog (libs.versions.toml)
- Compose BOM
- Latest stable Android architecture
- Single Activity Architecture
- Navigation Compose
- Material 3 theming
- Dynamic color support
- Modular clean architecture

Recommended package structure:
- core
- data
- domain
- feature
- navigation
- ui
- common

Include recommendations for:
- Feature modules
- Shared design system
- Reusable Compose components
- UI state patterns
- Sealed classes for state/events
- Result wrappers
- Error handling architecture
- Analytics tracking
- Crash reporting
- Logging strategy

--------------------------------------------------
BATCH 1 SCREENS
--------------------------------------------------

1. Foundations (Dark)
2. Foundations (Light)
3. App Icon Concepts
4. Empty & Error States
5. Design System
6. Splash Screen
7. Loading & Skeleton States
8. UI Elements Showcase

Focus:
- Design system
- Theme engine
- Typography
- Component library
- Base architecture
- Navigation setup
- Firebase initialization
- Dependency injection setup
- Core reusable UI system

--------------------------------------------------
BATCH 2 SCREENS
--------------------------------------------------

1. Account Setup Success
2. Forgot Password
3. Login
4. Onboarding 1: Traffic Updates
5. OTP Verification
6. Signup
7. Onboarding 2: Smart Routing
8. Language Selection
9. Permissions Onboarding
10. Onboarding 3: Safety Alerts
11. Splash Screen
12. Welcome Screen

Focus:
- Firebase Authentication
- Phone auth
- Email auth
- Onboarding flow
- Permissions flow
- DataStore onboarding persistence
- Authentication navigation graph

--------------------------------------------------
BATCH 3 SCREENS
--------------------------------------------------

1. Emergency Reroute Modal
2. Home with Nearby Alerts
3. Traffic Heatmap
4. Voice Navigation UI
5. Driving Mode UI
6. AI Reroute Recommendation
7. Navigation Dark Mode
8. Navigation Idle State
9. Hazard Ahead Warning
10. Navigation Active Route
11. Incident Detail Bottom Sheet
12. Flood Hazard Overlay
13. Navigation Rerouting State
14. Route Alternatives
15. Home Dashboard

Focus:
- Google Maps integration
- Real-time alerts
- Navigation system
- Location tracking
- Maps overlays
- Nearby hazard system
- Background location handling
- Realtime Firestore updates

--------------------------------------------------
BATCH 4 SCREENS
--------------------------------------------------

1. Trust Verification Progress
2. Report Success Screen
3. AI Processing Screen
4. Severity Selection
5. Voice Note Reporting
6. Add Report Details
7. Duplicate Report Detection
8. Offline Queued Report
9. Select Map Location
10. Quick Report Modal
11. Incident Category Selection
12. Upload Media

Focus:
- Incident reporting
- Firebase Storage uploads
- Offline queue system
- WorkManager sync
- Voice recording
- AI preprocessing placeholders
- Media compression
- Duplicate detection flow

--------------------------------------------------
BATCH 5 SCREENS
--------------------------------------------------

1. Trusted Contributor Profile
2. Alert Details
3. User Reputation
4. Community Leaderboard
5. Alerts Feed
6. Critical Alerts
7. False Report Warning
8. Incident Discussion
9. Community Confirmations
10. Nearby Alerts Map

Focus:
- Community system
- Reputation engine
- Social interactions
- Feed architecture
- Firestore pagination
- Real-time comments
- Trust scoring architecture

--------------------------------------------------
BATCH 6 SCREENS
--------------------------------------------------

1. SOS Screen
2. Privacy Settings
3. Data Saver Mode
4. Dark Mode Settings
5. Notification Settings
6. Offline Emergency Mode
7. Profile Screen
8. Emergency Shelter Routing
9. Accessibility Settings
10. User Statistics
11. Language Settings
12. Hospital Routing
13. Emergency Mode

Focus:
- Settings architecture
- Emergency systems
- Accessibility
- Offline mode
- Notification preferences
- User profile management
- Data usage optimization

--------------------------------------------------
ADDITIONAL REQUIREMENTS
--------------------------------------------------

Generate:
1. Recommended development order
2. Sprint planning suggestions
3. Suggested Git branching strategy
4. Firebase collection schema
5. Room database schema
6. DataStore keys
7. API layer structure
8. Repository contracts
9. Suggested sealed UI state classes
10. Suggested navigation routes
11. Suggested Compose reusable components
12. Suggested testing coverage
13. Suggested Firebase security rules
14. Suggested Proguard/R8 rules
15. Suggested background workers
16. Suggested caching strategy
17. Suggested analytics events
18. Suggested notification categories

--------------------------------------------------
VERY IMPORTANT
--------------------------------------------------

DO NOT generate actual Kotlin code yet.

ONLY generate:
- Detailed implementation roadmap
- Technical architecture
- Task breakdowns
- Todos
- Engineering planning
- Project organization
- Batch execution strategy

The goal is to create a step-by-step execution document that a coding AI agent can follow to build the entire Android application screen-by-screen and feature-by-feature in production quality.
```

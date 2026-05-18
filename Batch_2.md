# Batch 2: Onboarding, Language Selection, Permissions & Authentication

## # Batch Overview

### - Purpose
Onboard new users, capture system preferences, configure required system permissions, and handle user authentication utilizing Firebase Auth (Anonymous, Email/Password, and Phone Auth).

### - Features
- **Interactive Multi-Step Onboarding**: Elegant three-screen slides showcasing the core value props (Traffic Updates, Smart Routing, Safety Alerts).
- **Language Picker Screen**: Let users select English, Urdu, or Roman Urdu prior to entry.
- **Dynamic Permission Onboarding**: Gracefully explain system permissions (Location, Notifications) before launching the standard OS dialogs to optimize consent rates.
- **Firebase Authentication**: Full system supporting registration, login, forgot password, OTP verification, and anonymous logins.
- **Persistence Store**: Save onboarding states, selected language, and user credentials inside DataStore.

### - User flow
1. **First Boot**: Launch to Language Selection.
2. **Onboarding Slides**: Swipeable Onboarding cards showing major features.
3. **Permissions request**: Dynamic educational prompts for Location and Notifications.
4. **Welcome/Gate screen**: Options to Register, Sign-In, or Proceed Anonymously.
5. **Auth Forms**: Form submission with real-time feedback, followed by Auth Success redirection.

### - Firebase services required
- **Firebase Authentication**: Supporting Email/Password, Phone number SMS authentication, and Anonymous Sign-ins.
- **Firebase Firestore**: Early user record creations on signup completion.

### - APIs required
- **Twilio / Firebase OTP SMS gateway** for OTP deliveries.

### - Reusable components
- `AuthInputCard` (Specialized input box for emails, passwords, phone numbers)
- `OnboardingSlide` (Reusable slide format with vector layout support)
- `OtpInputBlock` (Interactive segmented digit code block)
- `PermissionItemCard` (Row design explaining a specific permission with toggles)
- `LanguagePill` (Selectable grid layout item representing language preferences)

### - Navigation routes
- `Route.LanguageSelection` (Initial setup language)
- `Route.Onboarding` (Step 1 to 3 slides)
- `Route.Permissions` (Request location/notifications)
- `Route.Welcome` (Entry gate selection)
- `Route.Login` (Sign in form)
- `Route.Signup` (Registration form)
- `Route.ForgotPassword` (Password recovery form)
- `Route.OtpVerification` (SMS code entry input)
- `Route.AuthSuccess` (Success feedback screen)

### - State management requirements
- `OnboardingState`: Active step index, selected preferences.
- `AuthState`: Form state inputs, error diagnostics, loading verification states, auth session references.

### - Offline support requirements
- Onboarding status and preferred language are stored offline in **DataStore**.

---

## # Screens Breakdown

### 1. Language Selection Screen
- **Screen Purpose**: Choose operational localization settings.
- **Required ViewModel**: `LanguageViewModel`.
- **UI states**:
  - `LanguageUiState(supportedLanguages: List<Lang>, selectedLanguage: Lang?)`
- **Compose components needed**:
  - Grid structures housing customized `LanguagePill` options.
  - Large Outfit title card.
  - Confirm buttons.
- **API/Firebase calls**: None.
- **Validation rules**: Confirm CTA is only active when a language is selected.
- **Navigation actions**: Navigates to `Route.Onboarding`.
- **Error handling**: Default to English if anything fails.
- **Loading handling**: Simple spinner if storage lookup is active.
- **Accessibility requirements**: High clarity text layout, dynamic localization content descriptions.
- **Animations/transitions**: Pulsing hover responses on selected grid tiles.
- **Dark mode considerations**: Smooth gradients blending back into background `#0F172A`.
- **Reusable widgets extracted from this screen**: `LanguagePill`.

### 2. Onboarding Screens (1: Traffic Updates, 2: Smart Routing, 3: Safety Alerts)
- **Screen Purpose**: Introduce key application features to the user.
- **Required ViewModel**: `OnboardingViewModel`.
- **UI states**:
  - `OnboardingUiState(currentSlideIndex: Int)`
- **Compose components needed**:
  - Horizontal pager (`HorizontalPager` from Compose Foundation).
  - Dot slide indicators.
  - Vector asset cards.
  - Action buttons (`Next`, `Skip`).
- **API/Firebase calls**: None.
- **Validation rules**: None.
- **Navigation actions**: Navigates to `Route.Permissions` on slide completion.
- **Error handling**: Fallback placeholder if asset loading has exceptions.
- **Loading handling**: Smooth transition loader between slides.
- **Accessibility requirements**: Screen reader custom descriptions for graphic onboarding assets.
- **Animations/transitions**: Spring physics on slide transitions, fade-in for action CTAs.
- **Dark mode considerations**: Semi-transparent slide overlays matching Material 3 Dark theme.
- **Reusable widgets extracted from this screen**: `OnboardingSlide`, `PagerIndicator`.

### 3. Permissions Onboarding Screen
- **Screen Purpose**: Educate and request crucial system permissions (Location, Notifications).
- **Required ViewModel**: `PermissionsViewModel`.
- **UI states**:
  - `PermissionsUiState(locationPermissionGranted: Boolean, notificationPermissionGranted: Boolean)`
- **Compose components needed**:
  - Informational graphic columns explaining why Location is vital.
  - Reusable `PermissionItemCard` items.
  - `Grant Permissions` buttons.
- **API/Firebase calls**: None.
- **Validation rules**: None.
- **Navigation actions**: Directs to `Route.Welcome`.
- **Error handling**: Displays warning alerts if crucial location access is denied.
- **Loading handling**: System overlay.
- **Accessibility requirements**: Explicit role labeling on grant buttons.
- **Animations/transitions**: Shake animation on permission requests if user denies them.
- **Dark mode considerations**: Dark borders `#334155` around permission lists.
- **Reusable widgets extracted from this screen**: `PermissionItemCard`, `PermissionRationaleDialog`.

### 4. Welcome Screen
- **Screen Purpose**: Action selector screen offering user entry methods.
- **Required ViewModel**: None.
- **UI states**: None.
- **Compose components needed**:
  - Central shield branding icon.
  - Stacked action buttons: `Log In`, `Sign Up`, `Guest Access` (Anonymous Login).
- **API/Firebase calls**: None.
- **Validation rules**: None.
- **Navigation actions**: 
  - `Log In` -> `Route.Login`
  - `Sign Up` -> `Route.Signup`
  - `Guest Access` -> Triggers anonymous authentication sequence then navigates to main dashboard.
- **Error handling**: Session error flags displayed as transient toast items.
- **Loading handling**: Fullscreen blur loading overlay during guest authentication.
- **Accessibility requirements**: Descriptive roles on buttons.
- **Animations/transitions**: Logo bounce and slideUp button entrance cascade.
- **Dark mode considerations**: Deep blue background `#0F172A` matching premium look.
- **Reusable widgets extracted from this screen**: Welcome branding header.

### 5. Signup & Login Screens
- **Screen Purpose**: Secure account creation or system sign-in.
- **Required ViewModel**: `AuthViewModel`.
- **UI states**:
  - `AuthFormState(emailInput: String, passwordInput: String, phoneInput: String, isLoading: Boolean, errorMessage: String?)`
- **Compose components needed**:
  - Input text containers (`AuthInputCard`).
  - Secure input view toggles (show/hide password).
  - Submit buttons.
  - Link text widgets (e.g. `Forgot Password?`, `Login instead`).
- **API/Firebase calls**: 
  - `FirebaseAuth.signInWithEmailAndPassword`
  - `FirebaseAuth.createUserWithEmailAndPassword`
- **Validation rules**: Email string verification, password length minimum (8 characters), phone number digits check.
- **Navigation actions**: Navigates to `Route.AuthSuccess` or `Route.OtpVerification`.
- **Error handling**: Captures Firebase Exception codes (e.g., incorrect password, duplicate email) and presents localized Roman Urdu hints.
- **Loading handling**: Progress shimmers on CTAs.
- **Accessibility requirements**: Input validation alerts announced via screen readers.
- **Animations/transitions**: Text field expansion with error displays.
- **Dark mode considerations**: Clean contrasting text colors over slate gray panels.
- **Reusable widgets extracted from this screen**: `AuthInputCard`.

### 6. OTP Verification Screen
- **Screen Purpose**: Handshake authentication layer via SMS credentials.
- **Required ViewModel**: `OtpViewModel`.
- **UI states**:
  - `OtpUiState(codeBuffer: String, countdownTimerSeconds: Int, isVerifying: Boolean, errorMessage: String?)`
- **Compose components needed**:
  - Segmented input characters block (`OtpInputBlock`).
  - Timer count card.
  - Retry button.
- **API/Firebase calls**: `PhoneAuthProvider.verifyPhoneNumber` and `PhoneAuthProvider.getCredential`.
- **Validation rules**: Matches SMS length (6 digits).
- **Navigation actions**: Navigates to `Route.AuthSuccess`.
- **Error handling**: Dynamic warning banner if OTP is incorrect or has expired.
- **Loading handling**: Spinner inside digits area.
- **Accessibility requirements**: Explicit focused character narration.
- **Animations/transitions**: Red-glow border transitions on incorrect submissions.
- **Dark mode considerations**: Dark gray inputs `#1E293B` highlighting bright blue text on select.
- **Reusable widgets extracted from this screen**: `OtpInputBlock`.

### 7. Forgot Password Screen
- **Screen Purpose**: Account password recovery.
- **Required ViewModel**: `ForgotPasswordViewModel`.
- **UI states**:
  - `ForgotPassState(emailInput: String, isSent: Boolean, isError: Boolean, errorMsg: String?)`
- **Compose components needed**:
  - Simple input form.
  - Confirm buttons.
- **API/Firebase calls**: `FirebaseAuth.sendPasswordResetEmail`.
- **Validation rules**: Email must be valid.
- **Navigation actions**: Navigates back to `Route.Login`.
- **Error handling**: Displays warning if email is not found in database.
- **Loading handling**: Custom progress indicator.
- **Accessibility requirements**: Explicit accessibility descriptors.
- **Animations/transitions**: Fade-in success checkmark.
- **Dark mode considerations**: Smooth background matching.
- **Reusable widgets extracted from this screen**: `AuthHeader`.

### 8. Account Setup Success Screen
- **Screen Purpose**: Confirm auth completions and ease user transitions.
- **Required ViewModel**: None.
- **UI states**: None.
- **Compose components needed**:
  - Central success graphic icon (large green animated tick).
  - Celebration Outfit typography.
  - `Get Started` button.
- **API/Firebase calls**: Database user initialization handshake.
- **Validation rules**: None.
- **Navigation actions**: Triggers navigation to main application dashboard (`Route.Home`).
- **Error handling**: Automatically signs out user if session becomes invalid during success initialization.
- **Loading handling**: None.
- **Accessibility requirements**: High visibility tick graphics with descriptive alternate tags.
- **Animations/transitions**: Bouncing success icon, delayed exit slide transition.
- **Dark mode considerations**: Emerald green features popping out of dark backdrops.
- **Reusable widgets extracted from this screen**: Success tick graphic.

---

## # Development Todo List

### 🛠 UI Tasks
- [ ] Implement `LanguageSelectionScreen` grid and language preferences selectors.
- [ ] Build swipable `OnboardingScreen` using Compose `HorizontalPager`.
- [ ] Create dynamic `PermissionsOnboardingScreen` visual tiles.
- [ ] Construct the primary `WelcomeScreen` branding visual items and layouts.
- [ ] Implement `SignupScreen` and `LoginScreen` layouts containing validation indicators.
- [ ] Design the dynamic custom `OtpInputBlock` component.
- [ ] Create the visual styling for `ForgotPasswordScreen` and `AccountSetupSuccessScreen`.

### 🛠 ViewModel Tasks
- [ ] Implement `LanguageViewModel` managing local preferences.
- [ ] Write `OnboardingViewModel` for page state index transitions.
- [ ] Implement `PermissionsViewModel` targeting OS permissions handling.
- [ ] Design `AuthViewModel` utilizing input validation state systems.
- [ ] Create `OtpViewModel` supporting active countdown timer structures.

### 🛠 Repository Tasks
- [ ] Design `AuthRepository` interface in the domain layer.
- [ ] Build the concrete `AuthRepositoryImpl` utilising Firebase Auth in the data layer.

### 🛠 Firebase Tasks
- [ ] Configure Email/Password provider within the Firebase console.
- [ ] Setup SMS OTP Verification configurations inside the Firebase Console.
- [ ] Write Firestore user creation triggers.

### 🛠 Navigation Tasks
- [ ] Create nested `AuthNavigationGraph` routing nodes.
- [ ] Implement route-based arguments (e.g. forwarding verification credentials to OTP screen).

### 🛠 Data Model Tasks
- [ ] Create the `UserSession` and `UserProfile` domain schemas.
- [ ] Models representing authentication inputs.

### 🛠 State Handling Tasks
- [ ] Package validation results inside distinct sealed validation model frameworks.
- [ ] Expose authentication loaders using safe state controllers.

### 🛠 Testing Tasks
- [ ] Write unit tests for form validation logic in `AuthViewModel`.
- [ ] Implement UI test checking the OTP segmented character focus jumps.
- [ ] Write integration test validating mock anonymous auth tokens.

---

## # Deliverables
- Fully completed `AuthNavigationGraph` integration.
- Offline settings integration matching language and onboarding flags.
- Functional Onboarding screens including system permission requests.
- Working Firebase anonymous, email, and phone authentication systems.
- Visual screens for Forgot Password, OTP, and Setup Success.

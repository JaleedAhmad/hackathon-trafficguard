# Batch 4: Incident Reporting, Offline Sync Queue & AI Preprocessing

## # Batch Overview

### - Purpose
Provide a robust multi-modal reporting workflow allowing users to submit urban hazards (floods, accidents, blockages) via text description, voice notes, and media uploads. This module ensures high reliability through an offline queue system backed by WorkManager, handles asset compression, and provides simulated AI verification previews.

### - Features
- **Dynamic Multi-Step Reporting Flow**: Guided setup selecting incident category, severity level, map coordinates, additional descriptions, and media attachments.
- **Voice Note Capture**: Record and append Roman Urdu/English audio commentary describing active hazards.
- **Offline Report Queue**: Cache report data locally when network connectivity drops, automatically retrying sync tasks when connection returns.
- **Media Compression & Storage Upload**: Automatically scale down captured images/videos to reduce bandwidth, uploading assets to Firebase Storage.
- **AI Preprocessing & Duplicate Detection**: In-app indicator showing AI-based credibility verification steps and warnings if reports match nearby duplicate reports.

### - User flow
1. **Initiate Report**: Tap the report button on the map home screen, opening the Quick Report modal.
2. **Category & Details**: Select hazard type (e.g. Flooding), severity (Low/Medium/High), and pin location on the mini-map.
3. **Capture Commentary**: Optionally attach a quick voice note or upload a photo of the hazard.
4. **AI Duplicate Check**: App runs checks against active nearby incidents. If a duplicate is found, the system requests user confirmation.
5. **Upload & Verification**: The user submits the report. The AI Processing screen displays verification steps, leading to the Report Success confirmation screen.

### - Firebase services required
- **Firebase Firestore**: Storing structured incident report records under `/reports` collections.
- **Firebase Storage**: Media upload directory hosting compressed images and audio files under `/reports/{reportId}/`.

### - APIs required
- **Android MediaRecorder API** (audio capture).
- **WorkManager API** (offline synchronization task runner).
- **Google Maps Geocoding API** (resolving pinned coordinates to readable addresses).

### - Reusable components
- `QuickReportModal` (Bottom sheet entry point for fast reports)
- `CategorySelectPill` (Visual grid items selecting incident categories)
- `SeveritySelector` (Dynamic slider/segmented buttons selecting severity levels)
- `VoiceNoteRecorderWidget` (Compact panel housing record/play capabilities)
- `MediaUploadThumbnail` (Thumbnail grid showing attachments with delete icons)
- `AiVerificationProgressIndicator` (Clean animation block displaying AI reasoning cycles)

### - Navigation routes
- `Route.QuickReport` (Initial bottom sheet dialog overlay)
- `Route.ReportWizard` (Wizard screen handling category, location, details, and media)
- `Route.VoiceNote` (Fullscreen recorder dialog)
- `Route.SelectLocation` (Interactive map pinning screen)
- `Route.DuplicateWarning` (Overlay screen warning of duplicate detections)
- `Route.AiProcessing` (Simulated verification pipeline progress)
- `Route.ReportSuccess` (Success confirmation screen)

### - State management requirements
- `ReportFormState`: Category, severity, coordinates, voice filepath, image URI list, textual description, status flags.
- `SyncQueueState`: Number of pending offline uploads.

### - Offline support requirements
- Fully offline-first layout using **Room Database** to queue reports.
- **WorkManager** schedules persistent sync jobs carrying retry strategies on network changes.

---

## # Screens Breakdown

### 1. Quick Report Modal & Category/Severity Selection
- **Screen Purpose**: Fast-access panel letting driving users report incidents with minimal taps.
- **Required ViewModel**: `QuickReportViewModel`.
- **UI states**:
  - `QuickReportUiState(selectedCategory: Category?, selectedSeverity: Severity?, isSubmitting: Boolean)`
- **Compose components needed**:
  - `ModalBottomSheet` (Material 3).
  - High-contrast visual grid showing incident buttons (Lucide icons).
  - Segmented buttons for Severity (`LOW`, `MEDIUM`, `HIGH`).
- **API/Firebase calls**: None.
- **Validation rules**: Submit option becomes clickable as soon as the Category is chosen.
- **Navigation actions**:
  - `Next` -> `Route.ReportWizard` (for detailed attachments)
  - `Quick Submit` -> Starts quick submit flow, navigating directly to `Route.AiProcessing`.
- **Error handling**: Closes sheet gracefully on error states.
- **Loading handling**: Circular loaders inside buttons.
- **Accessibility requirements**: Minimum touch targets of 48dp on all grid items.
- **Animations/transitions**: Bottom sheet slide entrance.
- **Dark mode considerations**: Semi-transparent dark cards matching dark glassmorphism styling.
- **Reusable widgets extracted from this screen**: `CategorySelectPill`, `SeveritySelector`.

### 2. Add Report Details & Select Map Location
- **Screen Purpose**: Form capturing detailed textual summaries and verifying geo-coordinates.
- **Required ViewModel**: `ReportWizardViewModel`.
- **UI states**:
  - `ReportDetailsState(textDescription: String, locationAddress: String, pinLocation: LatLng, isLocating: Boolean)`
- **Compose components needed**:
  - Clean styling `OutlinedTextField` supporting Roman Urdu placeholders.
  - Interactive mini-map container.
  - Clickable location bar resolving addresses.
- **API/Firebase calls**:
  - Reverse-geocoding API call resolving addresses.
- **Validation rules**: Location must be pinned within active operating limits.
- **Navigation actions**: 
  - `Pin Location` -> `Route.SelectLocation` (full-screen map pin)
- **Error handling**: Displays warning label if location telemetry fails to resolve.
- **Loading handling**: Shimmer lines on location address box.
- **Accessibility requirements**: Text fields include clear screen reader labels.
- **Animations/transitions**: Expandable text fields.
- **Dark mode considerations**: Dark gray inputs `#1E293B` featuring sharp white text.
- **Reusable widgets extracted from this screen**: MiniMapCard.

### 3. Voice Note & Media Uploads
- **Screen Purpose**: Append audio descriptions and photos of the hazard.
- **Required ViewModel**: `MediaAttachmentViewModel`.
- **UI states**:
  - `AttachmentUiState(isRecording: Boolean, recordDuration: Int, attachedMediaUris: List<Uri>, isCompressing: Boolean)`
- **Compose components needed**:
  - Audio waveform indicator or recording timer.
  - Large microphone button.
  - Grid of attached image cards.
  - Add Media FAB.
- **API/Firebase calls**:
  - Media compressor routines executing before file transfers.
- **Validation rules**: Voice clip duration limited to 60 seconds, maximum 3 photo attachments.
- **Navigation actions**: Returns data back to parent wizard screen.
- **Error handling**: Displays failure banner if file read fails.
- **Loading handling**: Horizontal progress bars showing file compression.
- **Accessibility requirements**: Audio actions provide descriptive sound triggers and high visibility controls.
- **Animations/transitions**: Pulsing wave rings on active microphone record.
- **Dark mode considerations**: Soft gray borders `#334155` containing subtle glow highlights.
- **Reusable widgets extracted from this screen**: `VoiceNoteRecorderWidget`.

### 4. Duplicate Report Detection Screen
- **Screen Purpose**: Alert user if a matching incident has already been confirmed nearby, preventing clutter.
- **Required ViewModel**: `DuplicateCheckViewModel`.
- **UI states**:
  - `DuplicateUiState(existingIncidents: List<Incident>, isChecking: Boolean)`
- **Compose components needed**:
  - Danger highlight boxes.
  - Map thumbnail marking overlapping coordinates.
  - Alternative action triggers: `My Report is Different`, `Upvote Existing Report`.
- **API/Firebase calls**:
  - Location proximity Firestore lookup.
- **Validation rules**: None.
- **Navigation actions**: 
  - `Upvote` -> Upvotes existing and directs to `Route.ReportSuccess`
  - `Force Submit` -> Continues creation flow to `Route.AiProcessing`
- **Error handling**: Defaults to direct submission route if matching logic errors out.
- **Loading handling**: Checking overlays.
- **Accessibility requirements**: Screen reader alerts announcing immediate duplicate detections.
- **Animations/transitions**: Danger banner drop-down bounce.
- **Dark mode considerations**: Amber warning colors `#F59E0B` styled over slate dark boxes.
- **Reusable widgets extracted from this screen**: ProximityIncidentCard.

### 5. AI Processing & Report Success Screens
- **Screen Purpose**: Visualize report verification steps and confirm submission.
- **Required ViewModel**: `AiProcessingViewModel`.
- **UI states**:
  - `ProcessingUiState(currentVerificationStep: String, progress: Float, isSuccess: Boolean)`
- **Compose components needed**:
  - Stepper visual column.
  - Shimmer line graphics.
  - Giant green success checklist inside confirmation view.
- **API/Firebase calls**:
  - Report insertion to Firestore `/reports` collection.
  - Firebase Storage file uploads.
- **Validation rules**: None.
- **Navigation actions**: Redirects to main home dashboard on confirmation click.
- **Error handling**: If upload fails, saves to Room database queue and displays standard Offline success screen.
- **Loading handling**: Step-by-step verification loader.
- **Accessibility requirements**: Captures focus immediately on confirmation buttons.
- **Animations/transitions**: Successful tick animation scales up dynamically using spring effects.
- **Dark mode considerations**: Slate background matching deep dark layouts.
- **Reusable widgets extracted from this screen**: VerificationStepRow.

---

## # Development Todo List

### 🛠 UI Tasks
- [ ] Implement `QuickReportModal` containing incident category buttons.
- [ ] Build the detailed `ReportWizardScreen` layout.
- [ ] Design the dynamic micro-animation `VoiceNoteRecorderWidget`.
- [ ] Implement `MediaUploadGrid` with compression progress cards.
- [ ] Create `DuplicateWarningScreen` layout elements.
- [ ] Build the dynamic `AiProcessingScreen` step-by-step verification.
- [ ] Construct the `ReportSuccessScreen` layout featuring the animated tick.

### 🛠 ViewModel Tasks
- [ ] Implement `QuickReportViewModel` managing fast selections.
- [ ] Write `ReportWizardViewModel` containing local state validation.
- [ ] Create `MediaAttachmentViewModel` coordinating voice recording and image selection.
- [ ] Implement `DuplicateCheckViewModel` checking nearby coordinate grids.
- [ ] Design `AiProcessingViewModel` triggering Firestore uploads.

### 🛠 Repository Tasks
- [ ] Implement `ReportRepository` contract with offline-first queues.
- [ ] Build `MediaCompressor` utility supporting JPEG and AAC compression.
- [ ] Implement `ReportSyncWorker` task (WorkManager) executing file transfer chains.

### 🛠 Firebase Tasks
- [ ] Setup Firebase Storage bucket structures and rules.
- [ ] Configure Firestore schema rules validating report creations.

### 🛠 Navigation Tasks
- [ ] Implement route configuration linking Map to Report screens.
- [ ] Configure return-path pipelines passing geographic data back to forms.

### 🛠 Data Model Tasks
- [ ] Create Room `ReportQueueEntity` database models.
- [ ] Model Firestore `/reports` data structures.

### 🛠 State Handling Tasks
- [ ] Expose progress states mapping current WorkManager upload jobs.
- [ ] Build active listeners mapping audio recorder statuses.

### 🛠 Testing Tasks
- [ ] Write unit tests for the WorkManager offline-queue sync recovery logic.
- [ ] Develop integration tests testing the media file compression ratios.
- [ ] Write UI validation test for the Incident Report wizard fields.

---

## # Deliverables
- Fully completed incident report wizard layout.
- Integrated voice notes and photo upload capture controls.
- Automatic image scaling/compression routines.
- Functional offline queue backing up reports when network connection drops.
- Persistent WorkManager integration with retry loops.
- Working duplicate verification overlays.
- Dynamic AI verification progress visualization screen.

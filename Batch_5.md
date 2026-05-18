# Batch 5: Community Feed, User Reputation Engine & Incident Discussion

## # Batch Overview

### - Purpose
Engage the user base by integrating social features, reputation tracking, and community validation. This module provides localized incident discussion boards, upvoting/downvoting (confirmation) features, public leaderboards highlighting top helpful users, and a "Trusted Contributor" verification program based on reputation algorithm metrics.

### - Features
- **Social Incident Discussion**: Real-time localized commenting on confirmed active hazards, allowing users to report road updates.
- **Dynamic Community Confirmation**: Let users upvote (confirm) or downvote (report as resolved/false) active alerts directly from cards.
- **Reputation Trust Score Engine**: Transparent scoring layout displaying user helpfulness levels (e.g. Bronze, Silver, Gold levels).
- **Public Leaderboards**: Highlight community heroes to encourage high quality, verified reporting.
- **Comprehensive Alerts Feed**: Filterable paginated list showing active hazards matching proximity bounds.

### - User flow
1. **Explore Alerts**: Tap the Alerts tab, opening a filterable Feed showing active city incidents.
2. **Review Incident details**: Open an incident alert, viewing confirmation counts, verification timelines, and community updates.
3. **Contribute feedback**: Post a comment confirming road status (e.g., "Pani kam ho gaya hai, traffic slow chal raha hai").
4. **Reputation Progression**: The user's confirmation updates active database statistics. Upon verification, their contributor profile score points increase.
5. **Leaderboards checking**: User checks the Community Leaderboard, auditing their active city-wide standing.

### - Firebase services required
- **Firebase Firestore**: Storing comments collections under `/incidents/{id}/comments` and user reputation metrics under `/users/{id}/reputation`.
- **Firebase Functions (Dynamic Triggers)**: Cloud functions updating incident statuses based on upvote/downvote ratios (trust logic triggers).

### - APIs required
- **None** (Core Firestore Pagination & Social Services only).

### - Reusable components
- `IncidentAlertFeedCard` (Compact card showing hazard details, upvotes, and comments shortcuts)
- `CommentBubble` (Sleek comment container displaying user reputation tags)
- `ReputationGauge` (Visual ring depicting current trust metrics and score progress)
- `LeaderboardRowItem` (Row highlighting rank, name, reputation class, and contribution counts)
- `TrustActionButtons` (Action widgets: `Confirm Active`, `Report Cleared`)

### - Navigation routes
- `Route.AlertsFeed` (Main list dashboard with pagination filters)
- `Route.AlertDetail` (Incident details, overlays, and social confirmation buttons)
- `Route.IncidentDiscussion` (Dedicated active commenting viewport)
- `Route.Leaderboard` (City-wide contributor ranking)
- `Route.ContributorProfile` (Detailed view showing trust metrics, badges, and history)
- `Route.FalseReportWarning` (Warning popup for users submitting false data)

### - State management requirements
- `FeedUiState`: Active incidents list, current pagination offset, active filters, pull-to-refresh indicators.
- `DiscussionUiState`: Current comments list, text field inputs, posting loaders, message status.
- `ReputationUiState`: Badges collection, score progress, profile stats.

### - Offline support requirements
- Alerts feed and user profile stats cached inside **Room Database**.
- Comments can be typed offline and queued for automatic transmission on reconnect.

---

## # Screens Breakdown

### 1. Alerts Feed & Critical Alerts Map
- **Screen Purpose**: Feed compiling nearby hazards sorted by severity levels.
- **Required ViewModel**: `AlertsFeedViewModel` (manages Firestore pagination and list filtering).
- **UI states**:
  - `FeedUiState(alerts: List<Incident>, activeFilters: FilterSet, isRefreshing: Boolean, isLoadingMore: Boolean)`
- **Compose components needed**:
  - Filter category tabs (e.g. `Critical`, `Flooding`, `Accidents`).
  - Paginated list container (`LazyColumn` with pagination detection).
  - Floating map toggles.
- **API/Firebase calls**:
  - Paginated query to `/incidents` collection (e.g., query limits of 10 items).
- **Validation rules**: None.
- **Navigation actions**:
  - `Select Alert` -> `Route.AlertDetail`
- **Error handling**: Displays failure card if page fetching has exceptions, falling back to local database.
- **Loading handling**: Page-end loader indicators.
- **Accessibility requirements**: Scroll positions announced properly, high contrast feed layouts.
- **Animations/transitions**: Swipe-to-delete or swipe-to-dismiss visual feed gestures.
- **Dark mode considerations**: Semi-transparent feed items blending into background `#0F172A`.
- **Reusable widgets extracted from this screen**: `IncidentAlertFeedCard`.

### 2. Alert Details & Community Confirmations
- **Screen Purpose**: In-depth look at a hazard highlighting upvotes and verification statistics.
- **Required ViewModel**: `AlertDetailViewModel`.
- **UI states**:
  - `AlertDetailUiState(incident: Incident?, totalVotes: Int, userVoteState: VoteType, isSubmittingVote: Boolean)`
- **Compose components needed**:
  - Custom information tables showing report statistics.
  - Interactive upvote/downvote containers (`TrustActionButtons`).
  - Discussion preview blocks.
- **API/Firebase calls**:
  - Transaction-based Firestore increments updating upvote counters.
- **Validation rules**: Users are limited to one vote per incident.
- **Navigation actions**:
  - `Comment` -> `Route.IncidentDiscussion`
- **Error handling**: Reverts optimistic UI upvote changes if network updates fail.
- **Loading handling**: Progressive shimmers.
- **Accessibility requirements**: Vote counters include spoken text descriptors.
- **Animations/transitions**: Pulsing confirmation animations on active voting.
- **Dark mode considerations**: Vivid success outlines (`#10B981`) and danger labels (`#EF4444`) designed to pop.
- **Reusable widgets extracted from this screen**: `TrustActionButtons`.

### 3. Incident Discussion & Comments
- **Screen Purpose**: Shared forum letting users discuss route updates.
- **Required ViewModel**: `DiscussionViewModel`.
- **UI states**:
  - `DiscussionUiState(comments: List<Comment>, textInput: String, isSending: Boolean)`
- **Compose components needed**:
  - Conversation bubble column.
  - Custom floating text field with send buttons.
  - User badges and ranks.
- **API/Firebase calls**:
  - Firestore query listening to `/incidents/{id}/comments` collection.
- **Validation rules**: Comments are limited to 200 characters, empty submissions are blocked.
- **Navigation actions**: None.
- **Error handling**: Highlights comment bubbles that failed to sync with warning icons.
- **Loading handling**: Mini-loader inline with posting comments.
- **Accessibility requirements**: Focused voice narration on new comments.
- **Animations/transitions**: Comment bubbles enter with a smooth slide-in from bottom.
- **Dark mode considerations**: Slate comments cards contrasted against darker backdrops.
- **Reusable widgets extracted from this screen**: `CommentBubble`.

### 4. User Reputation & Community Leaderboard
- **Screen Purpose**: Reward helpful contributors and display local rankings.
- **Required ViewModel**: `ReputationViewModel` (coordinates profile stats and leaderboard ranks).
- **UI states**:
  - `ReputationUiState(leaderboardUsers: List<UserRank>, currentUserRank: UserRank?, isLoading: Boolean)`
- **Compose components needed**:
  - Clean profiles showing user level gauges (`ReputationGauge`).
  - Vertical rankings list.
  - Contributor level descriptions.
- **API/Firebase calls**:
  - Query fetching top 20 users sorted by reputation scores.
- **Validation rules**: None.
- **Navigation actions**: 
  - `Select User` -> `Route.ContributorProfile`
- **Error handling**: Graceful fallback to offline cached leaderboard lists.
- **Loading handling**: Sleek shimmer rows.
- **Accessibility requirements**: Clear screen reader readings for ranks.
- **Animations/transitions**: Animated ring expansion on Reputation gauges.
- **Dark mode considerations**: Gold, silver, and bronze gradient accents styled to pop in dark UI.
- **Reusable widgets extracted from this screen**: `ReputationGauge`, `LeaderboardRowItem`.

### 5. False Report Warning Screen
- **Screen Purpose**: Warning interface shown to users whose reports were flagged as false.
- **Required ViewModel**: None.
- **UI states**: None.
- **Compose components needed**:
  - High-visibility warning triangle graphic.
  - Descriptive text outlining penalty policies and trust drops.
  - Acknowledgment CTA buttons.
- **API/Firebase calls**: Marks the user's warning status as acknowledged in Firestore.
- **Validation rules**: None.
- **Navigation actions**: Directs user back to main dashboard on confirm.
- **Error handling**: Retries warning flags on next app startup if acknowledgment fails.
- **Loading handling**: None.
- **Accessibility requirements**: Explicit spoken warnings on entry.
- **Animations/transitions**: Warning icon pulses softly.
- **Dark mode considerations**: Dark orange backgrounds `#7C2D12` featuring sharp warning alerts.
- **Reusable widgets extracted from this screen**: WarningHeader.

---

## # Development Todo List

### 🛠 UI Tasks
- [ ] Build the filterable `AlertsFeedScreen` layout.
- [ ] Implement `AlertDetailScreen` containing trust voting bars.
- [ ] Design the dynamic local forum layout for `IncidentDiscussionScreen`.
- [ ] Create the list structure for `LeaderboardScreen`.
- [ ] Build the contributor progress meters (`ReputationGauge`).
- [ ] Construct the overlay design for `FalseReportWarningScreen`.

### 🛠 ViewModel Tasks
- [ ] Implement `AlertsFeedViewModel` managing Firestore query cursors.
- [ ] Write `AlertDetailViewModel` handling upvote and downvote transaction locks.
- [ ] Implement `DiscussionViewModel` managing comments feeds.
- [ ] Design `ReputationViewModel` updating rank score systems.

### 🛠 Repository Tasks
- [ ] Design `CommunityRepository` contract handling social calls.
- [ ] Build localized Room database tables caching alerts and discussions.
- [ ] Implement cache synchronization policies.

### 🛠 Firebase Tasks
- [ ] Configure Firestore indexing on `/incidents` sorted by coordinates and timestamp.
- [ ] Setup Firestore triggers running reputation increases when a report is verified.

### 🛠 Navigation Tasks
- [ ] Configure routes linking Alerts list to Details and Comment sections.
- [ ] Implement argument transitions passing incident models through views.

### 🛠 Data Model Tasks
- [ ] Model `Comment`, `UserRank`, and `Vote` domain structures.
- [ ] Create Room `AlertCacheEntity` database models.

### 🛠 State Handling Tasks
- [ ] Expose state objects controlling lists refreshing loops.
- [ ] Build active validators restricting duplicate vote actions.

### 🛠 Testing Tasks
- [ ] Write unit tests verifying community trust score calculations.
- [ ] Implement integration tests validating paginated Firestore feeds.
- [ ] Write UI validation test for comment character limits.

---

## # Deliverables
- Fully completed alerts feed panel utilizing pagination.
- Social upvote and downvote structures updating incident active status.
- Real-time comment section integrated on active incidents.
- Leaderboard ranking displays showing local community scores.
- Dynamic contributor profiles displaying reputation rings and metrics.
- Operational warning screens for low trust user flags.

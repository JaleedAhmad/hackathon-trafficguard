Here is the **complete, end-to-end plan** to make the TrafficGuard Android app and Backend 100% fully functional and production-ready.

Currently, the core AI pipeline, persistent authentication, live incident fetching, and map rendering are working perfectly. However, the community engagement and gamification features are still mocked on the Android client, and the backend needs to expose endpoints to support them.

---

### Phase 1: Backend API Additions (`backend/`)

We need to implement standard REST endpoints to handle the remaining community features using Firestore.

1. **Voting API (`POST /report/{report_id}/vote`)**
   - **Purpose:** Allow users to confirm (upvote) or clear (downvote) an incident.
   - **Implementation:** Validate the user token. Create/update a document in a `votes` collection in Firestore. Update the `reputation` score of the user who originally submitted the report.

2. **Comments & Discussion API (`POST /report/{report_id}/comment` & `GET /report/{report_id}/comments`)**
   - **Purpose:** Allow drivers to discuss an incident (e.g., "Police has arrived", "Still flooded").
   - **Implementation:** Create a `comments` sub-collection under each report in Firestore. The GET endpoint will stream these ordered by timestamp.

3. **Gamification & Leaderboard API (`GET /leaderboard` & `GET /user/rank`)**
   - **Purpose:** Incentivize reporting by showing top contributors.
   - **Implementation:** Query a `users` collection in Firestore ordered by `reputation_score`. Return the top 10 for the leaderboard, and calculate the current user's relative rank.

4. **Fix Map Coordinates in Nearby Alerts (`GET /alerts/nearby`)**
   - **Purpose:** Fix the map markers so they don't stack on a default location.
   - **Implementation:** Currently, the backend `/alerts/nearby` endpoint returns distance, but omits the actual `lat` and `lng` of the alert. We need to expose `lat` and `lng` in the JSON response so the Android map can plot them accurately!

---

### Phase 2: Android Client Integration (`android/`)

Once the backend endpoints are live, we need to wire them up to the existing Android views and remove the local mocks.

1. **Update API Client (`TrafficGuardApiClient.kt`)**
   - Add new Retrofit data models (e.g., `CommentResponse`, `LeaderboardResponse`).
   - Add the corresponding Retrofit `@GET` and `@POST` interface methods for the voting, commenting, and leaderboard endpoints.
   - Update `NearbyAlert` data class to parse the new `lat` and `lng` fields.

2. **Remove Mocks in Community Repository (`CommunityRepositoryImpl.kt`)**
   - **Voting:** Replace the hardcoded `submitVote` and `getIncidentVoteStats` with the Retrofit API calls.
   - **Comments:** Replace the `localComments` list in `getCommentsStream` and `addComment` with actual API calls to fetch and post comments.
   - **Leaderboard:** Replace the static list of users in `getLeaderboard` and `getCurrentUserRank` with the API payload.
   - **Map Mapping:** Update `NearbyAlert.toIncident()` to use the actual `alert.lat` and `alert.lng` instead of defaulting to the center of Karachi/Islamabad.

3. **User Profile Persistence**
   - Currently, Firebase Auth manages the user dynamically, but we need to ensure their display name and reputation score map properly to the Dashboard and Leaderboard screens.

### Summary
The core app is extremely solid. The only remaining tasks are strictly standard CRUD operations (Voting, Comments, Leaderboard) connecting Android Retrofit to FastAPI/Firestore. 

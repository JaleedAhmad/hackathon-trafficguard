# TrafficGuard AI Backend

This is the backend service for the TrafficGuard AI Navigation application. It is built using FastAPI and runs an agentic pipeline to process community reports, detect crises, plan situations, and execute actions.

## Overview

The application utilizes a multi-agent system to handle incident reports:
1.  **Ingestion Agent:** Normalizes and parses incoming raw signals (e.g., text reports, social media, API inputs).
2.  **Trust Detection Agent:** Corroborates signals to detect true crises and filters out noise or false reports.
3.  **Situation Planning Agent:** Evaluates the severity and plans actionable responses based on trusted crisis data.
4.  **Execution Agent:** Logs the agent traces to Firebase and handles final dispatch actions.

---

## Environment Setup

To run this backend or integrate with it properly, ensure the following environment variables are set in a `.env` file at the root of the backend directory (refer to `.env.template`):
- `GEMINI_API_KEY`: API key for Gemini.
- `OLLAMA_CLOUD_API_KEY`: API key for Ollama Cloud (if used).
- `FIREBASE_CREDENTIALS_PATH`: Path to your `firebase_credentials.json` (defaults to `firebase_credentials.json`).

---

## Data Models (Schemas)

When integrating with Android (e.g., using Retrofit and Kotlin Data Classes), here are the exact schemas expected and returned by the APIs:

### 1. `RawSignal` (Request Body for POST `/report`)
```json
{
  "signal_id": "string",
  "text": "string",
  "source": "string",
  "lat": 0.0,
  "lng": 0.0,
  "timestamp": "string (ISO 8601 format)",
  "language": "string (optional)"
}
```

### 2. `SignalObject` (Returned as `ingestion_result`)
```json
{
  "signal_id": "string",
  "text_normalized": "string",
  "text_original": "string",
  "language_detected": "string",
  "source_type": "string",
  "lat": 0.0,
  "lng": 0.0,
  "timestamp": "string",
  "category_hint": "string"
}
```

### 3. `CrisisDetectionResult` (Returned as `trust_result`)
```json
{
  "crisis_detected": true,
  "status": "string",
  "crisis_type": "string",
  "confidence_score": 0,
  "reasoning": "string",
  "contradictions": ["string"]
}
```

### 4. `SituationPlanResult` (Returned as `situation_result`)
```json
{
  "severity": "string",
  "affected_radius_km": "string",
  "eta_impact_minutes": 0,
  "escalation_probability": "string",
  "actions": [
    {
      "action_id": "string",
      "priority": 0,
      "type": "string",
      "description": "string",
      "estimated_impact": "string",
      "requires_api": true
    }
  ],
  "ai_reasoning": "string"
}
```

---

## API Documentation

The following APIs are exposed for integration with client applications (e.g., Android Kotlin App). Base URL assumes local development unless deployed.

### 1. Submit a Report (Agent Pipeline Trigger)
Process a new traffic signal or incident report through the full AI agent pipeline.

-   **URL:** `/report`
-   **Method:** `POST`
-   **Headers:** `Content-Type: application/json`

**Request Body:**
```json
{
  "signal_id": "string (unique identifier)",
  "text": "string (the report content, e.g., 'Heavy flooding on Main St')",
  "source": "string (e.g., 'community_report', 'twitter')",
  "lat": float (latitude),
  "lng": float (longitude),
  "timestamp": "string (ISO 8601 format)",
  "language": "string (optional, e.g., 'en', 'ur')"
}
```

**Response (Success):**
Returns a JSON object detailing the results from each stage of the pipeline.
```json
{
  "report_id": "rep_1234567890",
  "status": "processed",
  "message": "Report received and processed by full pipeline",
  "estimated_processing_ms": 4500,
  "ingestion_result": { ... },
  "trust_result": { ... },
  "situation_result": { ... },
  "execution_result": { ... }
}
```

### 2. Get Current Crisis
Retrieves the currently active, aggregated crisis affecting the area.

-   **URL:** `/crisis/current`
-   **Method:** `GET`

**Response:**
```json
{
  "crisis_id": "c001",
  "location": "Canal Road, Karachi",
  "lat": 24.8607,
  "lng": 67.0011,
  "type": "urban_flooding",
  "confidence": 91,
  "severity": "High",
  "status": "ACTIVE",
  "affected_radius_km": 2.4,
  "eta_increase_mins": 21,
  "signals": [ ... ],
  "trust_analysis": { ... },
  "situation_plan": { ... }
}
```

### 3. Get Nearby Alerts
Retrieves alerts relevant to a specific location, intended for end-user notifications.

-   **URL:** `/alerts/nearby`
-   **Method:** `GET`
-   **Query Parameters:**
    -   `lat` (float, optional, default: 24.8607) - User's latitude
    -   `lng` (float, optional, default: 67.0011) - User's longitude

**Response:**
```json
{
  "alerts": [
    {
      "alert_id": "alt_001",
      "type": "urban_flooding",
      "message": "Heavy flooding detected on Canal Road. Avoid the area.",
      "message_urdu": "کینال روڈ پر شدید سیلاب۔ اس علاقے سے بچیں۔",
      "distance_km": 1.3,
      "severity": "High",
      "timestamp": "2026-05-16T09:30:00Z",
      "alternate_route": "Shahrah-e-Faisal"
    }
  ]
}
```

### 4. Get Agent Trace
Retrieves the most recent agent execution trace logs from Firebase. Useful for the simulation dashboard.

-   **URL:** `/agents/trace`
-   **Method:** `GET`

**Response:**
Returns a dictionary containing logs partitioned by Agent (Agent1, Agent2, Agent3, Agent4).

### 5. Baseline Comparison
Compares the output of a simple keyword heuristic model against the AI Agentic pipeline for a mock signal.

-   **URL:** `/baseline/compare`
-   **Method:** `GET`

**Response:**
```json
{
  "heuristic_result": { ... },
  "agentic_result": { ... },
  "raw_input_text": "string"
}
```

### Note on Simulation
The `/simulate` endpoint is **retired** as simulation is now dynamically handled by Agent 4 during the `/report` execution flow.

---

## Android Kotlin Integration Notes

To integrate these APIs into the Android app using libraries like Retrofit or Ktor:

1.  **Define Models:** Map the Request and Response structures (like `RawSignal`) into Kotlin data classes.
2.  **Network Client:** Set up a network client pointing to the backend host (e.g., `http://10.0.2.2:8000` for Android Emulator localhost, or your production IP).
3.  **POST `/report`:** Use this when a user submits an incident via the mobile app.
4.  **GET `/alerts/nearby`:** Use this for background location polling or refreshing the home screen to show active alerts near the user.

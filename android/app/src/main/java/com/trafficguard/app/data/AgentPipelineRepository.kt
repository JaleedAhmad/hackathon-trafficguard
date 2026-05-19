package com.traffic_guard.ai.data

import android.util.Log

/**
 * Repository for agent-pipeline specific endpoints:
 *  - GET /agents/trace
 *  - GET /baseline/compare
 */
class AgentPipelineRepository {

    private val tag = "AgentPipelineRepo"
    private val api = TrafficGuardApiClient.service

    /**
     * Fetches the latest agent execution trace from the backend.
     * Returns null on any network error.
     */
    suspend fun getAgentTrace(): Result<AgentTraceResponse> {
        return try {
            val response = api.getAgentTrace()
            Log.i(tag, "Agent trace loaded. Agent1=${response.agent1?.size ?: 0} steps")
            Result.Success(response)
        } catch (e: Exception) {
            Log.w(tag, "getAgentTrace failed: ${e.message}")
            Result.Error(Exception("Could not load agent trace: ${e.message}"))
        }
    }

    /**
     * Fetches the heuristic vs. AI pipeline comparison for a mock signal.
     */
    suspend fun getBaselineComparison(): Result<BaselineCompareResponse> {
        return try {
            val response = api.getBaselineComparison()
            Log.i(tag, "Baseline comparison loaded. agentic=${response.agenticResult?.detected}")
            Result.Success(response)
        } catch (e: Exception) {
            Log.w(tag, "getBaselineComparison failed: ${e.message}")
            Result.Error(Exception("Could not load baseline comparison: ${e.message}"))
        }
    }

    /**
     * Fetches the current active crisis.
     */
    suspend fun getCurrentCrisis(): Result<CurrentCrisisResponse> {
        return try {
            val response = api.getCurrentCrisis()
            Result.Success(response)
        } catch (e: Exception) {
            Log.w(tag, "getCurrentCrisis failed: ${e.message}")
            Result.Error(Exception("Could not load crisis data: ${e.message}"))
        }
    }
}

import { useState, useEffect, useCallback } from 'react'
import {
  AlertTriangle,
  RotateCw,
  Play,
  Pause,
  Activity,
  Shield,
  MapPin,
  Clock,
  Search,
  SlidersHorizontal,
  Flame,
  Radio,
  Cpu,
  TrendingUp,
  FileText
} from 'lucide-react'

interface Alert {
  alert_id: string
  type: string
  message: string
  distance_km: number
  severity: string
  timestamp: string
  lat: number
  lng: number
  confirmations: number
}

interface AgentStep {
  timestamp: string
  type: string
  message: string
  data?: any
}

interface AgentInfo {
  agent_name: string
  steps: AgentStep[]
}

interface AgentTrace {
  trace_id?: string
  crisis_id?: string
  signal_id?: string
  raw_text?: string
  source?: string
  language?: string
  confidence_score?: number
  crisis_detected?: boolean
  crisis_type?: string
  reasoning_short?: string
  rerouting_reasoning?: string
  execution_outcome?: string
  timestamp?: string
  agents?: AgentInfo[]
}

export default function App() {
  const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8000'

  const [alerts, setAlerts] = useState<Alert[]>([])
  const [trace, setTrace] = useState<AgentTrace | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [backendStatus, setBackendStatus] = useState<'online' | 'offline'>('online')

  // Auto-refresh state
  const [secondsLeft, setSecondsLeft] = useState(10)
  const [isPaused, setIsPaused] = useState(false)

  // Filters state
  const [searchTerm, setSearchTerm] = useState('')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [severityFilter, setSeverityFilter] = useState('ALL')

  const fetchData = useCallback(async () => {
    try {
      setLoading(true)
      // 1. Fetch alerts
      const alertsRes = await fetch(`${backendUrl}/alerts/nearby?lat=33.7220&lng=73.0580`)
      if (!alertsRes.ok) throw new Error('API server returned error code')
      const alertsData = await alertsRes.json()
      setAlerts(Array.isArray(alertsData.alerts) ? alertsData.alerts : [])

      // 2. Fetch agent trace
      const traceRes = await fetch(`${backendUrl}/agents/trace`)
      if (traceRes.ok) {
        const traceData = await traceRes.json()
        if (traceData && traceData.status !== 'no_trace_found_or_firebase_mocked') {
          setTrace(traceData)
        } else {
          setTrace(null)
        }
      }
      setBackendStatus('online')
      setError(null)
    } catch (err: any) {
      console.error(err)
      setError('Unable to reach the TrafficGuard backend service. Check connections.')
      setBackendStatus('offline')
    } finally {
      setLoading(false)
    }
  }, [backendUrl])

  // Initial load
  useEffect(() => {
    fetchData()
  }, [fetchData])

  // Auto-refresh countdown timer
  useEffect(() => {
    if (isPaused) return

    const timer = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          fetchData()
          return 10
        }
        return prev - 1
      })
    }, 1000)

    return () => clearInterval(timer)
  }, [isPaused, fetchData])

  const handleManualRefresh = () => {
    fetchData()
    setSecondsLeft(10)
  }

  // Filter logic
  const filteredAlerts = alerts.filter((alert) => {
    const matchesSearch = alert.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
      alert.type.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesType = typeFilter === 'ALL' || alert.type.toUpperCase() === typeFilter
    const matchesSeverity = severityFilter === 'ALL' || alert.severity.toUpperCase() === severityFilter.toUpperCase()
    return matchesSearch && matchesType && matchesSeverity
  })

  const sosCount = alerts.filter(a => a.type.toLowerCase() === 'sos').length
  const avgConfirmations = alerts.length > 0 
    ? (alerts.reduce((acc, a) => acc + (a.confirmations || 0), 0) / alerts.length).toFixed(1)
    : 0

  const ingestionAgentSteps = trace?.agents?.find(a => a.agent_name === 'IngestionAgent')?.steps || [];
  const trustAgentSteps = trace?.agents?.find(a => a.agent_name === 'TrustDetectionAgent')?.steps || [];
  const planningAgentSteps = trace?.agents?.find(a => a.agent_name === 'SituationPlanningAgent')?.steps || [];
  const executionAgentSteps = trace?.agents?.find(a => a.agent_name === 'ExecutionAgent')?.steps || [];

  const renderAgentLogs = (steps?: AgentStep[]) => {
    if (!steps || steps.length === 0) return null;
    return (
      <div className="mt-2.5 bg-black/60 border border-slate-900 rounded-lg p-2.5 font-mono text-[10px] space-y-1.5 max-h-36 overflow-y-auto">
        {steps.map((step, idx) => (
          <div key={idx} className="leading-relaxed">
            <span className="text-slate-500 select-none mr-1">
              [{step.timestamp ? new Date(step.timestamp).toLocaleTimeString() : 'Log'}]
            </span>
            <span className={step.type === 'DECISION' ? 'text-indigo-400 font-bold' : 'text-slate-300'}>
              {step.message}
            </span>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-[#030712] text-slate-100 p-6 font-sans">
      {/* Header Bar */}
      <header className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between border-b border-slate-800 pb-5 mb-8 gap-4">
        <div className="flex items-center gap-3">
          <div className="bg-red-600/10 p-2.5 rounded-xl border border-red-500/20">
            <Radio className="h-7 w-7 text-red-500 animate-pulse" />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2">
              TrafficGuard <span className="text-xs bg-red-600/20 text-red-400 border border-red-500/30 px-2 py-0.5 rounded-full font-bold">LIVE TELEMETRY</span>
            </h1>
            <p className="text-xs text-slate-400">Heuristic Engine and Agentic Pipeline Supervision Console</p>
          </div>
        </div>

        {/* Global Controls & Status */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Backend Status indicator */}
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs font-semibold ${
            backendStatus === 'online' 
              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
              : 'bg-red-500/10 text-red-400 border-red-500/20'
          }`}>
            <span className={`h-2 w-2 rounded-full ${backendStatus === 'online' ? 'bg-emerald-500 animate-ping' : 'bg-red-500'}`} />
            Backend: {backendStatus.toUpperCase()}
          </div>

          {/* Refresh Timer Widget */}
          <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 rounded-lg p-1">
            <button
              onClick={() => setIsPaused(!isPaused)}
              className="p-1 text-slate-400 hover:text-white hover:bg-slate-800 rounded transition"
              title={isPaused ? 'Resume Auto-refresh' : 'Pause Auto-refresh'}
            >
              {isPaused ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
            </button>
            <span className="text-xs px-2 text-slate-300 font-mono">
              {isPaused ? 'Auto-refresh paused' : `Refreshing in ${secondsLeft}s`}
            </span>
            <button
              onClick={handleManualRefresh}
              className="flex items-center gap-1.5 px-2.5 py-1 text-xs bg-slate-800 hover:bg-slate-700 text-white rounded font-medium transition"
            >
              <RotateCw className={`h-3 w-3 ${loading ? 'animate-spin' : ''}`} />
              Sync
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto space-y-6">
        {/* Connection Error Banner */}
        {error && (
          <div className="bg-red-950/20 border border-red-900/50 rounded-xl p-4 flex gap-3 text-red-200 text-sm">
            <AlertTriangle className="h-5 w-5 text-red-500 shrink-0" />
            <div>
              <span className="font-bold">Sync Error: </span> {error}
            </div>
          </div>
        )}

        {/* Stats Section */}
        <section className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-5 hover:border-slate-700/80 transition duration-300 relative overflow-hidden group">
            <div className="absolute right-3 top-3 opacity-10 group-hover:opacity-20 transition">
              <FileText className="h-12 w-12 text-slate-100" />
            </div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Total Hazards Feed</p>
            <h3 className="text-3xl font-black text-white mt-1.5">{alerts.length}</h3>
            <p className="text-xs text-slate-500 mt-2">Active road alerts reported</p>
          </div>

          <div className="bg-slate-950/60 border border-red-900/30 rounded-xl p-5 hover:border-red-900/50 transition duration-300 relative overflow-hidden group">
            <div className="absolute right-3 top-3 opacity-10 group-hover:opacity-20 transition">
              <Flame className="h-12 w-12 text-red-500" />
            </div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Active SOS Signals</p>
            <h3 className="text-3xl font-black text-red-500 mt-1.5">{sosCount}</h3>
            <p className="text-xs text-slate-500 mt-2">Critical emergency alerts in-range</p>
          </div>

          <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-5 hover:border-slate-700/80 transition duration-300 relative overflow-hidden group">
            <div className="absolute right-3 top-3 opacity-10 group-hover:opacity-20 transition">
              <TrendingUp className="h-12 w-12 text-slate-100" />
            </div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Avg Confirmations</p>
            <h3 className="text-3xl font-black text-white mt-1.5">{avgConfirmations}</h3>
            <p className="text-xs text-slate-500 mt-2">Upvotes per reported incident</p>
          </div>

          <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-5 hover:border-slate-700/80 transition duration-300 relative overflow-hidden group">
            <div className="absolute right-3 top-3 opacity-10 group-hover:opacity-20 transition">
              <Cpu className="h-12 w-12 text-slate-100" />
            </div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Agent Trace Status</p>
            <h3 className="text-lg font-black text-white mt-3 flex items-center gap-1.5">
              {trace ? (
                <>
                  <Activity className="h-4.5 w-4.5 text-emerald-500 animate-pulse" />
                  Active Ingestion
                </>
              ) : (
                <>
                  <Clock className="h-4.5 w-4.5 text-slate-400" />
                  Idle / Awaiting
                </>
              )}
            </h3>
            <p className="text-xs text-slate-500 mt-2">Pipeline execution trace logs</p>
          </div>
        </section>

        {/* Dashboard Grid Content */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Left panel: Incidents & SOS Alerts (7 Columns) */}
          <div className="lg:col-span-7 space-y-4">
            <div className="bg-slate-950/40 border border-slate-800 rounded-2xl p-5">
              <div className="flex flex-col md:flex-row md:items-center justify-between pb-4 border-b border-slate-800/60 gap-4 mb-4">
                <h2 className="text-lg font-extrabold text-white flex items-center gap-2">
                  <Activity className="h-5 w-5 text-red-500" />
                  Real-time Incidents & Alerts
                </h2>

                {/* Filter and search indicators */}
                <div className="flex items-center gap-1 text-xs text-slate-400 font-medium">
                  Showing {filteredAlerts.length} of {alerts.length} records
                </div>
              </div>

              {/* Filters Controls */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-5">
                <div className="relative">
                  <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-500" />
                  <input
                    type="text"
                    placeholder="Search logs..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full bg-slate-900/60 border border-slate-800 rounded-lg pl-8 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-slate-700 transition"
                  />
                </div>

                <div className="flex items-center bg-slate-900/60 border border-slate-800 rounded-lg px-2.5 py-1">
                  <SlidersHorizontal className="h-3 w-3 text-slate-500 mr-2" />
                  <select
                    value={typeFilter}
                    onChange={(e) => setTypeFilter(e.target.value)}
                    className="w-full bg-transparent text-xs text-slate-300 focus:outline-none border-none cursor-pointer"
                  >
                    <option value="ALL">All Types</option>
                    <option value="TRAFFIC">Traffic</option>
                    <option value="WEATHER">Weather</option>
                    <option value="SOS">SOS Emergency</option>
                    <option value="ACCIDENT">Accident</option>
                    <option value="CONSTRUCTION">Construction</option>
                  </select>
                </div>

                <div className="flex items-center bg-slate-900/60 border border-slate-800 rounded-lg px-2.5 py-1">
                  <SlidersHorizontal className="h-3 w-3 text-slate-500 mr-2" />
                  <select
                    value={severityFilter}
                    onChange={(e) => setSeverityFilter(e.target.value)}
                    className="w-full bg-transparent text-xs text-slate-300 focus:outline-none border-none cursor-pointer"
                  >
                    <option value="ALL">All Severities</option>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </div>
              </div>

              {/* Feed Card List */}
              <div className="space-y-3 max-h-[600px] overflow-y-auto pr-1">
                {filteredAlerts.length === 0 ? (
                  <div className="text-center py-12 border border-dashed border-slate-800 rounded-xl bg-slate-900/10">
                    <AlertTriangle className="h-8 w-8 text-slate-600 mx-auto mb-2" />
                    <p className="text-sm font-semibold text-slate-400">No matching incident logs found</p>
                    <p className="text-xs text-slate-500 mt-1">Adjust your filter options or report a test incident from the app.</p>
                  </div>
                ) : (
                  filteredAlerts.map((alert) => (
                    <div 
                      key={alert.alert_id} 
                      className={`p-4 rounded-xl border transition-all duration-200 hover:translate-x-1 ${
                        alert.type.toLowerCase() === 'sos'
                          ? 'bg-red-950/10 border-red-900/40 hover:border-red-900/60'
                          : 'bg-slate-900/30 border-slate-800/80 hover:border-slate-700/80'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-center gap-2">
                          <span className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full border ${
                            alert.type.toLowerCase() === 'sos'
                              ? 'bg-red-500/10 text-red-400 border-red-500/20'
                              : alert.type.toLowerCase() === 'weather'
                              ? 'bg-sky-500/10 text-sky-400 border-sky-500/20'
                              : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                          }`}>
                            {alert.type}
                          </span>
                          <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded border ${
                            alert.severity.toLowerCase() === 'critical' || alert.severity.toLowerCase() === 'high'
                              ? 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                              : 'bg-slate-800 text-slate-400 border-slate-700'
                          }`}>
                            {alert.severity}
                          </span>
                        </div>
                        <span className="text-[10px] text-slate-500 font-mono">
                          ID: {alert.alert_id.substring(0, 8)}
                        </span>
                      </div>

                      <p className="text-sm font-semibold text-white mt-2.5 leading-snug">
                        {alert.message}
                      </p>

                      <div className="flex flex-wrap items-center gap-y-2 gap-x-4 mt-4 pt-3 border-t border-slate-800/40 text-xs text-slate-400">
                        <div className="flex items-center gap-1 font-mono">
                          <MapPin className="h-3.5 w-3.5 text-slate-500" />
                          {alert.lat.toFixed(4)}, {alert.lng.toFixed(4)}
                        </div>
                        <div className="flex items-center gap-1">
                          <Clock className="h-3.5 w-3.5 text-slate-500" />
                          {alert.timestamp ? new Date(alert.timestamp).toLocaleTimeString() : 'Recent'}
                        </div>
                        <div className="flex items-center gap-1.5 ml-auto text-emerald-400 bg-emerald-500/5 px-2 py-0.5 rounded border border-emerald-500/10 font-bold">
                          <span>👍 {alert.confirmations} Upvotes</span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

          {/* Right panel: AI Agent Tracer pipeline (5 Columns) */}
          <div className="lg:col-span-5 space-y-4">
            <div className="bg-slate-950/40 border border-slate-800 rounded-2xl p-5">
              <h2 className="text-lg font-extrabold text-white flex items-center gap-2 pb-4 border-b border-slate-800/60 mb-5">
                <Cpu className="h-5 w-5 text-indigo-400" />
                AI Agent Pipeline Supervision
              </h2>

              {!trace ? (
                <div className="text-center py-16 border border-dashed border-slate-800 rounded-xl bg-slate-900/10">
                  <Shield className="h-9 w-9 text-slate-600 mx-auto mb-3" />
                  <p className="text-sm font-bold text-slate-300">Awaiting Agentic Trace Data</p>
                  <p className="text-xs text-slate-500 max-w-sm mx-auto mt-2 leading-relaxed">
                    Trace data will generate here in real-time when users report new incidents or SOS broadcasts requiring LLM ingestion and route planning.
                  </p>
                </div>
              ) : (
                <div className="space-y-5">
                  {/* Trace Header / Input */}
                  <div className="bg-slate-900/40 border border-slate-800 p-3.5 rounded-xl space-y-2">
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-slate-400 font-semibold">Incoming Signal Input:</span>
                      <span className="bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 font-mono text-[10px] px-2 py-0.5 rounded">
                        ID: {trace.signal_id?.substring(0, 8)}
                      </span>
                    </div>
                    <p className="text-xs italic text-slate-300 border-l-2 border-indigo-500/40 pl-3 leading-relaxed">
                      "{trace.raw_text}"
                    </p>
                    <div className="flex justify-between items-center text-[10px] text-slate-500 font-mono pt-1">
                      <span>Source: {trace.source}</span>
                      <span>Detected Lang: {trace.language}</span>
                    </div>
                  </div>

                  {/* Visual Timeline of Agent execution */}
                  <div className="space-y-4 relative before:absolute before:left-3.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-800">
                    
                    {/* Agent 1 Ingestion */}
                    <div className="flex gap-4 relative">
                      <div className="h-7 w-7 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 flex items-center justify-center shrink-0 text-xs font-black z-10">
                        1
                      </div>
                      <div className="bg-slate-900/20 border border-slate-800/80 p-3 rounded-xl flex-1">
                        <h4 className="text-xs font-black text-slate-200 flex justify-between">
                          Agent 1: Ingestion & Filter
                          <span className="text-emerald-400 font-bold text-[10px]">SUCCESS</span>
                        </h4>
                        <p className="text-[11px] text-slate-400 mt-1 leading-relaxed">
                          Cleaned and structured incident details, classified input text and extracted geo-coordinates.
                        </p>
                        {renderAgentLogs(ingestionAgentSteps)}
                      </div>
                    </div>

                    {/* Agent 2 Trust */}
                    <div className="flex gap-4 relative">
                      <div className="h-7 w-7 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/30 flex items-center justify-center shrink-0 text-xs font-black z-10">
                        2
                      </div>
                      <div className="bg-slate-900/20 border border-slate-800/80 p-3 rounded-xl flex-1">
                        <h4 className="text-xs font-black text-slate-200 flex justify-between">
                          Agent 2: Trust & Assessment
                          <span className="text-indigo-400 font-mono text-[10px]">
                            {trace.confidence_score ? `${trace.confidence_score}% Confidence` : 'Verified'}
                          </span>
                        </h4>
                        <div className="text-[11px] text-slate-400 mt-1.5 space-y-1">
                          <p>
                            <span className="font-bold text-slate-300">Crisis Type: </span>
                            {trace.crisis_type?.toUpperCase()}
                          </p>
                          <p className="italic text-slate-400 bg-slate-900/40 p-1.5 rounded text-[10px]">
                            {trace.reasoning_short}
                          </p>
                        </div>
                        {renderAgentLogs(trustAgentSteps)}
                      </div>
                    </div>

                    {/* Agent 3 Planning */}
                    {trace.rerouting_reasoning && (
                      <div className="flex gap-4 relative">
                        <div className="h-7 w-7 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/30 flex items-center justify-center shrink-0 text-xs font-black z-10">
                          3
                        </div>
                        <div className="bg-slate-900/20 border border-slate-800/80 p-3 rounded-xl flex-1">
                          <h4 className="text-xs font-black text-slate-200">
                            Agent 3: Route Planning Agent
                          </h4>
                          <p className="text-[11px] text-amber-300 bg-amber-500/5 p-2 border border-amber-500/10 rounded mt-1.5 leading-relaxed italic">
                            {trace.rerouting_reasoning}
                          </p>
                          {renderAgentLogs(planningAgentSteps)}
                        </div>
                      </div>
                    )}

                    {/* Agent 4 Execution */}
                    {trace.execution_outcome && (
                      <div className="flex gap-4 relative">
                        <div className="h-7 w-7 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/30 flex items-center justify-center shrink-0 text-xs font-black z-10">
                          4
                        </div>
                        <div className="bg-slate-900/20 border border-slate-800/80 p-3 rounded-xl flex-1">
                          <h4 className="text-xs font-black text-slate-200">
                            Agent 4: Execution & Notification
                          </h4>
                          <p className="text-[11px] text-slate-400 mt-1.5">
                            <span className="font-bold text-slate-300">Broadcast Action: </span>
                            {trace.execution_outcome}
                          </p>
                          {renderAgentLogs(executionAgentSteps)}
                        </div>
                      </div>
                    )}

                  </div>

                  {/* Trace timestamp info */}
                  <div className="text-[10px] text-slate-500 flex justify-between pt-2">
                    <span>Trace Telemetry Updated</span>
                    <span>{trace.timestamp ? new Date(trace.timestamp).toLocaleTimeString() : ''}</span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </section>
      </main>
    </div>
  )
}

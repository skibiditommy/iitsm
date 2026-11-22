import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { motion } from 'framer-motion';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';
import {
  Timer, Zap, GitFork, AlertOctagon, CheckCircle2, ChevronRight,
  ExternalLink, ShieldCheck, AlertTriangle, Layers, Edit3, Plus,
  RefreshCw, History, Server,
  Users, Activity, Check, X
} from 'lucide-react';

interface Analytics {
  totalIncidents: number;
  activeIncidents: number;
  resolvedIncidents: number;
  closedIncidents: number;
  slaBreachedCount: number;
  avgResolutionMinutes: number;
  avgSatisfactionRating: number;
  incidentsByStatus: Record<string, number>;
  incidentsByPriority: Record<string, number>;
  incidentsByCategory: Record<string, number>;
}

interface TechWorkload {
  id: number;
  name: string;
  currentWorkload: number;
  maxWorkload: number;
  utilizationPercent: number;
  available: boolean;
}

interface ServiceHealth {
  serviceId: number;
  serviceName: string;
  activeIncidentCount: number;
  slaBreachedCount: number;
  blastRadiusSize: number;
  healthStatus: string;
}

interface StrategyConfig {
  activePriorityStrategy: string;
  activeAssignmentStrategy: string;
  availablePriorityStrategies: string[];
  availableAssignmentStrategies: string[];
}

interface TicketRef {
  id: number;
  incidentId: string;
  title: string;
}

interface DependentPath {
  id: number;
  path: string[];
  activeTickets: TicketRef[];
}

interface ServiceImpact {
  id: number;
  name: string;
  activeTickets: TicketRef[];
  dependents: DependentPath[];
  affectedDependentCount: number;
}

interface RootCauseSuggestion {
  serviceId: number;
  serviceName: string;
  businessCriticality: number;
  blastRadiusCount: number;
  affectedDownstreamServicesCount: number;
  affectedDownstreamServiceNames: string[];
  activeIncidentIds: string[];
  confidenceScore: number;
  recommendation: string;
}

interface SlaRiskItem {
  incidentId: string;
  responseSlaStatus: string;
  resolutionSlaStatus: string;
  minutesToResponseDeadline: number;
  minutesToResolutionDeadline: number;
  riskLevel: string;
}

interface SlaPolicyItem {
  id: number;
  priority: string;
  serviceId: number | null;
  serviceName: string;
  responseMinutes: number;
  resolutionMinutes: number;
}

interface CategoryItem {
  id: number;
  name: string;
  description: string;
}

interface ServiceItem {
  id: number;
  name: string;
  description: string;
  businessCriticality: number;
  upstreamDependencies: { id: number; name: string }[];
  downstreamDependencies: { id: number; name: string }[];
  blastRadius: number;
}

interface IncidentItem {
  id: number;
  incidentId: string;
  title: string;
  calculatedPriority: string;
  status: string;
  category: string;
  assignedTechnicianName: string;
  responseSlaStatus: string;
  resolutionSlaStatus: string;
  createdAt: string;
}

interface AuditLogItem {
  id: number;
  timestamp: string;
  actorEmail: string;
  action: string;
  previousState: string;
  newState: string;
  description: string;
}

const COLORS = ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#ec4899', '#14b8a6'];

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [workloads, setWorkloads] = useState<TechWorkload[]>([]);
  const [health, setHealth] = useState<ServiceHealth[]>([]);
  const [strategies, setStrategies] = useState<StrategyConfig | null>(null);
  const [tab, setTab] = useState<'overview' | 'workloads' | 'services' | 'strategies' | 'sla-risk' | 'management'>('overview');

  // Service Dependency Explorer and Root Cause state
  const [serviceImpacts, setServiceImpacts] = useState<ServiceImpact[]>([]);
  const [rootCauses, setRootCauses] = useState<RootCauseSuggestion[]>([]);
  const [slaRisks, setSlaRisks] = useState<SlaRiskItem[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState<number | null>(null);

  // Configuration management items
  const [slaPolicies, setSlaPolicies] = useState<SlaPolicyItem[]>([]);
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [servicesList, setServicesList] = useState<ServiceItem[]>([]);
  const [incidentsList, setIncidentsList] = useState<IncidentItem[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLogItem[]>([]);
  const [auditFilter, setAuditFilter] = useState('');
  const [mgmtSection, setMgmtSection] = useState<'sla' | 'services' | 'categories' | 'technicians' | 'incidents' | 'audit'>('sla');

  // Inline SLA editor state
  const [editingPolicyId, setEditingPolicyId] = useState<number | null>(null);
  const [editRespMins, setEditRespMins] = useState<number>(30);
  const [editResolMins, setEditResolMins] = useState<number>(120);

  // Category creation
  const [newCatName, setNewCatName] = useState('');
  const [newCatDesc, setNewCatDesc] = useState('');

  // Service creation
  const [newServiceName, setNewServiceName] = useState('');
  const [newServiceDesc, setNewServiceDesc] = useState('');
  const [newServiceCrit, setNewServiceCrit] = useState<number>(3);
  const [newServiceUpstream, setNewServiceUpstream] = useState<number[]>([]);

  // Incident reassign state (incidentId -> technicianId)
  const [reassignMap, setReassignMap] = useState<Record<number, number>>({});

  // Management status alert
  const [mgmtMsg, setMgmtMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const refreshManagement = () => {
    api.get('/sla-policies').then((r) => setSlaPolicies(r.data || [])).catch(() => {});
    api.get('/categories').then((r) => setCategories(r.data || [])).catch(() => {});
    api.get('/services').then((r) => setServicesList(r.data || [])).catch(() => {});
    api.get('/analytics/technician-workloads').then((r) => setWorkloads(r.data || [])).catch(() => {});
    api.get('/incidents').then((r) => setIncidentsList(r.data || [])).catch(() => {});
    api.get('/audit').then((r) => setAuditLogs(r.data || [])).catch(() => {});
  };

  useEffect(() => {
    api.get('/analytics/overview').then((r) => setAnalytics(r.data)).catch(() => {});
    api.get('/analytics/technician-workloads').then((r) => setWorkloads(r.data)).catch(() => {});
    api.get('/analytics/service-health').then((r) => setHealth(r.data)).catch(() => {});
    api.get('/strategies').then((r) => setStrategies(r.data)).catch(() => {});
    api.get('/analytics/sla-risk').then((r) => setSlaRisks(r.data || [])).catch(() => {});

    // Management endpoints
    refreshManagement();

    // Fetch service impact and root cause suggestions
    api.get('/analytics/service-impacts').then((r) => {
      setServiceImpacts(r.data || []);
      if (r.data && r.data.length > 0) {
        const auth = r.data.find((s: ServiceImpact) => s.name.toLowerCase().includes('auth'));
        setSelectedServiceId(auth ? auth.id : r.data[0].id);
      }
    }).catch(() => {});

    api.get('/analytics/root-cause-suggestions').then((r) => setRootCauses(r.data || [])).catch(() => {});
  }, []);

  const handleUpdateSla = async (policyId: number) => {
    try {
      await api.put(`/sla-policies/${policyId}`, {
        responseMinutes: Number(editRespMins),
        resolutionMinutes: Number(editResolMins),
      });
      setMgmtMsg({ type: 'success', text: `SLA Policy #${policyId} updated successfully!` });
      setEditingPolicyId(null);
      refreshManagement();
    } catch (err: any) {
      setMgmtMsg({ type: 'error', text: err.response?.data?.message || 'Failed to update SLA policy' });
    }
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCatName.trim()) return;
    try {
      await api.post('/categories', { name: newCatName.trim(), description: newCatDesc.trim() });
      setMgmtMsg({ type: 'success', text: `Category "${newCatName}" created successfully!` });
      setNewCatName('');
      setNewCatDesc('');
      refreshManagement();
    } catch (err: any) {
      setMgmtMsg({ type: 'error', text: err.response?.data?.message || 'Failed to create category' });
    }
  };

  const handleCreateService = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newServiceName.trim()) return;
    try {
      await api.post('/services', {
        name: newServiceName.trim(),
        description: newServiceDesc.trim(),
        businessCriticality: Number(newServiceCrit),
        upstreamDependencyIds: newServiceUpstream,
      });
      setMgmtMsg({ type: 'success', text: `IT Service "${newServiceName}" registered successfully!` });
      setNewServiceName('');
      setNewServiceDesc('');
      setNewServiceCrit(3);
      setNewServiceUpstream([]);
      refreshManagement();
      api.get('/analytics/service-health').then((r) => setHealth(r.data)).catch(() => {});
    } catch (err: any) {
      setMgmtMsg({ type: 'error', text: err.response?.data?.message || 'Failed to create service' });
    }
  };

  const handleToggleTechAvailability = async (techId: number, currentAvailable: boolean) => {
    try {
      await api.put(`/technicians/${techId}`, { available: !currentAvailable });
      setMgmtMsg({ type: 'success', text: `Technician #${techId} status set to ${!currentAvailable ? 'Available' : 'Away'}` });
      refreshManagement();
    } catch (err: any) {
      setMgmtMsg({ type: 'error', text: 'Failed to update technician status' });
    }
  };

  const handleUpdateTechMaxWorkload = async (techId: number, currentMax: number, delta: number) => {
    const newMax = Math.max(1, currentMax + delta);
    try {
      await api.put(`/technicians/${techId}`, { maxWorkload: newMax });
      setMgmtMsg({ type: 'success', text: `Technician #${techId} max workload updated to ${newMax}` });
      refreshManagement();
    } catch (err: any) {
      setMgmtMsg({ type: 'error', text: 'Failed to update max workload' });
    }
  };

  const handleReassignIncident = async (incidentId: number) => {
    const techId = reassignMap[incidentId];
    if (!techId) {
      setMgmtMsg({ type: 'error', text: 'Please select a technician from the dropdown first.' });
      return;
    }
    try {
      await api.put(`/incidents/${incidentId}/reassign`, { technicianId: techId });
      setMgmtMsg({ type: 'success', text: `Incident #${incidentId} successfully reassigned!` });
      refreshManagement();
    } catch (err: any) {
      setMgmtMsg({ type: 'error', text: err.response?.data?.message || 'Failed to reassign incident' });
    }
  };

  const advanceTime = async (mins: number) => {
    await api.post('/demo/advance-time', { minutes: mins });
    await api.post('/demo/trigger-sla-check');
    api.get('/analytics/overview').then((r) => setAnalytics(r.data));
    api.get('/analytics/sla-risk').then((r) => setSlaRisks(r.data || []));
    alert(`⏩ [Demo Mode] Advanced ${mins} minutes & triggered SLA compliance verification`);
  };

  const switchStrategy = async (type: string, name: string) => {
    const res = await api.put('/strategies', { strategyType: type, strategyName: name });
    setStrategies(res.data);
  };

  if (!analytics) return <div className="text-center py-16 text-gray-400">Loading analytics...</div>;

  const priorityData = Object.entries(analytics.incidentsByPriority).map(([k, v]) => ({ name: k, value: v }));
  const categoryData = Object.entries(analytics.incidentsByCategory).map(([k, v]) => ({ name: k, count: v }));

  const tabs = [
    { key: 'overview', label: 'Overview' },
    { key: 'workloads', label: 'Workloads' },
    { key: 'services', label: 'Services & Root Causes' },
    { key: 'sla-risk', label: 'SLA Risk Detection' },
    { key: 'strategies', label: 'Strategies' },
    { key: 'management', label: 'System Configuration' },
  ] as const;

  const currentImpact = serviceImpacts.find((s) => s.id === selectedServiceId) || serviceImpacts[0];

  return (
    <div className="text-slate-900 dark:text-slate-100 transition-colors space-y-6">
      {/* Header Banner */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="glass-panel rounded-3xl p-6 sm:p-8 border border-slate-200/80 dark:border-slate-800/80 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4"
      >
        <div className="flex items-center gap-4">
          <div className="p-3.5 bg-gradient-to-tr from-indigo-600 to-purple-600 text-white rounded-2xl shadow-lg shadow-indigo-500/20">
            <Zap className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-[10px] font-bold text-indigo-600 dark:text-indigo-400 uppercase tracking-widest">Nexora Operations</span>
              <span className="text-slate-300 dark:text-slate-700">•</span>
              <span className="text-[10px] font-semibold text-slate-500 dark:text-slate-400">System Command & Governance</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight">Enterprise IT Administration Cockpit</h1>
            <p className="text-xs text-slate-500 mt-1 max-w-xl">
              Real-time analytics, runtime strategy swapping, service blast radius BFS graph, and SLA compliance monitoring.
            </p>
          </div>
        </div>

        <button
          onClick={refreshManagement}
          className="self-start sm:self-auto px-4 py-2.5 glass-panel hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-bold border border-slate-200/80 dark:border-slate-700/80 transition flex items-center gap-2 cursor-pointer shadow-xs"
        >
          <RefreshCw className="w-3.5 h-3.5 text-indigo-600" />
          <span>Sync State</span>
        </button>
      </motion.div>

      {/* Clearly Labeled Simulation Toolbar */}
      <div className="glass-panel rounded-2xl p-4 sm:p-5 border border-amber-500/30 bg-gradient-to-r from-amber-500/10 via-purple-500/10 to-indigo-500/10 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-amber-500 text-white shadow-sm shrink-0">
              <Timer className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-sm text-slate-900 dark:text-white">Time Acceleration Simulator</span>
                <span className="bg-amber-500/20 text-amber-700 dark:text-amber-300 text-[10px] font-bold px-2 py-0.5 rounded-full border border-amber-500/30">
                  Demo Tool
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                Simulates time passage to demonstrate SLA warnings, breaches, and automated 4-tier escalation chains.
              </p>
            </div>
          </div>

          <div className="flex gap-2">
            {[5, 15, 30, 60].map((m) => (
              <motion.button
                key={m}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => advanceTime(m)}
                className="bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-700 px-3.5 py-1.5 rounded-xl text-xs font-bold transition border border-slate-200 dark:border-slate-700 shadow-xs cursor-pointer text-slate-800 dark:text-slate-200"
              >
                +{m} min
              </motion.button>
            ))}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap gap-1.5 p-1.5 glass-panel rounded-2xl border border-slate-200/80 dark:border-slate-800/80 w-fit">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer relative ${
              tab === t.key
                ? 'text-white'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            {tab === t.key && (
              <motion.div
                layoutId="adminTab"
                className="absolute inset-0 bg-gradient-to-r from-indigo-600 to-purple-600 rounded-xl shadow-xs"
                transition={{ type: 'spring', stiffness: 400, damping: 30 }}
              />
            )}
            <span className="relative z-10">{t.label}</span>
          </button>
        ))}
      </div>

      {/* Overview Tab */}
      {tab === 'overview' && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
            {[
              { label: 'Total Incidents', value: analytics.totalIncidents, color: 'text-indigo-600 dark:text-indigo-400', border: 'border-slate-200/80 dark:border-slate-800' },
              { label: 'Active Incidents', value: analytics.activeIncidents, color: 'text-blue-600 dark:text-blue-400', border: 'border-blue-500/20' },
              {
                label: 'Resolved / Closed',
                value: analytics.resolvedIncidents + analytics.closedIncidents,
                color: 'text-emerald-600 dark:text-emerald-400',
                border: 'border-emerald-500/20',
              },
              { label: 'SLA Breached', value: analytics.slaBreachedCount, color: 'text-rose-600 dark:text-rose-400', border: 'border-rose-500/20' },
            ].map((s) => (
              <motion.div
                key={s.label}
                whileHover={{ y: -2 }}
                className={`glass-panel rounded-2xl p-5 border ${s.border} shadow-xs`}
              >
                <p className="text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">{s.label}</p>
                <p className={`text-3xl font-black mt-1 tracking-tight ${s.color}`}>{s.value}</p>
              </motion.div>
            ))}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="glass-panel rounded-3xl border border-slate-200/80 dark:border-slate-800/80 p-6 sm:p-7 shadow-xs">
              <h3 className="font-extrabold text-sm text-slate-900 dark:text-white uppercase tracking-wider mb-4">
                Incidents by Priority Distribution
              </h3>
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={priorityData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    outerRadius={85}
                    label
                  >
                    {priorityData.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                  <Legend />
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>

            <div className="glass-panel rounded-3xl border border-slate-200/80 dark:border-slate-800/80 p-6 sm:p-7 shadow-xs">
              <h3 className="font-extrabold text-sm text-slate-900 dark:text-white uppercase tracking-wider mb-4">
                Incidents by Service Category
              </h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={categoryData}>
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="count" fill="#6366f1" radius={[8, 8, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}

      {/* Workloads Tab */}
      {tab === 'workloads' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {workloads.map((t) => (
            <div key={t.id} className="bg-white dark:bg-slate-900 rounded-xl shadow border border-slate-200/80 dark:border-slate-800 p-6 text-slate-900 dark:text-slate-100">
              <h3 className="font-bold text-slate-900 dark:text-white">{t.name}</h3>
              <div className="mt-3">
                <div className="flex justify-between text-xs text-slate-500 dark:text-slate-400 mb-1 font-medium">
                  <span>Workload: {t.currentWorkload} / {t.maxWorkload}</span>
                  <span>{Math.round(t.utilizationPercent)}%</span>
                </div>
                <div className="w-full bg-slate-100 dark:bg-slate-800 rounded-full h-3 overflow-hidden">
                  <div
                    className={`h-3 rounded-full transition-all ${
                      t.utilizationPercent > 80
                        ? 'bg-red-500'
                        : t.utilizationPercent > 50
                        ? 'bg-amber-500'
                        : 'bg-green-500'
                    }`}
                    style={{ width: `${Math.min(t.utilizationPercent, 100)}%` }}
                  />
                </div>
              </div>
              <span
                className={`mt-3 inline-block text-xs font-semibold px-2.5 py-0.5 rounded-full ${
                  t.available ? 'bg-green-100 dark:bg-green-950/60 text-green-700 dark:text-green-300' : 'bg-red-100 dark:bg-red-950/60 text-red-700 dark:text-red-300'
                }`}
              >
                {t.available ? 'Available for Assignment' : 'At Capacity'}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Services & Root Causes Tab */}
      {tab === 'services' && (
        <div className="space-y-6">
          {/* Section 1: Automated Root-Cause Suggestions */}
          <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-indigo-100 dark:border-slate-800 p-6 transition-colors">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <AlertOctagon className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                  <span>Possible Shared Causes (Automated Root-Cause Suggestions)</span>
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                  Ranks upstream services by how many downstream dependent services have active incidents.
                </p>
              </div>
              <span className="text-xs bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 font-bold px-3 py-1 rounded-full border border-indigo-200 dark:border-indigo-800">
                AI Graph Analytics
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {rootCauses.map((rc, idx) => (
                <div
                  key={rc.serviceId}
                  className={`border rounded-xl p-4 transition-all ${
                    idx === 0
                      ? 'bg-gradient-to-br from-indigo-50/70 to-purple-50/50 dark:from-indigo-950/40 dark:to-purple-950/30 border-indigo-300 dark:border-indigo-800 ring-2 ring-indigo-200 dark:ring-indigo-900'
                      : 'bg-slate-50/60 dark:bg-slate-850/60 border-slate-200 dark:border-slate-800'
                  }`}
                >
                  <div className="flex justify-between items-center mb-2">
                    <div className="flex items-center gap-2">
                      <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                        idx === 0 ? 'bg-indigo-600 text-white' : 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300'
                      }`}>
                        #{idx + 1}
                      </span>
                      <h4 className="font-bold text-slate-900 dark:text-white">{rc.serviceName}</h4>
                    </div>
                    <span className="text-xs font-bold text-indigo-700 dark:text-indigo-300 bg-white dark:bg-slate-800 border border-indigo-200 dark:border-indigo-700 px-2 py-0.5 rounded-full" title="Ratio of impacted downstream services to blast radius">
                      Score: {rc.confidenceScore.toFixed(2)} ({rc.affectedDownstreamServicesCount}/{rc.blastRadiusCount} impacted)
                    </span>
                  </div>

                  <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed mt-2">{rc.recommendation}</p>

                  <div className="mt-3 pt-3 border-t border-slate-200/60 dark:border-slate-800 flex flex-wrap gap-2 text-xs">
                    <span className="bg-red-100 dark:bg-red-950/60 text-red-800 dark:text-red-300 px-2 py-0.5 rounded font-semibold border border-red-200 dark:border-red-800">
                      {rc.affectedDownstreamServicesCount} Downstream Services Affected
                    </span>
                    <span className="bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 px-2 py-0.5 rounded font-semibold border border-amber-200 dark:border-amber-800">
                      Blast Radius: {rc.blastRadiusCount}
                    </span>
                    <span className="bg-purple-100 dark:bg-purple-950/60 text-purple-800 dark:text-purple-300 px-2 py-0.5 rounded font-semibold border border-purple-200 dark:border-purple-800">
                      Criticality: {rc.businessCriticality}/5
                    </span>
                  </div>

                  {rc.affectedDownstreamServiceNames?.length > 0 && (
                    <div className="mt-2 text-[11px] text-slate-600 dark:text-slate-400">
                      <strong>Downstream paths:</strong> {rc.affectedDownstreamServiceNames.join(', ')}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Section 2: Service Dependency Explorer */}
          <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 mb-4 pb-3 border-b border-slate-200 dark:border-slate-800">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <GitFork className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                  <span>Service Dependency Explorer</span>
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Trace cascading impact paths and inspect linked downstream incident tickets.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <label className="text-xs font-bold text-slate-600 dark:text-slate-400 uppercase">Select Service:</label>
                <select
                  value={selectedServiceId || ''}
                  onChange={(e) => setSelectedServiceId(+e.target.value)}
                  className="border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-1.5 text-sm font-semibold text-slate-800 dark:text-slate-200 bg-slate-50 dark:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-400"
                >
                  {serviceImpacts.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.dependents.length} downstream paths)
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {currentImpact && (
              <div className="space-y-4">
                <div className="bg-indigo-50/50 dark:bg-indigo-950/40 p-4 rounded-xl border border-indigo-100 dark:border-indigo-900">
                  <div className="flex justify-between items-center">
                    <div>
                      <h4 className="font-bold text-indigo-900 dark:text-indigo-300 text-base">{currentImpact.name}</h4>
                      <p className="text-xs text-indigo-700 dark:text-indigo-400 mt-0.5">
                        Selected Upstream Service • {currentImpact.affectedDependentCount} affected downstream dependents
                      </p>
                    </div>
                  </div>

                  {currentImpact.activeTickets?.length > 0 ? (
                    <div className="mt-3 flex flex-wrap gap-2">
                      <span className="text-xs font-semibold text-indigo-900 dark:text-indigo-300 self-center">Direct Active Tickets:</span>
                      {currentImpact.activeTickets.map((t) => (
                        <button
                          key={t.id}
                          onClick={() => navigate(`/incidents/${t.id}`)}
                          className="inline-flex items-center gap-1 bg-white dark:bg-slate-800 hover:bg-indigo-100 dark:hover:bg-indigo-900/60 border border-indigo-200 dark:border-indigo-800 text-indigo-800 dark:text-indigo-300 text-xs px-2.5 py-1 rounded-lg font-medium transition cursor-pointer"
                        >
                          <span className="font-mono font-bold">{t.incidentId}</span>
                          <span className="truncate max-w-[150px]">{t.title}</span>
                          <ExternalLink className="w-3 h-3 text-indigo-500" />
                        </button>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 italic">No direct open incidents on this service.</p>
                  )}
                </div>

                <div>
                  <h4 className="text-xs font-bold text-slate-600 dark:text-slate-400 uppercase mb-3 tracking-wider">
                    Downstream Dependency Paths & Impacted Applications
                  </h4>

                  <div className="space-y-3">
                    {currentImpact.dependents?.map((dep, idx) => (
                      <div
                        key={idx}
                        className="p-4 bg-slate-50/70 dark:bg-slate-800/60 rounded-xl border border-slate-200 dark:border-slate-700 flex flex-col md:flex-row md:items-center justify-between gap-3 hover:bg-slate-50 dark:hover:bg-slate-800 transition"
                      >
                        <div className="flex flex-wrap items-center gap-1.5">
                          {dep.path.map((node, i) => (
                            <span key={i} className="flex items-center gap-1.5">
                              <span
                                className={`text-xs px-2.5 py-1 rounded-lg font-semibold ${
                                  i === 0
                                    ? 'bg-indigo-100 dark:bg-indigo-950/80 text-indigo-800 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800'
                                    : i === dep.path.length - 1
                                    ? 'bg-purple-100 dark:bg-purple-950/80 text-purple-800 dark:text-purple-300 border border-purple-200 dark:border-purple-800'
                                    : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300'
                                }`}
                              >
                                {node}
                              </span>
                              {i < dep.path.length - 1 && (
                                <ChevronRight className="w-4 h-4 text-slate-400 dark:text-slate-500" />
                              )}
                            </span>
                          ))}
                        </div>

                        <div className="flex flex-wrap items-center gap-2">
                          {dep.activeTickets && dep.activeTickets.length > 0 ? (
                            dep.activeTickets.map((t) => (
                              <button
                                key={t.id}
                                onClick={() => navigate(`/incidents/${t.id}`)}
                                className="inline-flex items-center gap-1 bg-white dark:bg-slate-800 hover:bg-red-50 dark:hover:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-xs px-2.5 py-1 rounded-lg font-medium transition shadow-xs cursor-pointer"
                                title={`Open ${t.incidentId}: ${t.title}`}
                              >
                                <span className="font-mono font-bold">{t.incidentId}</span>
                                <span className="truncate max-w-[140px]">{t.title}</span>
                                <ExternalLink className="w-3 h-3 text-red-500" />
                              </button>
                            ))
                          ) : (
                            <span className="text-xs text-emerald-700 dark:text-emerald-400 font-medium flex items-center gap-1 bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800 px-2 py-0.5 rounded">
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" /> Healthy
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* SLA Risk Detection Tab */}
      {tab === 'sla-risk' && (
        <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-200 dark:border-slate-800">
            <div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-amber-500" />
                <span>Proactive SLA Risk Detection</span>
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Flags active incidents approaching or violating SLA resolution windows.</p>
            </div>
            <span className="text-xs bg-amber-50 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 font-bold px-3 py-1 rounded-full border border-amber-200 dark:border-amber-800">
              {slaRisks.length} Monitored Tickets
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-50 dark:bg-slate-800/80 text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">
                <tr>
                  <th className="px-4 py-3 text-left">Incident ID</th>
                  <th className="px-4 py-3 text-left">Risk Level</th>
                  <th className="px-4 py-3 text-left">Response SLA</th>
                  <th className="px-4 py-3 text-left">Resolution SLA</th>
                  <th className="px-4 py-3 text-left">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-xs text-slate-800 dark:text-slate-200">
                {slaRisks.map((risk) => (
                  <tr key={risk.incidentId} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                    <td className="px-4 py-3 font-mono font-bold text-indigo-600 dark:text-indigo-400">{risk.incidentId}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded font-bold border ${
                        risk.riskLevel === 'HIGH' ? 'bg-red-100 dark:bg-red-950/60 text-red-800 dark:text-red-300 border-red-200 dark:border-red-800' :
                        risk.riskLevel === 'MEDIUM' ? 'bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 border-amber-200 dark:border-amber-800' : 'bg-green-100 dark:bg-green-950/60 text-green-800 dark:text-green-300 border-green-200 dark:border-green-800'
                      }`}>
                        {risk.riskLevel} RISK
                      </span>
                    </td>
                    <td className="px-4 py-3 font-medium">{risk.responseSlaStatus}</td>
                    <td className="px-4 py-3 font-medium">{risk.resolutionSlaStatus}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => navigate(`/incidents/by-display-id/${risk.incidentId}`)}
                        className="text-indigo-600 dark:text-indigo-400 hover:underline font-bold cursor-pointer"
                      >
                        Inspect & Intervene →
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Strategies Tab */}
      {tab === 'strategies' && strategies && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white dark:bg-slate-900 rounded-xl shadow border border-slate-200/80 dark:border-slate-800 p-6 transition-colors">
            <h3 className="font-bold text-slate-900 dark:text-white mb-2 flex items-center gap-2">
              <Zap className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
              <span>Priority Evaluation Strategy</span>
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">
              Active Strategy: <span className="font-bold text-indigo-600 dark:text-indigo-400">{strategies.activePriorityStrategy}</span>
            </p>
            <div className="space-y-2">
              {strategies.availablePriorityStrategies.map((s) => (
                <button
                  key={s}
                  onClick={() => switchStrategy('priority', s)}
                  className={`w-full text-left px-4 py-3 rounded-xl border transition-all cursor-pointer ${
                    s === strategies.activePriorityStrategy
                      ? 'bg-indigo-50 dark:bg-indigo-950/60 border-indigo-300 dark:border-indigo-700 ring-2 ring-indigo-100 dark:ring-indigo-900 font-semibold text-indigo-900 dark:text-indigo-200'
                      : 'border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                  }`}
                >
                  <div className="flex justify-between items-center">
                    <span>{s}</span>
                    {s === strategies.activePriorityStrategy && (
                      <span className="text-xs text-indigo-600 dark:text-indigo-400 font-bold bg-white dark:bg-slate-800 px-2 py-0.5 rounded-full border border-indigo-200 dark:border-indigo-700">
                        ● Active
                      </span>
                    )}
                  </div>
                </button>
              ))}
            </div>
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-xl shadow border border-slate-200/80 dark:border-slate-800 p-6 transition-colors">
            <h3 className="font-bold text-slate-900 dark:text-white mb-2 flex items-center gap-2">
              <Zap className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
              <span>Assignment Optimization Strategy</span>
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">
              Active Strategy: <span className="font-bold text-emerald-600 dark:text-emerald-400">{strategies.activeAssignmentStrategy}</span>
            </p>
            <div className="space-y-2">
              {strategies.availableAssignmentStrategies.map((s) => (
                <button
                  key={s}
                  onClick={() => switchStrategy('assignment', s)}
                  className={`w-full text-left px-4 py-3 rounded-xl border transition-all cursor-pointer ${
                    s === strategies.activeAssignmentStrategy
                      ? 'bg-emerald-50 dark:bg-emerald-950/60 border-emerald-300 dark:border-emerald-700 ring-2 ring-emerald-100 dark:ring-emerald-900 font-semibold text-emerald-900 dark:text-emerald-200'
                      : 'border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                  }`}
                >
                  <div className="flex justify-between items-center">
                    <span>{s}</span>
                    {s === strategies.activeAssignmentStrategy && (
                      <span className="text-xs text-emerald-600 dark:text-emerald-400 font-bold bg-white dark:bg-slate-800 px-2 py-0.5 rounded-full border border-emerald-200 dark:border-emerald-700">
                        ● Active
                      </span>
                    )}
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Management / Configuration Tab */}
      {tab === 'management' && (
        <div className="space-y-6">
          {/* Management feedback notification */}
          {mgmtMsg && (
            <div className={`p-4 rounded-xl text-xs flex items-center justify-between border ${
              mgmtMsg.type === 'success'
                ? 'bg-emerald-50 dark:bg-emerald-500/10 border-emerald-200 dark:border-emerald-500/30 text-emerald-800 dark:text-emerald-300'
                : 'bg-red-50 dark:bg-red-500/10 border-red-200 dark:border-red-500/30 text-red-800 dark:text-red-300'
            }`}>
              <div className="flex items-center gap-2">
                {mgmtMsg.type === 'success' ? <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400" /> : <AlertOctagon className="w-4 h-4 text-red-600 dark:text-red-400" />}
                <span className="font-medium">{mgmtMsg.text}</span>
              </div>
              <button onClick={() => setMgmtMsg(null)} className="p-1 hover:opacity-75 cursor-pointer">
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Sub-Navigation for Management Sections */}
          <div className="flex flex-wrap gap-2 border-b border-slate-200 dark:border-slate-800 pb-3">
            {[
              { id: 'sla', label: 'SLA Policies', icon: ShieldCheck },
              { id: 'services', label: 'IT Services & Blast Radius', icon: Server },
              { id: 'categories', label: 'Incident Categories', icon: Layers },
              { id: 'technicians', label: 'Technician Fleet', icon: Users },
              { id: 'incidents', label: 'All Incidents & Dispatch', icon: Activity },
              { id: 'audit', label: 'Immutable Audit Trail', icon: History },
            ].map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => setMgmtSection(id as any)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                  mgmtSection === id
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{label}</span>
              </button>
            ))}
          </div>

          {/* 1. SLA Policies Management */}
          {mgmtSection === 'sla' && (
            <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                    <ShieldCheck className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                    <span>Configured SLA Policies & Targets</span>
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                    Manage response and resolution target windows per priority level and IT service.
                  </p>
                </div>
                <button
                  onClick={refreshManagement}
                  className="p-2 text-slate-500 hover:text-slate-900 dark:hover:text-white rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-xs flex items-center gap-1 cursor-pointer"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Refresh</span>
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {slaPolicies.map((p) => {
                  const isEditing = editingPolicyId === p.id;
                  return (
                    <div key={p.id} className="border border-slate-200 dark:border-slate-700 rounded-xl p-4 bg-slate-50/50 dark:bg-slate-800/50 flex flex-col justify-between">
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${
                            p.priority === 'P1_CRITICAL' ? 'bg-red-100 dark:bg-red-500/20 text-red-700 dark:text-red-400' :
                            p.priority === 'P2_HIGH' ? 'bg-amber-100 dark:bg-amber-500/20 text-amber-700 dark:text-amber-400' :
                            p.priority === 'P3_MEDIUM' ? 'bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-blue-400' :
                            'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300'
                          }`}>
                            {p.priority}
                          </span>
                          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-medium">
                            {p.serviceName || 'Default (All Services)'}
                          </span>
                        </div>

                        {!isEditing ? (
                          <div className="space-y-1.5 text-xs text-slate-600 dark:text-slate-300 mt-3">
                            <p className="flex justify-between">
                              <span>Response Target:</span>
                              <strong className="text-slate-900 dark:text-white font-mono">{p.responseMinutes} mins</strong>
                            </p>
                            <p className="flex justify-between">
                              <span>Resolution Target:</span>
                              <strong className="text-slate-900 dark:text-white font-mono">{p.resolutionMinutes} mins</strong>
                            </p>
                          </div>
                        ) : (
                          <div className="space-y-2 mt-3 text-xs">
                            <div>
                              <label className="block text-[10px] font-semibold text-slate-500 uppercase">Response (mins):</label>
                              <input
                                type="number"
                                min="1"
                                value={editRespMins}
                                onChange={(e) => setEditRespMins(Number(e.target.value))}
                                className="w-full bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-600 rounded px-2 py-1 text-xs"
                              />
                            </div>
                            <div>
                              <label className="block text-[10px] font-semibold text-slate-500 uppercase">Resolution (mins):</label>
                              <input
                                type="number"
                                min="1"
                                value={editResolMins}
                                onChange={(e) => setEditResolMins(Number(e.target.value))}
                                className="w-full bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-600 rounded px-2 py-1 text-xs"
                              />
                            </div>
                          </div>
                        )}
                      </div>

                      <div className="mt-4 pt-3 border-t border-slate-200 dark:border-slate-700">
                        {!isEditing ? (
                          <button
                            onClick={() => {
                              setEditingPolicyId(p.id);
                              setEditRespMins(p.responseMinutes);
                              setEditResolMins(p.resolutionMinutes);
                            }}
                            className="w-full py-1.5 px-3 rounded-lg bg-indigo-50 dark:bg-indigo-500/10 hover:bg-indigo-100 dark:hover:bg-indigo-500/20 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-500/20 text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                            <span>Edit Target</span>
                          </button>
                        ) : (
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleUpdateSla(p.id)}
                              className="flex-1 py-1.5 px-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold flex items-center justify-center gap-1 cursor-pointer"
                            >
                              <Check className="w-3 h-3" />
                              <span>Save</span>
                            </button>
                            <button
                              onClick={() => setEditingPolicyId(null)}
                              className="py-1.5 px-2.5 rounded-lg bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 text-xs cursor-pointer"
                            >
                              <X className="w-3 h-3" />
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* 2. IT Services & Blast Radius Configuration */}
          {mgmtSection === 'services' && (
            <div className="space-y-6">
              <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors">
                <h3 className="font-bold text-slate-900 dark:text-white mb-2 flex items-center gap-2">
                  <Server className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                  <span>Register New IT Service</span>
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">
                  Define business criticality and upstream dependencies to maintain graph-theoretic blast radius calculations.
                </p>

                <form onSubmit={handleCreateService} className="space-y-3.5">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="sm:col-span-2">
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1">
                        Service Name *
                      </label>
                      <input
                        type="text"
                        value={newServiceName}
                        onChange={(e) => setNewServiceName(e.target.value)}
                        placeholder="e.g. Payment Gateway Service"
                        className="w-full bg-slate-50 dark:bg-slate-950/70 border border-slate-200 dark:border-slate-700/80 rounded-xl px-3.5 py-2 text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:border-indigo-500"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1">
                        Business Criticality (1-5)
                      </label>
                      <select
                        value={newServiceCrit}
                        onChange={(e) => setNewServiceCrit(Number(e.target.value))}
                        className="w-full bg-slate-50 dark:bg-slate-950/70 border border-slate-200 dark:border-slate-700/80 rounded-xl px-3.5 py-2 text-sm text-slate-900 dark:text-white focus:outline-none focus:border-indigo-500"
                      >
                        <option value="1">1 - Minimal Impact</option>
                        <option value="2">2 - Low Operational</option>
                        <option value="3">3 - Moderate Business</option>
                        <option value="4">4 - High Criticality</option>
                        <option value="5">5 - Mission Critical</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1">
                      Service Description
                    </label>
                    <input
                      type="text"
                      value={newServiceDesc}
                      onChange={(e) => setNewServiceDesc(e.target.value)}
                      placeholder="e.g. Invoicing and secure online credit/debit card transaction processor"
                      className="w-full bg-slate-50 dark:bg-slate-950/70 border border-slate-200 dark:border-slate-700/80 rounded-xl px-3.5 py-2 text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:border-indigo-500"
                    />
                  </div>

                  {servicesList.length > 0 && (
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                        Upstream Dependencies (Services this service depends on):
                      </label>
                      <div className="flex flex-wrap gap-2">
                        {servicesList.map((s) => {
                          const isSelected = newServiceUpstream.includes(s.id);
                          return (
                            <button
                              type="button"
                              key={s.id}
                              onClick={() => {
                                setNewServiceUpstream(isSelected
                                  ? newServiceUpstream.filter(id => id !== s.id)
                                  : [...newServiceUpstream, s.id]
                                );
                              }}
                              className={`px-2.5 py-1 rounded-lg text-xs font-medium border transition-all cursor-pointer ${
                                isSelected
                                  ? 'bg-indigo-600 text-white border-indigo-600'
                                  : 'bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700'
                              }`}
                            >
                              {s.name}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}

                  <button
                    type="submit"
                    className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 cursor-pointer shadow-xs"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Create & Map IT Service</span>
                  </button>
                </form>
              </div>

              {/* Service Inventory & Graph Metadata */}
              <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors">
                <h3 className="font-bold text-slate-900 dark:text-white mb-3 flex items-center justify-between">
                  <span>Registered Service Catalog ({servicesList.length})</span>
                  <button onClick={refreshManagement} className="text-xs text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1">
                    <RefreshCw className="w-3.5 h-3.5" /> Refresh
                  </button>
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                  {servicesList.map((s) => {
                    const svcHealth = health.find(h => h.serviceId === s.id);
                    return (
                      <div key={s.id} className="border border-slate-200 dark:border-slate-700 rounded-xl p-3.5 bg-slate-50/50 dark:bg-slate-800/50 space-y-1.5">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-xs text-slate-900 dark:text-white">{s.name}</span>
                          <div className="flex items-center gap-1.5">
                            {svcHealth && (
                              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${
                                svcHealth.healthStatus === 'HEALTHY' ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200' :
                                svcHealth.healthStatus === 'WARNING' ? 'bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border-amber-200' :
                                'bg-red-100 dark:bg-red-950/60 text-red-700 dark:text-red-300 border-red-200'
                              }`}>
                                {svcHealth.healthStatus}
                              </span>
                            )}
                            <span className="text-[10px] font-semibold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 px-2 py-0.5 rounded-full">
                              Tier {s.businessCriticality}
                            </span>
                          </div>
                        </div>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-1">{s.description || 'No description provided.'}</p>
                        <div className="pt-2 text-[11px] text-slate-600 dark:text-slate-400 border-t border-slate-200 dark:border-slate-700 space-y-0.5">
                          <p>Upstream: <strong className="text-slate-800 dark:text-slate-200">{s.upstreamDependencies?.length || 0}</strong></p>
                          <p>Downstream Blast: <strong className="text-slate-800 dark:text-slate-200">{s.blastRadius || 0} services</strong></p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* 3. Incident Categories */}
          {mgmtSection === 'categories' && (
            <div className="space-y-6">
              <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors">
                <h3 className="font-bold text-slate-900 dark:text-white mb-2 flex items-center gap-2">
                  <Layers className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                  <span>Configure Incident Categories</span>
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">
                  Categories power keyword routing, skill dispatch, and automatic classification.
                </p>

                <form onSubmit={handleCreateCategory} className="flex flex-col sm:flex-row gap-3">
                  <input
                    type="text"
                    value={newCatName}
                    onChange={(e) => setNewCatName(e.target.value)}
                    placeholder="Category Name (e.g. Cloud Infrastructure)"
                    className="flex-1 bg-slate-50 dark:bg-slate-950/70 border border-slate-200 dark:border-slate-700/80 rounded-xl px-3.5 py-2 text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:border-indigo-500"
                    required
                  />
                  <input
                    type="text"
                    value={newCatDesc}
                    onChange={(e) => setNewCatDesc(e.target.value)}
                    placeholder="Description (optional)"
                    className="flex-1 bg-slate-50 dark:bg-slate-950/70 border border-slate-200 dark:border-slate-700/80 rounded-xl px-3.5 py-2 text-sm text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:border-indigo-500"
                  />
                  <button
                    type="submit"
                    className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer shadow-xs whitespace-nowrap"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Add Category</span>
                  </button>
                </form>
              </div>

              <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors">
                <h3 className="font-bold text-slate-900 dark:text-white mb-3">Active System Categories ({categories.length})</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                  {categories.map((c) => (
                    <div key={c.id} className="border border-slate-200 dark:border-slate-700 rounded-xl p-3.5 bg-slate-50/50 dark:bg-slate-800/50">
                      <span className="font-bold text-xs text-indigo-700 dark:text-indigo-400">{c.name}</span>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">{c.description || 'No detailed description available.'}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* 4. Technician Fleet & Workload Administration */}
          {mgmtSection === 'technicians' && (
            <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                    <Users className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                    <span>Technician Fleet & Workload Control</span>
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                    Manage technician availability status, maximum concurrent capacity, and shift readiness.
                  </p>
                </div>
                <button onClick={refreshManagement} className="text-xs text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1">
                  <RefreshCw className="w-3.5 h-3.5" /> Refresh Roster
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-600 dark:text-slate-300">
                  <thead className="bg-slate-100 dark:bg-slate-800/60 uppercase text-[10px] text-slate-500 dark:text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-3 rounded-l-lg">Technician</th>
                      <th className="p-3">Current Workload</th>
                      <th className="p-3">Utilization</th>
                      <th className="p-3">Status</th>
                      <th className="p-3 text-right rounded-r-lg">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {workloads.map((t) => {
                      const util = t.utilizationPercent || (t.maxWorkload > 0 ? Math.round((t.currentWorkload / t.maxWorkload) * 100) : 0);
                      return (
                        <tr key={t.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                          <td className="p-3 font-semibold text-slate-900 dark:text-white">
                            {t.name}
                          </td>
                          <td className="p-3">
                            <span className="font-mono">{t.currentWorkload}</span> / <span className="font-mono">{t.maxWorkload} tickets</span>
                          </td>
                          <td className="p-3">
                            <div className="flex items-center gap-2">
                              <div className="w-20 bg-slate-200 dark:bg-slate-700 rounded-full h-1.5 overflow-hidden">
                                <div
                                  className={`h-full rounded-full ${
                                    util >= 100 ? 'bg-red-500' : util >= 70 ? 'bg-amber-500' : 'bg-emerald-500'
                                  }`}
                                  style={{ width: `${Math.min(util, 100)}%` }}
                                />
                              </div>
                              <span className="font-mono text-[11px]">{util}%</span>
                            </div>
                          </td>
                          <td className="p-3">
                            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold border ${
                              t.available
                                ? 'bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-200 dark:border-emerald-500/20'
                                : 'bg-slate-100 dark:bg-slate-800 text-slate-500 border-slate-200 dark:border-slate-700'
                            }`}>
                              <span className={`w-1.5 h-1.5 rounded-full ${t.available ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                              {t.available ? 'Available' : 'Away / Off-Shift'}
                            </span>
                          </td>
                          <td className="p-3 text-right">
                            <div className="inline-flex items-center gap-2">
                              {/* Capacity +/- buttons */}
                              <div className="inline-flex border border-slate-200 dark:border-slate-700 rounded-lg overflow-hidden">
                                <button
                                  onClick={() => handleUpdateTechMaxWorkload(t.id, t.maxWorkload, -1)}
                                  className="px-2 py-0.5 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 font-bold"
                                  title="Decrease Max Capacity"
                                >
                                  -
                                </button>
                                <span className="px-2 py-0.5 font-mono text-[11px] bg-slate-50 dark:bg-slate-850">
                                  Cap: {t.maxWorkload}
                                </span>
                                <button
                                  onClick={() => handleUpdateTechMaxWorkload(t.id, t.maxWorkload, 1)}
                                  className="px-2 py-0.5 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 font-bold"
                                  title="Increase Max Capacity"
                                >
                                  +
                                </button>
                              </div>

                              {/* Toggle Available Button */}
                              <button
                                onClick={() => handleToggleTechAvailability(t.id, t.available)}
                                className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold border transition-all cursor-pointer ${
                                  t.available
                                    ? 'bg-amber-50 dark:bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-200 dark:border-amber-500/20 hover:bg-amber-100'
                                    : 'bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-200 dark:border-emerald-500/20 hover:bg-emerald-100'
                                }`}
                              >
                                {t.available ? 'Set Away' : 'Set Available'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* 5. All Incidents & Dispatch Reassignment Cockpit */}
          {mgmtSection === 'incidents' && (
            <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                    <Activity className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                    <span>All-Incidents Dispatch & Manual Reassignment</span>
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                    Live ticket queue with administrative override to manually reassign tickets to available technicians.
                  </p>
                </div>
                <button onClick={refreshManagement} className="text-xs text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1">
                  <RefreshCw className="w-3.5 h-3.5" /> Refresh Queue
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-600 dark:text-slate-300">
                  <thead className="bg-slate-100 dark:bg-slate-800/60 uppercase text-[10px] text-slate-500 dark:text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-3 rounded-l-lg">ID</th>
                      <th className="p-3">Title</th>
                      <th className="p-3">Priority</th>
                      <th className="p-3">Status</th>
                      <th className="p-3">Assigned Tech</th>
                      <th className="p-3">SLA Status</th>
                      <th className="p-3 text-right rounded-r-lg">Manual Reassign</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {incidentsList.map((inc) => (
                      <tr key={inc.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                        <td className="p-3">
                          <button
                            onClick={() => navigate(`/incidents/${inc.id}`)}
                            className="font-mono font-bold text-indigo-600 dark:text-indigo-400 hover:underline"
                          >
                            {inc.incidentId}
                          </button>
                        </td>
                        <td className="p-3 font-medium text-slate-900 dark:text-white max-w-xs truncate">
                          {inc.title}
                        </td>
                        <td className="p-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            inc.calculatedPriority === 'P1_CRITICAL' ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' :
                            inc.calculatedPriority === 'P2_HIGH' ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300' :
                            inc.calculatedPriority === 'P3_MEDIUM' ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' :
                            'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                          }`}>
                            {inc.calculatedPriority}
                          </span>
                        </td>
                        <td className="p-3">
                          <span className="font-semibold text-[11px]">{inc.status}</span>
                        </td>
                        <td className="p-3 text-slate-700 dark:text-slate-200">
                          {inc.assignedTechnicianName || 'Unassigned'}
                        </td>
                        <td className="p-3">
                          <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${
                            inc.resolutionSlaStatus === 'BREACHED' ? 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300' :
                            inc.resolutionSlaStatus === 'PAUSED' ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/40 dark:text-purple-300' :
                            inc.resolutionSlaStatus === 'WARNING' ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300' :
                            'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'
                          }`}>
                            {inc.resolutionSlaStatus}
                          </span>
                        </td>
                        <td className="p-3 text-right">
                          <div className="inline-flex items-center gap-1.5">
                            <select
                              value={reassignMap[inc.id] || ''}
                              onChange={(e) => setReassignMap({ ...reassignMap, [inc.id]: Number(e.target.value) })}
                              className="bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded px-2 py-1 text-[11px] text-slate-900 dark:text-white"
                            >
                              <option value="">Select Tech...</option>
                              {workloads.map((w) => (
                                <option key={w.id} value={w.id}>
                                  {w.name} ({w.currentWorkload}/{w.maxWorkload})
                                </option>
                              ))}
                            </select>
                            <button
                              onClick={() => handleReassignIncident(inc.id)}
                              className="px-2.5 py-1 rounded bg-indigo-600 hover:bg-indigo-700 text-white text-[11px] font-semibold cursor-pointer shadow-xs"
                            >
                              Reassign
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* 6. Immutable Audit Trail */}
          {mgmtSection === 'audit' && (
            <div className="bg-white dark:bg-slate-900 rounded-xl shadow-md border border-slate-200/80 dark:border-slate-800 p-6 transition-colors space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                    <History className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                    <span>Immutable System Audit Trail</span>
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                    Append-only audit repository. Records are architecturally restricted against update or deletion.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={auditFilter}
                    onChange={(e) => setAuditFilter(e.target.value)}
                    placeholder="Filter actor or action..."
                    className="bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-900 dark:text-white placeholder-slate-400"
                  />
                  <button onClick={refreshManagement} className="p-1.5 text-slate-500 hover:text-slate-900 dark:hover:text-white border border-slate-200 dark:border-slate-700 rounded-lg">
                    <RefreshCw className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              <div className="overflow-x-auto max-h-[500px]">
                <table className="w-full text-left text-xs text-slate-600 dark:text-slate-300">
                  <thead className="sticky top-0 bg-slate-100 dark:bg-slate-800 uppercase text-[10px] text-slate-500 dark:text-slate-400 tracking-wider">
                    <tr>
                      <th className="p-3 rounded-l-lg">Timestamp</th>
                      <th className="p-3">Actor</th>
                      <th className="p-3">Action</th>
                      <th className="p-3">State Transition</th>
                      <th className="p-3 rounded-r-lg">Details</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {auditLogs
                      .filter(a =>
                        !auditFilter ||
                        a.actorEmail.toLowerCase().includes(auditFilter.toLowerCase()) ||
                        a.action.toLowerCase().includes(auditFilter.toLowerCase()) ||
                        a.description?.toLowerCase().includes(auditFilter.toLowerCase())
                      )
                      .slice()
                      .reverse()
                      .map((log) => (
                        <tr key={log.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                          <td className="p-3 font-mono text-[11px] text-slate-500 dark:text-slate-400 whitespace-nowrap">
                            {log.timestamp ? new Date(log.timestamp).toLocaleString() : 'N/A'}
                          </td>
                          <td className="p-3 font-mono text-[11px] text-indigo-700 dark:text-indigo-400">
                            {log.actorEmail}
                          </td>
                          <td className="p-3">
                            <span className="font-bold text-[10px] bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded border border-slate-200 dark:border-slate-700">
                              {log.action}
                            </span>
                          </td>
                          <td className="p-3 font-mono text-[11px]">
                            {log.previousState && log.newState ? (
                              <span>
                                {log.previousState} <span className="text-slate-400">→</span> <span className="font-bold text-slate-900 dark:text-white">{log.newState}</span>
                              </span>
                            ) : (
                              <span className="text-slate-400">—</span>
                            )}
                          </td>
                          <td className="p-3 text-slate-700 dark:text-slate-300 max-w-sm truncate">
                            {log.description}
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

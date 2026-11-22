import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { motion } from 'framer-motion';
import {
  Activity, AlertTriangle, Clock, Calendar, Image, Video,
  Search, ArrowRight, CheckCircle2, AlertOctagon, Check,
  RefreshCw, Wrench
} from 'lucide-react';

interface Incident {
  id: number;
  incidentId: string;
  title: string;
  calculatedPriority: string;
  status: string;
  category: string;
  responseSlaStatus: string;
  resolutionSlaStatus: string;
  resolutionDeadline?: string;
  attachmentType?: string;
  attachmentName?: string;
  createdAt: string;
}

export default function TechnicianDashboard() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState<'ALL' | 'CRITICAL' | 'IN_PROGRESS' | 'BREACHED'>('ALL');
  const navigate = useNavigate();

  const fetchAssigned = () => {
    setLoading(true);
    api.get('/incidents/assigned')
      .then((r) => setIncidents(r.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchAssigned();
  }, []);

  const priorityOrder = ['P1', 'P2', 'P3', 'P4'];
  const sorted = [...incidents].sort(
    (a, b) => priorityOrder.indexOf(a.calculatedPriority) - priorityOrder.indexOf(b.calculatedPriority)
  );

  const filtered = sorted.filter((inc) => {
    if (activeTab === 'CRITICAL' && inc.calculatedPriority !== 'P1' && inc.calculatedPriority !== 'P2') {
      return false;
    }
    if (activeTab === 'IN_PROGRESS' && inc.status !== 'IN_PROGRESS') {
      return false;
    }
    if (activeTab === 'BREACHED' && inc.responseSlaStatus !== 'BREACHED' && inc.resolutionSlaStatus !== 'BREACHED') {
      return false;
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const match =
        inc.incidentId.toLowerCase().includes(q) ||
        inc.title.toLowerCase().includes(q) ||
        (inc.category && inc.category.toLowerCase().includes(q));
      if (!match) return false;
    }
    return true;
  });

  const criticalCount = incidents.filter((i) => i.calculatedPriority === 'P1' || i.calculatedPriority === 'P2').length;
  const inProgressCount = incidents.filter((i) => i.status === 'IN_PROGRESS').length;
  const breachedCount = incidents.filter(
    (i) => i.responseSlaStatus === 'BREACHED' || i.resolutionSlaStatus === 'BREACHED'
  ).length;

  const priorityStyles: Record<string, { badge: string; dot: string; ping?: boolean }> = {
    P1: { badge: 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/20', dot: 'bg-rose-500', ping: true },
    P2: { badge: 'bg-orange-500/10 text-orange-700 dark:text-orange-400 border-orange-500/20', dot: 'bg-orange-500' },
    P3: { badge: 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/20', dot: 'bg-amber-500' },
    P4: { badge: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/20', dot: 'bg-emerald-500' },
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="glass-panel rounded-3xl p-6 sm:p-8 border border-slate-200/80 dark:border-slate-800/80 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4"
      >
        <div className="flex items-center gap-4">
          <div className="p-3.5 bg-gradient-to-tr from-amber-500 to-amber-600 text-white rounded-2xl shadow-lg shadow-amber-500/20">
            <Wrench className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-[10px] font-bold text-indigo-600 dark:text-indigo-400 uppercase tracking-widest">
                Nexora Operations
              </span>
              <span className="text-slate-300 dark:text-slate-700">•</span>
              <span className="text-[10px] font-semibold text-slate-500 dark:text-slate-400">
                L2 Support Specialization
              </span>
            </div>
            <div className="flex items-center gap-2.5">
              <h1 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight">
                Technician Cockpit
              </h1>
              <span className="text-[10px] font-bold bg-amber-500/10 text-amber-700 dark:text-amber-300 border border-amber-500/20 px-2.5 py-0.5 rounded-full flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
                Live SLA Monitoring
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-xl">
              Assigned incident queue organized by urgency, SLA deadlines, and domain skills.
            </p>
          </div>
        </div>

        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={fetchAssigned}
          className="self-start md:self-auto px-4 py-2.5 glass-panel hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-bold border border-slate-200/80 dark:border-slate-700/80 transition-all flex items-center gap-2 cursor-pointer shadow-xs"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-indigo-600' : ''}`} />
          <span>Refresh Queue</span>
        </motion.button>
      </motion.div>

      {/* KPI Metrics Strip */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <motion.div
          whileHover={{ y: -2 }}
          className="glass-panel rounded-2xl p-4 sm:p-5 border border-slate-200/80 dark:border-slate-800/80 shadow-xs flex items-center justify-between"
        >
          <div>
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 block">Total Assigned</span>
            <span className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white mt-0.5 block">{incidents.length}</span>
          </div>
          <div className="p-3 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border border-indigo-500/20">
            <Activity className="w-5 h-5" />
          </div>
        </motion.div>

        <motion.div
          whileHover={{ y: -2 }}
          className="glass-panel rounded-2xl p-4 sm:p-5 border border-rose-500/20 shadow-xs flex items-center justify-between"
        >
          <div>
            <span className="text-xs font-semibold text-rose-700 dark:text-rose-400 block">Critical P1/P2</span>
            <span className="text-2xl sm:text-3xl font-black text-rose-600 dark:text-rose-400 mt-0.5 block">{criticalCount}</span>
          </div>
          <div className="p-3 rounded-xl bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20">
            <AlertOctagon className="w-5 h-5" />
          </div>
        </motion.div>

        <motion.div
          whileHover={{ y: -2 }}
          className="glass-panel rounded-2xl p-4 sm:p-5 border border-amber-500/20 shadow-xs flex items-center justify-between"
        >
          <div>
            <span className="text-xs font-semibold text-amber-700 dark:text-amber-400 block">In Investigation</span>
            <span className="text-2xl sm:text-3xl font-black text-amber-600 dark:text-amber-400 mt-0.5 block">{inProgressCount}</span>
          </div>
          <div className="p-3 rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
            <Clock className="w-5 h-5" />
          </div>
        </motion.div>

        <motion.div
          whileHover={{ y: -2 }}
          className="glass-panel rounded-2xl p-4 sm:p-5 border border-slate-200/80 dark:border-slate-800/80 shadow-xs flex items-center justify-between"
        >
          <div>
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 block">SLA Breached</span>
            <span className={`text-2xl sm:text-3xl font-black mt-0.5 block ${breachedCount > 0 ? 'text-rose-600 dark:text-rose-400' : 'text-slate-900 dark:text-white'}`}>
              {breachedCount}
            </span>
          </div>
          <div className="p-3 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-700">
            <AlertTriangle className="w-5 h-5" />
          </div>
        </motion.div>
      </div>

      {/* Filter Tabs and Search Bar */}
      <div className="glass-panel rounded-2xl p-3 border border-slate-200/80 dark:border-slate-800/80 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-3">
        {/* Animated Tabs */}
        <div className="flex items-center gap-1.5 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
          {[
            { id: 'ALL', label: `All Tickets (${incidents.length})` },
            { id: 'CRITICAL', label: `Critical P1/P2 (${criticalCount})` },
            { id: 'IN_PROGRESS', label: `In Progress (${inProgressCount})` },
            { id: 'BREACHED', label: `SLA Breached (${breachedCount})` },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all shrink-0 cursor-pointer relative ${
                activeTab === tab.id
                  ? 'text-white'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              {activeTab === tab.id && (
                <motion.div
                  layoutId="techTab"
                  className={`absolute inset-0 rounded-xl shadow-xs ${
                    tab.id === 'CRITICAL' ? 'bg-rose-600' : tab.id === 'BREACHED' ? 'bg-rose-700' : tab.id === 'IN_PROGRESS' ? 'bg-amber-600' : 'bg-slate-900 dark:bg-indigo-600'
                  }`}
                  transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                />
              )}
              <span className="relative z-10">{tab.label}</span>
            </button>
          ))}
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-72">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search ticket ID, title, or category..."
            className="w-full pl-9 pr-3 py-1.5 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {/* Incident Queue Cards */}
      <div className="space-y-3">
        {filtered.map((i) => {
          const badge = priorityStyles[i.calculatedPriority] || { badge: 'bg-slate-100 text-slate-700 border-slate-300', dot: 'bg-slate-400' };
          const isBreached = i.responseSlaStatus === 'BREACHED' || i.resolutionSlaStatus === 'BREACHED';

          return (
            <motion.div
              key={i.id}
              whileHover={{ y: -2, scale: 1.005 }}
              whileTap={{ scale: 0.995 }}
              onClick={() => navigate(`/incidents/${i.id}`)}
              className="glass-panel rounded-2xl border border-slate-200/80 dark:border-slate-800/80 hover:border-indigo-400/80 dark:hover:border-indigo-500/80 hover:shadow-lg transition-all duration-150 p-4 sm:p-5 cursor-pointer group"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="space-y-2 flex-1 min-w-0">
                  {/* Meta Chips */}
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs font-extrabold text-indigo-600 dark:text-indigo-400 bg-indigo-500/10 px-2.5 py-0.5 rounded-full border border-indigo-500/20">
                      {i.incidentId}
                    </span>

                    <span className={`text-[11px] font-extrabold px-2.5 py-0.5 rounded-full border ${badge.badge} flex items-center gap-1.5`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${badge.dot} ${badge.ping ? 'animate-ping' : ''}`} />
                      {i.calculatedPriority}
                    </span>

                    <span className="text-[11px] font-semibold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 px-2.5 py-0.5 rounded-full border border-slate-200 dark:border-slate-700">
                      {i.status}
                    </span>

                    {isBreached ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 dark:text-rose-400 bg-rose-500/10 border border-rose-500/20 px-2 py-0.5 rounded-full">
                        <AlertTriangle className="w-3 h-3 text-rose-500" />
                        SLA Breached
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 dark:text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-full">
                        <CheckCircle2 className="w-3 h-3 text-emerald-500" />
                        SLA On Track
                      </span>
                    )}

                    {i.attachmentType && (
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-slate-600 dark:text-slate-400 bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 px-2 py-0.5 rounded-full">
                        {i.attachmentType.startsWith('video/') ? (
                          <Video className="w-3 h-3 text-indigo-500" />
                        ) : (
                          <Image className="w-3 h-3 text-indigo-500" />
                        )}
                        <span>Evidence</span>
                      </span>
                    )}
                  </div>

                  {/* Title */}
                  <h3 className="text-base font-bold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                    {i.title}
                  </h3>

                  {/* Details / Deadlines */}
                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 dark:text-slate-400">
                    <span>
                      Domain: <strong className="text-slate-700 dark:text-slate-300 font-semibold">{i.category || 'General IT'}</strong>
                    </span>

                    {i.resolutionDeadline && (
                      <span className="inline-flex items-center gap-1.5 font-semibold text-amber-800 dark:text-amber-300 bg-amber-500/10 border border-amber-500/20 px-2.5 py-0.5 rounded-full">
                        <Calendar className="w-3 h-3 text-amber-500" />
                        Target: {new Date(i.resolutionDeadline).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                      </span>
                    )}
                  </div>
                </div>

                {/* Right Action */}
                <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center gap-2 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100 dark:border-slate-800">
                  <span className="text-[11px] text-slate-400 flex items-center gap-1 font-mono">
                    <Clock className="w-3 h-3" />
                    {i.createdAt ? new Date(i.createdAt).toLocaleDateString() : ''}
                  </span>
                  <div className="inline-flex items-center gap-1 text-xs font-bold text-indigo-600 dark:text-indigo-400 group-hover:translate-x-1 transition-transform">
                    <span>Inspect Ticket</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </div>
                </div>
              </div>
            </motion.div>
          );
        })}

        {!loading && filtered.length === 0 && (
          <div className="text-center py-16 glass-panel rounded-3xl border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mx-auto mb-3">
              <Check className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-slate-800 dark:text-slate-200 text-sm">No incidents in this queue</h4>
            <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
              Your assignment queue is clear for the selected filter.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

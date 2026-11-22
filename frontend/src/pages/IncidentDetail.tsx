import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import api from '../api';
import { motion } from 'framer-motion';
import {
  ArrowLeft, Clock, Calendar, CheckCircle2, Download,
  Image as ImageIcon, Video as VideoIcon, UserCheck, AlertTriangle,
  RotateCcw, HelpCircle, Copy, AlertOctagon, Sparkles, Check,
  Send, Star
} from 'lucide-react';

const LIFECYCLE_STATES = [
  'NEW',
  'CLASSIFIED',
  'ASSIGNED',
  'ACKNOWLEDGED',
  'IN_PROGRESS',
  'RESOLVED',
  'VERIFIED',
  'CLOSED',
];

export default function IncidentDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [incident, setIncident] = useState<any>(null);
  const [suggestions, setSuggestions] = useState<any[]>([]);
  const [duplicates, setDuplicates] = useState<any[]>([]);
  const [comment, setComment] = useState('');
  const [resolveForm, setResolveForm] = useState({ rootCause: '', resolution: '', preventiveMeasures: '' });
  const [verifyForm, setVerifyForm] = useState({ feedback: '', rating: 5 });

  // Reopen and Info Request state
  const [showReopen, setShowReopen] = useState(false);
  const [reopenReason, setReopenReason] = useState('');
  const [showRequestInfo, setShowRequestInfo] = useState(false);
  const [infoQuestion, setInfoQuestion] = useState('');

  // Admin deadline management state
  const [deadlineInput, setDeadlineInput] = useState('');
  const [deadlineReason, setDeadlineReason] = useState('');
  const [deadlineLoading, setDeadlineLoading] = useState(false);
  const [deadlineSuccess, setDeadlineSuccess] = useState('');
  const [copiedId, setCopiedId] = useState(false);

  const load = () => {
    api.get(`/incidents/${id}`).then((r) => {
      setIncident(r.data);
      if (r.data.resolutionDeadline) {
        const d = new Date(r.data.resolutionDeadline);
        const iso = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
        setDeadlineInput(iso);
      }
    });
    api.get(`/incidents/${id}/suggestions`).then((r) => setSuggestions(r.data)).catch(() => {});
    api.get(`/incidents/${id}/duplicates`).then((r) => setDuplicates(r.data)).catch(() => {});
  };

  useEffect(load, [id]);

  if (!incident) {
    return (
      <div className="flex flex-col items-center justify-center py-24">
        <div className="w-10 h-10 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin" />
        <p className="mt-4 text-xs font-semibold text-slate-500">Loading incident lifecycle data...</p>
      </div>
    );
  }

  const action = async (endpoint: string, body?: any) => {
    await api.put(`/incidents/${id}/${endpoint}`, body || {});
    load();
  };

  const handleReopen = async () => {
    if (!reopenReason.trim()) return;
    await api.put(`/incidents/${id}/reopen`, { reason: reopenReason });
    setShowReopen(false);
    setReopenReason('');
    load();
  };

  const handleRequestInfo = async () => {
    if (!infoQuestion.trim()) return;
    await api.put(`/incidents/${id}/request-info`, { question: infoQuestion });
    setShowRequestInfo(false);
    setInfoQuestion('');
    load();
  };

  const addComment = async () => {
    if (!comment.trim()) return;
    await api.post(`/incidents/${id}/comments`, { content: comment, internal: false });
    setComment('');
    load();
  };

  const handleUpdateDeadline = async (customDateIso?: string) => {
    setDeadlineLoading(true);
    setDeadlineSuccess('');
    try {
      const targetDate = customDateIso || (deadlineInput ? new Date(deadlineInput).toISOString() : new Date(Date.now() + 4 * 3600000).toISOString());
      await api.put(`/incidents/${id}/deadline`, {
        deadline: targetDate,
        reason: deadlineReason || 'Admin updated SLA resolution target deadline',
      });
      setDeadlineSuccess('Deadline updated successfully!');
      setTimeout(() => setDeadlineSuccess(''), 4000);
      load();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update deadline');
    }
    setDeadlineLoading(false);
  };

  const addHoursToDeadline = (hours: number) => {
    const d = new Date(Date.now() + hours * 3600000);
    const isoLocal = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    setDeadlineInput(isoLocal);
    handleUpdateDeadline(d.toISOString());
  };

  const copyIncidentId = () => {
    navigator.clipboard.writeText(incident.incidentId);
    setCopiedId(true);
    setTimeout(() => setCopiedId(false), 2000);
  };

  const priorityStyles: Record<string, { badge: string; dot: string }> = {
    P1: { badge: 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/20', dot: 'bg-rose-500' },
    P2: { badge: 'bg-orange-500/10 text-orange-700 dark:text-orange-400 border-orange-500/20', dot: 'bg-orange-500' },
    P3: { badge: 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/20', dot: 'bg-amber-500' },
    P4: { badge: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/20', dot: 'bg-emerald-500' },
  };

  const isAtRisk = incident.responseSlaStatus === 'AT_RISK' || incident.resolutionSlaStatus === 'AT_RISK';
  const isBreached = incident.responseSlaStatus === 'BREACHED' || incident.resolutionSlaStatus === 'BREACHED';

  const currentStateIndex = LIFECYCLE_STATES.indexOf(incident.status);

  return (
    <div className="space-y-6 pb-12">
      {/* Top Navigation */}
      <div className="flex items-center justify-between">
        <motion.button
          whileHover={{ x: -2 }}
          whileTap={{ scale: 0.98 }}
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-2 text-xs font-bold text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 glass-panel px-3.5 py-2 rounded-xl border border-slate-200/80 dark:border-slate-800/80 cursor-pointer shadow-xs"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Dashboard</span>
        </motion.button>

        <div className="flex items-center gap-2">
          <button
            onClick={copyIncidentId}
            className="inline-flex items-center gap-1.5 text-xs font-mono font-bold text-slate-600 dark:text-slate-300 glass-panel px-3 py-1.5 rounded-xl border border-slate-200/80 dark:border-slate-800/80 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer shadow-xs"
            title="Click to copy Ticket ID"
          >
            {copiedId ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{incident.incidentId}</span>
          </button>
        </div>
      </div>

      {/* 9-State Lifecycle Visual Stepper */}
      <div className="glass-panel rounded-3xl p-6 border border-slate-200/80 dark:border-slate-800/80 shadow-xs overflow-x-auto">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <span className="text-[10px] font-bold text-indigo-600 dark:text-indigo-400 uppercase tracking-widest">
              State Pattern Engine
            </span>
            <span className="text-slate-300 dark:text-slate-700">•</span>
            <span className="text-xs font-semibold text-slate-500">9-State Finite Machine</span>
          </div>
          <span className="text-xs font-extrabold px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 border border-indigo-500/20">
            Current: {incident.status}
          </span>
        </div>

        <div className="flex items-center min-w-[700px] justify-between relative py-2">
          {/* Progress bar background */}
          <div className="absolute top-1/2 left-0 right-0 h-1 bg-slate-200 dark:bg-slate-800 -translate-y-1/2 z-0" />
          {/* Active progress fill */}
          <div
            className="absolute top-1/2 left-0 h-1 bg-gradient-to-r from-indigo-600 to-purple-600 -translate-y-1/2 z-0 transition-all duration-500"
            style={{
              width: `${Math.max(0, Math.min(100, (currentStateIndex / (LIFECYCLE_STATES.length - 1)) * 100))}%`,
            }}
          />

          {LIFECYCLE_STATES.map((st, idx) => {
            const isCompleted = currentStateIndex > idx || incident.status === 'CLOSED';
            const isCurrent = incident.status === st;

            return (
              <div key={st} className="relative z-10 flex flex-col items-center">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs transition-all duration-200 ${
                    isCurrent
                      ? 'bg-indigo-600 text-white ring-4 ring-indigo-500/20 scale-110 shadow-md'
                      : isCompleted
                      ? 'bg-emerald-500 text-white shadow-xs'
                      : 'bg-white dark:bg-slate-800 text-slate-400 border border-slate-300 dark:border-slate-700'
                  }`}
                >
                  {isCompleted ? <Check className="w-4 h-4" /> : idx + 1}
                </div>
                <span className={`text-[10px] font-bold mt-2 tracking-tight ${isCurrent ? 'text-indigo-600 dark:text-indigo-400 font-extrabold' : 'text-slate-500'}`}>
                  {st}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* SLA Alert Warnings */}
      {isBreached && (
        <motion.div
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-between shadow-xs"
        >
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-rose-500 text-white shadow-sm">
              <AlertOctagon className="w-5 h-5" />
            </div>
            <div>
              <p className="font-extrabold text-rose-900 dark:text-rose-200 text-sm">CRITICAL: SLA Breached</p>
              <p className="text-xs text-rose-700 dark:text-rose-300">
                This incident has exceeded targeted resolution window and is eligible for automatic 4-tier escalation.
              </p>
            </div>
          </div>
          <span className="text-xs font-mono bg-rose-500 text-white px-3 py-1 rounded-full font-black">
            BREACHED
          </span>
        </motion.div>
      )}

      {!isBreached && isAtRisk && (
        <motion.div
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-between shadow-xs"
        >
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-amber-500 text-white shadow-sm">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <p className="font-extrabold text-amber-900 dark:text-amber-200 text-sm">
                SLA Warning: Resolution Deadline Approaching
              </p>
              <p className="text-xs text-amber-700 dark:text-amber-300">
                Resolution target is within threshold. Immediate technician attention recommended.
              </p>
            </div>
          </div>
          <span className="text-xs font-mono bg-amber-500 text-white px-3 py-1 rounded-full font-black">
            AT RISK
          </span>
        </motion.div>
      )}

      {/* Duplicate Candidates Alert */}
      {duplicates.length > 0 && (
        <div className="glass-panel rounded-2xl p-4 border border-purple-500/30 bg-purple-500/5 shadow-xs">
          <div className="flex items-center gap-2 mb-2">
            <Copy className="w-4 h-4 text-purple-600 dark:text-purple-400" />
            <h4 className="font-bold text-sm text-purple-900 dark:text-purple-200">
              Duplicate Incident Candidates Detected (NLP Jaccard Engine)
            </h4>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mb-3">
            The following open tickets exhibit high semantic overlap with this issue:
          </p>
          <div className="flex flex-wrap gap-2">
            {duplicates.map((dup, i) => (
              <button
                key={i}
                onClick={() => navigate(`/incidents/by-display-id/${dup.incidentId}`)}
                className="inline-flex items-center gap-2 glass-panel hover:bg-purple-500/10 border border-purple-300 dark:border-purple-800 px-3 py-1.5 rounded-xl text-xs transition cursor-pointer shadow-xs"
              >
                <span className="font-mono font-bold text-purple-600 dark:text-purple-400">{dup.incidentId}</span>
                <span className="truncate max-w-[200px] text-slate-700 dark:text-slate-300 font-medium">{dup.title}</span>
                <span className="bg-purple-500/20 text-purple-700 dark:text-purple-300 font-extrabold px-1.5 py-0.5 rounded-full text-[10px]">
                  {Math.round(dup.similarityScore * 100)}% Match
                </span>
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Column */}
        <div className="lg:col-span-2 space-y-6">
          {/* Main Ticket Details Card */}
          <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
            <div className="flex justify-between items-start gap-4">
              <div>
                <div className="flex items-center gap-2 mb-2">
                  <span className="font-mono text-indigo-600 dark:text-indigo-400 font-extrabold text-sm bg-indigo-500/10 px-2.5 py-0.5 rounded-full border border-indigo-500/20">
                    {incident.incidentId}
                  </span>
                  {incident.reopenCount > 0 && (
                    <span className="bg-orange-500/10 text-orange-700 dark:text-orange-400 text-xs font-bold px-2 py-0.5 rounded-full flex items-center gap-1 border border-orange-500/20">
                      <RotateCcw className="w-3 h-3" /> Reopened ({incident.reopenCount}x)
                    </span>
                  )}
                </div>
                <h1 className="text-2xl font-black text-slate-900 dark:text-white leading-tight">
                  {incident.title}
                </h1>
                <p className="text-sm text-slate-600 dark:text-slate-300 mt-3 leading-relaxed whitespace-pre-wrap">
                  {incident.description}
                </p>
              </div>

              <div className="text-right shrink-0">
                <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black border ${priorityStyles[incident.calculatedPriority]?.badge || ''}`}>
                  <span className={`w-2 h-2 rounded-full ${priorityStyles[incident.calculatedPriority]?.dot || 'bg-slate-400'}`} />
                  {incident.calculatedPriority}
                </span>
                <p className="text-xs font-bold text-slate-500 mt-2 uppercase tracking-wider">
                  {incident.status}
                </p>
              </div>
            </div>

            {/* SLA & Attributes Matrix */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4 mt-6 p-4 sm:p-5 rounded-2xl bg-slate-50/80 dark:bg-slate-800/40 border border-slate-200/80 dark:border-slate-800 text-xs">
              <div>
                <span className="text-slate-500 dark:text-slate-400 font-semibold block">Category</span>
                <p className="font-bold text-slate-900 dark:text-white text-sm mt-0.5">{incident.category || '—'}</p>
              </div>
              <div>
                <span className="text-slate-500 dark:text-slate-400 font-semibold block">Service</span>
                <p className="font-bold text-slate-900 dark:text-white text-sm mt-0.5">{incident.affectedService || '—'}</p>
              </div>
              <div>
                <span className="text-slate-500 dark:text-slate-400 font-semibold block">Escalation Tier</span>
                <p className="font-bold text-slate-900 dark:text-white text-sm mt-0.5">{incident.escalationLevel}</p>
              </div>
              <div>
                <span className="text-slate-500 dark:text-slate-400 font-semibold block">Response SLA</span>
                <p className={`font-black text-sm mt-0.5 ${incident.responseSlaStatus === 'BREACHED' ? 'text-rose-600' : 'text-emerald-600'}`}>
                  {incident.responseSlaStatus}
                </p>
              </div>
              <div>
                <span className="text-slate-500 dark:text-slate-400 font-semibold block">Resolution SLA</span>
                <p className={`font-black text-sm mt-0.5 ${incident.resolutionSlaStatus === 'BREACHED' ? 'text-rose-600' : 'text-emerald-600'}`}>
                  {incident.resolutionSlaStatus}
                </p>
              </div>
              <div>
                <span className="text-slate-500 dark:text-slate-400 font-semibold block">Assigned Specialist</span>
                <p className="font-bold text-slate-900 dark:text-white text-sm mt-0.5 flex items-center gap-1">
                  <UserCheck className="w-3.5 h-3.5 text-indigo-600" />
                  {incident.assignedTechnicianName || 'Unassigned'}
                </p>
              </div>
            </div>

            {/* Target Resolution Deadline */}
            {incident.resolutionDeadline && (
              <div className="mt-4 p-3.5 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  <Clock className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                  <span className="font-bold text-amber-900 dark:text-amber-200">
                    Resolution Target Deadline: {new Date(incident.resolutionDeadline).toLocaleString()}
                  </span>
                </div>
                <span className="text-[11px] font-semibold text-amber-700 dark:text-amber-400">
                  {new Date(incident.resolutionDeadline) < new Date() ? 'Deadline passed' : 'Active countdown'}
                </span>
              </div>
            )}
          </div>

          {/* Evidence / Media File */}
          {incident.attachmentData && (
            <div className="glass-panel rounded-3xl p-6 border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
              <div className="flex justify-between items-center mb-4">
                <h3 className="font-bold text-slate-900 dark:text-white text-sm flex items-center gap-2">
                  {incident.attachmentType?.startsWith('video/') ? (
                    <VideoIcon className="w-4 h-4 text-indigo-600" />
                  ) : (
                    <ImageIcon className="w-4 h-4 text-indigo-600" />
                  )}
                  <span>Attached Visual Evidence ({incident.attachmentName || 'Media File'})</span>
                </h3>

                <a
                  href={incident.attachmentData}
                  download={incident.attachmentName || 'attachment'}
                  className="inline-flex items-center gap-1 text-xs text-indigo-600 dark:text-indigo-400 font-bold glass-panel px-3 py-1.5 rounded-xl border border-indigo-200 dark:border-indigo-800 transition cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" /> Download
                </a>
              </div>

              <div className="rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-700 bg-slate-100 dark:bg-slate-950 flex items-center justify-center p-2">
                {incident.attachmentType?.startsWith('video/') ? (
                  <video controls src={incident.attachmentData} className="max-h-96 w-full rounded-xl bg-black">
                    Your browser does not support video playback.
                  </video>
                ) : (
                  <img
                    src={incident.attachmentData}
                    alt={incident.attachmentName || 'Screenshot'}
                    className="max-h-96 w-auto object-contain rounded-xl"
                  />
                )}
              </div>
            </div>
          )}

          {/* Admin Custom Deadline Setter */}
          {user?.role === 'ADMIN' && (
            <div className="glass-panel rounded-3xl p-6 border border-indigo-500/20 shadow-xs">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-bold text-slate-900 dark:text-white text-sm flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
                  <span>Admin: SLA Target Deadline Adjustment</span>
                </h3>
                {deadlineSuccess && (
                  <span className="text-xs text-emerald-600 font-bold bg-emerald-500/10 px-2.5 py-1 rounded-full flex items-center gap-1 border border-emerald-500/20">
                    <CheckCircle2 className="w-3.5 h-3.5" /> {deadlineSuccess}
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-500 mb-4">
                Manually adjust deadline for vendor patch windows or executive escalations.
              </p>

              <div className="flex flex-wrap gap-2 mb-4">
                <button
                  type="button"
                  onClick={() => addHoursToDeadline(2)}
                  className="px-3 py-1.5 text-xs bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-500/20 rounded-xl font-bold border border-indigo-500/20 cursor-pointer"
                >
                  +2 Hours
                </button>
                <button
                  type="button"
                  onClick={() => addHoursToDeadline(4)}
                  className="px-3 py-1.5 text-xs bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-500/20 rounded-xl font-bold border border-indigo-500/20 cursor-pointer"
                >
                  +4 Hours
                </button>
                <button
                  type="button"
                  onClick={() => addHoursToDeadline(24)}
                  className="px-3 py-1.5 text-xs bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-500/20 rounded-xl font-bold border border-indigo-500/20 cursor-pointer"
                >
                  +24 Hours
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
                <input
                  type="datetime-local"
                  value={deadlineInput}
                  onChange={(e) => setDeadlineInput(e.target.value)}
                  className="w-full bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-900 dark:text-white"
                />
                <input
                  type="text"
                  placeholder="Audit reason / change note..."
                  value={deadlineReason}
                  onChange={(e) => setDeadlineReason(e.target.value)}
                  className="w-full bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-900 dark:text-white"
                />
              </div>

              <button
                type="button"
                disabled={deadlineLoading}
                onClick={() => handleUpdateDeadline()}
                className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl transition cursor-pointer"
              >
                {deadlineLoading ? 'Applying...' : 'Save & Recalibrate SLA'}
              </button>
            </div>
          )}

          {/* Workflow Actions */}
          <div className="glass-panel rounded-3xl p-6 border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
            <h3 className="font-bold text-slate-900 dark:text-white text-sm mb-4">
              State Pattern Actions
            </h3>

            {/* PENDING_USER Status Notification Banner */}
            {incident.status === 'PENDING_USER' && (
              <div className="w-full bg-amber-500/10 border border-amber-500/30 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                <div className="flex items-start gap-2.5">
                  <AlertTriangle className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />
                  <div>
                    <h4 className="font-bold text-xs text-amber-900 dark:text-amber-200">
                      Awaiting User Diagnostic Details • SLA Clock Paused
                    </h4>
                    <p className="text-[11px] text-amber-700 dark:text-amber-300 mt-0.5">
                      The technician requested additional logs. Click resume below to continue resolution.
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => action('resume', { reason: 'Clarification details submitted' })}
                  className="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shrink-0 cursor-pointer shadow-xs"
                >
                  Resume Investigation
                </button>
              </div>
            )}

            <div className="flex flex-wrap gap-2.5">
              {incident.status === 'PENDING_USER' && (
                <button
                  onClick={() => action('resume', { reason: 'Manual resumption' })}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                >
                  Resume Work
                </button>
              )}
              {user?.role === 'ADMIN' && incident.status === 'CLASSIFIED' && (
                <button
                  onClick={() => action('assign')}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                >
                  Auto-Assign (Smart Composite Strategy)
                </button>
              )}
              {user?.role === 'TECHNICIAN' && incident.status === 'ASSIGNED' && (
                <button
                  onClick={() => action('acknowledge')}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                >
                  Acknowledge Incident
                </button>
              )}
              {user?.role === 'TECHNICIAN' && incident.status === 'ACKNOWLEDGED' && (
                <button
                  onClick={() => action('investigate')}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                >
                  Begin Investigation
                </button>
              )}
              {user?.role === 'TECHNICIAN' && (incident.status === 'IN_PROGRESS' || incident.status === 'ACKNOWLEDGED') && (
                <button
                  onClick={() => setShowRequestInfo(!showRequestInfo)}
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                >
                  <HelpCircle className="w-3.5 h-3.5" /> Request Info from Reporter
                </button>
              )}

              {['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS'].includes(incident.status) && (
                <button
                  onClick={() => action('escalate', { reason: 'Manual escalation triggered' })}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                >
                  Escalate Incident
                </button>
              )}
            </div>

            {/* Request Info Dropdown Form */}
            {showRequestInfo && (
              <div className="w-full bg-purple-500/10 p-4 rounded-2xl border border-purple-500/20 space-y-3 mt-3">
                <h4 className="font-bold text-xs text-purple-900 dark:text-purple-200">Request Diagnostic Info from Reporter</h4>
                <input
                  placeholder="e.g. Please provide your browser version, OS, or screenshot..."
                  value={infoQuestion}
                  onChange={(e) => setInfoQuestion(e.target.value)}
                  className="w-full bg-white dark:bg-slate-800 border border-purple-300 dark:border-purple-700 rounded-xl px-3.5 py-2 text-xs text-slate-900 dark:text-white"
                />
                <div className="flex justify-end gap-2">
                  <button
                    onClick={() => setShowRequestInfo(false)}
                    className="px-3 py-1.5 border border-purple-300 dark:border-purple-700 rounded-xl text-xs text-slate-700 dark:text-slate-300 bg-white dark:bg-slate-800 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleRequestInfo}
                    className="px-4 py-1.5 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                  >
                    Dispatch Request
                  </button>
                </div>
              </div>
            )}

            {/* Technician Resolve Form */}
            {user?.role === 'TECHNICIAN' && incident.status === 'IN_PROGRESS' && (
              <div className="w-full space-y-3 p-5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 mt-4">
                <h4 className="font-bold text-xs text-slate-900 dark:text-white uppercase tracking-wider">
                  Complete & Resolve Incident
                </h4>
                <input
                  placeholder="Root Cause (e.g. Expired SSL certificate on auth gateway)"
                  value={resolveForm.rootCause}
                  onChange={(e) => setResolveForm({ ...resolveForm, rootCause: e.target.value })}
                  className="w-full bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl px-3.5 py-2 text-xs text-slate-900 dark:text-white"
                />
                <input
                  placeholder="Resolution (e.g. Renewed certificate and restarted gateway proxy)"
                  value={resolveForm.resolution}
                  onChange={(e) => setResolveForm({ ...resolveForm, resolution: e.target.value })}
                  className="w-full bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl px-3.5 py-2 text-xs text-slate-900 dark:text-white"
                />
                <input
                  placeholder="Preventive Measures (e.g. Configured auto-renew cron and monitoring alert)"
                  value={resolveForm.preventiveMeasures}
                  onChange={(e) => setResolveForm({ ...resolveForm, preventiveMeasures: e.target.value })}
                  className="w-full bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl px-3.5 py-2 text-xs text-slate-900 dark:text-white"
                />
                <button
                  onClick={() => action('resolve', resolveForm)}
                  className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold cursor-pointer"
                >
                  Submit Resolution
                </button>
              </div>
            )}

            {/* Employee Verification Form */}
            {user?.role === 'EMPLOYEE' && incident.status === 'RESOLVED' && (
              <div className="w-full space-y-4 p-5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 mt-4">
                <h4 className="font-bold text-xs text-emerald-900 dark:text-emerald-200 uppercase tracking-wider">
                  Verify Resolution & Employee Feedback
                </h4>
                <input
                  placeholder="Verification Feedback (e.g. Verified resolution, service is responsive)"
                  value={verifyForm.feedback}
                  onChange={(e) => setVerifyForm({ ...verifyForm, feedback: e.target.value })}
                  className="w-full bg-white dark:bg-slate-800 border border-emerald-300 dark:border-emerald-700 rounded-xl px-3.5 py-2 text-xs text-slate-900 dark:text-white"
                />
                <div className="flex items-center gap-3">
                  <span className="text-xs font-bold text-slate-700 dark:text-slate-300">Satisfaction:</span>
                  <div className="flex gap-1">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setVerifyForm({ ...verifyForm, rating: star })}
                        className="p-1 cursor-pointer"
                      >
                        <Star className={`w-5 h-5 ${star <= verifyForm.rating ? 'fill-amber-400 text-amber-400' : 'text-slate-300'}`} />
                      </button>
                    ))}
                  </div>
                </div>
                <div className="flex items-center gap-3 pt-2">
                  <button
                    onClick={() => action('verify', verifyForm)}
                    className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold cursor-pointer shadow-xs"
                  >
                    Verify & Close Incident
                  </button>
                  <button
                    onClick={() => setShowReopen(!showReopen)}
                    className="px-4 py-2.5 bg-orange-500/10 text-orange-700 dark:text-orange-400 hover:bg-orange-500/20 border border-orange-500/20 rounded-xl text-xs font-bold flex items-center gap-1.5 cursor-pointer"
                  >
                    <RotateCcw className="w-3.5 h-3.5" /> Reject Resolution & Reopen
                  </button>
                </div>

                {showReopen && (
                  <div className="p-3.5 rounded-xl bg-white dark:bg-slate-900 border border-orange-500/30 space-y-2 mt-2">
                    <p className="text-xs font-bold text-orange-900 dark:text-orange-300">Reason for resolution rejection:</p>
                    <input
                      placeholder="e.g. Issue still occurs when accessing via VPN..."
                      value={reopenReason}
                      onChange={(e) => setReopenReason(e.target.value)}
                      className="w-full bg-slate-50 dark:bg-slate-800 border border-orange-300 dark:border-orange-700 rounded-lg px-3 py-1.5 text-xs text-slate-900 dark:text-white"
                    />
                    <button
                      onClick={handleReopen}
                      className="px-4 py-1.5 bg-orange-600 hover:bg-orange-700 text-white rounded-lg text-xs font-bold cursor-pointer"
                    >
                      Confirm Reopen
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* AI Historical Resolutions */}
          {suggestions.length > 0 && (
            <div className="glass-panel rounded-3xl p-6 border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
              <div className="flex items-center gap-2 mb-4">
                <Sparkles className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
                <h3 className="font-bold text-slate-900 dark:text-white text-sm">
                  AI Historical Resolution Suggestions (NLP Pattern Match)
                </h3>
              </div>
              <div className="space-y-3">
                {suggestions.map((s: any, i: number) => (
                  <div key={i} className="rounded-2xl p-4 bg-indigo-500/5 border border-indigo-500/20">
                    <div className="flex justify-between items-center mb-1">
                      <span className="font-mono text-xs font-bold text-indigo-600 dark:text-indigo-400">{s.incidentId}</span>
                      <span className="text-[10px] bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 px-2 py-0.5 rounded-full font-bold">
                        {Math.round(s.similarityScore * 100)}% Similarity
                      </span>
                    </div>
                    <p className="text-xs font-bold text-slate-800 dark:text-slate-200">{s.title}</p>
                    <p className="text-xs text-slate-500 mt-1">
                      <strong className="text-slate-700 dark:text-slate-300">Root Cause:</strong> {s.rootCause || 'N/A'}
                    </p>
                    <p className="text-xs text-slate-700 dark:text-slate-300 mt-1">
                      <strong className="text-slate-900 dark:text-white">Resolution:</strong> {s.resolution}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Communication & Comments */}
          <div className="glass-panel rounded-3xl p-6 border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
            <h3 className="font-bold text-slate-900 dark:text-white text-sm mb-4">
              Discussion & Audit Notes
            </h3>
            <div className="space-y-3 mb-4 max-h-80 overflow-y-auto">
              {incident.comments?.map((c: any) => (
                <div key={c.id} className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/60 dark:border-slate-700/60 text-xs">
                  <div className="flex justify-between text-slate-500 mb-1">
                    <span className="font-bold text-slate-800 dark:text-slate-200">{c.authorName}</span>
                    <span className="font-mono">{c.createdAt ? new Date(c.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</span>
                  </div>
                  <p className="text-slate-700 dark:text-slate-300 leading-relaxed">{c.content}</p>
                </div>
              ))}
              {(!incident.comments || incident.comments.length === 0) && (
                <p className="text-xs text-slate-400 italic text-center py-4">No comments logged yet.</p>
              )}
            </div>

            <div className="flex gap-2">
              <input
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && addComment()}
                placeholder="Post comment or diagnostic note..."
                className="flex-1 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl px-4 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <button
                onClick={addComment}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold cursor-pointer"
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Sidebar Info & Audit Trail */}
        <div className="space-y-6">
          <div className="glass-panel rounded-3xl p-6 border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
            <h3 className="font-bold text-slate-900 dark:text-white text-sm mb-3">Ticket Overview</h3>
            <dl className="space-y-2.5 text-xs divide-y divide-slate-100 dark:divide-slate-800">
              <div className="pt-2 flex justify-between">
                <dt className="text-slate-500">Reporter</dt>
                <dd className="font-bold text-slate-900 dark:text-white">{incident.reporterName}</dd>
              </div>
              <div className="pt-2 flex justify-between">
                <dt className="text-slate-500">Email</dt>
                <dd className="font-mono text-slate-700 dark:text-slate-300">{incident.reporterEmail}</dd>
              </div>
              <div className="pt-2 flex justify-between">
                <dt className="text-slate-500">Impact</dt>
                <dd className="font-bold text-slate-900 dark:text-white">{incident.impact}</dd>
              </div>
              <div className="pt-2 flex justify-between">
                <dt className="text-slate-500">Urgency</dt>
                <dd className="font-bold text-slate-900 dark:text-white">{incident.urgency}</dd>
              </div>
              <div className="pt-2 flex justify-between">
                <dt className="text-slate-500">Affected Users</dt>
                <dd className="font-bold text-slate-900 dark:text-white">{incident.affectedUsersCount}</dd>
              </div>
              <div className="pt-2 flex justify-between">
                <dt className="text-slate-500">Logged At</dt>
                <dd className="font-mono text-slate-700 dark:text-slate-300">
                  {incident.createdAt ? new Date(incident.createdAt).toLocaleDateString() : '—'}
                </dd>
              </div>
              {incident.satisfactionRating && (
                <div className="pt-2 flex justify-between items-center">
                  <dt className="text-slate-500">Satisfaction</dt>
                  <dd className="text-amber-500 font-bold tracking-widest text-sm">
                    {'★'.repeat(incident.satisfactionRating)}
                  </dd>
                </div>
              )}
            </dl>
          </div>

          <div className="glass-panel rounded-3xl p-6 border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
            <h3 className="font-bold text-slate-900 dark:text-white text-sm mb-4">Lifecycle Audit Trail</h3>
            <div className="space-y-3 max-h-96 overflow-y-auto">
              {incident.auditTrail?.slice().reverse().map((a: any) => (
                <div key={a.id} className="border-l-2 border-indigo-500 pl-3 py-1 text-xs">
                  <p className="text-[10px] text-slate-400 font-mono">
                    {a.timestamp ? new Date(a.timestamp).toLocaleString() : ''}
                  </p>
                  <p className="font-bold text-slate-900 dark:text-white mt-0.5">{a.action}</p>
                  <p className="text-slate-500 mt-0.5 leading-relaxed">{a.description}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

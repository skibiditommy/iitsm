import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import Chatbot from '../components/Chatbot';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, Clock, Mic, MicOff,
  Paperclip, Image, Video, X, CheckCircle2, Mail, ShieldAlert,
  Search, Filter, Copy, ChevronRight, FileText, Sparkles, Volume2
} from 'lucide-react';

interface Incident {
  id: number;
  incidentId: string;
  title: string;
  description?: string;
  calculatedPriority: string;
  status: string;
  category: string;
  responseSlaStatus: string;
  resolutionSlaStatus?: string;
  createdAt: string;
  attachmentName?: string;
  attachmentType?: string;
  reopenCount?: number;
}

export default function EmployeeDashboard() {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    title: '',
    description: '',
    impact: 'MEDIUM',
    urgency: 'MEDIUM',
    affectedUsersCount: 1,
    attachmentName: '',
    attachmentType: '',
    attachmentData: '',
  });

  // Multi-Criteria Search & Filter State
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [slaFilter, setSlaFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');

  // Voice dictation state
  const [isListening, setIsListening] = useState(false);
  const [speechError, setSpeechError] = useState<string | null>(null);
  const [interimTranscript, setInterimTranscript] = useState('');
  const recognitionRef = useRef<any>(null);
  const baseTextRef = useRef('');
  const finalTranscriptRef = useRef('');

  // File attachment state
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [attachmentPreview, setAttachmentPreview] = useState<string | null>(null);

  // Success alert state
  const [successBanner, setSuccessBanner] = useState<{
    id: string;
    priority: string;
    message: string;
  } | null>(null);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  const navigate = useNavigate();

  const fetchIncidents = () => {
    api.get('/incidents/my')
      .then((r) => setIncidents(r.data))
      .catch(() => {});
  };

  useEffect(() => {
    fetchIncidents();
  }, []);

  // Stop listening helper
  const stopListening = () => {
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop();
      } catch (_) {}
      recognitionRef.current = null;
    }
    setIsListening(false);
    setInterimTranscript('');
  };

  useEffect(() => {
    return () => {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort();
        } catch (_) {}
      }
    };
  }, []);

  // Web Speech API
  const toggleListening = () => {
    if (isListening) {
      stopListening();
      return;
    }

    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setSpeechError(
        'Speech recognition is not supported in this browser. You can use the quick sample dictation presets below or type directly.'
      );
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = navigator.language || 'en-US';
      recognition.maxAlternatives = 1;

      baseTextRef.current = form.description ? form.description.trim() : '';
      finalTranscriptRef.current = '';
      setSpeechError(null);
      setInterimTranscript('');

      recognition.onstart = () => {
        setIsListening(true);
        setSpeechError(null);
      };

      recognition.onresult = (event: any) => {
        let currentInterim = '';

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const item = event.results[i];
          const textChunk = item[0]?.transcript || '';
          if (item.isFinal) {
            finalTranscriptRef.current = (
              finalTranscriptRef.current +
              ' ' +
              textChunk.trim()
            ).trim();
          } else {
            currentInterim = (currentInterim + ' ' + textChunk.trim()).trim();
          }
        }

        setInterimTranscript(currentInterim);

        // Update form description cleanly without repeating interim fragments:
        const parts = [baseTextRef.current, finalTranscriptRef.current].filter(Boolean);
        const combined = parts.join(' ').trim();
        setForm((prev) => ({
          ...prev,
          description: combined,
        }));
      };

      recognition.onerror = (event: any) => {
        console.warn('Speech recognition error:', event.error);
        if (event.error === 'no-speech') {
          // Do not interrupt on soft pause
          return;
        }

        const notices: Record<string, string> = {
          'not-allowed': 'Microphone permission was denied. Check your browser site/microphone settings or try a preset below.',
          'service-not-allowed': 'Browser speech recognition service is not available in this environment. Try a preset below.',
          'network': 'Network error reaching browser speech servers. Check your connection or use sample presets below.',
          'audio-capture': 'No microphone hardware found on your device.',
        };

        setSpeechError(notices[event.error] || `Voice error: ${event.error}`);
        stopListening();
      };

      recognition.onend = () => {
        setIsListening(false);
        setInterimTranscript('');
        recognitionRef.current = null;
      };

      recognitionRef.current = recognition;
      recognition.start();
    } catch (err: any) {
      setSpeechError(`Could not start microphone: ${err.message || 'Permission denied'}`);
      stopListening();
    }
  };

  const applyDictationPreset = (sampleText: string) => {
    setForm((prev) => ({
      ...prev,
      description: prev.description
        ? `${prev.description.trim()} ${sampleText}`
        : sampleText,
    }));
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 25 * 1024 * 1024) {
      setErrorBanner('Attachment file size exceeds 25 MB limit.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const base64Data = reader.result as string;
      setAttachmentPreview(base64Data);
      setForm((prev) => ({
        ...prev,
        attachmentName: file.name,
        attachmentType: file.type,
        attachmentData: base64Data,
      }));
    };
    reader.readAsDataURL(file);
  };

  const removeAttachment = () => {
    setAttachmentPreview(null);
    setForm((prev) => ({
      ...prev,
      attachmentName: '',
      attachmentType: '',
      attachmentData: '',
    }));
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const createIncident = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorBanner(null);

    if (!form.title.trim() || !form.description.trim()) {
      setErrorBanner('Please provide both an incident title and description.');
      return;
    }

    try {
      const res = await api.post('/incidents', form);
      const newInc = res.data;
      const catName = typeof newInc.category === 'object' && newInc.category ? newInc.category.name : (newInc.category || 'General');

      const summaryItem: Incident = {
        id: newInc.id,
        incidentId: newInc.incidentId,
        title: newInc.title,
        description: newInc.description,
        calculatedPriority: newInc.calculatedPriority,
        status: newInc.status || 'NEW',
        category: catName,
        responseSlaStatus: newInc.responseSlaStatus || 'ON_TRACK',
        resolutionSlaStatus: newInc.resolutionSlaStatus || 'ON_TRACK',
        attachmentType: newInc.attachmentType,
        reopenCount: newInc.reopenCount || 0,
        createdAt: newInc.createdAt || new Date().toISOString(),
      };

      setIncidents((prev) => [summaryItem, ...prev.filter((item) => item.id !== summaryItem.id)]);

      setSuccessBanner({
        id: newInc.incidentId,
        priority: newInc.calculatedPriority,
        message: `Incident ${newInc.incidentId} was created with priority ${newInc.calculatedPriority}. A confirmation email notification has been dispatched to your inbox.`,
      });

      setForm({
        title: '',
        description: '',
        impact: 'MEDIUM',
        urgency: 'MEDIUM',
        affectedUsersCount: 1,
        attachmentName: '',
        attachmentType: '',
        attachmentData: '',
      });
      setAttachmentPreview(null);
      setShowForm(false);

      setTimeout(() => {
        fetchIncidents();
      }, 350);
    } catch (err: any) {
      setErrorBanner(err.response?.data?.detail || err.response?.data?.message || 'Failed to submit incident.');
    }
  };

  const filteredIncidents = incidents.filter((i) => {
    const catName = typeof i.category === 'object' && i.category ? (i.category as any).name : (i.category || '');
    if (statusFilter !== 'ALL' && i.status !== statusFilter) return false;
    if (priorityFilter !== 'ALL' && i.calculatedPriority !== priorityFilter) return false;
    if (slaFilter !== 'ALL' && i.responseSlaStatus !== slaFilter && i.resolutionSlaStatus !== slaFilter) return false;
    if (categoryFilter !== 'ALL' && catName !== categoryFilter) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const match =
        (i.incidentId && i.incidentId.toLowerCase().includes(q)) ||
        (i.title && i.title.toLowerCase().includes(q)) ||
        (i.description && i.description.toLowerCase().includes(q)) ||
        (catName && catName.toLowerCase().includes(q));
      if (!match) return false;
    }
    return true;
  });

  const duplicateWarning = form.title.length > 5 ? incidents.find((i) => {
    const words = form.title.toLowerCase().split(/\s+/).filter((w) => w.length > 3);
    return words.some((w) => i.title.toLowerCase().includes(w));
  }) : null;

  // Approximate priority preview for real-time feedback
  const getPreviewPriority = () => {
    if (form.impact === 'CRITICAL' && form.urgency === 'CRITICAL') return 'P1';
    if (form.impact === 'CRITICAL' || form.urgency === 'CRITICAL') return 'P2';
    if (form.impact === 'HIGH' && form.urgency === 'HIGH') return 'P2';
    if (form.impact === 'HIGH' || form.urgency === 'HIGH') return 'P3';
    if (form.impact === 'MEDIUM' && form.urgency === 'MEDIUM') return 'P3';
    return 'P4';
  };

  const priorityStyles: Record<string, { badge: string; dot: string }> = {
    P1: { badge: 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/20', dot: 'bg-rose-500' },
    P2: { badge: 'bg-orange-500/10 text-orange-700 dark:text-orange-400 border-orange-500/20', dot: 'bg-orange-500' },
    P3: { badge: 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/20', dot: 'bg-amber-500' },
    P4: { badge: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/20', dot: 'bg-emerald-500' },
  };

  // KPI counts
  const totalCount = incidents.length;
  const activeCount = incidents.filter((i) => !['CLOSED', 'RESOLVED'].includes(i.status)).length;
  const resolvedCount = incidents.filter((i) => ['CLOSED', 'RESOLVED', 'VERIFIED'].includes(i.status)).length;
  const highPriorityCount = incidents.filter((i) => ['P1', 'P2'].includes(i.calculatedPriority)).length;

  return (
    <div className="space-y-6">
      {/* Top Banner Confirmed Notification */}
      <AnimatePresence>
        {successBanner && (
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            className="glass-panel border border-emerald-500/30 bg-emerald-500/10 text-emerald-900 dark:text-emerald-200 p-4 rounded-2xl shadow-lg flex items-start justify-between"
          >
            <div className="flex items-start gap-3.5">
              <div className="bg-emerald-500 text-white p-2 rounded-xl shrink-0 mt-0.5 shadow-sm">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-extrabold text-base text-slate-900 dark:text-white">
                    Incident Logged: {successBanner.id}
                  </span>
                  <span className="bg-emerald-500 text-white text-[10px] px-2 py-0.5 rounded-full font-bold">
                    {successBanner.priority}
                  </span>
                </div>
                <p className="text-xs text-slate-600 dark:text-slate-300 mt-1 leading-relaxed">
                  {successBanner.message}
                </p>
                <div className="flex items-center gap-1.5 mt-2 text-[11px] text-emerald-700 dark:text-emerald-400 font-semibold">
                  <Mail className="w-3.5 h-3.5" />
                  <span>Notification dispatched via simulated multi-channel outbox</span>
                </div>
              </div>
            </div>
            <button
              onClick={() => setSuccessBanner(null)}
              className="text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 p-1.5 rounded-lg cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Error alert */}
      <AnimatePresence>
        {errorBanner && (
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-800 dark:text-rose-300 text-xs flex items-center justify-between"
          >
            <div className="flex items-center gap-2.5">
              <ShieldAlert className="w-4 h-4 text-rose-500 shrink-0" />
              <span>{errorBanner}</span>
            </div>
            <button onClick={() => setErrorBanner(null)} className="text-rose-400 hover:text-rose-600 p-1">
              <X className="w-4 h-4" />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Hero Welcome & Quick Stats */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-slate-200/80 dark:border-slate-800/80 shadow-xs relative overflow-hidden">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 relative z-10">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="text-[10px] font-bold text-indigo-600 dark:text-indigo-400 uppercase tracking-widest">
                Nexora Global Technologies
              </span>
              <span className="text-slate-300 dark:text-slate-700">•</span>
              <span className="text-[10px] font-semibold text-slate-500 dark:text-slate-400">
                Self-Service Portal
              </span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight">
              My Support Incidents
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1 max-w-2xl leading-relaxed">
              Report disruptions, dictate voice tickets, monitor live SLA resolution countdowns, and verify completed solutions.
            </p>
          </div>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => setShowForm(!showForm)}
            className="self-start lg:self-auto px-5 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white font-bold text-xs sm:text-sm rounded-2xl shadow-lg shadow-indigo-500/25 flex items-center gap-2 cursor-pointer transition-all"
          >
            {showForm ? (
              <>
                <X className="w-4 h-4" />
                <span>Close Incident Form</span>
              </>
            ) : (
              <>
                <Plus className="w-4 h-4" />
                <span>Report New Incident</span>
              </>
            )}
          </motion.button>
        </div>

        {/* Quick KPI Stat Strip */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 mt-6 pt-6 border-t border-slate-200/60 dark:border-slate-800/60">
          <div className="p-3.5 rounded-2xl bg-slate-50/80 dark:bg-slate-800/50 border border-slate-200/60 dark:border-slate-800/60">
            <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 block">Total Reported</span>
            <span className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white mt-0.5 block">{totalCount}</span>
          </div>
          <div className="p-3.5 rounded-2xl bg-amber-500/5 dark:bg-amber-500/10 border border-amber-500/20">
            <span className="text-[11px] font-semibold text-amber-700 dark:text-amber-400 block">Active / In Progress</span>
            <span className="text-xl sm:text-2xl font-black text-amber-600 dark:text-amber-400 mt-0.5 block">{activeCount}</span>
          </div>
          <div className="p-3.5 rounded-2xl bg-emerald-500/5 dark:bg-emerald-500/10 border border-emerald-500/20">
            <span className="text-[11px] font-semibold text-emerald-700 dark:text-emerald-400 block">Resolved / Closed</span>
            <span className="text-xl sm:text-2xl font-black text-emerald-600 dark:text-emerald-400 mt-0.5 block">{resolvedCount}</span>
          </div>
          <div className="p-3.5 rounded-2xl bg-rose-500/5 dark:bg-rose-500/10 border border-rose-500/20">
            <span className="text-[11px] font-semibold text-rose-700 dark:text-rose-400 block">High Severity (P1/P2)</span>
            <span className="text-xl sm:text-2xl font-black text-rose-600 dark:text-rose-400 mt-0.5 block">{highPriorityCount}</span>
          </div>
        </div>
      </div>

      {/* Incident Reporting Form Drawer */}
      <AnimatePresence>
        {showForm && (
          <motion.div
            initial={{ opacity: 0, height: 0, y: -20 }}
            animate={{ opacity: 1, height: 'auto', y: 0 }}
            exit={{ opacity: 0, height: 0, y: -20 }}
            transition={{ duration: 0.3, ease: 'easeOut' }}
            className="overflow-hidden"
          >
            <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-indigo-200/80 dark:border-indigo-500/30 shadow-xl shadow-indigo-500/5 text-slate-900 dark:text-slate-100">
              <div className="flex justify-between items-center mb-6 pb-4 border-b border-slate-200/80 dark:border-slate-800">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 border border-indigo-500/20">
                    <FileText className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                      Report Service Incident
                    </h3>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      Auto-classified with NLP keyword indexing and weighted priority calculation
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setShowForm(false)}
                  className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <form onSubmit={createIncident} className="space-y-5">
                <div>
                  <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                    Incident Title *
                  </label>
                  <input
                    placeholder="e.g. Cannot access CRM database or authentication server"
                    value={form.title}
                    onChange={(e) => setForm({ ...form, title: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white placeholder-slate-400"
                    required
                  />

                  {/* Duplicate Detection Warning */}
                  {duplicateWarning && (
                    <motion.div
                      initial={{ opacity: 0, y: -4 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="mt-2.5 p-3 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-900 dark:text-purple-300 text-xs flex items-center gap-2"
                    >
                      <Copy className="w-4 h-4 text-purple-600 dark:text-purple-400 shrink-0" />
                      <span>
                        <strong>Duplicate Candidate Alert:</strong> A similar open incident exists ({duplicateWarning.incidentId}: "{duplicateWarning.title}"). You can still proceed with submission.
                      </span>
                    </motion.div>
                  )}
                </div>

                <div>
                  <div className="flex justify-between items-center mb-1.5">
                    <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                      Description & Symptoms *
                    </label>

                    {/* Voice Dictation Button with pulsating wave */}
                    <div className="flex items-center gap-2">
                      {isListening && (
                        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-semibold">
                          <span className="flex gap-0.5 items-end h-3">
                            <span className="w-0.5 h-2 bg-rose-500 animate-bounce" style={{ animationDelay: '0ms' }} />
                            <span className="w-0.5 h-3 bg-rose-500 animate-bounce" style={{ animationDelay: '150ms' }} />
                            <span className="w-0.5 h-1.5 bg-rose-500 animate-bounce" style={{ animationDelay: '300ms' }} />
                            <span className="w-0.5 h-2.5 bg-rose-500 animate-bounce" style={{ animationDelay: '75ms' }} />
                          </span>
                          <span>Recording Voice</span>
                        </div>
                      )}

                      <button
                        type="button"
                        onClick={toggleListening}
                        className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold transition-all cursor-pointer ${
                          isListening
                            ? 'bg-rose-500 text-white animate-pulse shadow-md shadow-rose-500/30'
                            : 'bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-500/20 border border-indigo-500/20'
                        }`}
                        title="Dictate description with Speech-to-Text"
                      >
                        {isListening ? (
                          <>
                            <MicOff className="w-3.5 h-3.5" />
                            <span>Stop Dictating</span>
                          </>
                        ) : (
                          <>
                            <Mic className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
                            <span>Dictate Description</span>
                          </>
                        )}
                      </button>
                    </div>
                  </div>

                  {/* Real-time Interim Voice Feedback */}
                  {interimTranscript && (
                    <motion.div
                      initial={{ opacity: 0, y: -4 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="mb-2 p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-800 text-indigo-800 dark:text-indigo-200 text-xs flex items-center gap-2"
                    >
                      <Volume2 className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400 shrink-0 animate-pulse" />
                      <span className="truncate">
                        <strong>Hearing:</strong> <em>"{interimTranscript}..."</em>
                      </span>
                    </motion.div>
                  )}

                  {/* Speech Error Banner */}
                  {speechError && (
                    <motion.div
                      initial={{ opacity: 0, y: -4 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="mb-2 p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-800 dark:text-amber-300 text-xs flex items-start justify-between gap-2"
                    >
                      <div className="flex items-start gap-2">
                        <ShieldAlert className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                        <span>{speechError}</span>
                      </div>
                      <button
                        type="button"
                        onClick={() => setSpeechError(null)}
                        className="text-amber-500 hover:text-amber-700 p-0.5 cursor-pointer shrink-0"
                      >
                        <X className="w-3.5 h-3.5" />
                      </button>
                    </motion.div>
                  )}

                  <textarea
                    placeholder="Describe the problem, affected systems, or dictate directly via microphone..."
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white placeholder-slate-400 h-28"
                    required
                  />

                  {/* Voice dictation presets & helper shortcuts */}
                  <div className="flex flex-wrap items-center gap-1.5 mt-2">
                    <span className="text-[11px] font-semibold text-slate-400 dark:text-slate-500 flex items-center gap-1">
                      <Sparkles className="w-3 h-3 text-indigo-500" />
                      Dictation Presets:
                    </span>
                    <button
                      type="button"
                      onClick={() => applyDictationPreset('VPN connection drops repeatedly every 5 minutes when connecting to internal ERP portal.')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 transition cursor-pointer"
                    >
                      🎧 VPN Drop
                    </button>
                    <button
                      type="button"
                      onClick={() => applyDictationPreset('PostgreSQL primary cluster query timeout on port 5432 during batch analytics job.')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 transition cursor-pointer"
                    >
                      🎧 DB Timeout
                    </button>
                    <button
                      type="button"
                      onClick={() => applyDictationPreset('SSO authentication failure: OAuth token expired with HTTP 401 error across Slack and Jira.')}
                      className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 transition cursor-pointer"
                    >
                      🎧 SSO Auth Error
                    </button>
                    {form.description && (
                      <button
                        type="button"
                        onClick={() => setForm(prev => ({ ...prev, description: '' }))}
                        className="text-[11px] px-2 py-0.5 text-slate-400 hover:text-rose-500 ml-auto transition cursor-pointer"
                      >
                        Clear text
                      </button>
                    )}
                  </div>
                </div>

                {/* Impact, Urgency, Affected Users with live Priority preview */}
                <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800">
                  <div>
                    <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 uppercase mb-1">Impact</label>
                    <select
                      value={form.impact}
                      onChange={(e) => setForm({ ...form, impact: e.target.value })}
                      className="w-full bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white rounded-xl px-3 py-2 text-xs focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value="CRITICAL">Critical (Org-wide)</option>
                      <option value="HIGH">High (Department)</option>
                      <option value="MEDIUM">Medium (Multiple)</option>
                      <option value="LOW">Low (Single User)</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 uppercase mb-1">Urgency</label>
                    <select
                      value={form.urgency}
                      onChange={(e) => setForm({ ...form, urgency: e.target.value })}
                      className="w-full bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white rounded-xl px-3 py-2 text-xs focus:ring-2 focus:ring-indigo-500"
                    >
                      <option value="CRITICAL">Critical (Immediate)</option>
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-600 dark:text-slate-400 uppercase mb-1">Users Affected</label>
                    <input
                      type="number"
                      min="1"
                      value={form.affectedUsersCount}
                      onChange={(e) => setForm({ ...form, affectedUsersCount: Math.max(1, +e.target.value) })}
                      className="w-full bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white rounded-xl px-3 py-2 text-xs focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div className="flex flex-col justify-center items-center p-2 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700/80">
                    <span className="text-[10px] font-bold uppercase text-slate-400">Calculated Priority</span>
                    <span className={`mt-0.5 px-2.5 py-0.5 rounded-full text-xs font-extrabold border ${priorityStyles[getPreviewPriority()]?.badge || ''}`}>
                      {getPreviewPriority()}
                    </span>
                  </div>
                </div>

                {/* Media Attachment */}
                <div className="pt-2">
                  <label className="block text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                    Visual Evidence / Media Attachment
                  </label>

                  <div className="flex flex-wrap items-center gap-3">
                    <input
                      type="file"
                      ref={fileInputRef}
                      accept="image/*,video/*"
                      onChange={handleFileChange}
                      className="hidden"
                    />

                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      className="inline-flex items-center gap-2 px-4 py-2 border border-slate-300 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-700 transition cursor-pointer shadow-xs"
                    >
                      <Paperclip className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
                      <span>Attach Screenshot or Recording</span>
                    </button>

                    <span className="text-xs text-slate-500">
                      PNG, JPG, MP4, WebM (up to 25 MB)
                    </span>
                  </div>

                  {attachmentPreview && (
                    <div className="mt-3 p-3 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl flex items-center justify-between shadow-xs">
                      <div className="flex items-center gap-3">
                        {form.attachmentType.startsWith('video/') ? (
                          <div className="p-2.5 bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 rounded-xl">
                            <Video className="w-6 h-6" />
                          </div>
                        ) : (
                          <img
                            src={attachmentPreview}
                            alt="Preview"
                            className="w-12 h-12 object-cover rounded-xl border border-slate-200 dark:border-slate-700"
                          />
                        )}
                        <div>
                          <p className="text-xs font-bold text-slate-900 dark:text-white truncate max-w-xs">{form.attachmentName}</p>
                          <span className="text-[10px] text-slate-500 font-mono">{form.attachmentType}</span>
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={removeAttachment}
                        className="p-1.5 text-slate-400 hover:text-rose-600 rounded-xl cursor-pointer"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  )}
                </div>

                <div className="flex justify-end gap-3 pt-3 border-t border-slate-200/80 dark:border-slate-800">
                  <button
                    type="button"
                    onClick={() => setShowForm(false)}
                    className="px-4 py-2.5 border border-slate-300 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-6 py-2.5 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white font-bold text-xs rounded-xl shadow-md shadow-indigo-500/20 cursor-pointer"
                  >
                    Submit Incident
                  </button>
                </div>
              </form>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Multi-Criteria Search & Filter Toolbar */}
      <div className="glass-panel rounded-2xl p-4 border border-slate-200/80 dark:border-slate-800/80 shadow-xs">
        <div className="flex items-center gap-2 mb-3">
          <Filter className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
          <span className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
            Multi-Criteria Ticket Filtering
          </span>
          <span className="text-xs text-slate-400 dark:text-slate-500 ml-auto font-mono">
            {filteredIncidents.length} of {incidents.length} shown
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3">
          <div className="relative">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-3" />
            <input
              type="text"
              placeholder="Search ID, title, keyword..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl px-3 py-2 text-xs text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">Status: All</option>
            <option value="NEW">New</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="VERIFIED">Verified</option>
            <option value="CLOSED">Closed</option>
            <option value="ESCALATED">Escalated</option>
          </select>

          <select
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
            className="bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl px-3 py-2 text-xs text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">Priority: All</option>
            <option value="P1">P1 - Critical</option>
            <option value="P2">P2 - High</option>
            <option value="P3">P3 - Medium</option>
            <option value="P4">P4 - Low</option>
          </select>

          <select
            value={slaFilter}
            onChange={(e) => setSlaFilter(e.target.value)}
            className="bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl px-3 py-2 text-xs text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">SLA Status: All</option>
            <option value="ON_TRACK">On Track</option>
            <option value="AT_RISK">At Risk</option>
            <option value="BREACHED">Breached</option>
          </select>

          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl px-3 py-2 text-xs text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">Category: All</option>
            <option value="Network">Network</option>
            <option value="Hardware">Hardware</option>
            <option value="Software">Software</option>
            <option value="Security">Security</option>
            <option value="Database">Database</option>
            <option value="Email">Email</option>
            <option value="Cloud Services">Cloud Services</option>
            <option value="Storage">Storage</option>
          </select>
        </div>
      </div>

      {/* Incidents Table Card */}
      <div className="glass-panel rounded-3xl border border-slate-200/80 dark:border-slate-800/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200/80 dark:border-slate-800">
              <tr>
                <th className="px-5 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Ticket ID</th>
                <th className="px-5 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Title</th>
                <th className="px-5 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Priority</th>
                <th className="px-5 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Status</th>
                <th className="px-5 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Evidence</th>
                <th className="px-5 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">SLA Status</th>
                <th className="px-5 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">Reported</th>
                <th className="px-5 py-3.5 text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
              {filteredIncidents.map((i) => (
                <motion.tr
                  key={i.id}
                  whileHover={{ backgroundColor: 'rgba(99, 102, 241, 0.04)' }}
                  onClick={() => navigate(`/incidents/${i.id}`)}
                  className="cursor-pointer transition-colors group"
                >
                  <td className="px-5 py-4 font-mono text-xs font-bold text-indigo-600 dark:text-indigo-400">
                    <div className="flex items-center gap-1.5">
                      <span>{i.incidentId}</span>
                      {i.reopenCount && i.reopenCount > 0 ? (
                        <span className="text-[10px] bg-orange-500/10 text-orange-700 dark:text-orange-400 border border-orange-500/20 px-1.5 py-0.5 rounded-full font-bold">
                          {i.reopenCount}x
                        </span>
                      ) : null}
                    </div>
                  </td>
                  <td className="px-5 py-4">
                    <span className="text-sm font-semibold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                      {i.title}
                    </span>
                    <span className="block text-[11px] text-slate-400 mt-0.5">
                      {i.category || 'General IT'}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold border ${priorityStyles[i.calculatedPriority]?.badge || ''}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${priorityStyles[i.calculatedPriority]?.dot || 'bg-slate-400'}`} />
                      {i.calculatedPriority}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-700 dark:text-slate-300">
                      <Clock className="w-3.5 h-3.5 text-indigo-500" />
                      {i.status}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    {i.attachmentType ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-indigo-600 dark:text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded-full border border-indigo-500/20">
                        {i.attachmentType.startsWith('video/') ? (
                          <Video className="w-3 h-3" />
                        ) : (
                          <Image className="w-3 h-3" />
                        )}
                        <span>Media</span>
                      </span>
                    ) : (
                      <span className="text-slate-300 dark:text-slate-600 text-xs">—</span>
                    )}
                  </td>
                  <td className="px-5 py-4">
                    <span
                      className={`text-xs font-bold inline-flex items-center gap-1 ${
                        i.responseSlaStatus === 'BREACHED'
                          ? 'text-rose-600 dark:text-rose-400'
                          : i.responseSlaStatus === 'AT_RISK'
                          ? 'text-amber-600 dark:text-amber-400'
                          : 'text-emerald-600 dark:text-emerald-400'
                      }`}
                    >
                      <span className={`w-1.5 h-1.5 rounded-full ${
                        i.responseSlaStatus === 'BREACHED' ? 'bg-rose-500' : i.responseSlaStatus === 'AT_RISK' ? 'bg-amber-500' : 'bg-emerald-500'
                      }`} />
                      {i.responseSlaStatus}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-xs text-slate-500 font-mono">
                    {i.createdAt ? new Date(i.createdAt).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-5 py-4 text-right">
                    <span className="inline-flex items-center gap-1 text-xs font-bold text-indigo-600 dark:text-indigo-400 group-hover:translate-x-0.5 transition-transform">
                      View <ChevronRight className="w-3.5 h-3.5" />
                    </span>
                  </td>
                </motion.tr>
              ))}

              {filteredIncidents.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-5 py-12 text-center text-slate-400 dark:text-slate-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <FileText className="w-8 h-8 text-slate-300 dark:text-slate-600" />
                      <p className="text-xs font-semibold">No incidents match the active search criteria.</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <Chatbot />
    </div>
  );
}

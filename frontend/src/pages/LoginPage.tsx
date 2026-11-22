import React, { useState } from 'react';
import { useAuth } from '../AuthContext';
import { useTheme } from '../ThemeContext';
import api from '../api';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Shield, User, Wrench, Settings, ArrowLeft,
  Mail, CheckCircle2, AlertCircle, Lock, Sparkles, ChevronRight,
  Sun, Moon, UserPlus, Check, KeyRound
} from 'lucide-react';

const quickLogins = [
  {
    role: 'Employee',
    name: 'Rahul Sharma',
    email: 'employee@iitsm.org',
    tag: 'Self-Service Portal',
    desc: 'Report incidents, voice dictation, view live ticket status & verify resolutions.',
    icon: <User className="w-5 h-5 text-emerald-500" />,
    badgeColor: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border-emerald-500/20',
    accent: 'emerald',
    ringColor: 'hover:border-emerald-500/60 focus:ring-emerald-500/30',
  },
  {
    role: 'Technician',
    name: 'Alex Johnson',
    email: 'tech.alex@iitsm.org',
    tag: 'L2 Operations Cockpit',
    desc: 'SLA countdown queue, AI-suggested resolutions & incident lifecycle transitions.',
    icon: <Wrench className="w-5 h-5 text-amber-500" />,
    badgeColor: 'bg-amber-500/10 text-amber-700 dark:text-amber-300 border-amber-500/20',
    accent: 'amber',
    ringColor: 'hover:border-amber-500/60 focus:ring-amber-500/30',
  },
  {
    role: 'Administrator',
    name: 'Devon Vance',
    email: 'admin@iitsm.org',
    tag: 'System Command Center',
    desc: 'Real-time analytics, runtime strategy switching, SLA simulation & blast radius explorer.',
    icon: <Settings className="w-5 h-5 text-indigo-500" />,
    badgeColor: 'bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 border-indigo-500/20',
    accent: 'indigo',
    ringColor: 'hover:border-indigo-500/60 focus:ring-indigo-500/30',
  },
];

export default function LoginPage() {
  const { login } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const [authMode, setAuthMode] = useState<'login' | 'register' | 'forgot'>('login');

  // Sign In state
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const [copiedPassword, setCopiedPassword] = useState(false);

  // Self-Registration state (Employee)
  const [regFirstName, setRegFirstName] = useState('');
  const [regLastName, setRegLastName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regDepartment, setRegDepartment] = useState('Engineering');
  const [regPhone, setRegPhone] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');

  // Forgot password flow states
  const [forgotEmail, setForgotEmail] = useState('');
  const [verificationCode, setVerificationCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [resetStep, setResetStep] = useState<1 | 2>(1);
  const [simulatedEmailCode, setSimulatedEmailCode] = useState<string | null>(null);

  const copyDemoPassword = () => {
    navigator.clipboard.writeText('Password@123');
    setCopiedPassword(true);
    setTimeout(() => setCopiedPassword(false), 2000);
  };

  const handleLogin = async (e?: React.FormEvent, overrideEmail?: string) => {
    e?.preventDefault();
    setLoading(true);
    setError('');
    setSuccessMsg('');
    try {
      const loginEmail = overrideEmail || email;
      const loginPassword = overrideEmail ? 'Password@123' : password;

      if (!loginEmail || !loginPassword) {
        setError('Please enter both email and password.');
        setLoading(false);
        return;
      }

      const res = await api.post('/auth/login', {
        email: loginEmail.trim(),
        password: loginPassword,
      });
      login(res.data);
    } catch (err: any) {
      const detail = err.response?.data?.detail || err.response?.data?.message || 'Invalid email or password.';
      setError(detail + ' (Hint: Demo accounts use password "Password@123")');
    }
    setLoading(false);
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!regFirstName.trim() || !regLastName.trim() || !regEmail.trim() || !regPassword) {
      setError('Please fill out all required fields.');
      return;
    }
    if (regPassword !== regConfirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    if (regPassword.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }
    setLoading(true);
    setError('');
    setSuccessMsg('');
    try {
      const res = await api.post('/auth/register', {
        firstName: regFirstName.trim(),
        lastName: regLastName.trim(),
        email: regEmail.trim(),
        password: regPassword,
        role: 'EMPLOYEE',
        department: regDepartment.trim() || 'General Operations',
        phone: regPhone.trim() || '+1-555-0100',
      });
      setSuccessMsg('Registration successful! Automatically signing you in...');
      setTimeout(() => {
        login(res.data);
      }, 700);
    } catch (err: any) {
      const detail = err.response?.data?.detail || err.response?.data?.message || 'Employee self-registration failed.';
      setError(detail);
    }
    setLoading(false);
  };

  const handleSendResetCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!forgotEmail) {
      setError('Please enter your account email.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await api.post('/auth/forgot-password', { email: forgotEmail.trim() });
      const code = res.data.verificationCode || res.data.resetCode;
      setSimulatedEmailCode(code);
      setVerificationCode(code);
      setResetStep(2);
      setSuccessMsg('Verification code sent to simulated email outbox.');
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Unable to send reset email.');
    }
    setLoading(false);
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!verificationCode || !newPassword) {
      setError('Please provide the verification code and new password.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await api.post('/auth/reset-password', {
        email: forgotEmail.trim(),
        resetToken: verificationCode.trim(),
        newPassword,
      });
      setSuccessMsg('Password has been reset successfully! You can now log in.');
      setEmail(forgotEmail);
      setPassword(newPassword);
      setAuthMode('login');
      setResetStep(1);
      setSimulatedEmailCode(null);
    } catch (err: any) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Password reset failed.');
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col justify-between relative overflow-hidden bg-grid-pattern transition-colors duration-200">
      {/* Ambient background glows */}
      <div className="absolute top-[-10%] left-[-10%] w-[550px] h-[550px] rounded-full bg-indigo-500/10 dark:bg-indigo-600/15 blur-[140px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[550px] h-[550px] rounded-full bg-purple-500/10 dark:bg-purple-600/15 blur-[140px] pointer-events-none" />
      <div className="absolute top-[35%] right-[25%] w-[380px] h-[380px] rounded-full bg-emerald-500/5 dark:bg-emerald-600/10 blur-[120px] pointer-events-none" />

      {/* Top Header */}
      <header className="relative z-10 max-w-7xl mx-auto w-full px-6 py-6 flex items-center justify-between">
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.4 }}
          className="flex items-center gap-3.5"
        >
          <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/25 border border-indigo-400/30">
            <Shield className="w-6 h-6 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl font-extrabold tracking-tight text-slate-900 dark:text-white">
                Nexora <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600 dark:from-indigo-400 dark:to-purple-400">I-ITSM</span>
              </span>
              <span className="text-[10px] font-bold bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 border border-indigo-500/20 px-2.5 py-0.5 rounded-full">
                Enterprise v2.4
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 hidden sm:block">
              Intelligent IT Service Management & Autonomous Incident Resolution
            </p>
          </div>
        </motion.div>

        {/* Right side controls */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.4 }}
          className="flex items-center gap-3"
        >
          <button
            onClick={toggleTheme}
            className="p-2 text-slate-600 dark:text-slate-300 bg-white/80 dark:bg-slate-900/80 border border-slate-200 dark:border-slate-800 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-all flex items-center gap-1.5 text-xs font-semibold shadow-xs cursor-pointer"
            title={isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          >
            {isDark ? (
              <>
                <Sun className="w-4 h-4 text-amber-400" />
                <span className="hidden sm:inline">Light Mode</span>
              </>
            ) : (
              <>
                <Moon className="w-4 h-4 text-indigo-600" />
                <span className="hidden sm:inline">Dark Mode</span>
              </>
            )}
          </button>

          <div className="hidden sm:flex items-center gap-2 text-xs text-slate-600 dark:text-slate-400 bg-white/80 dark:bg-slate-900/80 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-xs">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span>Sandbox Ready</span>
          </div>
        </motion.div>
      </header>

      {/* Main Container */}
      <main className="relative z-10 w-full max-w-6xl mx-auto px-4 sm:px-6 py-6 flex-1 flex items-center justify-center">
        <div className="w-full grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          
          {/* Left Column: 1-Click Evaluation Personas & System Highlights */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="lg:col-span-6 space-y-6"
          >
            <div>
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-xs font-semibold text-indigo-700 dark:text-indigo-300 mb-3">
                <Sparkles className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
                <span>CS F213 / MAC F212 Capstone Architecture</span>
              </div>
              <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-slate-900 dark:text-white leading-tight">
                Enterprise Incident Resolution,{' '}
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 dark:from-indigo-400 dark:via-purple-400 dark:to-pink-400">
                  Engineered with OOP
                </span>
              </h1>
              <p className="mt-2.5 text-sm sm:text-base text-slate-600 dark:text-slate-400 leading-relaxed">
                Featuring the State Pattern (9-state lifecycle), Strategy Pattern (runtime algorithm hot-swapping), Observer Pattern (multi-channel event dispatching), and AI Root Cause BFS blast-radius analysis.
              </p>
            </div>

            {/* 1-Click Persona Cards */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Select 1-Click Evaluation Persona
                </span>
                <button
                  type="button"
                  onClick={copyDemoPassword}
                  className="inline-flex items-center gap-1.5 text-xs text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 font-semibold cursor-pointer"
                  title="Universal password for demo personas is Password@123"
                >
                  {copiedPassword ? (
                    <>
                      <Check className="w-3 h-3 text-emerald-500" />
                      <span className="text-emerald-500">Password Copied!</span>
                    </>
                  ) : (
                    <>
                      <KeyRound className="w-3 h-3" />
                      <span>Password: <code className="font-mono font-bold bg-slate-200/70 dark:bg-slate-800 px-1 py-0.5 rounded">Password@123</code></span>
                    </>
                  )}
                </button>
              </div>

              <div className="grid grid-cols-1 gap-2.5">
                {quickLogins.map((p) => (
                  <motion.button
                    key={p.role}
                    type="button"
                    whileHover={{ y: -2, scale: 1.01 }}
                    whileTap={{ scale: 0.99 }}
                    onClick={() => handleLogin(undefined, p.email)}
                    className="w-full p-3.5 rounded-2xl glass-panel border border-slate-200/80 dark:border-slate-800/80 hover:border-indigo-400/80 dark:hover:border-indigo-500/80 text-left transition-all duration-150 shadow-xs hover:shadow-md cursor-pointer group"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="p-2.5 rounded-xl bg-slate-100 dark:bg-slate-800/80 group-hover:scale-110 transition-transform">
                          {p.icon}
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-bold text-slate-900 dark:text-white">
                              {p.name}
                            </span>
                            <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${p.badgeColor}`}>
                              {p.role}
                            </span>
                          </div>
                          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                            {p.desc}
                          </p>
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 group-hover:translate-x-1 transition-all shrink-0 ml-2" />
                    </div>
                  </motion.button>
                ))}
              </div>
            </div>

            {/* Architecture Highlights Pills */}
            <div className="pt-2 grid grid-cols-3 gap-2.5">
              <div className="p-2.5 rounded-xl bg-slate-100/70 dark:bg-slate-900/60 border border-slate-200/60 dark:border-slate-800/60 text-center">
                <div className="text-base font-extrabold text-indigo-600 dark:text-indigo-400">9 States</div>
                <div className="text-[10px] text-slate-500 dark:text-slate-400 font-medium">State Pattern</div>
              </div>
              <div className="p-2.5 rounded-xl bg-slate-100/70 dark:bg-slate-900/60 border border-slate-200/60 dark:border-slate-800/60 text-center">
                <div className="text-base font-extrabold text-purple-600 dark:text-purple-400">4 Tiers</div>
                <div className="text-[10px] text-slate-500 dark:text-slate-400 font-medium">SLA Escalation</div>
              </div>
              <div className="p-2.5 rounded-xl bg-slate-100/70 dark:bg-slate-900/60 border border-slate-200/60 dark:border-slate-800/60 text-center">
                <div className="text-base font-extrabold text-emerald-600 dark:text-emerald-400">BFS Graph</div>
                <div className="text-[10px] text-slate-500 dark:text-slate-400 font-medium">Blast Radius</div>
              </div>
            </div>
          </motion.div>

          {/* Right Column: Interactive Sign In / Register / Reset Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="lg:col-span-6"
          >
            <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-slate-200/80 dark:border-slate-800/80 shadow-xl shadow-slate-200/50 dark:shadow-none relative">
              
              {/* Tab Selector */}
              <div className="flex p-1 bg-slate-100 dark:bg-slate-800/80 rounded-2xl mb-6">
                <button
                  type="button"
                  onClick={() => { setAuthMode('login'); setError(''); setSuccessMsg(''); }}
                  className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all cursor-pointer relative ${
                    authMode === 'login'
                      ? 'text-slate-900 dark:text-white shadow-xs'
                      : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                  }`}
                >
                  {authMode === 'login' && (
                    <motion.div
                      layoutId="authTab"
                      className="absolute inset-0 bg-white dark:bg-slate-900 rounded-xl shadow-xs"
                      transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                    />
                  )}
                  <span className="relative z-10">Sign In</span>
                </button>

                <button
                  type="button"
                  onClick={() => { setAuthMode('register'); setError(''); setSuccessMsg(''); }}
                  className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all cursor-pointer relative ${
                    authMode === 'register'
                      ? 'text-slate-900 dark:text-white shadow-xs'
                      : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                  }`}
                >
                  {authMode === 'register' && (
                    <motion.div
                      layoutId="authTab"
                      className="absolute inset-0 bg-white dark:bg-slate-900 rounded-xl shadow-xs"
                      transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                    />
                  )}
                  <span className="relative z-10">Self-Registration</span>
                </button>
              </div>

              {/* Feedback banners */}
              <AnimatePresence mode="wait">
                {error && (
                  <motion.div
                    initial={{ opacity: 0, y: -8 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -8 }}
                    className="mb-4 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-700 dark:text-rose-400 text-xs flex items-start gap-2.5"
                  >
                    <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                    <span>{error}</span>
                  </motion.div>
                )}
                {successMsg && (
                  <motion.div
                    initial={{ opacity: 0, y: -8 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -8 }}
                    className="mb-4 p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 dark:text-emerald-400 text-xs flex items-start gap-2.5"
                  >
                    <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
                    <span>{successMsg}</span>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* AUTH MODE: LOGIN */}
              {authMode === 'login' && (
                <form onSubmit={(e) => handleLogin(e)} className="space-y-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                      Work Email Address
                    </label>
                    <div className="relative">
                      <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                      <input
                        type="email"
                        required
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="you@iitsm.org"
                        className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                      />
                    </div>
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                        Password
                      </label>
                      <button
                        type="button"
                        onClick={() => { setAuthMode('forgot'); setError(''); setSuccessMsg(''); }}
                        className="text-[11px] font-semibold text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer"
                      >
                        Forgot Password?
                      </button>
                    </div>
                    <div className="relative">
                      <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                      <input
                        type="password"
                        required
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="••••••••••••"
                        className="w-full pl-10 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                      />
                    </div>
                  </div>

                  <motion.button
                    whileHover={{ scale: 1.01 }}
                    whileTap={{ scale: 0.99 }}
                    type="submit"
                    disabled={loading}
                    className="w-full mt-2 py-3 px-4 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white font-bold text-sm rounded-xl shadow-lg shadow-indigo-500/25 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                  >
                    {loading ? (
                      <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    ) : (
                      <>
                        <span>Sign In to Service Portal</span>
                        <ChevronRight className="w-4 h-4" />
                      </>
                    )}
                  </motion.button>
                </form>
              )}

              {/* AUTH MODE: REGISTER */}
              {authMode === 'register' && (
                <form onSubmit={handleRegister} className="space-y-3.5">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                        First Name
                      </label>
                      <input
                        type="text"
                        required
                        value={regFirstName}
                        onChange={(e) => setRegFirstName(e.target.value)}
                        placeholder="Rahul"
                        className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                        Last Name
                      </label>
                      <input
                        type="text"
                        required
                        value={regLastName}
                        onChange={(e) => setRegLastName(e.target.value)}
                        placeholder="Sharma"
                        className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                      Corporate Email
                    </label>
                    <input
                      type="email"
                      required
                      value={regEmail}
                      onChange={(e) => setRegEmail(e.target.value)}
                      placeholder="rahul.sharma@enterprise.com"
                      className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                        Department
                      </label>
                      <input
                        type="text"
                        value={regDepartment}
                        onChange={(e) => setRegDepartment(e.target.value)}
                        placeholder="Engineering"
                        className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                        Phone
                      </label>
                      <input
                        type="text"
                        value={regPhone}
                        onChange={(e) => setRegPhone(e.target.value)}
                        placeholder="+1-555-0100"
                        className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                        Password
                      </label>
                      <input
                        type="password"
                        required
                        value={regPassword}
                        onChange={(e) => setRegPassword(e.target.value)}
                        placeholder="••••••••"
                        className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                        Confirm Password
                      </label>
                      <input
                        type="password"
                        required
                        value={regConfirmPassword}
                        onChange={(e) => setRegConfirmPassword(e.target.value)}
                        placeholder="••••••••"
                        className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                      />
                    </div>
                  </div>

                  <motion.button
                    whileHover={{ scale: 1.01 }}
                    whileTap={{ scale: 0.99 }}
                    type="submit"
                    disabled={loading}
                    className="w-full mt-2 py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm rounded-xl shadow-md shadow-emerald-600/20 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                  >
                    {loading ? (
                      <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    ) : (
                      <>
                        <UserPlus className="w-4 h-4" />
                        <span>Register Employee Profile</span>
                      </>
                    )}
                  </motion.button>
                </form>
              )}

              {/* AUTH MODE: FORGOT PASSWORD */}
              {authMode === 'forgot' && (
                <div className="space-y-4">
                  <div className="flex items-center gap-2 mb-2">
                    <button
                      type="button"
                      onClick={() => { setAuthMode('login'); setError(''); setSuccessMsg(''); }}
                      className="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 cursor-pointer"
                    >
                      <ArrowLeft className="w-4 h-4" />
                    </button>
                    <span className="text-sm font-bold text-slate-900 dark:text-white">
                      Password Recovery
                    </span>
                  </div>

                  {resetStep === 1 ? (
                    <form onSubmit={handleSendResetCode} className="space-y-4">
                      <div>
                        <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                          Enter your registered account email
                        </label>
                        <input
                          type="email"
                          required
                          value={forgotEmail}
                          onChange={(e) => setForgotEmail(e.target.value)}
                          placeholder="employee@iitsm.org"
                          className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                        />
                      </div>
                      <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm rounded-xl transition-all cursor-pointer"
                      >
                        {loading ? 'Sending Code...' : 'Send Verification Code'}
                      </button>
                    </form>
                  ) : (
                    <form onSubmit={handleResetPassword} className="space-y-3.5">
                      {simulatedEmailCode && (
                        <div className="p-3 bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 rounded-xl text-xs text-indigo-900 dark:text-indigo-300">
                          Simulated Outbox Code: <strong className="font-mono text-sm">{simulatedEmailCode}</strong>
                        </div>
                      )}
                      <div>
                        <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                          Verification Code
                        </label>
                        <input
                          type="text"
                          required
                          value={verificationCode}
                          onChange={(e) => setVerificationCode(e.target.value)}
                          className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                        />
                      </div>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                            New Password
                          </label>
                          <input
                            type="password"
                            required
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            placeholder="••••••••"
                            className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                          />
                        </div>
                        <div>
                          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                            Confirm
                          </label>
                          <input
                            type="password"
                            required
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            placeholder="••••••••"
                            className="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700/80 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-900 dark:text-white"
                          />
                        </div>
                      </div>
                      <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm rounded-xl transition-all cursor-pointer"
                      >
                        {loading ? 'Resetting Password...' : 'Reset & Save Password'}
                      </button>
                    </form>
                  )}
                </div>
              )}

            </div>
          </motion.div>

        </div>
      </main>

      {/* Footer */}
      <footer className="relative z-10 max-w-7xl mx-auto w-full px-6 py-4 text-center text-xs text-slate-500 dark:text-slate-500 border-t border-slate-200/60 dark:border-slate-800/60">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>CS F213 / MAC F212 Object-Oriented Programming Capstone Project</span>
          <span>Spring Boot 3.2 • Java 21 LTS • React 19 • Tailwind CSS 4 • Framer Motion</span>
        </div>
      </footer>
    </div>
  );
}

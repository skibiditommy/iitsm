import { useState, useRef, useEffect } from 'react';
import api from '../api';
import { motion, AnimatePresence } from 'framer-motion';
import { MessageCircle, X, Send, Bot, User, Sparkles, Zap } from 'lucide-react';

export default function Chatbot() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Array<{ role: string; text: string; time?: string; actions?: string[] }>>([
    {
      role: 'bot',
      text: "Hello! I am your AI IT Service Assistant powered by Groq LPU™ inference (OpenAI GPT-OSS-120B). How can I help diagnose an issue, check SLA compliance, or assist with your incident ticket today?",
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (open) {
      scrollToBottom();
    }
  }, [messages, open]);

  const quickPrompts = [
    'How do I report a P1 outage?',
    'Troubleshoot VPN connection drops',
    'Reset employee SSO password',
    'Explain SLA escalation tiers',
  ];

  const send = async (customText?: string) => {
    const textToSend = customText || input;
    if (!textToSend.trim()) return;

    const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    setMessages((prev) => [...prev, { role: 'user', text: textToSend, time: currentTime }]);
    if (!customText) setInput('');
    setLoading(true);

    try {
      const res = await api.post('/incidents/chatbot', { message: textToSend });
      setMessages((prev) => [
        ...prev,
        {
          role: 'bot',
          text: res.data.reply,
          actions: res.data.suggestedActions,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          role: 'bot',
          text: "I'm having trouble connecting to the AI assistant service. Please verify network connectivity or create an incident ticket directly.",
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    }
    setLoading(false);
  };

  return (
    <>
      {/* Floating Trigger Button */}
      <motion.button
        whileHover={{ scale: 1.08 }}
        whileTap={{ scale: 0.92 }}
        onClick={() => setOpen(!open)}
        className="fixed bottom-6 right-6 bg-gradient-to-tr from-indigo-600 via-indigo-500 to-purple-600 text-white p-4 rounded-2xl shadow-xl shadow-indigo-500/30 hover:shadow-indigo-500/50 z-50 transition-all cursor-pointer border border-indigo-400/30 flex items-center justify-center"
        title="Open Groq AI IT Assistant"
      >
        <AnimatePresence mode="wait" initial={false}>
          {open ? (
            <motion.div
              key="close"
              initial={{ rotate: -90, opacity: 0 }}
              animate={{ rotate: 0, opacity: 1 }}
              exit={{ rotate: 90, opacity: 0 }}
              transition={{ duration: 0.15 }}
            >
              <X className="w-6 h-6" />
            </motion.div>
          ) : (
            <motion.div
              key="chat"
              initial={{ rotate: 90, opacity: 0 }}
              animate={{ rotate: 0, opacity: 1 }}
              exit={{ rotate: -90, opacity: 0 }}
              transition={{ duration: 0.15 }}
              className="relative"
            >
              <MessageCircle className="w-6 h-6" />
              <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-emerald-400 rounded-full ring-2 ring-indigo-600 animate-pulse" />
            </motion.div>
          )}
        </AnimatePresence>
      </motion.button>

      {/* Chat Window */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="fixed bottom-24 right-6 w-96 max-w-[calc(100vw-3rem)] glass-panel rounded-3xl shadow-2xl border border-slate-200/90 dark:border-slate-800/90 z-50 flex flex-col h-[540px] overflow-hidden"
          >
            {/* Header */}
            <div className="bg-gradient-to-r from-indigo-600 via-indigo-500 to-purple-600 text-white px-5 py-4 flex items-center justify-between shadow-xs">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-xl bg-white/20 backdrop-blur-md">
                  <Bot className="w-5 h-5 text-white" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-extrabold text-sm tracking-tight">AI Service Assistant</span>
                    <span className="text-[10px] font-bold bg-amber-400/25 text-amber-200 border border-amber-300/30 px-2 py-0.5 rounded-full flex items-center gap-1">
                      <Zap className="w-2.5 h-2.5 text-amber-300" /> Groq LPU™
                    </span>
                  </div>
                  <p className="text-[11px] text-white/80">gpt-oss-120b ultra-fast triage</p>
                </div>
              </div>

              <button
                onClick={() => setOpen(false)}
                className="p-1.5 rounded-lg text-white/80 hover:text-white hover:bg-white/10 transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Quick Prompt Chips */}
            <div className="px-4 py-2.5 bg-slate-50/80 dark:bg-slate-800/40 border-b border-slate-200/60 dark:border-slate-800 flex items-center gap-1.5 overflow-x-auto">
              <span className="text-[10px] font-bold text-slate-400 shrink-0 flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-indigo-500" /> Prompts:
              </span>
              {quickPrompts.map((p, idx) => (
                <button
                  key={idx}
                  onClick={() => send(p)}
                  className="text-[11px] font-semibold text-indigo-700 dark:text-indigo-300 bg-indigo-500/10 hover:bg-indigo-500/20 px-2.5 py-1 rounded-full whitespace-nowrap transition cursor-pointer shrink-0"
                >
                  {p}
                </button>
              ))}
            </div>

            {/* Message Feed */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3.5">
              {messages.map((m, i) => (
                <div
                  key={i}
                  className={`flex flex-col ${m.role === 'user' ? 'items-end' : 'items-start'}`}
                >
                  <div className={`flex items-end gap-2 max-w-[88%] ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                    {m.role === 'bot' && (
                      <div className="w-7 h-7 rounded-xl bg-gradient-to-tr from-indigo-500 to-purple-600 text-white flex items-center justify-center shrink-0 shadow-xs mb-1">
                        <Bot className="w-3.5 h-3.5" />
                      </div>
                    )}

                    <div
                      className={`px-4 py-2.5 rounded-2xl text-xs leading-relaxed ${
                        m.role === 'user'
                          ? 'bg-gradient-to-r from-indigo-600 to-purple-600 text-white rounded-br-none shadow-md shadow-indigo-500/15'
                          : 'glass-panel border border-slate-200/80 dark:border-slate-700/80 text-slate-800 dark:text-slate-200 rounded-bl-none shadow-xs'
                      }`}
                    >
                      <p className="whitespace-pre-wrap">{m.text}</p>
                      {m.time && (
                        <span
                          className={`block text-[9px] mt-1 text-right ${
                            m.role === 'user' ? 'text-white/70' : 'text-slate-400'
                          }`}
                        >
                          {m.time}
                        </span>
                      )}
                    </div>

                    {m.role === 'user' && (
                      <div className="w-7 h-7 rounded-xl bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 flex items-center justify-center shrink-0 shadow-xs mb-1">
                        <User className="w-3.5 h-3.5" />
                      </div>
                    )}
                  </div>

                  {m.actions && m.actions.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 mt-2 ml-9">
                      {m.actions.map((act, actIdx) => (
                        <button
                          key={actIdx}
                          onClick={() => send(act)}
                          className="text-[10px] font-bold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 px-2 py-0.5 rounded-full hover:bg-indigo-100 cursor-pointer"
                        >
                          → {act}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              ))}

              {loading && (
                <div className="flex items-center gap-2 text-slate-400 text-xs">
                  <div className="w-6 h-6 rounded-lg bg-indigo-500/10 text-indigo-500 flex items-center justify-center">
                    <Bot className="w-3.5 h-3.5" />
                  </div>
                  <div className="flex items-center gap-1.5 py-1 px-3 glass-panel rounded-full border border-slate-200 dark:border-slate-800">
                    <span className="text-[10px] font-bold text-indigo-600 dark:text-indigo-400">Groq LPU thinking</span>
                    <span className="w-1.5 h-1.5 bg-indigo-500 rounded-full animate-bounce [animation-delay:-0.3s]" />
                    <span className="w-1.5 h-1.5 bg-indigo-500 rounded-full animate-bounce [animation-delay:-0.15s]" />
                    <span className="w-1.5 h-1.5 bg-indigo-500 rounded-full animate-bounce" />
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Footer */}
            <div className="border-t border-slate-200/80 dark:border-slate-800 p-3 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md flex items-center gap-2">
              <input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && send()}
                placeholder="Ask Groq AI IT Assistant..."
                className="flex-1 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl px-3.5 py-2 text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => send()}
                disabled={loading || !input.trim()}
                className="p-2.5 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white rounded-xl disabled:opacity-50 transition cursor-pointer shadow-sm"
              >
                <Send className="w-4 h-4" />
              </motion.button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}

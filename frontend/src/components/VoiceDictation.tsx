import { useEffect, useRef, useState } from 'react';
import { Mic, MicOff, Volume2, ShieldAlert } from 'lucide-react';

interface Recognition {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: ((event: any) => void) | null;
  onerror: ((event: any) => void) | null;
  onend: (() => void) | null;
  start(): void;
  stop(): void;
  abort(): void;
}
type RecognitionConstructor = new () => Recognition;
type SpeechWindow = Window & {
  SpeechRecognition?: RecognitionConstructor;
  webkitSpeechRecognition?: RecognitionConstructor;
};

export default function VoiceDictation({ onText, onActive }: {
  onText: (text: string) => void;
  onActive: (active: boolean) => void;
}) {
  const session = useRef<Recognition | null>(null);
  const [phase, setPhase] = useState<'idle' | 'listening' | 'stopping'>('idle');
  const [interim, setInterim] = useState('');
  const [message, setMessage] = useState('');
  const [isError, setIsError] = useState(false);
  const speechWindow = window as SpeechWindow;
  const Recognizer = speechWindow.SpeechRecognition ?? speechWindow.webkitSpeechRecognition;

  useEffect(() => () => {
    const recognition = session.current;
    session.current = null;
    recognition?.abort();
    onActive(false);
  }, [onActive]);

  if (!Recognizer) {
    return (
      <p className="text-xs text-slate-500 dark:text-slate-400">
        Voice input is not supported in this browser. Please type directly.
      </p>
    );
  }

  function toggle() {
    if (session.current) {
      setPhase('stopping');
      try {
        session.current.stop();
      } catch (_) {}
      return;
    }

    if (!Recognizer) return;

    let accumulatedFinal = '';
    const finish = (notice: string, error = false) => {
      session.current = null;
      setPhase('idle');
      setInterim('');
      setMessage(notice);
      setIsError(error);
      onActive(false);
    };

    try {
      const recognition = new Recognizer();
      session.current = recognition;
      recognition.lang = navigator.language || 'en-US';
      recognition.continuous = true;
      recognition.interimResults = true;

      recognition.onresult = (event: any) => {
        if (session.current !== recognition) return;
        let currentInterim = '';

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const result = event.results[i];
          const text = result[0]?.transcript || '';
          if (result.isFinal) {
            accumulatedFinal = (accumulatedFinal + ' ' + text.trim()).trim();
            onText(accumulatedFinal);
          } else {
            currentInterim = (currentInterim + ' ' + text.trim()).trim();
          }
        }
        setInterim(currentInterim);
      };

      recognition.onerror = (event: any) => {
        if (session.current !== recognition) return;
        if (event.error === 'no-speech') return;

        const notices: Record<string, string> = {
          'not-allowed': 'Microphone access was denied. Please allow microphone permissions.',
          'service-not-allowed': 'Speech service is unavailable in this environment.',
          'audio-capture': 'No microphone hardware found.',
          'network': 'Network error reaching browser speech recognition service.',
        };

        finish(notices[event.error] ?? `Voice recognition error: ${event.error}`, true);
      };

      recognition.onend = () => {
        if (session.current === recognition) {
          finish(accumulatedFinal ? 'Dictation added to description.' : 'Listening finished.');
        }
      };

      setPhase('listening');
      setIsError(false);
      onActive(true);
      setMessage('Listening... Speak into your microphone.');
      recognition.start();
    } catch {
      finish('Could not start dictation. Check your microphone access.', true);
    }
  }

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex flex-wrap items-center gap-2.5">
        <button
          type="button"
          onClick={toggle}
          disabled={phase === 'stopping'}
          className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
            phase === 'listening'
              ? 'bg-rose-500 text-white animate-pulse shadow-md shadow-rose-500/30'
              : 'bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-500/20 border border-indigo-500/20'
          }`}
        >
          {phase === 'listening' ? (
            <>
              <MicOff size={14} />
              <span>Stop Dictation</span>
            </>
          ) : (
            <>
              <Mic size={14} className="text-indigo-600 dark:text-indigo-400" />
              <span>{phase === 'stopping' ? 'Finishing…' : 'Dictate Description'}</span>
            </>
          )}
        </button>

        {phase === 'listening' && (
          <div className="flex items-center gap-1.5 text-xs text-rose-500 font-medium">
            <span className="flex gap-0.5 items-end h-3">
              <span className="w-0.5 h-2 bg-rose-500 animate-bounce" style={{ animationDelay: '0ms' }} />
              <span className="w-0.5 h-3 bg-rose-500 animate-bounce" style={{ animationDelay: '150ms' }} />
              <span className="w-0.5 h-1.5 bg-rose-500 animate-bounce" style={{ animationDelay: '300ms' }} />
            </span>
            <span>Microphone active</span>
          </div>
        )}

        {message && (
          <span
            role="status"
            className={`text-xs inline-flex items-center gap-1 ${
              isError ? 'text-amber-600 dark:text-amber-400' : 'text-slate-500 dark:text-slate-400'
            }`}
          >
            {isError && <ShieldAlert className="w-3.5 h-3.5" />}
            {message}
          </span>
        )}
      </div>

      {interim && (
        <div className="flex items-center gap-2 text-xs text-indigo-600 dark:text-indigo-400 bg-indigo-50/50 dark:bg-indigo-950/30 px-2.5 py-1 rounded-lg border border-indigo-200/50 dark:border-indigo-800/50">
          <Volume2 className="w-3.5 h-3.5 animate-pulse" />
          <span>Hearing: <em>"{interim}..."</em></span>
        </div>
      )}
    </div>
  );
}

const GYM_NAME = 'Sport Gym';

let audioUnlocked = false;
let keepAliveTimer: ReturnType<typeof setInterval> | null = null;
let cachedVoice: SpeechSynthesisVoice | undefined;

function getSpeech(): SpeechSynthesis | null {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
    return null;
  }
  return window.speechSynthesis;
}

function unlockAudioContext(): void {
  try {
    const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Ctor) {
      return;
    }
    const ctx = new Ctor();
    void ctx.resume();
    const buffer = ctx.createBuffer(1, 1, 22050);
    const source = ctx.createBufferSource();
    source.buffer = buffer;
    source.connect(ctx.destination);
    source.start(0);
  } catch {
    /* ignorar */
  }
}

function startSpeechKeepAlive(speech: SpeechSynthesis): void {
  stopSpeechKeepAlive();
  keepAliveTimer = setInterval(() => {
    if (speech.speaking || speech.pending) {
      speech.resume();
    } else {
      stopSpeechKeepAlive();
    }
  }, 400);
}

function stopSpeechKeepAlive(): void {
  if (keepAliveTimer !== null) {
    clearInterval(keepAliveTimer);
    keepAliveTimer = null;
  }
}

/** Evita voces robóticas (espeak, pico…) y prioriza Google / Microsoft en español. */
function scoreVoice(voice: SpeechSynthesisVoice): number {
  const name = voice.name.toLowerCase();
  const lang = voice.lang.toLowerCase();
  let score = 0;

  if (lang.startsWith('es-co')) score += 40;
  if (lang.startsWith('es-es')) score += 28;
  if (lang.startsWith('es-mx')) score += 24;
  if (lang.startsWith('es-us')) score += 22;
  if (lang.startsWith('es')) score += 10;

  if (/google.*español|español.*google|google español|google español/i.test(voice.name)) score += 55;
  if (/microsoft.*(helena|laura|sabina|elvira|raúl|raul)/i.test(voice.name)) score += 50;
  if (/paulina|monica|flo|soledad|jorge|diego|carlos/i.test(name)) score += 42;
  if (/neural|natural|premium|online|enhanced/i.test(name)) score += 30;
  if (!voice.localService) score += 12;

  if (/espeak|festival|pico|android|samsung|samb|mbrola|kal|diphone/i.test(name)) score -= 120;

  return score;
}

function pickSpanishVoice(speech: SpeechSynthesis): SpeechSynthesisVoice | undefined {
  if (cachedVoice) {
    return cachedVoice;
  }
  const voices = speech.getVoices();
  if (voices.length === 0) {
    return undefined;
  }
  const es = voices.filter((v) => v.lang.toLowerCase().startsWith('es'));
  const pool = es.length > 0 ? es : voices;
  const best = [...pool].sort((a, b) => scoreVoice(b) - scoreVoice(a))[0];
  cachedVoice = best;
  return best;
}

interface UtteranceOpts {
  rate?: number;
  pitch?: number;
  lang?: string;
}

function buildUtterance(text: string, opts?: UtteranceOpts): SpeechSynthesisUtterance {
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = opts?.lang ?? 'es-CO';
  utterance.rate = opts?.rate ?? 0.94;
  utterance.pitch = opts?.pitch ?? 1;
  utterance.volume = 1;
  return utterance;
}

interface SpeechSegment {
  text: string;
  rate?: number;
  pitch?: number;
  pauseBeforeMs?: number;
}

/**
 * Varias frases encadenadas suenan más naturales que una sola oración larga.
 * Llamar solo desde un clic/toque (sin await antes).
 */
function speakSequence(speech: SpeechSynthesis, segments: SpeechSegment[]): void {
  if (segments.length === 0) {
    return;
  }

  unlockAudioContext();
  speech.resume();
  const voice = pickSpanishVoice(speech);
  let index = 0;

  const speakNext = (): void => {
    if (index >= segments.length) {
      stopSpeechKeepAlive();
      return;
    }

    const seg = segments[index++];
    const run = (): void => {
      const utterance = buildUtterance(seg.text, { rate: seg.rate, pitch: seg.pitch });
      if (voice) {
        utterance.voice = voice;
      }
      utterance.onend = speakNext;
      utterance.onerror = speakNext;
      startSpeechKeepAlive(speech);
      speech.speak(utterance);
    };

    if (seg.pauseBeforeMs && seg.pauseBeforeMs > 0) {
      setTimeout(run, seg.pauseBeforeMs);
    } else {
      run();
    }
  };

  speech.cancel();
  setTimeout(speakNext, 60);
}

/** Carga y cachea la mejor voz disponible. */
export function prepareWelcomeSpeech(): void {
  const speech = getSpeech();
  if (!speech) {
    return;
  }
  const refresh = (): void => {
    cachedVoice = undefined;
    pickSpanishVoice(speech);
  };
  refresh();
  speech.addEventListener('voiceschanged', refresh, { once: true });
}

export function unlockWelcomeAudio(): boolean {
  const speech = getSpeech();
  if (!speech) {
    return false;
  }

  speakSequence(speech, [{ text: 'Listo. Bienvenida con voz activada.', rate: 0.95 }]);
  audioUnlocked = true;
  return true;
}

export function isWelcomeAudioUnlocked(): boolean {
  return audioUnlocked;
}

export function isWelcomeAudioSupported(): boolean {
  return getSpeech() !== null;
}

/** Nombre con capitalización natural para la voz. */
function formatNameForSpeech(name: string): string {
  const trimmed = name.trim();
  if (!trimmed) {
    return '';
  }
  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1).toLowerCase();
}

export function welcomeSpeechText(firstName?: string | null): string {
  const name = firstName?.trim();
  return name
    ? `¡Bienvenido a ${GYM_NAME}, ${name}!`
    : `¡Bienvenido a ${GYM_NAME}!`;
}

export function welcomeHeadline(firstName?: string | null): string {
  const name = firstName?.trim();
  return name ? `¡Bienvenido, ${name}!` : '¡Bienvenido!';
}

function welcomeSegments(firstName?: string | null): SpeechSegment[] {
  const name = formatNameForSpeech(firstName ?? '');
  if (name) {
    return [
      { text: `¡Hola! Bienvenido a ${GYM_NAME}.`, rate: 0.96, pitch: 1 },
      { text: name, rate: 0.86, pitch: 1.06, pauseBeforeMs: 220 },
      { text: 'Que tengas un excelente entrenamiento.', rate: 0.93, pitch: 0.98, pauseBeforeMs: 180 },
    ];
  }
  return [
    { text: `¡Hola! Bienvenido a ${GYM_NAME}.`, rate: 0.96, pitch: 1 },
    { text: 'Que tengas un excelente entrenamiento.', rate: 0.93, pitch: 0.98, pauseBeforeMs: 200 },
  ];
}

/** Mensaje de bienvenida con nombre — llamar desde clic/toque, sin await antes. */
export function playAccessWelcome(firstName?: string | null): boolean {
  const speech = getSpeech();
  if (!speech) {
    return false;
  }
  speakSequence(speech, welcomeSegments(firstName));
  return true;
}

export function firstNameFromFullName(fullName?: string | null): string | null {
  if (!fullName?.trim()) {
    return null;
  }
  return fullName.trim().split(/\s+/)[0] ?? null;
}

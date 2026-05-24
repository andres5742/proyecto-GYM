import type { AccessVerifyResponse } from '../models/access.model';
import type { Gender } from '../models/member.model';

const GYM_NAME = 'Sport Gym R.10';
const MEMBERSHIP_WARNING_DAYS = 5;
const TIQUETERA_LOW_ENTRIES = 3;

export interface AccessWelcomeAudioHints {
  membershipDaysRemaining?: number | null;
  tiqueteraEntriesRemainingAfter?: number | null;
  tiqueteraPlan?: boolean | null;
}

/** "Bienvenida" solo para FEMALE; en los demás casos "Bienvenido". */
export function welcomeWord(gender?: Gender | null): string {
  return gender === 'FEMALE' ? 'Bienvenida' : 'Bienvenido';
}

let audioUnlocked = false;
let keepAliveTimer: ReturnType<typeof setInterval> | null = null;
let cachedVoice: SpeechSynthesisVoice | undefined;
let lastSpeechSignature = '';
let lastSpeechAt = 0;

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

function dedupeSpeech(signature: string, windowMs = 9000): boolean {
  const now = Date.now();
  if (signature === lastSpeechSignature && now - lastSpeechAt < windowMs) {
    return false;
  }
  lastSpeechSignature = signature;
  lastSpeechAt = now;
  return true;
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

  speakSequence(speech, [{ text: 'Sistema activado.', rate: 0.95 }]);
  audioUnlocked = true;
  return true;
}

/** Desbloquea audio sin reproducir frase (atajos como F2 en recepción). */
export function ensureWelcomeAudioUnlocked(): boolean {
  const speech = getSpeech();
  if (!speech) {
    return false;
  }
  unlockAudioContext();
  speech.resume();
  prepareWelcomeSpeech();
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

export function welcomeSpeechText(firstName?: string | null, gender?: Gender | null): string {
  const word = welcomeWord(gender);
  const name = firstName?.trim();
  return name ? `¡${word} a ${GYM_NAME}, ${name}!` : `¡${word} a ${GYM_NAME}!`;
}

export function welcomeHeadline(firstName?: string | null, gender?: Gender | null): string {
  const word = welcomeWord(gender);
  const name = firstName?.trim();
  return name ? `¡${word}, ${name}!` : `¡${word}!`;
}

/** Saludo de entrenador en torniquete e historial. */
export function staffWelcomeHeadline(gender?: Gender | null): string {
  const word = welcomeWord(gender);
  return `¡${word}! Que tenga un excelente entreno.`;
}

function staffWelcomeSegments(gender?: Gender | null): SpeechSegment[] {
  const word = welcomeWord(gender);
  const pitch = gender === 'FEMALE' ? 1.04 : 1;
  return [
    { text: `¡${word}!`, rate: 0.95, pitch },
    {
      text: 'Que tenga un excelente entreno.',
      rate: 0.93,
      pitch: 0.98,
      pauseBeforeMs: 320,
    },
  ];
}

function staffWelcomeSegmentsFromMessage(
  logMessage?: string | null,
  gender?: Gender | null,
): SpeechSegment[] {
  const base = welcomeBaseFromLogMessage(logMessage);
  if (!base) {
    return staffWelcomeSegments(gender);
  }
  const excelenteIdx = base.toLowerCase().indexOf('que tenga un excelente entreno');
  if (excelenteIdx < 0) {
    const pitch = gender === 'FEMALE' ? 1.04 : 1;
    return [{ text: base, rate: 0.95, pitch }];
  }
  const intro = base.slice(0, excelenteIdx).trim();
  const outro = base.slice(excelenteIdx).trim();
  const pitch = gender === 'FEMALE' ? 1.04 : 1;
  return [
    { text: intro, rate: 0.95, pitch },
    { text: outro, rate: 0.93, pitch: 0.98, pauseBeforeMs: 320 },
  ];
}

/** Voz para entrenador: bienvenida + excelente entreno. */
export function playStaffAccessWelcome(gender?: Gender | null, logMessage?: string | null): boolean {
  const speech = getSpeech();
  if (!speech) {
    return false;
  }
  const segments = staffWelcomeSegmentsFromMessage(logMessage, gender);
  const signature = `staff|${segments.map((s) => s.text).join('|')}`;
  if (!dedupeSpeech(signature)) {
    return true;
  }
  speakSequence(speech, segments);
  return true;
}

export function resolveStaffWelcomeText(
  message: string | null | undefined,
  gender?: Gender | null,
): string {
  return welcomeBaseFromLogMessage(message) ?? staffWelcomeHeadline(gender);
}

function membershipDaysPhrase(days: number): string {
  if (days === 1) {
    return 'Te queda un día de entreno antes de que venza tu membresía.';
  }
  return `Te quedan ${days} días de entreno antes de que venza tu membresía.`;
}

function tiqueteraEntriesPhrase(left: number): string {
  if (left === 0) {
    return 'Este era tu último entreno de la tiquetera.';
  }
  if (left === 1) {
    return 'Te queda un entreno en tu tiquetera.';
  }
  return `Te quedan ${left} entrenos en tu tiquetera.`;
}

function welcomeSegments(
  firstName?: string | null,
  gender?: Gender | null,
  hints?: AccessWelcomeAudioHints | null,
): SpeechSegment[] {
  const word = welcomeWord(gender);
  const name = formatNameForSpeech(firstName ?? '');
  const pitch = gender === 'FEMALE' ? 1.04 : 1;
  const segments: SpeechSegment[] = [];

  if (name) {
    segments.push({ text: `¡${word}, ${name}!`, rate: 0.95, pitch });
  } else {
    segments.push({ text: `¡${word}!`, rate: 0.95, pitch });
  }

  const days = hints?.membershipDaysRemaining;
  if (days != null && days > 0 && days <= MEMBERSHIP_WARNING_DAYS) {
    segments.push({
      text: membershipDaysPhrase(days),
      rate: 0.92,
      pitch: 0.98,
      pauseBeforeMs: 380,
    });
  }

  if (hints?.tiqueteraPlan) {
    const left = hints.tiqueteraEntriesRemainingAfter;
    if (left != null && left >= 0 && left < TIQUETERA_LOW_ENTRIES) {
      segments.push({
        text: tiqueteraEntriesPhrase(left),
        rate: 0.91,
        pitch: 0.98,
        pauseBeforeMs: 360,
      });
    }
  }

  return segments;
}

/** Misma frase base que guarda el backend en el historial de ingresos. */
export function welcomeBaseFromLogMessage(message?: string | null): string | null {
  if (!message?.trim()) {
    return null;
  }
  const cut = message.trim().split(/\s+Te quedan|\s+Este era/)[0]?.trim();
  return cut || null;
}

function welcomeSegmentsWithHeadline(
  headline: string,
  hints?: AccessWelcomeAudioHints | null,
  gender?: Gender | null,
): SpeechSegment[] {
  const pitch = gender === 'FEMALE' ? 1.04 : 1;
  const segments: SpeechSegment[] = [{ text: headline, rate: 0.95, pitch }];

  const days = hints?.membershipDaysRemaining;
  if (days != null && days > 0 && days <= MEMBERSHIP_WARNING_DAYS) {
    segments.push({
      text: membershipDaysPhrase(days),
      rate: 0.92,
      pitch: 0.98,
      pauseBeforeMs: 380,
    });
  }

  if (hints?.tiqueteraPlan) {
    const left = hints.tiqueteraEntriesRemainingAfter;
    if (left != null && left >= 0 && left < TIQUETERA_LOW_ENTRIES) {
      segments.push({
        text: tiqueteraEntriesPhrase(left),
        rate: 0.91,
        pitch: 0.98,
        pauseBeforeMs: 360,
      });
    }
  }

  return segments;
}

export function accessWelcomeHintsFromResponse(
  res: Pick<
    AccessVerifyResponse,
    'membershipDaysRemaining' | 'tiqueteraEntriesRemainingAfter' | 'tiqueteraPlan'
  >,
): AccessWelcomeAudioHints {
  return {
    membershipDaysRemaining: res.membershipDaysRemaining ?? null,
    tiqueteraEntriesRemainingAfter: res.tiqueteraEntriesRemainingAfter ?? null,
    tiqueteraPlan: res.tiqueteraPlan ?? null,
  };
}

/** Mensaje de bienvenida con nombre — llamar desde clic/toque, sin await antes. */
export function playAccessWelcome(
  firstName?: string | null,
  gender?: Gender | null,
  hints?: AccessWelcomeAudioHints | null,
  logMessage?: string | null,
): boolean {
  const speech = getSpeech();
  if (!speech) {
    return false;
  }
  const fromLog = welcomeBaseFromLogMessage(logMessage);
  const segments = fromLog
    ? welcomeSegmentsWithHeadline(fromLog, hints, gender)
    : welcomeSegments(firstName, gender, hints);
  const signature = `member|${segments.map((s) => s.text).join('|')}`;
  if (!dedupeSpeech(signature)) {
    return true;
  }
  speakSequence(speech, segments);
  return true;
}

/** Texto visible y hablado alineado con el historial de ingresos. */
export function resolveMemberWelcomeText(
  message: string | null | undefined,
  firstName?: string | null,
  gender?: Gender | null,
): string {
  return welcomeBaseFromLogMessage(message) ?? welcomeHeadline(firstName, gender);
}

/** Anuncio corto (ej. último ticket en recepción). */
export function speakAnnouncement(text: string): boolean {
  const speech = getSpeech();
  if (!speech || !text.trim()) {
    return false;
  }
  speakSequence(speech, [{ text: text.trim(), rate: 0.93, pitch: 1 }]);
  return true;
}

export function firstNameFromFullName(fullName?: string | null): string | null {
  if (!fullName?.trim()) {
    return null;
  }
  return fullName.trim().split(/\s+/)[0] ?? null;
}

/** Voz de bienvenida en el torniquete (síntesis en español). */
export function playAccessWelcome(firstName?: string | null): void {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
    return;
  }
  const name = firstName?.trim();
  const text = name
    ? `¡Bienvenido a Sport Gym, ${name}! Que tengas un excelente entrenamiento.`
    : '¡Bienvenido a Sport Gym! Que tengas un excelente entrenamiento.';

  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'es-CO';
  utterance.rate = 0.92;
  utterance.pitch = 1.05;
  utterance.volume = 1;

  const pickSpanishVoice = (): SpeechSynthesisVoice | undefined => {
    const voices = window.speechSynthesis.getVoices();
    return (
      voices.find((v) => v.lang.toLowerCase().startsWith('es') && v.name.toLowerCase().includes('female'))
      ?? voices.find((v) => v.lang.toLowerCase().startsWith('es'))
      ?? voices[0]
    );
  };

  const speak = (): void => {
    const voice = pickSpanishVoice();
    if (voice) {
      utterance.voice = voice;
    }
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
  };

  if (window.speechSynthesis.getVoices().length === 0) {
    window.speechSynthesis.onvoiceschanged = () => {
      window.speechSynthesis.onvoiceschanged = null;
      speak();
    };
  } else {
    speak();
  }
}

export function firstNameFromFullName(fullName?: string | null): string | null {
  if (!fullName?.trim()) {
    return null;
  }
  return fullName.trim().split(/\s+/)[0] ?? null;
}

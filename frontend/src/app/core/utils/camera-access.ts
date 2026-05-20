/** Mensajes claros para errores de getUserMedia. */
export function cameraErrorMessage(error: unknown): string {
  if (!isSecureCameraContext()) {
    return (
      'La cámara solo funciona en HTTPS o en http://localhost. ' +
      'Abre el sitio como https://sportgymr10.com o http://localhost:4201'
    );
  }

  if (!navigator.mediaDevices?.getUserMedia) {
    return 'Tu navegador no soporta acceso a la cámara. Usa Chrome, Edge o Brave actualizado.';
  }

  const name = error instanceof DOMException ? error.name : '';
  switch (name) {
    case 'NotAllowedError':
    case 'PermissionDeniedError':
      return (
        'Permiso de cámara denegado. En el candado de la barra de direcciones elige ' +
        '«Permitir» y recarga la página (o pulsa Reintentar).'
      );
    case 'NotFoundError':
    case 'DevicesNotFoundError':
      return 'No se encontró ninguna cámara conectada al equipo.';
    case 'NotReadableError':
    case 'TrackStartError':
      return (
        'La cámara está en uso por otra aplicación (Zoom, Teams, otra pestaña). ' +
        'Ciérrala y pulsa Reintentar.'
      );
    case 'OverconstrainedError':
      return 'La cámara no aceptó la resolución pedida. Pulsa Reintentar.';
    case 'AbortError':
      return 'Acceso a la cámara cancelado. Pulsa Reintentar.';
    default:
      if (error instanceof Error && error.message) {
        return `Cámara: ${error.message}`;
      }
      return 'No se pudo acceder a la webcam. Pulsa Reintentar o recarga la página.';
  }
}

export function isSecureCameraContext(): boolean {
  return typeof window !== 'undefined' && window.isSecureContext;
}

/** Prueba varias restricciones (algunas webcams fallan con max width/height). */
export async function openCameraStream(): Promise<MediaStream> {
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new DOMException('getUserMedia no disponible', 'NotSupportedError');
  }

  const attempts: MediaStreamConstraints[] = [
    { video: { facingMode: 'user' }, audio: false },
    { video: { width: { ideal: 640 }, height: { ideal: 480 } }, audio: false },
    { video: true, audio: false },
  ];

  let lastError: unknown;
  for (const constraints of attempts) {
    try {
      return await navigator.mediaDevices.getUserMedia(constraints);
    } catch (error) {
      lastError = error;
      if (error instanceof DOMException && error.name === 'NotAllowedError') {
        throw error;
      }
    }
  }
  throw lastError ?? new DOMException('No camera', 'NotFoundError');
}

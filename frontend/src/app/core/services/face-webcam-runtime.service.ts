import { Injectable, signal } from '@angular/core';

type FaceApiModule = typeof import('@vladmandic/face-api');

/** Opciones rápidas para ingreso en tiempo real. */
const ACCESS_DETECTOR = { inputSize: 224, scoreThreshold: 0.42 } as const;
/** Opciones más precisas al registrar en recepción. */
const ENROLL_DETECTOR = { inputSize: 320, scoreThreshold: 0.5 } as const;

@Injectable({ providedIn: 'root' })
export class FaceWebcamRuntimeService {
  private readonly modelBase = 'https://cdn.jsdelivr.net/npm/@vladmandic/face-api@1.7.15/model';
  readonly modelsReady = signal(false);
  readonly modelsError = signal<string | null>(null);

  private loadPromise: Promise<FaceApiModule> | null = null;
  private faceApi: FaceApiModule | null = null;

  ensureModels(): Promise<FaceApiModule> {
    if (this.faceApi && this.modelsReady()) {
      return Promise.resolve(this.faceApi);
    }
    if (!this.loadPromise) {
      this.loadPromise = this.loadModels();
    }
    return this.loadPromise;
  }

  private async loadModels(): Promise<FaceApiModule> {
    try {
      const faceapi = await import('@vladmandic/face-api');
      await Promise.all([
        faceapi.nets.tinyFaceDetector.loadFromUri(this.modelBase),
        faceapi.nets.faceLandmark68Net.loadFromUri(this.modelBase),
        faceapi.nets.faceRecognitionNet.loadFromUri(this.modelBase),
      ]);
      this.faceApi = faceapi;
      this.modelsReady.set(true);
      return faceapi;
    } catch {
      this.modelsError.set('No se pudieron cargar los modelos de reconocimiento facial');
      throw new Error(this.modelsError() ?? 'Model load failed');
    }
  }

  /** Detección rápida para torniquete / acceso continuo. */
  async detectDescriptorForAccess(video: HTMLVideoElement): Promise<number[] | null> {
    return this.detectDescriptor(video, ACCESS_DETECTOR);
  }

  /** Detección para enrolamiento en recepción. */
  async detectDescriptorForEnroll(video: HTMLVideoElement): Promise<number[] | null> {
    return this.detectDescriptor(video, ENROLL_DETECTOR);
  }

  private async detectDescriptor(
    video: HTMLVideoElement,
    options: { inputSize: number; scoreThreshold: number },
  ): Promise<number[] | null> {
    if (video.readyState < 2 || video.videoWidth === 0) {
      return null;
    }
    const faceapi = await this.ensureModels();
    const detection = await faceapi
      .detectSingleFace(video, new faceapi.TinyFaceDetectorOptions(options))
      .withFaceLandmarks()
      .withFaceDescriptor();
    if (!detection) {
      return null;
    }
    return Array.from(detection.descriptor);
  }

  /** Registro en recepción: 2 muestras rápidas y promedio. */
  async captureStableDescriptor(video: HTMLVideoElement): Promise<number[] | null> {
    const collected: number[][] = [];
    for (let i = 0; i < 2; i++) {
      const sample = await this.detectDescriptorForEnroll(video);
      if (sample) {
        collected.push(sample);
      }
      if (i === 0) {
        await delay(180);
      }
    }
    if (collected.length === 0) {
      return null;
    }
    return averageDescriptors(collected);
  }
}

function averageDescriptors(descriptors: number[][]): number[] {
  const length = descriptors[0].length;
  const out = new Array<number>(length).fill(0);
  for (const descriptor of descriptors) {
    for (let i = 0; i < length; i++) {
      out[i] += descriptor[i];
    }
  }
  for (let i = 0; i < length; i++) {
    out[i] /= descriptors.length;
  }
  return out;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

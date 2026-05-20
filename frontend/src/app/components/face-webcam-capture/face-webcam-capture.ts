import {
  afterNextRender,
  Component,
  ElementRef,
  inject,
  Injector,
  input,
  OnDestroy,
  OnInit,
  output,
  signal,
  viewChild,
} from '@angular/core';
import {
  CameraFacing,
  cameraErrorMessage,
  openCameraStream,
} from '../../core/utils/camera-access';
import { FaceWebcamRuntimeService } from '../../core/services/face-webcam-runtime.service';

@Component({
  selector: 'app-face-webcam-capture',
  standalone: true,
  templateUrl: './face-webcam-capture.html',
  styleUrl: './face-webcam-capture.scss',
})
export class FaceWebcamCaptureComponent implements OnInit, OnDestroy {
  private readonly runtime = inject(FaceWebcamRuntimeService);
  private readonly injector = inject(Injector);

  readonly compact = input(false);
  /** Muestra botones frontal / trasera (registro en celular). */
  readonly allowCameraSwitch = input(false);
  readonly statusChange = output<string>();
  readonly faceDetected = output<boolean>();

  protected readonly cameraError = signal<string | null>(null);
  protected readonly modelsLoading = signal(true);
  protected readonly faceInFrame = signal(false);
  protected readonly facingMode = signal<CameraFacing>('user');
  protected readonly switchingCamera = signal(false);

  private readonly videoRef = viewChild<ElementRef<HTMLVideoElement>>('video');
  private stream: MediaStream | null = null;

  async ngOnInit(): Promise<void> {
    await this.whenViewReady();
    await this.initCapture();
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }

  async retryCamera(): Promise<void> {
    this.cameraError.set(null);
    this.statusChange.emit('Reintentando cámara…');
    await this.whenViewReady();
    await this.startCamera();
  }

  async selectCamera(facing: CameraFacing): Promise<void> {
    if (!this.allowCameraSwitch() || this.facingMode() === facing || this.switchingCamera()) {
      return;
    }
    this.facingMode.set(facing);
    this.switchingCamera.set(true);
    this.statusChange.emit(facing === 'user' ? 'Cambiando a cámara frontal…' : 'Cambiando a cámara trasera…');
    await this.startCamera();
    this.switchingCamera.set(false);
  }

  private whenViewReady(): Promise<void> {
    return new Promise((resolve) => {
      afterNextRender(() => resolve(), { injector: this.injector });
    });
  }

  private async initCapture(): Promise<void> {
    this.statusChange.emit('Preparando cámara…');

    const modelsTask = this.runtime
      .ensureModels()
      .then(() => {
        this.modelsLoading.set(false);
      })
      .catch(() => {
        const msg = this.runtime.modelsError() ?? 'No se pudieron cargar los modelos faciales';
        this.cameraError.set(msg);
        this.statusChange.emit(msg);
        this.modelsLoading.set(false);
      });

    await this.startCamera();
    await modelsTask;
  }

  getVideoElement(): HTMLVideoElement | null {
    if (this.cameraError()) {
      return null;
    }
    return this.videoRef()?.nativeElement ?? null;
  }

  async captureDescriptor(): Promise<number[] | null> {
    const video = this.getVideoElement();
    if (!video) {
      return null;
    }
    this.statusChange.emit('Capturando rostro…');
    const descriptor = await this.runtime.captureStableDescriptor(video);
    if (!descriptor) {
      this.statusChange.emit('No se detectó un rostro claro. Mira de frente a la cámara.');
    }
    return descriptor;
  }

  async detectForAccess(): Promise<number[] | null> {
    const video = this.getVideoElement();
    if (!video || video.readyState < 2) {
      return null;
    }
    const descriptor = await this.runtime.detectDescriptorForAccess(video);
    const found = descriptor !== null;
    this.faceInFrame.set(found);
    this.faceDetected.emit(found);
    return descriptor;
  }

  private async startCamera(): Promise<void> {
    this.stopCamera();
    const video = this.videoRef()?.nativeElement;
    if (!video) {
      return;
    }

    try {
      this.stream = await openCameraStream({ facingMode: this.facingMode() });
      video.setAttribute('playsinline', 'true');
      video.muted = true;
      video.srcObject = this.stream;
      await video.play();
      this.cameraError.set(null);
      const label =
        this.facingMode() === 'user'
          ? 'Mira a la cámara frontal'
          : 'Mira a la cámara trasera';
      this.statusChange.emit(label);
    } catch (error) {
      const msg = cameraErrorMessage(error);
      this.cameraError.set(msg);
      this.statusChange.emit(msg);
    }
  }

  private stopCamera(): void {
    if (this.stream) {
      this.stream.getTracks().forEach((t) => t.stop());
      this.stream = null;
    }
    const video = this.videoRef()?.nativeElement;
    if (video) {
      video.srcObject = null;
    }
  }
}

import { Injectable } from '@angular/core';

export type CameraFacingMode = 'user' | 'environment';

@Injectable({ providedIn: 'root' })
export class CameraCaptureService {
  async open(facingMode: CameraFacingMode): Promise<MediaStream> {
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error('Camera is not available on this device.');
    }

    const constraints = this.constraintsFor(facingMode);
    try {
      return await navigator.mediaDevices.getUserMedia(constraints);
    } catch (error) {
      if (facingMode === 'environment') {
        return navigator.mediaDevices.getUserMedia(this.constraintsFor('user'));
      }
      return navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 640 }, height: { ideal: 480 } }
      });
    }
  }

  attach(videoElement: HTMLVideoElement, stream: MediaStream): void {
    videoElement.srcObject = stream;
    void videoElement.play();
  }

  capture(videoElement: HTMLVideoElement): string {
    const width = videoElement.videoWidth;
    const height = videoElement.videoHeight;
    if (!width || !height) {
      throw new Error('Camera is not initialized.');
    }

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('Unable to capture photo.');
    }

    context.drawImage(videoElement, 0, 0);
    return canvas.toDataURL('image/jpeg', 0.8);
  }

  stop(stream: MediaStream | null): void {
    stream?.getTracks().forEach(track => track.stop());
  }

  toggle(facingMode: CameraFacingMode): CameraFacingMode {
    return facingMode === 'user' ? 'environment' : 'user';
  }

  label(facingMode: CameraFacingMode): string {
    return facingMode === 'user' ? 'Front camera' : 'Back camera';
  }

  private constraintsFor(facingMode: CameraFacingMode): MediaStreamConstraints {
    return {
      video: {
        facingMode: { ideal: facingMode },
        width: { ideal: 640 },
        height: { ideal: 480 },
      }
    };
  }
}

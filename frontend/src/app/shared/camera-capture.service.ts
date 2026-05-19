import { Injectable } from '@angular/core';

export type CameraFacingMode = 'user' | 'environment';

export interface CameraDeviceOption {
  deviceId: string;
  label: string;
}

@Injectable({ providedIn: 'root' })
export class CameraCaptureService {
  async open(facingMode: CameraFacingMode, deviceId?: string): Promise<MediaStream> {
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error('Camera is not available on this device.');
    }

    const constraints = deviceId ? this.deviceConstraintsFor(deviceId) : this.constraintsFor(facingMode);
    try {
      return await navigator.mediaDevices.getUserMedia(constraints);
    } catch (error) {
      if (deviceId) {
        return navigator.mediaDevices.getUserMedia(this.constraintsFor(facingMode));
      }
      if (facingMode === 'environment') {
        return navigator.mediaDevices.getUserMedia(this.constraintsFor('user'));
      }
      return navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 640 }, height: { ideal: 480 } }
      });
    }
  }

  async listVideoDevices(): Promise<CameraDeviceOption[]> {
    if (!navigator.mediaDevices?.enumerateDevices) {
      return [];
    }
    const devices = await navigator.mediaDevices.enumerateDevices();
    return devices
      .filter(device => device.kind === 'videoinput')
      .map((device, index) => ({
        deviceId: device.deviceId,
        label: device.label || `Camera ${index + 1}`,
      }));
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
    return canvas.toDataURL('image/jpeg', 0.85);
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

  deviceLabel(device: CameraDeviceOption, index: number): string {
    return device.label || `Camera ${index + 1}`;
  }

  private constraintsFor(facingMode: CameraFacingMode): MediaStreamConstraints {
    return {
      video: {
        facingMode: { ideal: facingMode },
        width: { ideal: 1280 },
        height: { ideal: 720 },
      }
    };
  }

  private deviceConstraintsFor(deviceId: string): MediaStreamConstraints {
    return {
      video: {
        deviceId: { exact: deviceId },
        width: { ideal: 1280 },
        height: { ideal: 720 },
      }
    };
  }
}

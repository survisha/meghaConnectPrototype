import { Injectable } from '@angular/core';
import { FaceLandmarker, FilesetResolver, NormalizedLandmark } from '@mediapipe/tasks-vision';
import {
  FACE_LIVENESS_INITIAL_RESULT,
  FaceLivenessCaptureMetadata,
  FaceLivenessResult,
} from './face-liveness-result.model';

@Injectable({ providedIn: 'root' })
export class CameraLivenessService {
  private faceLandmarker: FaceLandmarker | null = null;
  private loadingPromise: Promise<FaceLandmarker> | null = null;
  private readonly lightCanvas = document.createElement('canvas');

  async analyze(video: HTMLVideoElement): Promise<FaceLivenessResult> {
    if (!video.videoWidth || !video.videoHeight) {
      return {
        ...FACE_LIVENESS_INITIAL_RESULT,
        loading: true,
        message: 'Preparing face validation...',
      };
    }

    let faceLandmarker: FaceLandmarker;
    try {
      faceLandmarker = await this.getFaceLandmarker();
    } catch {
      return {
        ...FACE_LIVENESS_INITIAL_RESULT,
        message: 'Face validation is unavailable. Please try opening the camera again.',
      };
    }

    const result = faceLandmarker.detectForVideo(video, performance.now());
    const faceCount = result.faceLandmarks.length;
    const lighting = this.measureLighting(video);

    if (faceCount === 0) {
      return this.invalid('No face detected', faceCount, lighting.poorLighting);
    }

    if (faceCount > 1) {
      return this.invalid('Only one person should be visible', faceCount, lighting.poorLighting, true);
    }

    const landmarks = result.faceLandmarks[0];
    const box = this.faceBox(landmarks);
    const centerX = box.left + box.width / 2;
    const centerY = box.top + box.height / 2;

    // Thresholds are intentionally conservative because this is a first-level
    // capture guard, not a production biometric proof. They allow normal user
    // movement but block obvious side poses, tilt, distance issues, and dim frames.
    const faceCentered = Math.abs(centerX - 0.5) <= 0.14 && Math.abs(centerY - 0.5) <= 0.18;
    const tooFar = box.width < 0.22 || box.height < 0.28;
    const tooClose = box.width > 0.62 || box.height > 0.78;
    const tilted = Math.abs(this.eyeLineAngleDegrees(landmarks)) > 10;
    const frontFacing = this.isFrontFacing(landmarks);
    const poorLighting = lighting.poorLighting;

    const score = this.score({
      faceCentered,
      frontFacing,
      tilted,
      tooClose,
      tooFar,
      poorLighting,
    });

    const message = this.guidanceMessage({
      faceCentered,
      frontFacing,
      tilted,
      tooClose,
      tooFar,
      poorLighting,
    });

    const valid = faceCentered && frontFacing && !tilted && !tooClose && !tooFar && !poorLighting;

    return {
      valid,
      loading: false,
      message,
      score,
      faceCount,
      faceCentered,
      frontFacing,
      multipleFacesDetected: false,
      tilted,
      tooClose,
      tooFar,
      poorLighting,
      box,
    };
  }

  toCaptureMetadata(result: FaceLivenessResult): FaceLivenessCaptureMetadata {
    return {
      faceCentered: result.faceCentered,
      frontFacing: result.frontFacing,
      multipleFacesDetected: result.multipleFacesDetected,
      livenessScore: result.score,
      capturedAt: new Date().toISOString(),
    };
  }

  private async getFaceLandmarker(): Promise<FaceLandmarker> {
    if (this.faceLandmarker) {
      return this.faceLandmarker;
    }

    this.loadingPromise ??= this.createFaceLandmarker();
    this.faceLandmarker = await this.loadingPromise;
    return this.faceLandmarker;
  }

  private async createFaceLandmarker(): Promise<FaceLandmarker> {
    const vision = await FilesetResolver.forVisionTasks('/assets/mediapipe/wasm');
    return FaceLandmarker.createFromOptions(vision, {
      baseOptions: {
        modelAssetPath: '/assets/mediapipe/face_landmarker.task',
      },
      runningMode: 'VIDEO',
      numFaces: 2,
      minFaceDetectionConfidence: 0.55,
      minFacePresenceConfidence: 0.55,
      minTrackingConfidence: 0.55,
    });
  }

  private faceBox(landmarks: NormalizedLandmark[]) {
    const xs = landmarks.map(point => point.x);
    const ys = landmarks.map(point => point.y);
    const left = Math.max(0, Math.min(...xs));
    const top = Math.max(0, Math.min(...ys));
    const right = Math.min(1, Math.max(...xs));
    const bottom = Math.min(1, Math.max(...ys));
    return {
      left,
      top,
      width: right - left,
      height: bottom - top,
    };
  }

  private eyeLineAngleDegrees(landmarks: NormalizedLandmark[]): number {
    const leftEyeOuter = landmarks[33];
    const rightEyeOuter = landmarks[263];
    if (!leftEyeOuter || !rightEyeOuter) {
      return 0;
    }
    return Math.atan2(rightEyeOuter.y - leftEyeOuter.y, rightEyeOuter.x - leftEyeOuter.x) * 180 / Math.PI;
  }

  private isFrontFacing(landmarks: NormalizedLandmark[]): boolean {
    const nose = landmarks[1];
    const leftEyeOuter = landmarks[33];
    const rightEyeOuter = landmarks[263];
    const leftCheek = landmarks[234];
    const rightCheek = landmarks[454];

    if (!nose || !leftEyeOuter || !rightEyeOuter || !leftCheek || !rightCheek) {
      return false;
    }

    const eyeSpan = Math.max(Math.abs(rightEyeOuter.x - leftEyeOuter.x), 0.001);
    const eyeMidX = (leftEyeOuter.x + rightEyeOuter.x) / 2;
    const noseOffset = Math.abs(nose.x - eyeMidX) / eyeSpan;
    const leftCheekDistance = Math.abs(nose.x - leftCheek.x);
    const rightCheekDistance = Math.abs(rightCheek.x - nose.x);
    const cheekSymmetry = Math.abs(leftCheekDistance - rightCheekDistance) / Math.max(leftCheekDistance, rightCheekDistance, 0.001);

    return noseOffset <= 0.16 && cheekSymmetry <= 0.28;
  }

  private measureLighting(video: HTMLVideoElement): { average: number; poorLighting: boolean } {
    const sampleWidth = 80;
    const sampleHeight = 60;
    this.lightCanvas.width = sampleWidth;
    this.lightCanvas.height = sampleHeight;
    const context = this.lightCanvas.getContext('2d', { willReadFrequently: true });
    if (!context) {
      return { average: 128, poorLighting: false };
    }

    context.drawImage(video, 0, 0, sampleWidth, sampleHeight);
    const data = context.getImageData(0, 0, sampleWidth, sampleHeight).data;
    let total = 0;
    let totalSquared = 0;
    for (let index = 0; index < data.length; index += 4) {
      const luminance = 0.2126 * data[index] + 0.7152 * data[index + 1] + 0.0722 * data[index + 2];
      total += luminance;
      totalSquared += luminance * luminance;
    }

    const pixels = data.length / 4;
    const average = total / pixels;
    const variance = totalSquared / pixels - average * average;
    const contrast = Math.sqrt(Math.max(variance, 0));

    return {
      average,
      poorLighting: average < 48 || average > 235 || contrast < 16,
    };
  }

  private guidanceMessage(checks: {
    faceCentered: boolean;
    frontFacing: boolean;
    tilted: boolean;
    tooClose: boolean;
    tooFar: boolean;
    poorLighting: boolean;
  }): string {
    if (checks.poorLighting) return 'Improve lighting';
    if (!checks.faceCentered) return 'Move your face to the center';
    if (checks.tooFar) return 'Move closer to the camera';
    if (checks.tooClose) return 'Move slightly back';
    if (checks.tilted) return 'Keep your head level';
    if (!checks.frontFacing) return 'Please look straight at the camera';
    return 'Face verified. You can capture now.';
  }

  private score(checks: {
    faceCentered: boolean;
    frontFacing: boolean;
    tilted: boolean;
    tooClose: boolean;
    tooFar: boolean;
    poorLighting: boolean;
  }): number {
    let score = 1;
    if (!checks.faceCentered) score -= 0.2;
    if (!checks.frontFacing) score -= 0.25;
    if (checks.tilted) score -= 0.18;
    if (checks.tooClose || checks.tooFar) score -= 0.16;
    if (checks.poorLighting) score -= 0.18;
    return Math.max(0, Math.round(score * 100) / 100);
  }

  private invalid(message: string, faceCount: number, poorLighting: boolean, multipleFacesDetected = false): FaceLivenessResult {
    return {
      ...FACE_LIVENESS_INITIAL_RESULT,
      message: poorLighting ? 'Improve lighting' : message,
      faceCount,
      poorLighting,
      multipleFacesDetected,
    };
  }
}

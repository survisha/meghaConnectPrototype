export interface FaceLivenessBox {
  left: number;
  top: number;
  width: number;
  height: number;
}

export interface FaceLivenessCaptureMetadata {
  faceCentered: boolean;
  frontFacing: boolean;
  multipleFacesDetected: boolean;
  livenessScore: number;
  capturedAt: string;
}

export interface FaceLivenessResult {
  valid: boolean;
  loading: boolean;
  message: string;
  score: number;
  faceCount: number;
  faceCentered: boolean;
  frontFacing: boolean;
  multipleFacesDetected: boolean;
  tilted: boolean;
  tooClose: boolean;
  tooFar: boolean;
  poorLighting: boolean;
  box?: FaceLivenessBox;
}

export const FACE_LIVENESS_INITIAL_RESULT: FaceLivenessResult = {
  valid: false,
  loading: false,
  message: 'Start camera to verify face position.',
  score: 0,
  faceCount: 0,
  faceCentered: false,
  frontFacing: false,
  multipleFacesDetected: false,
  tilted: false,
  tooClose: false,
  tooFar: false,
  poorLighting: false,
};

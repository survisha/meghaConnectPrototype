import { of, Subject, Subscription } from 'rxjs';
import { PublicIdentificationComponent } from './public-identification.component';
import { FaceSearchResult } from '../services/face-recognition.service';
import { AutoFaceDetection } from '../shared/camera-liveness.service';
import { AUTO_FACE_RESULT_TIMEOUT_MS } from '../config/public-identification.constants';

describe('PublicIdentificationComponent face queue', () => {
  let component: PublicIdentificationComponent;
  let responses: Map<string, Subject<FaceSearchResult>>;
  let searchCalls: string[];
  let captureNumber: number;

  const face = (left: number, descriptor: number[]): AutoFaceDetection => ({
    box: { left, top: 0.2, width: 0.2, height: 0.3 },
    valid: true,
    score: 0.9,
    descriptor,
  });

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date('2026-01-01T00:00:00Z'));
    responses = new Map();
    searchCalls = [];
    captureNumber = 0;
    component = new PublicIdentificationComponent(
      { getPublicIdentificationHistory: () => of({ visitCount: 0, appointments: [], schemes: [] }) } as never,
      { captureCrop: () => `photo-${++captureNumber}`, stop: () => undefined } as never,
      { search: (photo: string) => {
        searchCalls.push(photo);
        const response = new Subject<FaceSearchResult>();
        responses.set(photo, response);
        return response;
      }} as never,
      {} as never,
      { error: () => undefined, success: () => undefined } as never,
      { search: () => of({ matches: [] }) } as never,
      {
        addRemark: () => of({}),
        completeAppointment: () => of({}),
      } as never,
      { hasRole: (role: string) => role === 'APPROVER' } as never,
    );
    component.faceCameraActive = true;
  });

  afterEach(() => {
    component.ngOnDestroy();
    jasmine.clock().uninstall();
  });

  it('captures a continuously visible face only once', () => {
    const detection = face(0.2, [0.1, 0.2, 0.3]);

    (component as any).processDetectedFaces({} as HTMLVideoElement, [detection]);
    jasmine.clock().tick(20000);
    (component as any).processDetectedFaces({} as HTMLVideoElement, [detection]);

    expect(searchCalls).toEqual(['photo-1']);
    expect(component.faceDetections.length).toBe(1);
  });

  it('allows a disappeared face again only after the retry timeout', () => {
    const detection = face(0.2, [0.1, 0.2, 0.3]);
    (component as any).processDetectedFaces({} as HTMLVideoElement, [detection]);
    jasmine.clock().tick(3000);
    (component as any).processDetectedFaces({} as HTMLVideoElement, []);
    jasmine.clock().tick(5000);
    (component as any).processDetectedFaces({} as HTMLVideoElement, [detection]);
    expect(searchCalls.length).toBe(1);

    jasmine.clock().tick(16000);
    (component as any).processDetectedFaces({} as HTMLVideoElement, []);
    (component as any).processDetectedFaces({} as HTMLVideoElement, [detection]);
    expect(searchCalls.length).toBe(2);
  });

  it('keeps detection order when search responses complete out of order', () => {
    (component as any).processDetectedFaces({} as HTMLVideoElement, [
      face(0.1, [0.1, 0.2, 0.3]),
      face(0.65, [0.7, 0.8, 0.9]),
    ]);

    responses.get('photo-2')!.next({ success: true, matched: false, message: 'No match' });
    responses.get('photo-1')!.next({ success: true, matched: true, message: 'Matched', enrollmentId: 'VISITOR_1',
      score: 0.97, visitor: {
        id: 1, fullName: 'First Visitor', phoneNumber: '9999999999', epicNumber: 'ABC1234567',
        designation: 'Citizen', district: 'Ri Bhoi', constituency: 'Nongpoh', booth: '1'
      } });

    expect(component.faceDetections.map(item => item.trackingId)).toEqual(['Face 1', 'Face 2']);
    expect(component.faceDetections.map(item => item.status)).toEqual(['MATCHED', 'NOT_REGISTERED']);
  });

  it('keeps the selected visitor when a later face result completes', () => {
    (component as any).processDetectedFaces({} as HTMLVideoElement, [
      face(0.1, [0.1, 0.2, 0.3]), face(0.65, [0.7, 0.8, 0.9]),
    ]);
    const firstVisitor = { id: 1, fullName: 'First Visitor', phoneNumber: '9999999999', epicNumber: 'A', designation: 'Citizen', district: 'Ri Bhoi', constituency: 'Nongpoh', booth: '1' };
    const secondVisitor = { ...firstVisitor, id: 2, fullName: 'Second Visitor' };

    responses.get('photo-1')!.next({ success: true, matched: true, message: 'Matched', visitor: firstVisitor });
    responses.get('photo-2')!.next({ success: true, matched: true, message: 'Matched', visitor: secondVisitor });

    expect(component.selected?.id).toBe(1);
  });

  it('expires completed results after the configured timeout and clears an expired selection', () => {
    (component as any).processDetectedFaces({} as HTMLVideoElement, [face(0.1, [0.1, 0.2, 0.3])]);
    const visitor = { id: 1, fullName: 'Visitor', phoneNumber: '9999999999', epicNumber: 'A', designation: 'Citizen', district: 'Ri Bhoi', constituency: 'Nongpoh', booth: '1' };
    responses.get('photo-1')!.next({ success: true, matched: true, message: 'Matched', visitor });
    component.selectFaceResult(component.faceDetections[0] as never);

    jasmine.clock().tick(AUTO_FACE_RESULT_TIMEOUT_MS + 1);

    expect(component.faceDetections).toEqual([]);
    expect(component.selected).toBeNull();
    expect(component.selectedFaceTrackingId).toBeNull();
  });

  it('maps each face state to a reusable status class', () => {
    expect(component.getStatusClass('MATCHED' as never)).toBe('face-status-success');
    expect(component.getStatusClass('NOT_REGISTERED' as never)).toBe('face-status-warning');
    expect(component.getStatusClass('SEARCHING' as never)).toBe('face-status-searching');
    expect(component.getStatusClass('FAILED' as never)).toBe('face-status-error');
    expect(component.getStatusClass('TIMEOUT' as never)).toBe('face-status-timeout');
    expect(component.getStatusClass('UNAVAILABLE' as never)).toBe('face-status-unavailable');
  });

  it('saves pending remarks without completing the appointment', () => {
    const appointmentService = (component as any).appointmentService;
    spyOn(appointmentService, 'addRemark').and.returnValue(of({}));
    spyOn(appointmentService, 'completeAppointment').and.returnValue(of({}));
    component.selectedPendingAppointment = {
      appointmentId: 42,
      status: 'PENDING',
    } as never;
    component.pendingAppointmentRemarks = 'Pilot follow-up';

    component.savePendingAppointmentRemarks();

    expect(appointmentService.addRemark).toHaveBeenCalled();
    expect(appointmentService.completeAppointment).not.toHaveBeenCalled();
    expect((component.selectedPendingAppointment as any)?.status).toBe('PENDING');
  });

  it('closes the camera without clearing results or cancelling an in-flight face search', () => {
    const stop = jasmine.createSpy('stop');
    (component as any).cameraCapture = { captureCrop: () => 'photo-1', stop };
    const stream = {} as MediaStream;
    component.faceCameraStream = stream;
    component.faceDetections = [{ trackingId: 'Face 1', capturedImage: 'photo', status: 'MATCHED' }] as never;
    component.results = [{ id: 1, fullName: 'Existing Result' }] as never;
    const pendingSearch = new Subscription();
    (component as any).faceSearchSubscriptions.add(pendingSearch);

    const video = document.createElement('video');
    video.id = 'publicFaceVideo';
    Object.defineProperty(video, 'srcObject', { value: stream, writable: true });
    document.body.appendChild(video);

    component.closeIdentificationCamera();

    expect(stop).toHaveBeenCalledWith(stream);
    expect(video.srcObject).toBeNull();
    expect(component.faceCameraActive).toBeFalse();
    expect(component.faceDetections.length).toBe(1);
    expect(component.results.length).toBe(1);
    expect(pendingSearch.closed).toBeFalse();
    video.remove();
  });
});

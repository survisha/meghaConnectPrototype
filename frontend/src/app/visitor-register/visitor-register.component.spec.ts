import { of } from 'rxjs';
import { VisitorRegisterComponent } from './visitor-register.component';
import { VisitorFormExtractionResponse } from '../services/visitor-form-extraction.service';

describe('VisitorRegisterComponent form extraction', () => {
  function create() {
    const kyc = jasmine.createSpyObj('VisitorKycService', ['verifyEpic']);
    kyc.verifyEpic.and.returnValue(of({ code: '200', data: {
      verifiedName: 'Rahul', voteridnumber: 'ABC1234567', pollingdetails: { pollingpartno: '12' }
    }}));
    const toast = jasmine.createSpyObj('ToastService', ['success', 'warning']);
    const component = new VisitorRegisterComponent(
      {} as any, {} as any, { snapshot: { url: [{ path: 'register-visitor' }] } } as any,
      kyc, { isLoggedIn: () => false } as any, { detectChanges: () => undefined } as any,
      { instant: (key: string) => key } as any, { stop: () => undefined } as any, {} as any, {} as any, {} as any, toast
    );
    return { component, kyc, toast };
  }

  function result(epic: string | null, valid: boolean): VisitorFormExtractionResponse {
    const field = <T>(value: T | null, fieldValid = true) => ({ value, valid: fieldValid,
      status: value == null ? 'NOT_FOUND' as const : 'EXTRACTED' as const,
      confidence: value == null ? 'NONE' as const : 'HIGH' as const, reason: null });
    return { success: true, epic: field(epic, valid), name: field('Rahul'), mobileNumber: field('9876543210'),
      address: field('Shillong'), warnings: [], requiresManualReview: true,
      imageQuality: { acceptable: true, issues: [] }, requestId: 'r', message: 'Review' };
  }

  it('selects EPIC, populates fields, and performs one lookup without advancing', () => {
    const { component, kyc, toast } = create();
    const extracted = result('ABC1234567', true);
    component.formExtractionResult = extracted;
    component.applyExtractedVisitorData(extracted);
    component.applyExtractedVisitorData(extracted);
    expect(component.form.idType).toBe('EPIC');
    expect(component.form.epicNumber).toBe('ABC1234567');
    expect(component.form.visitorName).toBe('Rahul');
    expect(component.manualPhone).toBe('9876543210');
    expect(component.form.address).toBe('Shillong');
    expect(kyc.verifyEpic).toHaveBeenCalledTimes(1);
    expect(component.currentStep).toBe('id-entry');
    expect(component.extractionReviewPending).toBeTrue();
    expect(toast.success).toHaveBeenCalled();
  });

  it('selects No ID when EPIC is missing or invalid', () => {
    const missing = create(); missing.component.applyExtractedVisitorData(result(null, false));
    expect(missing.component.form.idType).toBe('NONE');
    expect(missing.component.form.fullName).toBe('Rahul');
    expect(missing.kyc.verifyEpic).not.toHaveBeenCalled();
    expect(missing.toast.warning).toHaveBeenCalledWith('EPIC was not found. No ID has been selected.');

    const invalid = create(); invalid.component.applyExtractedVisitorData(result('BAD123', false));
    expect(invalid.component.form.idType).toBe('NONE');
    expect(invalid.toast.warning).toHaveBeenCalledWith('Extracted EPIC requires manual verification.');
  });

  it('does not overwrite manually entered values or selected ID type', () => {
    const { component, kyc } = create();
    component.form.idType = 'NONE'; component.form.fullName = 'Manual Name';
    component.manualPhone = '9999999999'; component.form.address = 'Manual Address';
    component.applyExtractedVisitorData(result('ABC1234567', true));
    expect(component.form.idType).toBe('NONE');
    expect(component.form.fullName).toBe('Manual Name');
    expect(component.manualPhone).toBe('9999999999');
    expect(component.form.address).toBe('Manual Address');
    expect(kyc.verifyEpic).not.toHaveBeenCalled();
  });
});

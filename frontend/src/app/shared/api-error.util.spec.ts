import { HttpErrorResponse } from '@angular/common/http';
import { apiErrorBodyMessage, apiErrorMessage, looksLikeHtml } from './api-error.util';

describe('API error messages', () => {
  const gatewayHtml = '<html><head><title>504 Gateway Time-out</title></head><body>nginx</body></html>';

  it('maps a 504 HTML response to a safe extraction message', () => {
    const error = new HttpErrorResponse({ status: 504, error: gatewayHtml });
    expect(apiErrorMessage(error)).toBe('Form extraction is taking longer than expected. Please try again.');
  });

  it('never returns raw HTML', () => {
    expect(looksLikeHtml(gatewayHtml)).toBeTrue();
    expect(apiErrorBodyMessage(gatewayHtml, 'Safe fallback')).toBe('Safe fallback');
  });

  it('preserves a structured backend timeout message', () => {
    expect(apiErrorBodyMessage({ errorCode: 'FORM_EXTRACTION_INFERENCE_TIMEOUT', message: 'Please retry.' }))
      .toBe('FORM_EXTRACTION_INFERENCE_TIMEOUT: Please retry.');
  });
});

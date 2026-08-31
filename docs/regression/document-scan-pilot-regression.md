# Document Scan Pilot Regression

The camera scan path must converge on the existing secured appointment document upload path. No OCR, workflow, authorization, storage, or database behavior is changed.

| ID | Scenario | Expected result |
|---|---|---|
| DOC-01 | Angular existing file selection | Existing PDF/image remains attached and uploads with appointment submission. |
| DOC-02 | Flutter existing file upload | Selected file uploads through the existing supporting-document endpoint. |
| DOC-03 | Angular Documents section | Upload PDF and Scan Document controls are visible. |
| DOC-04 | Flutter appointment Documents section | Upload Supporting Document and Scan Document controls are visible. |
| DOC-05 | Angular scan camera | Environment camera opens; denial leaves file upload usable and shows guidance. |
| DOC-06 | Flutter scan camera | Device camera opens; denial leaves file upload usable and shows guidance. |
| DOC-07 | Single-page capture | One page is retained for PDF generation. |
| DOC-08 | Single-page Finish | A valid one-page `application/pdf` file is produced. |
| DOC-09 | Multi-page capture | Every accepted page is retained in capture order. |
| DOC-10 | Five-page Finish | One PDF is produced with pages 1-5 in order. |
| DOC-11 | Maximum pages | The tenth page is accepted and further capture is prevented. |
| DOC-12 | Angular Finish | Generated PDF is attached to the selected document type without reopening a picker. |
| DOC-13 | Flutter Finish | Generated PDF uploads automatically through the existing API. |
| DOC-14 | Successful Flutter upload | Document list refreshes and the PDF can be opened. |
| DOC-15 | Cancel | Captures are discarded and nothing is attached/uploaded. |
| DOC-16 | Delete/retake | Incorrect page is removed before generation. |
| DOC-17 | Repeated Finish | Processing state prevents duplicate generation/upload. |
| DOC-18 | Upload failure/offline | Existing queue/error behavior is retained; generated PDF remains available to the queue. |
| DOC-19 | Invalid/empty PDF | Generation aborts and no upload request is sent. |
| DOC-20 | Security | Existing JWT, role, MIME, size, filename, storage, and access validation remains enforced. |
| DOC-21 | Appointment workflow | Document attachment does not change appointment status. |
| DOC-22 | Temporary files | Camera page images are deleted after finish, cancel, or error; successful temporary PDF is deleted. |
| DOC-23 | Responsive layout | Angular controls wrap without overlap; Flutter buttons do not overflow. |
| DOC-24 | Android three-page pilot | Capture three pages, Finish, verify one uploaded PDF containing all three readable pages. |

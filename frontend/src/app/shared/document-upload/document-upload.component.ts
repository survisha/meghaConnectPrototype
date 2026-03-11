import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FileService } from '../../services/file.service';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

export interface UploadedDocument {
  id?: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  url?: string;
  uploadedAt?: string;
}

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, Toast],
  providers: [MessageService],
  template: `
    <p-toast></p-toast>
    <div class="doc-upload-container">
      <div class="upload-header">
        <mat-icon style="color:#1a237e">attach_file</mat-icon>
        <span style="font-weight:600;color:#1a237e">{{ label }}</span>
      </div>
      <div class="drop-zone" (click)="fileInput.click()" (dragover)="$event.preventDefault()" (drop)="onDrop($event)">
        <mat-icon style="font-size:2rem;color:#9ca3af">cloud_upload</mat-icon>
        <p style="margin:0.5rem 0 0;color:#6b7280;font-size:0.85rem">Click to browse or drag & drop files here</p>
        <p style="margin:0.25rem 0 0;color:#9ca3af;font-size:0.75rem">Supported: PDF, JPG, PNG, DOCX (max {{ maxSizeMb }}MB each)</p>
      </div>
      <input #fileInput type="file" [accept]="accept" [multiple]="multiple" style="display:none" (change)="onFileChange($event)">

      <div *ngIf="documents.length > 0" class="doc-list">
        <div *ngFor="let doc of documents; let i = index" class="doc-item">
          <mat-icon style="color:#1a237e;font-size:1rem">insert_drive_file</mat-icon>
          <span style="flex:1;font-size:0.85rem;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ doc.fileName }}</span>
          <span style="font-size:0.75rem;color:#6b7280">{{ formatSize(doc.fileSize) }}</span>
          <button mat-icon-button (click)="removeDoc(i)" style="color:#dc2626">
            <mat-icon style="font-size:1rem">close</mat-icon>
          </button>
        </div>
      </div>

      <div style="margin-top:0.75rem;display:flex;gap:0.5rem" *ngIf="documents.length > 0 && !autoUpload">
        <button mat-raised-button color="primary" (click)="uploadAll()" [disabled]="uploading">
          <mat-icon>upload</mat-icon>
          {{ uploading ? 'Uploading...' : 'Upload All' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .doc-upload-container { background: white; border-radius: 8px; padding: 1rem; border: 1px solid #e5e7eb; }
    .upload-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.75rem; }
    .drop-zone { border: 2px dashed #d1d5db; border-radius: 8px; padding: 1.5rem; text-align: center; cursor: pointer; transition: border-color 0.2s; }
    .drop-zone:hover { border-color: #1a237e; background: #f8fafc; }
    .doc-list { margin-top: 0.75rem; display: flex; flex-direction: column; gap: 0.5rem; }
    .doc-item { display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem; background: #f8fafc; border-radius: 4px; border: 1px solid #e5e7eb; }
  `]
})
export class DocumentUploadComponent {
  @Input() label = 'Upload Documents';
  @Input() accept = '.pdf,.jpg,.jpeg,.png,.docx';
  @Input() multiple = true;
  @Input() autoUpload = false;
  @Input() entityId?: number;
  @Input() entityType = 'appointment';
  @Input() maxSizeMb = 10;
  @Output() uploaded = new EventEmitter<UploadedDocument[]>();
  @Output() removed = new EventEmitter<number>();

  documents: UploadedDocument[] = [];
  uploading = false;

  constructor(private fileService: FileService, private msg: MessageService) {}

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files) this.addFiles(Array.from(input.files));
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    if (event.dataTransfer?.files) this.addFiles(Array.from(event.dataTransfer.files));
  }

  private addFiles(files: File[]) {
    const maxBytes = this.maxSizeMb * 1024 * 1024;
    for (const f of files) {
      if (f.size > maxBytes) { this.msg.add({ severity: 'warn', summary: 'File too large', detail: `${f.name} exceeds ${this.maxSizeMb}MB` }); continue; }
      this.documents.push({ fileName: f.name, fileType: f.type, fileSize: f.size });
      if (this.autoUpload && this.entityId) this.uploadFile(f, this.documents.length - 1);
    }
  }

  private uploadFile(file: File, index: number) {
    if (!this.entityId) return;
    this.uploading = true;
    this.fileService.upload(this.entityId, this.entityType, file).subscribe({
      next: (res: any) => {
        this.documents[index].url = res?.fileUrl;
        this.documents[index].id = res?.id;
        this.uploading = false;
        this.uploaded.emit([...this.documents]);
      },
      error: () => {
        this.uploading = false;
        this.msg.add({ severity: 'error', summary: 'Upload failed', detail: this.documents[index].fileName });
      }
    });
  }

  uploadAll() {
    // Placeholder - in real implementation iterate pending files
    this.msg.add({ severity: 'info', summary: 'Upload', detail: `${this.documents.length} file(s) queued for upload` });
    this.uploaded.emit([...this.documents]);
  }

  removeDoc(index: number) {
    const removed = this.documents.splice(index, 1);
    if (removed[0].id) this.removed.emit(removed[0].id);
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  }
}

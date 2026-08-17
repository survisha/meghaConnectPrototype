import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges } from '@angular/core';
import { resolvePhotoUrl } from './photo-url.util';

@Component({
  selector: 'app-authenticated-photo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <img *ngIf="displayUrl && !failed" [src]="displayUrl" [alt]="alt" (error)="failed = true" />
    <span *ngIf="!displayUrl || failed" class="photo-fallback">{{ initial }}</span>
  `,
  styles: [`
    :host { display: block; width: 100%; height: 100%; overflow: hidden; }
    img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .photo-fallback { width: 100%; height: 100%; display: grid; place-items: center;
      background: #1a237e; color: white; font-weight: 800; }
  `],
})
export class AuthenticatedPhotoComponent implements OnChanges {
  @Input() source?: string | null;
  @Input() alt = 'Visitor photo';
  @Input() name = '';

  displayUrl = '';
  failed = false;

  get initial(): string { return (this.name.trim()[0] || '?').toUpperCase(); }

  ngOnChanges(): void {
    this.failed = false;
    this.displayUrl = resolvePhotoUrl(this.source) || '';
  }
}

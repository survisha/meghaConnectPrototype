import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-brand-logo',
  standalone: true,
  template: `
    <img
      role="img"
      [attr.aria-label]="label"
      [attr.alt]="label"
      [src]="src"
    />
  `,
  styles: [`
    :host {
      display: block;
    }

    img {
      display: block;
      width: 100%;
      height: auto;
      object-fit: contain;
    }
  `],
})
export class BrandLogoComponent {
  @Input() label = 'MEGHACONNECT AI logo';
  @Input() src = '/asserts/branding/meghaconnect-ai-logo-transparent.png';
}

import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-brand-logo',
  standalone: true,
  template: `
    <svg
      role="img"
      [attr.aria-label]="label"
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 760 176"
      fill="none"
    >
      <title>{{ label }}</title>
      <defs>
        <linearGradient id="brandLogoGrad" x1="44" y1="30" x2="168" y2="168" gradientUnits="userSpaceOnUse">
          <stop stop-color="#22D3EE"/>
          <stop offset="0.55" stop-color="#1E40AF"/>
          <stop offset="1" stop-color="#8B5CF6"/>
        </linearGradient>
        <filter id="brandLogoShadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="14" stdDeviation="16" flood-color="#020617" flood-opacity="0.24"/>
        </filter>
      </defs>
      <g filter="url(#brandLogoShadow)">
        <rect x="24" y="16" width="144" height="144" rx="34" fill="url(#brandLogoGrad)"/>
        <path d="M56.6 57.1C69 38.5 117.9 37.9 134.2 57.6" stroke="#fff" stroke-width="5.1" stroke-linecap="round" opacity="0.42"/>
        <path d="M69.6 61L96 88M123 61.6L96 88M96 88L70.7 115.6M96 88L121.9 115.6M69.6 61L123 61.6M70.7 115.6H121.9" stroke="#fff" stroke-width="2" stroke-linecap="round" opacity="0.72"/>
        <rect x="73.5" y="67.8" width="45" height="43.3" rx="6.8" fill="#fff" opacity="0.24"/>
        <rect x="79.1" y="77.9" width="33.8" height="2.2" rx="1.1" fill="#fff" opacity="0.82"/>
        <rect x="80.2" y="88" width="6.2" height="5.1" rx="1.4" fill="#fff" opacity="0.96"/>
        <rect x="90.9" y="88" width="6.2" height="5.1" rx="1.4" fill="#fff" opacity="0.44"/>
        <rect x="101.6" y="88" width="6.2" height="5.1" rx="1.4" fill="#fff" opacity="0.44"/>
        <rect x="112.3" y="88" width="6.2" height="5.1" rx="1.4" fill="#fff" opacity="0.96"/>
        <circle cx="69.6" cy="61" r="3.4" fill="#fff" opacity="0.96"/>
        <circle cx="69.6" cy="61" r="6.2" fill="#22D3EE" opacity="0.22"/>
        <circle cx="123" cy="61.6" r="3.4" fill="#fff" opacity="0.96"/>
        <circle cx="123" cy="61.6" r="6.2" fill="#22D3EE" opacity="0.22"/>
        <circle cx="96" cy="88" r="3.4" fill="#fff" opacity="0.96"/>
        <circle cx="96" cy="88" r="6.2" fill="#22D3EE" opacity="0.22"/>
        <circle cx="70.7" cy="115.6" r="3.4" fill="#fff" opacity="0.96"/>
        <circle cx="70.7" cy="115.6" r="6.2" fill="#22D3EE" opacity="0.22"/>
        <circle cx="121.9" cy="115.6" r="3.4" fill="#fff" opacity="0.96"/>
        <circle cx="121.9" cy="115.6" r="6.2" fill="#22D3EE" opacity="0.22"/>
        <path d="M63.4 131.9C80.8 143.1 111.2 143.1 128.6 131.9" stroke="#fff" stroke-width="4.5" stroke-linecap="round" opacity="0.5"/>
      </g>
      <g transform="translate(196 50)">
        <text x="0" y="38" font-family="Inter, Segoe UI, Arial, sans-serif" font-size="38" font-weight="850" fill="#0B1F5C">MeghaConnect AI</text>
        <text x="1" y="70" font-family="Inter, Segoe UI, Arial, sans-serif" font-size="15" font-weight="650" fill="#475569">AI Powered Appointment &amp; Scheme Management Platform</text>
      </g>
    </svg>
  `,
  styles: [`
    :host {
      display: block;
    }

    svg {
      display: block;
      width: 100%;
      height: auto;
    }
  `],
})
export class BrandLogoComponent {
  @Input() label = 'MeghaConnect AI logo';
}

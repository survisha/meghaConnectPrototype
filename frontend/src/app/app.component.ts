import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LanguageService } from './i18n/language.service';
import { BrandLogoComponent } from './shared/brand-logo/brand-logo.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, BrandLogoComponent],
  template: `
    <div *ngIf="showWelcome" class="welcome-screen">
      <div class="welcome-top">
        <app-brand-logo class="welcome-logo-lockup" label="MeghaConnect logo"></app-brand-logo>
      </div>
      <div class="welcome-main">
        <h1>MeghaConnect</h1>
        <p>Connecting People, Empowering Lives</p>
        <div class="welcome-features" aria-label="Platform highlights">
          <span>Appointment Management</span>
          <span>Scheme Tracking</span>
          <span>Citizen Services</span>
          <span>Public Darbar</span>
          <span>Visitor Management</span>
        </div>
        <div *ngIf="welcomeLoading" class="welcome-spinner" aria-label="Loading"></div>
      </div>
    </div>
    <router-outlet *ngIf="!showWelcome"></router-outlet>
  `,
  styles: [`
    .welcome-screen {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1.5rem;
      padding: 1.25rem;
      background:
        radial-gradient(circle at 50% 20%, rgba(23, 195, 200, 0.18), transparent 34%),
        linear-gradient(145deg, #f8fcff 0%, #eef8fb 52%, #ffffff 100%);
      color: #082b7a;
      text-align: center;
      overflow: hidden;
    }
    .welcome-top {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.35rem;
    }
    .welcome-logo-lockup {
      width: min(84vw, 390px);
      height: auto;
      filter: none;
    }
    .welcome-main {
      position: relative;
      width: min(92vw, 560px);
      min-height: 390px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border: 1px solid rgba(20, 150, 232, 0.18);
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.9);
      box-shadow: 0 18px 54px rgba(8, 43, 122, 0.12);
      overflow: hidden;
      padding: 2rem 1.25rem;
      backdrop-filter: blur(18px);
    }
    .welcome-main h1 {
      position: relative;
      margin: 0;
      color: #082b7a;
      font-size: clamp(2.1rem, 8vw, 3.6rem);
      font-weight: 900;
      letter-spacing: 0;
    }
    .welcome-main p {
      position: relative;
      margin: 0.5rem 0 0;
      color: #555555;
      font-size: 0.95rem;
    }
    .welcome-features {
      position: relative;
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 0.45rem;
      margin-top: 1.1rem;
    }
    .welcome-features span {
      border: 1px solid rgba(23, 195, 200, 0.22);
      border-radius: 999px;
      background: rgba(23, 195, 200, 0.08);
      color: #082b7a;
      font-size: 0.72rem;
      font-weight: 750;
      padding: 0.34rem 0.65rem;
    }
    .welcome-spinner {
      position: relative;
      width: 34px;
      height: 34px;
      margin-top: 1.25rem;
      border: 3px solid rgba(11, 94, 215, 0.14);
      border-top-color: #17c3c8;
      border-radius: 50%;
      animation: welcome-spin 0.8s linear infinite;
    }
    @keyframes welcome-spin { to { transform: rotate(360deg); } }
    @keyframes welcome-pulse {
      0%, 100% { transform: scale(1); opacity: 0.9; }
      50% { transform: scale(1.04); opacity: 1; }
    }
  `],
})
export class AppComponent implements OnInit {
  showWelcome = true;
  welcomeLoading = false;

  constructor(languageService: LanguageService) {
    languageService.initialize();
  }

  ngOnInit() {
    setTimeout(() => this.welcomeLoading = true, 900);
    setTimeout(() => this.showWelcome = false, 3000);
  }
}

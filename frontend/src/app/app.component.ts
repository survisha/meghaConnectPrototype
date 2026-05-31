import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LanguageService } from './i18n/language.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <div *ngIf="showWelcome" class="welcome-screen">
      <div class="welcome-top">
        <img src="/asserts/CM_Profile_Picture.jpg" alt="Chief Minister" class="welcome-cm-photo">
        <div class="welcome-cm-name">Hon'ble Chief Minister</div>
        <div class="welcome-cm-subtitle">Government of Meghalaya</div>
      </div>
      <div class="welcome-main">
        <img src="/asserts/state_map.png" alt="Meghalaya map" class="welcome-map">
        <img src="/asserts/logo.png" alt="Meghalaya Government Logo" class="welcome-logo">
        <h1>MeghaConnect</h1>
        <p>Chief Minister's Office citizen service platform</p>
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
      background: #f4f6fb;
      color: #172554;
      text-align: center;
    }
    .welcome-top {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.35rem;
    }
    .welcome-cm-photo {
      width: 92px;
      height: 92px;
      border-radius: 50%;
      object-fit: cover;
      border: 3px solid #fff;
      box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
    }
    .welcome-cm-name {
      font-weight: 800;
      font-size: 0.95rem;
    }
    .welcome-cm-subtitle {
      color: #64748b;
      font-size: 0.8rem;
    }
    .welcome-main {
      position: relative;
      width: min(92vw, 420px);
      min-height: 330px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border: 1px solid #dbeafe;
      border-radius: 18px;
      background: #ffffff;
      box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
      overflow: hidden;
      padding: 2rem 1.25rem;
    }
    .welcome-map {
      position: absolute;
      inset: 1rem;
      margin: auto;
      width: 78%;
      max-height: 70%;
      object-fit: contain;
      opacity: 0.1;
      pointer-events: none;
    }
    .welcome-logo {
      position: relative;
      width: 76px;
      height: 76px;
      object-fit: contain;
      margin-bottom: 0.85rem;
    }
    .welcome-main h1 {
      position: relative;
      margin: 0;
      color: #1a237e;
      font-size: clamp(2rem, 8vw, 3rem);
      font-weight: 900;
      letter-spacing: 0;
    }
    .welcome-main p {
      position: relative;
      margin: 0.5rem 0 0;
      color: #475569;
      font-size: 0.95rem;
    }
    .welcome-spinner {
      position: relative;
      width: 34px;
      height: 34px;
      margin-top: 1.25rem;
      border: 3px solid #dbeafe;
      border-top-color: #1a237e;
      border-radius: 50%;
      animation: welcome-spin 0.8s linear infinite;
    }
    @keyframes welcome-spin { to { transform: rotate(360deg); } }
  `],
})
export class AppComponent implements OnInit {
  showWelcome = true;
  welcomeLoading = false;

  constructor(languageService: LanguageService) {
    languageService.initialize();
  }

  ngOnInit() {
    setTimeout(() => this.welcomeLoading = true, 5000);
    setTimeout(() => this.showWelcome = false, 5800);
  }
}

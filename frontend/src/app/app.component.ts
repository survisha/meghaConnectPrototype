import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterOutlet } from '@angular/router';
import { LanguageService } from './i18n/language.service';
import { BrandLogoComponent } from './shared/brand-logo/brand-logo.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, MatIconModule, BrandLogoComponent],
  template: `
    <div *ngIf="showWelcome" class="welcome-screen">
      <div class="welcome-top">
        <app-brand-logo class="welcome-logo-lockup" label="MeghaConnect AI logo"></app-brand-logo>
      </div>
      <div class="welcome-main">
        <div class="ai-orbit" aria-hidden="true">
          <span class="node node-a"></span>
          <span class="node node-b"></span>
          <span class="node node-c"></span>
          <span class="node node-d"></span>
          <span class="node node-e"></span>
          <span class="link link-a"></span>
          <span class="link link-b"></span>
          <span class="link link-c"></span>
          <span class="link link-d"></span>
          <mat-icon class="orbit-calendar">event_available</mat-icon>
        </div>
        <h1>MeghaConnect AI</h1>
        <p>AI Powered Appointment & Scheme Management Platform</p>
        <div class="welcome-features" aria-label="Platform highlights">
          <span>Appointment Management</span>
          <span>Scheme Tracking</span>
          <span>AI Insights</span>
          <span>AI Notes</span>
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
        radial-gradient(circle at 50% 30%, rgba(34, 211, 238, 0.18), transparent 32%),
        linear-gradient(145deg, #071538 0%, #0b1f5c 50%, #111827 100%);
      color: #ffffff;
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
      filter: drop-shadow(0 18px 42px rgba(0, 0, 0, 0.24));
    }
    .welcome-main {
      position: relative;
      width: min(92vw, 560px);
      min-height: 390px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border: 1px solid rgba(125, 211, 252, 0.26);
      border-radius: 24px;
      background: rgba(7, 21, 56, 0.64);
      box-shadow: 0 24px 70px rgba(0, 0, 0, 0.3);
      overflow: hidden;
      padding: 2rem 1.25rem;
      backdrop-filter: blur(18px);
    }
    .ai-orbit {
      position: relative;
      width: 168px;
      height: 168px;
      margin-bottom: 1rem;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(34, 211, 238, 0.2), rgba(139, 92, 246, 0.08) 54%, transparent 70%);
      animation: welcome-pulse 1.8s ease-in-out infinite;
    }
    .node,
    .link,
    .orbit-calendar {
      position: absolute;
    }
    .node {
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: #ffffff;
      box-shadow: 0 0 0 8px rgba(34, 211, 238, 0.2), 0 0 20px rgba(34, 211, 238, 0.72);
    }
    .node-a { left: 28px; top: 34px; }
    .node-b { right: 28px; top: 34px; }
    .node-c { left: 76px; top: 76px; }
    .node-d { left: 28px; bottom: 34px; }
    .node-e { right: 28px; bottom: 34px; }
    .link {
      height: 2px;
      width: 72px;
      background: linear-gradient(90deg, rgba(34, 211, 238, 0.2), rgba(255, 255, 255, 0.84), rgba(139, 92, 246, 0.24));
      transform-origin: left center;
    }
    .link-a { left: 42px; top: 47px; }
    .link-b { left: 42px; top: 48px; transform: rotate(38deg); }
    .link-c { left: 88px; top: 90px; transform: rotate(-38deg); }
    .link-d { left: 42px; bottom: 47px; }
    .orbit-calendar {
      inset: 0;
      margin: auto;
      width: 58px;
      height: 58px;
      display: grid;
      place-items: center;
      border-radius: 18px;
      background: rgba(255, 255, 255, 0.14);
      color: #ffffff;
      font-size: 34px;
    }
    .welcome-main h1 {
      position: relative;
      margin: 0;
      color: #ffffff;
      font-size: clamp(2.1rem, 8vw, 3.6rem);
      font-weight: 900;
      letter-spacing: 0;
    }
    .welcome-main p {
      position: relative;
      margin: 0.5rem 0 0;
      color: #c7d2fe;
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
      border: 1px solid rgba(125, 211, 252, 0.28);
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.08);
      color: #e0f2fe;
      font-size: 0.72rem;
      font-weight: 750;
      padding: 0.34rem 0.65rem;
    }
    .welcome-spinner {
      position: relative;
      width: 34px;
      height: 34px;
      margin-top: 1.25rem;
      border: 3px solid rgba(255, 255, 255, 0.16);
      border-top-color: #22d3ee;
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
    setTimeout(() => this.welcomeLoading = true, 3600);
    setTimeout(() => this.showWelcome = false, 4600);
  }
}

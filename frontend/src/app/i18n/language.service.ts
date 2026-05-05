import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export const APP_LANGUAGE_STORAGE_KEY = 'app_language';
export const DEFAULT_LANGUAGE = 'en';

export interface AppLanguage {
  code: string;
  label: string;
}

export const SUPPORTED_LANGUAGES: AppLanguage[] = [
  { code: 'en', label: 'English' },
  { code: 'kh', label: 'Khasi' },
  { code: 'gr', label: 'Garo' },
  { code: 'hi', label: 'Hindi' },
];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  readonly supportedLanguages = SUPPORTED_LANGUAGES;

  constructor(private translate: TranslateService) {}

  initialize(): void {
    const supportedCodes = this.supportedLanguages.map(language => language.code);
    this.translate.addLangs(supportedCodes);
    this.translate.setFallbackLang(DEFAULT_LANGUAGE);
    this.setLanguage(this.getStoredLanguage(), false);
  }

  setLanguage(language: string, persist = true): void {
    const selectedLanguage = this.isSupportedLanguage(language) ? language : DEFAULT_LANGUAGE;
    this.translate.use(selectedLanguage);

    if (persist) {
      this.storeLanguage(selectedLanguage);
    }
  }

  getCurrentLanguage(): string {
    return this.translate.getCurrentLang() || this.getStoredLanguage();
  }

  private getStoredLanguage(): string {
    try {
      const storedLanguage = localStorage.getItem(APP_LANGUAGE_STORAGE_KEY);
      return this.isSupportedLanguage(storedLanguage) ? storedLanguage : DEFAULT_LANGUAGE;
    } catch {
      return DEFAULT_LANGUAGE;
    }
  }

  private storeLanguage(language: string): void {
    try {
      localStorage.setItem(APP_LANGUAGE_STORAGE_KEY, language);
    } catch {
      // Ignore storage failures so language switching still works in restricted browsers.
    }
  }

  private isSupportedLanguage(language: string | null): language is string {
    return !!language && this.supportedLanguages.some(supported => supported.code === language);
  }
}

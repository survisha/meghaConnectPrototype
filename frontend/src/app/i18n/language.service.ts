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
    this.clearStoredLanguage();
    this.setLanguage(DEFAULT_LANGUAGE);
  }

  setLanguage(language: string): void {
    const selectedLanguage = this.isSupportedLanguage(language) ? language : DEFAULT_LANGUAGE;
    this.translate.use(selectedLanguage);
  }

  getCurrentLanguage(): string {
    return this.translate.getCurrentLang() || DEFAULT_LANGUAGE;
  }

  private clearStoredLanguage(): void {
    try {
      localStorage.removeItem(APP_LANGUAGE_STORAGE_KEY);
    } catch {
      // Ignore storage failures so language initialization still works in restricted browsers.
    }
  }

  private isSupportedLanguage(language: string | null): language is string {
    return !!language && this.supportedLanguages.some(supported => supported.code === language);
  }
}

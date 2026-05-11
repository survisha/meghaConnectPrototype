import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { DEFAULT_LANGUAGE, LanguageService, SUPPORTED_LANGUAGES } from '../../i18n/language.service';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [CommonModule, TranslateModule, MatIconModule],
  templateUrl: './language-selector.component.html',
  styleUrls: ['./language-selector.component.scss'],
})
export class LanguageSelectorComponent implements OnInit, OnDestroy {
  languages = SUPPORTED_LANGUAGES;
  selectedLanguage = DEFAULT_LANGUAGE;
  private languageSubscription?: Subscription;

  constructor(
    private languageService: LanguageService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.selectedLanguage = this.languageService.getCurrentLanguage();
    this.languageSubscription = this.translate.onLangChange.subscribe(event => {
      this.selectedLanguage = event.lang;
    });
  }

  ngOnDestroy(): void {
    this.languageSubscription?.unsubscribe();
  }

  changeLanguage(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.languageService.setLanguage(select.value);
  }
}

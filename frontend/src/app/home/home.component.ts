import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { LoginComponent } from '../auth/login/login.component';
import { LanguageSelectorComponent } from '../shared/language-selector/language-selector.component';
import { AiChatbotComponent } from '../ai-chatbot/ai-chatbot.component';
import { AuthService } from '../services/auth.service';

type HomeSection = 'home' | 'about' | 'connect' | 'faq' | 'contact' | 'directory' | 'login';

interface DepartmentDirectoryItem {
  name: string;
  nameKey: string;
  descriptionKey: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    TranslateModule,
    LoginComponent,
    LanguageSelectorComponent,
    AiChatbotComponent
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  activeSection: HomeSection = 'home';
  selectedLetter = 'A';
  readonly letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

  readonly navItems: { id: HomeSection; labelKey: string }[] = [
    { id: 'home', labelKey: 'HOME' },
    { id: 'about', labelKey: 'ABOUT' },
    { id: 'connect', labelKey: 'HOME_NAV_CONNECT' },
    { id: 'faq', labelKey: 'FAQ' },
    { id: 'contact', labelKey: 'CONTACT_US' },
    { id: 'directory', labelKey: 'HOME_NAV_DIRECTORY' },
    { id: 'login', labelKey: 'LOGIN' }
  ];

  departments: DepartmentDirectoryItem[] = [
    { name: 'Agriculture and Farmers Welfare Department', nameKey: 'HOME_DEPT_AGRICULTURE_NAME', descriptionKey: 'HOME_DEPT_AGRICULTURE_DESC' },
    { name: 'Animal Husbandry and Veterinary Department', nameKey: 'HOME_DEPT_ANIMAL_NAME', descriptionKey: 'HOME_DEPT_ANIMAL_DESC' },
    { name: 'Arts and Culture Department', nameKey: 'HOME_DEPT_ARTS_NAME', descriptionKey: 'HOME_DEPT_ARTS_DESC' },
    { name: 'Border Areas Development Department', nameKey: 'HOME_DEPT_BORDER_NAME', descriptionKey: 'HOME_DEPT_BORDER_DESC' },
    { name: 'Commerce and Industries Department', nameKey: 'HOME_DEPT_COMMERCE_NAME', descriptionKey: 'HOME_DEPT_COMMERCE_DESC' },
    { name: 'Community and Rural Development Department', nameKey: 'HOME_DEPT_COMMUNITY_NAME', descriptionKey: 'HOME_DEPT_COMMUNITY_DESC' },
    { name: 'Education Department', nameKey: 'HOME_DEPT_EDUCATION_NAME', descriptionKey: 'HOME_DEPT_EDUCATION_DESC' },
    { name: 'Excise, Registration, Taxation and Stamps Department', nameKey: 'HOME_DEPT_EXCISE_NAME', descriptionKey: 'HOME_DEPT_EXCISE_DESC' },
    { name: 'Finance Department', nameKey: 'HOME_DEPT_FINANCE_NAME', descriptionKey: 'HOME_DEPT_FINANCE_DESC' },
    { name: 'Food, Civil Supplies and Consumer Affairs Department', nameKey: 'HOME_DEPT_FOOD_NAME', descriptionKey: 'HOME_DEPT_FOOD_DESC' },
    { name: 'Forest and Environment Department', nameKey: 'HOME_DEPT_FOREST_NAME', descriptionKey: 'HOME_DEPT_FOREST_DESC' },
    { name: 'Health and Family Welfare Department', nameKey: 'HOME_DEPT_HEALTH_NAME', descriptionKey: 'HOME_DEPT_HEALTH_DESC' },
    { name: 'Home Police Department', nameKey: 'HOME_DEPT_HOME_POLICE_NAME', descriptionKey: 'HOME_DEPT_HOME_POLICE_DESC' },
    { name: 'Information and Public Relations Department', nameKey: 'HOME_DEPT_IPR_NAME', descriptionKey: 'HOME_DEPT_IPR_DESC' },
    { name: 'Labour Department', nameKey: 'HOME_DEPT_LABOUR_NAME', descriptionKey: 'HOME_DEPT_LABOUR_DESC' },
    { name: 'Law Department', nameKey: 'HOME_DEPT_LAW_NAME', descriptionKey: 'HOME_DEPT_LAW_DESC' },
    { name: 'Planning, Investment Promotion and Sustainable Development Department', nameKey: 'HOME_DEPT_PLANNING_NAME', descriptionKey: 'HOME_DEPT_PLANNING_DESC' },
    { name: 'Public Health Engineering Department', nameKey: 'HOME_DEPT_PHE_NAME', descriptionKey: 'HOME_DEPT_PHE_DESC' },
    { name: 'Public Works Department', nameKey: 'HOME_DEPT_PWD_NAME', descriptionKey: 'HOME_DEPT_PWD_DESC' },
    { name: 'Revenue and Disaster Management Department', nameKey: 'HOME_DEPT_REVENUE_NAME', descriptionKey: 'HOME_DEPT_REVENUE_DESC' },
    { name: 'Social Welfare Department', nameKey: 'HOME_DEPT_SOCIAL_NAME', descriptionKey: 'HOME_DEPT_SOCIAL_DESC' },
    { name: 'Soil and Water Conservation Department', nameKey: 'HOME_DEPT_SOIL_WATER_NAME', descriptionKey: 'HOME_DEPT_SOIL_WATER_DESC' },
    { name: 'Tourism Department', nameKey: 'HOME_DEPT_TOURISM_NAME', descriptionKey: 'HOME_DEPT_TOURISM_DESC' },
    { name: 'Transport Department', nameKey: 'HOME_DEPT_TRANSPORT_NAME', descriptionKey: 'HOME_DEPT_TRANSPORT_DESC' },
    { name: 'Urban Affairs Department', nameKey: 'HOME_DEPT_URBAN_NAME', descriptionKey: 'HOME_DEPT_URBAN_DESC' },
    { name: 'Water Resources Department', nameKey: 'HOME_DEPT_WATER_NAME', descriptionKey: 'HOME_DEPT_WATER_DESC' }
  ];

  constructor(private auth: AuthService, private router: Router) {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }

    this.loadDepartments();
  }

  get filteredDepartments(): DepartmentDirectoryItem[] {
    return this.departments
      .filter(department => department.name.toUpperCase().startsWith(this.selectedLetter))
      .sort((left, right) => left.name.localeCompare(right.name));
  }

  get hasDepartmentsForLetter(): boolean {
    return this.filteredDepartments.length > 0;
  }

  selectLetter(letter: string): void {
    this.selectedLetter = letter;
  }

  scrollToSection(section: HomeSection): void {
    this.activeSection = section;
    document.getElementById(section)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  @HostListener('window:scroll')
  onWindowScroll(): void {
    const current = this.navItems
      .map(item => ({ id: item.id, top: document.getElementById(item.id)?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY }))
      .filter(item => item.top <= 120)
      .sort((left, right) => right.top - left.top)[0];

    if (current) {
      this.activeSection = current.id;
    }
  }

  private loadDepartments(): void {
    // TODO: Replace this sample list with a ReferenceDataService/API call when
    // the backend exposes a department directory endpoint.
  }
}

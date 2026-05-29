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
  addressKey: string;
  contactKey: string;
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
    { name: 'Agriculture and Farmers Welfare Department', nameKey: 'HOME_DEPT_AGRICULTURE_NAME', descriptionKey: 'HOME_DEPT_AGRICULTURE_DESC', addressKey: 'Address: Cleve Colony, Shillong' ,contactKey: 'Phone: 0364-2226043' },
    { name: 'Animal Husbandry and Veterinary Department', nameKey: 'HOME_DEPT_ANIMAL_NAME', descriptionKey: 'HOME_DEPT_ANIMAL_DESC' , addressKey: 'Address: Additional Secretariat,Room No. 408, Meghalaya, Shillong' ,contactKey: 'Phone : 0364-2548388'},
    { name: 'Arts and Culture Department', nameKey: 'HOME_DEPT_ARTS_NAME', descriptionKey: 'HOME_DEPT_ARTS_DESC' ,addressKey: 'Address: Additional Secretariat,Room No. 408, Meghalaya, Shillong' , contactKey: 'Phone : 0364-2225212, S-2259'},
    { name: 'Border Areas Development Department', nameKey: 'HOME_DEPT_BORDER_NAME', descriptionKey: 'HOME_DEPT_BORDER_DESC', addressKey: 'Address: ' ,contactKey: '' },
    { name: 'Commerce and Industries Department', nameKey: 'HOME_DEPT_COMMERCE_NAME', descriptionKey: 'HOME_DEPT_COMMERCE_DESC', addressKey: 'Address: Plot No L/D 015 Lower Nongrim Hills, East Khasi Hills District, Shillong, Meghalaya' ,contactKey: 'Phone : 0364-2226253' },
    { name: 'Community and Rural Development Department', nameKey: 'HOME_DEPT_COMMUNITY_NAME', descriptionKey: 'HOME_DEPT_COMMUNITY_DESC', addressKey: 'Address: Additional Secretariat Building, Room No. 104, Shillong 793001' ,contactKey: 'Phone : 0364-2212460' },
    { name: 'Education Department', nameKey: 'HOME_DEPT_EDUCATION_NAME', descriptionKey: 'HOME_DEPT_EDUCATION_DESC', addressKey: 'Address: ' ,contactKey: 'Phone:0364-2506506' },
    { name: 'Excise, Registration, Taxation and Stamps Department', nameKey: 'HOME_DEPT_EXCISE_NAME', descriptionKey: 'HOME_DEPT_EXCISE_DESC',addressKey: 'Address: Main Secretariat Building, Shillong Room No. 240' , contactKey: 'Phone : 0364-2223949, S-2676' },
    { name: 'Finance Department', nameKey: 'HOME_DEPT_FINANCE_NAME', descriptionKey: 'HOME_DEPT_FINANCE_DESC',addressKey: 'Address: Phone : 0364-2223949, S-2676' , contactKey: 'Phone : 0364-2226043' },
    { name: 'Food, Civil Supplies and Consumer Affairs Department', nameKey: 'HOME_DEPT_FOOD_NAME', descriptionKey: 'HOME_DEPT_FOOD_DESC',addressKey: 'Address: ' , contactKey: 'Phone : 0364-2226350' },
    { name: 'Forest and Environment Department', nameKey: 'HOME_DEPT_FOREST_NAME', descriptionKey: 'HOME_DEPT_FOREST_DESC' ,addressKey: 'Address: ' , contactKey: 'Phone : 0364-2500862'},
    { name: 'Health and Family Welfare Department', nameKey: 'HOME_DEPT_HEALTH_NAME', descriptionKey: 'HOME_DEPT_HEALTH_DESC', addressKey: 'Address: ' ,contactKey: 'Phone : 0364-2212460' },
    { name: 'Home Police Department', nameKey: 'HOME_DEPT_HOME_POLICE_NAME', descriptionKey: 'HOME_DEPT_HOME_POLICE_DESC', addressKey: 'Address: Additional Secretariat, Room No. 408, Meghalaya, Shillong' ,contactKey: 'Phone : S-2259, 0364-2225212' },
    { name: 'Information and Public Relations Department', nameKey: 'HOME_DEPT_IPR_NAME', descriptionKey: 'HOME_DEPT_IPR_DESC', addressKey: 'Address: ' ,contactKey: '' },
    { name: 'Labour Department', nameKey: 'HOME_DEPT_LABOUR_NAME', descriptionKey: 'HOME_DEPT_LABOUR_DESC' , addressKey: 'Address: ' ,contactKey: ''},
    { name: 'Law Department', nameKey: 'HOME_DEPT_LAW_NAME', descriptionKey: 'HOME_DEPT_LAW_DESC', addressKey: 'Address: Meghalaya (Civil) Secretariat, Main Building Secretariat, Room No. 502, Shillong' ,contactKey: 'Phone : 0364-2222501' },
    { name: 'Planning, Investment Promotion and Sustainable Development Department', nameKey: 'HOME_DEPT_PLANNING_NAME', descriptionKey: 'HOME_DEPT_PLANNING_DESC',addressKey: 'Address: Additional Secretariat, Room No. 408, Meghalaya, Shillong' , contactKey: 'Phone : 0364-2225212, S-2259' },
    { name: 'Public Health Engineering Department', nameKey: 'HOME_DEPT_PHE_NAME', descriptionKey: 'HOME_DEPT_PHE_DESC', addressKey: 'Address: ' ,contactKey: 'Phone : 0364-2226350' },
    { name: 'Public Works Department', nameKey: 'HOME_DEPT_PWD_NAME', descriptionKey: 'HOME_DEPT_PWD_DESC', addressKey: 'Address: Chief Engineer PWD (Building), Lower Lachumiere, Shillong' ,contactKey: 'Phone : 0364-2222394' },
    { name: 'Revenue and Disaster Management Department', nameKey: 'HOME_DEPT_REVENUE_NAME', descriptionKey: 'HOME_DEPT_REVENUE_DESC',addressKey: 'Address: ' , contactKey: 'Phone : 0364-2223509' },
    { name: 'Social Welfare Department', nameKey: 'HOME_DEPT_SOCIAL_NAME', descriptionKey: 'HOME_DEPT_SOCIAL_DESC',addressKey: 'Address: ' , contactKey: 'Phone : 0364-2212460' },
    { name: 'Soil and Water Conservation Department', nameKey: 'HOME_DEPT_SOIL_WATER_NAME', descriptionKey: 'HOME_DEPT_SOIL_WATER_DESC',addressKey: 'Address: Room No. 303, Additional Secretariat Building, Shillong' , contactKey: 'Phone : 0364 - 2212401' },
    { name: 'Tourism Department', nameKey: 'HOME_DEPT_TOURISM_NAME', descriptionKey: 'HOME_DEPT_TOURISM_DESC' , addressKey: 'Address: Main Secretariat Building, Shillong' ,contactKey: 'Phone : 0364-2226043'},
    { name: 'Transport Department', nameKey: 'HOME_DEPT_TRANSPORT_NAME', descriptionKey: 'HOME_DEPT_TRANSPORT_DESC', addressKey: 'Address: ' ,contactKey: 'Phone : 0364-2223509' },
    { name: 'Urban Affairs Department', nameKey: 'HOME_DEPT_URBAN_NAME', descriptionKey: 'HOME_DEPT_URBAN_DESC',addressKey: 'Address: ' , contactKey: 'Phone : 0364-2226043' },
    { name: 'Water Resources Department', nameKey: 'HOME_DEPT_WATER_NAME', descriptionKey: 'HOME_DEPT_WATER_DESC' , addressKey: 'Address: Additional Secretariat Building, Shillong Room No. 314' ,contactKey: 'Phone : 0364-2210132, S-2695'}
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

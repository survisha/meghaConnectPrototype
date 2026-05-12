import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { apiErrorMessage } from '../shared/api-error.util';

interface SchemeInsight { scheme: string; count: number; percentage: number; }
interface DistrictInsight { district: string; applications: number; }
interface CategoryInsight { category: string; count: number; }

interface AiDashboardInsights {
  totalApplicationsThisMonth: number;
  topSchemes: SchemeInsight[];
  districtDistribution: DistrictInsight[];
  topCategories: CategoryInsight[];
  aiNote: string;
}

@Component({
  selector: 'app-ai-insights-dashboard',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './ai-insights-dashboard.component.html',
  styleUrls: ['./ai-insights-dashboard.component.scss'],
})
export class AiInsightsDashboardComponent implements OnInit {
  insights: AiDashboardInsights | null = null;
  loading = true;
  errorMsg = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<AiDashboardInsights>('/api/ai/dashboard-insights').subscribe({
      next: data => {
        this.insights = data ?? null;
        this.loading = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to load AI dashboard insights.');
        this.insights = null;
        this.loading = false;
      },
    });
  }

  getBarWidth(count: number, max: number): string {
    return max > 0 ? Math.round((count / max) * 100) + '%' : '0%';
  }

  get maxDistrictApps(): number {
    return Math.max(...(this.insights?.districtDistribution.map(d => d.applications) ?? [1]));
  }

  get maxCategoryCount(): number {
    return Math.max(...(this.insights?.topCategories.map(c => c.count) ?? [1]));
  }
}

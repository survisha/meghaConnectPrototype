import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

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
  imports: [CommonModule],
  templateUrl: './ai-insights-dashboard.component.html',
  styleUrls: ['./ai-insights-dashboard.component.scss'],
})
export class AiInsightsDashboardComponent implements OnInit {
  insights: AiDashboardInsights | null = null;
  loading = true;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<AiDashboardInsights>('/api/ai/dashboard-insights').pipe(
      catchError(() => of(this.getMockInsights()))
    ).subscribe((data: AiDashboardInsights) => {
      this.insights = data;
      this.loading = false;
    });
  }

  getBarWidth(count: number, max: number): string {
    return Math.round((count / max) * 100) + '%';
  }

  get maxDistrictApps(): number {
    return Math.max(...(this.insights?.districtDistribution.map(d => d.applications) ?? [1]));
  }

  get maxCategoryCount(): number {
    return Math.max(...(this.insights?.topCategories.map(c => c.count) ?? [1]));
  }

  private getMockInsights(): AiDashboardInsights {
    return {
      totalApplicationsThisMonth: 247,
      topSchemes: [
        { scheme: 'CMSDF', count: 89, percentage: 36 },
        { scheme: 'CM Care', count: 62, percentage: 25 },
        { scheme: 'CM Elevate', count: 45, percentage: 18 },
        { scheme: 'CMSG', count: 31, percentage: 13 },
        { scheme: 'CM Connect', count: 20, percentage: 8 },
      ],
      districtDistribution: [
        { district: 'East Khasi Hills', applications: 78 },
        { district: 'West Garo Hills', applications: 54 },
        { district: 'East Jaintia Hills', applications: 38 },
        { district: 'Ri Bhoi', applications: 32 },
        { district: 'East Garo Hills', applications: 28 },
        { district: 'West Khasi Hills', applications: 17 },
      ],
      topCategories: [
        { category: 'Road', count: 55 },
        { category: 'School Infrastructure', count: 48 },
        { category: 'Medical Assistance', count: 41 },
        { category: 'Community Hall', count: 37 },
        { category: 'Electricity', count: 29 },
      ],
      aiNote: 'AI analysis indicates a 12% increase in CMSDF applications compared to last month. Road and infrastructure projects dominate requests from Garo Hills region.',
    };
  }
}

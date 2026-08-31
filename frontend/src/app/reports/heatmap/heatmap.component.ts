import { AfterViewInit, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ReferenceDataDto, SchemeService } from '../../services/scheme.service';
import { ReportAnalyticsService } from '../../services/report-analytics.service';

declare const L: any;
interface SchemeOption { label: string; value: string; }
interface DistrictRow { name: string; applications: number; approved: number; approvalRate: number; color: string; }

// Geographic reference coordinates are presentation configuration, not business statistics.
const DISTRICT_COORDINATES: Record<string, [number, number]> = {
  'East Khasi Hills': [25.57, 91.88], 'West Garo Hills': [25.51, 90.22],
  'East Garo Hills': [25.58, 90.65], 'West Khasi Hills': [25.38, 91.28],
  'Ri Bhoi': [25.80, 91.85], 'South Garo Hills': [25.19, 90.41],
  'West Jaintia Hills': [25.48, 92.20], 'East Jaintia Hills': [25.32, 92.47],
  'North Garo Hills': [25.73, 90.44], 'South West Khasi Hills': [25.10, 91.45],
  'Eastern West Khasi Hills': [25.47, 91.60],
};

@Component({
  selector: 'app-heatmap', standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatSelectModule, MatFormFieldModule, MatButtonModule, MatIconModule],
  templateUrl: './heatmap.component.html', styleUrls: ['./heatmap.component.scss'],
})
export class HeatmapComponent implements OnInit, AfterViewInit {
  @ViewChild('mapContainer') mapContainer!: ElementRef;
  private map: any;
  private rows: Array<{ scheme: string; district: string; total: number; approved: number }> = [];
  mapLoading = true;
  leafletAvailable = false;
  errorMsg = '';
  selectedScheme = 'ALL';
  schemeOptionsLoading = false;
  schemeOptions: SchemeOption[] = [{ label: 'All Schemes', value: 'ALL' }];
  districtData: DistrictRow[] = [];

  constructor(private schemeService: SchemeService, private analytics: ReportAnalyticsService) {}
  get totalApplications() { return this.districtData.reduce((sum, row) => sum + row.applications, 0); }
  get totalApproved() { return this.districtData.reduce((sum, row) => sum + row.approved, 0); }
  get totalPending() { return this.totalApplications - this.totalApproved; }
  get approvalRate() { return this.totalApplications ? this.totalApproved / this.totalApplications * 100 : 0; }

  ngOnInit(): void {
    this.leafletAvailable = typeof L !== 'undefined';
    this.loadSchemeOptions();
    this.analytics.load().subscribe({
      next: data => {
        this.rows = data.schemeDistricts.map(row => ({ scheme: row.scheme, district: row.district, total: Number(row.total), approved: Number(row.approved) }));
        this.updateDistrictData(); this.mapLoading = false; this.addMarkers();
      },
      error: () => { this.errorMsg = 'Unable to load heatmap data.'; this.rows = []; this.updateDistrictData(); this.mapLoading = false; }
    });
  }

  ngAfterViewInit(): void { setTimeout(() => this.initMap(), 100); }

  private loadSchemeOptions(): void {
    this.schemeOptionsLoading = true;
    this.schemeService.getSchemeTypes().subscribe({
      next: data => { this.schemeOptions = [{ label: 'All Schemes', value: 'ALL' }, ...this.toSchemeOptions(data)]; this.schemeOptionsLoading = false; },
      error: () => { this.schemeOptionsLoading = false; }
    });
  }

  private toSchemeOptions(data: ReferenceDataDto[] | null | undefined): SchemeOption[] {
    const unique = new Map<string, SchemeOption>();
    (data ?? []).filter(item => item?.code).forEach(item => unique.set(item.code, { value: item.code, label: item.value || item.code }));
    return [...unique.values()];
  }

  updateDistrictData(): void {
    const grouped = new Map<string, { applications: number; approved: number }>();
    this.rows.filter(row => this.selectedScheme === 'ALL' || row.scheme === this.selectedScheme).forEach(row => {
      const value = grouped.get(row.district) ?? { applications: 0, approved: 0 };
      value.applications += row.total; value.approved += row.approved; grouped.set(row.district, value);
    });
    this.districtData = [...grouped.entries()].map(([name, value]) => ({
      name, ...value,
      approvalRate: value.applications ? Math.round(value.approved / value.applications * 100) : 0,
      color: value.applications > 40 ? '#dc2626' : value.applications > 20 ? '#f59e0b' : '#16a34a'
    })).sort((left, right) => right.applications - left.applications);
  }

  initMap(): void {
    if (typeof L === 'undefined') { this.mapLoading = false; this.leafletAvailable = false; return; }
    if (this.map) this.map.remove();
    this.map = L.map(this.mapContainer.nativeElement, { center: [25.5, 91.4], zoom: 8 });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OpenStreetMap contributors' }).addTo(this.map);
    this.leafletAvailable = true; this.addMarkers();
  }

  addMarkers(): void {
    this.updateDistrictData();
    if (!this.map || typeof L === 'undefined') return;
    this.map.eachLayer((layer: any) => { if (layer instanceof L.CircleMarker) this.map.removeLayer(layer); });
    const max = Math.max(...this.districtData.map(row => row.applications), 1);
    this.districtData.forEach(row => {
      const coordinates = DISTRICT_COORDINATES[row.name];
      if (!coordinates) return;
      const marker = L.circleMarker(coordinates, { radius: 10 + row.applications / max * 25, fillColor: row.color, color: 'white', weight: 2, fillOpacity: .75 }).addTo(this.map);
      marker.bindPopup(`<b>${row.name}</b><br>Applications: ${row.applications}<br>Approved: ${row.approved}<br>Pending: ${row.applications - row.approved}<br>Approval rate: ${row.approvalRate}%`);
    });
  }
}

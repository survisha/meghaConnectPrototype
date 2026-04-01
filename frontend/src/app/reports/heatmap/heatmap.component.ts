import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

// Angular Material
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

declare const L: any;

@Component({
  selector: 'app-heatmap',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatSelectModule, MatFormFieldModule, MatButtonModule, MatIconModule
  ],
  templateUrl: './heatmap.component.html',
  styleUrls: ['./heatmap.component.scss'],
})
export class HeatmapComponent implements OnInit, AfterViewInit {
  @ViewChild('mapContainer') mapContainer!: ElementRef;
  private map: any;
  mapLoading = true;
  leafletAvailable = false;

  selectedScheme = 'ALL';
  schemeOptions = [
    { label: 'All Schemes', value: 'ALL' },
    { label: 'CMSDF', value: 'CMSDF' },
    { label: 'CMSG', value: 'CMSG' },
    { label: 'CM Care', value: 'CM_CARE' },
    { label: 'CM Connect', value: 'CM_CONNECT' },
    { label: 'CM Elevate', value: 'CM_ELEVATE' },
    { label: 'Focus Plus', value: 'FOCUS_PLUS' },
  ];

  // Comprehensive district data with scheme-wise breakdown
  private allDistrictData = [
    { 
      name: 'East Khasi Hills', 
      lat: 25.57, 
      lng: 91.88, 
      schemes: {
        ALL: { applications: 142, approved: 98 },
        CMSDF: { applications: 45, approved: 32 },
        CMSG: { applications: 38, approved: 27 },
        CM_CARE: { applications: 22, approved: 15 },
        CM_CONNECT: { applications: 18, approved: 12 },
        CM_ELEVATE: { applications: 12, approved: 8 },
        FOCUS_PLUS: { applications: 7, approved: 4 }
      }
    },
    { 
      name: 'West Garo Hills', 
      lat: 25.51, 
      lng: 90.22, 
      schemes: {
        ALL: { applications: 118, approved: 79 },
        CMSDF: { applications: 38, approved: 25 },
        CMSG: { applications: 32, approved: 22 },
        CM_CARE: { applications: 19, approved: 13 },
        CM_CONNECT: { applications: 15, approved: 10 },
        CM_ELEVATE: { applications: 9, approved: 6 },
        FOCUS_PLUS: { applications: 5, approved: 3 }
      }
    },
    { 
      name: 'East Garo Hills', 
      lat: 25.58, 
      lng: 90.65, 
      schemes: {
        ALL: { applications: 87, approved: 61 },
        CMSDF: { applications: 28, approved: 20 },
        CMSG: { applications: 24, approved: 17 },
        CM_CARE: { applications: 14, approved: 10 },
        CM_CONNECT: { applications: 11, approved: 8 },
        CM_ELEVATE: { applications: 7, approved: 4 },
        FOCUS_PLUS: { applications: 3, approved: 2 }
      }
    },
    { 
      name: 'West Khasi Hills', 
      lat: 25.38, 
      lng: 91.28, 
      schemes: {
        ALL: { applications: 64, approved: 43 },
        CMSDF: { applications: 20, approved: 14 },
        CMSG: { applications: 17, approved: 12 },
        CM_CARE: { applications: 11, approved: 7 },
        CM_CONNECT: { applications: 8, approved: 5 },
        CM_ELEVATE: { applications: 5, approved: 3 },
        FOCUS_PLUS: { applications: 3, approved: 2 }
      }
    },
    { 
      name: 'Ri Bhoi', 
      lat: 25.80, 
      lng: 91.85, 
      schemes: {
        ALL: { applications: 56, approved: 39 },
        CMSDF: { applications: 18, approved: 13 },
        CMSG: { applications: 15, approved: 11 },
        CM_CARE: { applications: 9, approved: 6 },
        CM_CONNECT: { applications: 7, approved: 5 },
        CM_ELEVATE: { applications: 5, approved: 3 },
        FOCUS_PLUS: { applications: 2, approved: 1 }
      }
    },
    { 
      name: 'South Garo Hills', 
      lat: 25.19, 
      lng: 90.41, 
      schemes: {
        ALL: { applications: 48, approved: 32 },
        CMSDF: { applications: 15, approved: 10 },
        CMSG: { applications: 13, approved: 9 },
        CM_CARE: { applications: 8, approved: 5 },
        CM_CONNECT: { applications: 6, approved: 4 },
        CM_ELEVATE: { applications: 4, approved: 3 },
        FOCUS_PLUS: { applications: 2, approved: 1 }
      }
    },
    { 
      name: 'West Jaintia Hills', 
      lat: 25.48, 
      lng: 92.20, 
      schemes: {
        ALL: { applications: 39, approved: 26 },
        CMSDF: { applications: 12, approved: 8 },
        CMSG: { applications: 10, approved: 7 },
        CM_CARE: { applications: 7, approved: 5 },
        CM_CONNECT: { applications: 5, approved: 3 },
        CM_ELEVATE: { applications: 3, approved: 2 },
        FOCUS_PLUS: { applications: 2, approved: 1 }
      }
    },
    { 
      name: 'East Jaintia Hills', 
      lat: 25.32, 
      lng: 92.47, 
      schemes: {
        ALL: { applications: 35, approved: 24 },
        CMSDF: { applications: 11, approved: 8 },
        CMSG: { applications: 9, approved: 6 },
        CM_CARE: { applications: 6, approved: 4 },
        CM_CONNECT: { applications: 5, approved: 3 },
        CM_ELEVATE: { applications: 3, approved: 2 },
        FOCUS_PLUS: { applications: 1, approved: 1 }
      }
    },
    { 
      name: 'North Garo Hills', 
      lat: 25.73, 
      lng: 90.44, 
      schemes: {
        ALL: { applications: 31, approved: 21 },
        CMSDF: { applications: 10, approved: 7 },
        CMSG: { applications: 8, approved: 6 },
        CM_CARE: { applications: 5, approved: 3 },
        CM_CONNECT: { applications: 4, approved: 3 },
        CM_ELEVATE: { applications: 3, approved: 2 },
        FOCUS_PLUS: { applications: 1, approved: 0 }
      }
    },
    { 
      name: 'South West Khasi Hills', 
      lat: 25.10, 
      lng: 91.45, 
      schemes: {
        ALL: { applications: 24, approved: 16 },
        CMSDF: { applications: 8, approved: 5 },
        CMSG: { applications: 6, approved: 4 },
        CM_CARE: { applications: 4, approved: 3 },
        CM_CONNECT: { applications: 3, approved: 2 },
        CM_ELEVATE: { applications: 2, approved: 1 },
        FOCUS_PLUS: { applications: 1, approved: 1 }
      }
    },
    { 
      name: 'Eastern West Khasi Hills', 
      lat: 25.47, 
      lng: 91.60, 
      schemes: {
        ALL: { applications: 18, approved: 12 },
        CMSDF: { applications: 6, approved: 4 },
        CMSG: { applications: 5, approved: 3 },
        CM_CARE: { applications: 3, approved: 2 },
        CM_CONNECT: { applications: 2, approved: 2 },
        CM_ELEVATE: { applications: 1, approved: 1 },
        FOCUS_PLUS: { applications: 1, approved: 0 }
      }
    },
  ];

  districtData: any[] = [];

  // Computed properties for statistics
  get totalApplications(): number {
    return this.districtData.reduce((sum, d) => sum + d.applications, 0);
  }

  get totalApproved(): number {
    return this.districtData.reduce((sum, d) => sum + d.approved, 0);
  }

  get totalPending(): number {
    return this.districtData.reduce((sum, d) => sum + (d.applications - d.approved), 0);
  }

  get approvalRate(): number {
    if (this.totalApplications === 0) return 0;
    return (this.totalApproved / this.totalApplications * 100);
  }

  ngOnInit() {
    this.updateDistrictData();
    // Check if Leaflet is available
    this.leafletAvailable = typeof L !== 'undefined';
  }

  updateDistrictData() {
    this.districtData = this.allDistrictData.map(d => {
      const schemeData = d.schemes[this.selectedScheme as keyof typeof d.schemes];
      const applications = schemeData.applications;
      const approved = schemeData.approved;
      
      // Determine color based on application volume
      let color = '#16a34a'; // Low (green)
      if (applications > 40) {
        color = '#dc2626'; // High (red)
      } else if (applications > 20) {
        color = '#f59e0b'; // Medium (orange)
      }

      return {
        name: d.name,
        lat: d.lat,
        lng: d.lng,
        applications,
        approved,
        color,
        approvalRate: Math.round((approved / applications) * 100)
      };
    }).sort((a, b) => b.applications - a.applications);
  }

  ngAfterViewInit() {
    setTimeout(() => this.initMap(), 100);
  }

  initMap() {
    if (typeof L === 'undefined') {
      console.error('Leaflet library not loaded');
      this.mapLoading = false;
      this.leafletAvailable = false;
      return;
    }
    
    if (this.map) { this.map.remove(); }

    try {
      this.map = L.map(this.mapContainer.nativeElement, {
        center: [25.5, 91.4],
        zoom: 8,
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
      }).addTo(this.map);

      this.addMarkers();
      this.mapLoading = false;
      this.leafletAvailable = true;
    } catch (error) {
      console.error('Error initializing map:', error);
      this.mapLoading = false;
      this.leafletAvailable = false;
    }
  }

  addMarkers() {
    if (!this.map) return;
    
    // Update district data first
    this.updateDistrictData();
    
    // Clear existing layers except base tile layer
    this.map.eachLayer((layer: any) => {
      if (layer instanceof L.CircleMarker) {
        this.map.removeLayer(layer);
      }
    });
    
    // Find max applications for scaling
    const maxApplications = Math.max(...this.districtData.map(d => d.applications));
    
    this.districtData.forEach(d => {
      const radius = 10 + (d.applications / maxApplications) * 25;
      const circle = L.circleMarker([d.lat, d.lng], {
        radius,
        fillColor: d.color,
        color: 'white',
        weight: 2,
        opacity: 1,
        fillOpacity: 0.75,
      }).addTo(this.map);

      const scheme = this.schemeOptions.find(s => s.value === this.selectedScheme)?.label || 'All Schemes';
      circle.bindPopup(`
        <div style="font-family:system-ui;min-width:200px">
          <div style="font-weight:700;font-size:1rem;margin-bottom:0.5rem;color:#1a237e">${d.name}</div>
          <div style="font-size:0.85rem;color:#6b7280;margin-bottom:0.5rem"><b>Scheme:</b> ${scheme}</div>
          <div style="display:grid;grid-template-columns:auto 1fr;gap:0.5rem;font-size:0.85rem">
            <span style="color:#6b7280">Applications:</span>
            <b style="color:#1f2937">${d.applications}</b>
            <span style="color:#6b7280">Approved:</span>
            <b style="color:#16a34a">${d.approved}</b>
            <span style="color:#6b7280">Pending:</span>
            <b style="color:#f59e0b">${d.applications - d.approved}</b>
            <span style="color:#6b7280">Approval Rate:</span>
            <b style="color:#2563eb">${d.approvalRate}%</b>
          </div>
        </div>
      `);
    });
  }
}

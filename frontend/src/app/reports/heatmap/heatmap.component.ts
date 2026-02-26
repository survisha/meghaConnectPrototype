import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

declare const L: any;

@Component({
  selector: 'app-heatmap',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Button, Select, Tag],
  templateUrl: './heatmap.component.html',
  styleUrls: ['./heatmap.component.scss'],
})
export class HeatmapComponent implements OnInit, AfterViewInit {
  @ViewChild('mapContainer') mapContainer!: ElementRef;
  private map: any;

  selectedScheme = 'ALL';
  schemeOptions = [
    { label: 'All Schemes', value: 'ALL' },
    { label: 'CMSDF', value: 'CMSDF' },
    { label: 'CMSG', value: 'CMSG' },
    { label: 'CM Care', value: 'CM_CARE' },
    { label: 'CM Connect', value: 'CM_CONNECT' },
    { label: 'CM Elevate', value: 'CM_ELEVATE' },
  ];

  districtData = [
    { name: 'East Khasi Hills', lat: 25.57, lng: 91.88, applications: 82, approved: 58, color: '#dc2626' },
    { name: 'West Khasi Hills', lat: 25.38, lng: 91.28, applications: 34, approved: 22, color: '#f59e0b' },
    { name: 'Ri Bhoi', lat: 25.80, lng: 91.85, applications: 28, approved: 19, color: '#f59e0b' },
    { name: 'East Jaintia Hills', lat: 25.32, lng: 92.47, applications: 19, approved: 12, color: '#16a34a' },
    { name: 'West Jaintia Hills', lat: 25.48, lng: 92.20, applications: 15, approved: 10, color: '#16a34a' },
    { name: 'East Garo Hills', lat: 25.58, lng: 90.65, applications: 47, approved: 32, color: '#dc2626' },
    { name: 'West Garo Hills', lat: 25.51, lng: 90.22, applications: 61, approved: 41, color: '#dc2626' },
    { name: 'South Garo Hills', lat: 25.19, lng: 90.41, applications: 23, approved: 15, color: '#f59e0b' },
    { name: 'North Garo Hills', lat: 25.73, lng: 90.44, applications: 17, approved: 11, color: '#16a34a' },
    { name: 'South West Khasi Hills', lat: 25.10, lng: 91.45, applications: 12, approved: 8, color: '#16a34a' },
    { name: 'Eastern West Khasi Hills', lat: 25.47, lng: 91.60, applications: 9, approved: 6, color: '#16a34a' },
  ];

  ngOnInit() {}

  ngAfterViewInit() {
    setTimeout(() => this.initMap(), 100);
  }

  initMap() {
    if (typeof L === 'undefined') { return; }
    if (this.map) { this.map.remove(); }

    this.map = L.map(this.mapContainer.nativeElement, {
      center: [25.5, 91.4],
      zoom: 8,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.addMarkers();
  }

  addMarkers() {
    if (!this.map) return;
    this.districtData.forEach(d => {
      const radius = 8 + (d.applications / 82) * 22;
      const circle = L.circleMarker([d.lat, d.lng], {
        radius,
        fillColor: d.color,
        color: 'white',
        weight: 2,
        opacity: 1,
        fillOpacity: 0.75,
      }).addTo(this.map);

      circle.bindPopup(`
        <strong>${d.name}</strong><br>
        Applications: <b>${d.applications}</b><br>
        Approved: <b style="color:#16a34a">${d.approved}</b><br>
        Approval Rate: <b>${Math.round(d.approved/d.applications*100)}%</b>
      `);
    });
  }
}

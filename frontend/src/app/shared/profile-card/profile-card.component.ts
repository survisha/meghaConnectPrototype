import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Person } from '../../models';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';

@Component({
  selector: 'app-profile-card',
  standalone: true,
  imports: [CommonModule, Tag, Divider],
  template: `
    <div style="display:flex;gap:1rem;align-items:flex-start" *ngIf="person">
      <div style="width:48px;height:48px;border-radius:50%;background:linear-gradient(135deg,#1a237e,#3949ab);color:white;font-size:1.3rem;font-weight:700;display:flex;align-items:center;justify-content:center;flex-shrink:0">
        {{ person.fullName[0] }}
      </div>
      <div style="flex:1">
        <div style="font-weight:700;font-size:0.95rem">{{ person.fullName }}</div>
        <div style="font-size:0.8rem;color:#6b7280">{{ person.designation }}</div>
        <div style="font-size:0.78rem;color:#9ca3af">{{ person.constituency }}, {{ person.district }}</div>
        <div style="display:flex;gap:0.5rem;flex-wrap:wrap;margin-top:4px">
          <p-tag value="EPIC Verified" severity="info" styleClass="text-xs"></p-tag>
          <p-tag value="Active" severity="success" styleClass="text-xs"></p-tag>
        </div>
      </div>
    </div>
  `
})
export class ProfileCardComponent {
  @Input() person: Person | null = null;
}

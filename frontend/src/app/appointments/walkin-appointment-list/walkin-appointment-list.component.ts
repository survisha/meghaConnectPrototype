import { Component } from '@angular/core';
import { AppointmentListComponent } from '../appointment-list/appointment-list.component';

@Component({
  selector: 'app-walkin-appointment-list',
  standalone: true,
  imports: [AppointmentListComponent],
  templateUrl: './walkin-appointment-list.component.html',
  styleUrls: ['./walkin-appointment-list.component.scss'],
})
export class WalkinAppointmentListComponent {}

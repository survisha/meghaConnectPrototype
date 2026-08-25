import { Component } from '@angular/core';
import { AppointmentListComponent } from '../appointments/appointment-list/appointment-list.component';

@Component({selector:'app-closed-appointments',standalone:true,imports:[AppointmentListComponent],templateUrl:'./closed-appointments.component.html',styleUrls:['./closed-appointments.component.scss']})
export class ClosedAppointmentsComponent {}

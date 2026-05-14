import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { VisitorSearchService } from '../../services/visitor-search.service';
import { AppointmentService, PilotImportResult } from '../../services/appointment.service';
import { Visitor } from '../../models';
import { apiErrorMessage } from '../../shared/api-error.util';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-walkin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatChipsModule, MatIconModule, MatCardModule, MatTooltipModule],
  templateUrl: './walkin.component.html',
  styleUrls: ['./walkin.component.scss'],
})
export class WalkinComponent implements OnDestroy {
  phoneNumber = '';
  epicNumber = '';
  referenceId = '';
  foundPerson: Visitor | null = null;
  notFound = false;
  checkedIn = false;
  ticketId = '';
  agendaType = '';
  requestedLocation = 'SHILLONG';
  agendaBrief = '';
  errorMsg = '';
  associates: Visitor[] = [];
  searching = false;
  creating = false;
  pilotFile: File | null = null;
  pilotImporting = false;
  pilotImportResult: PilotImportResult | null = null;
  pilotImportError = '';
  visitorUpdateForm = this.emptyVisitorUpdateForm();
  visitorUpdatePhoto = '';
  visitorUpdateMsg = '';
  visitorUpdateError = '';
  savingVisitorUpdate = false;
  visitorCameraStream: MediaStream | null = null;
  visitorCameraActive = false;

  agendaTypes = [
    { label: 'Scheme availment (CM)', value: 'Scheme availment (CM)' },
    { label: 'Governance', value: 'Governance' },
    { label: 'Trade & Commerce', value: 'Trade & Commerce' },
    { label: 'Political Discussion', value: 'Political Discussion' },
    { label: 'Public Grievance', value: 'Public Grievance' }
  ];
  locations = ['SHILLONG', 'TURA', 'DELHI', 'OTHERS'];

  constructor(
    private visitorSearchService: VisitorSearchService,
    private appointmentService: AppointmentService,
    private router: Router
  ) {}

  ngOnDestroy() {
    this.stopVisitorCamera();
  }

  search() {
    this.errorMsg = '';
    this.notFound = false; this.foundPerson = null; this.searching = true;
    const phone = this.phoneNumber.trim();
    const epic = this.epicNumber.trim();
    const referenceId = this.referenceId.trim();

    if (!phone && !epic && !referenceId) {
      this.notFound = true;
      this.searching = false;
      this.errorMsg = 'Enter mobile, EPIC, or visitor reference ID to search.';
      return;
    }

    this.visitorSearchService.search({ mobile: phone, epic, referenceId }).subscribe({
      next: results => {
        this.foundPerson = results[0] ?? null;
        this.notFound = !this.foundPerson;
        this.visitorUpdateMsg = '';
        this.visitorUpdateError = '';
        this.stopVisitorCamera();
        if (this.foundPerson) {
          this.initVisitorUpdateForm(this.foundPerson);
        }
        this.searching = false;
      },
      error: err => {
        this.errorMsg = apiErrorMessage(err, 'Unable to search visitor records.');
        this.notFound = false;
        this.searching = false;
      }
    });
  }

  continueToAppointmentForm() {
    this.errorMsg = '';
    if (!this.foundPerson?.id) {
      this.errorMsg = 'Select an existing visitor before opening the appointment form.';
      return;
    }
    this.router.navigate(['/appointments/new'], {
      queryParams: {
        visitorId: this.foundPerson.id,
        source: 'walkin',
        walkin: true,
      }
    });
  }

  addAssociate() {
    // No-op without a real search; user must search separately
  }

  sanitizeVisitorEpic() {
    this.visitorUpdateForm.epicNumber = this.visitorUpdateForm.epicNumber.toUpperCase().replace(/[^A-Z0-9]/g, '');
  }

  sanitizeVisitorPhone() {
    this.visitorUpdateForm.phoneNumber = this.visitorUpdateForm.phoneNumber.replace(/\D/g, '').slice(0, 10);
  }

  sanitizeVisitorPincode() {
    this.visitorUpdateForm.pincode = this.visitorUpdateForm.pincode.replace(/\D/g, '').slice(0, 6);
  }

  async openVisitorCamera() {
    try {
      this.visitorUpdateError = '';
      this.visitorCameraStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } }
      });
      this.visitorCameraActive = true;
      setTimeout(() => {
        const video = document.getElementById('deo-camera-preview') as HTMLVideoElement;
        if (video && this.visitorCameraStream) {
          video.srcObject = this.visitorCameraStream;
          video.play();
        }
      }, 100);
    } catch {
      this.visitorUpdateError = 'Camera access was blocked. Please allow camera permission and try again.';
    }
  }

  captureVisitorPhoto() {
    const video = document.getElementById('deo-camera-preview') as HTMLVideoElement;
    if (!video) {
      this.visitorUpdateError = 'Camera is not ready yet.';
      return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext('2d');
    if (!context) {
      this.visitorUpdateError = 'Unable to capture photo.';
      return;
    }

    context.drawImage(video, 0, 0);
    this.visitorUpdatePhoto = canvas.toDataURL('image/jpeg', 0.8);
    this.stopVisitorCamera();
    this.visitorUpdateMsg = 'Photo captured. Save updates to attach it to this visitor.';
  }

  retakeVisitorPhoto() {
    this.visitorUpdatePhoto = '';
    this.openVisitorCamera();
  }

  stopVisitorCamera() {
    if (this.visitorCameraStream) {
      this.visitorCameraStream.getTracks().forEach(track => track.stop());
      this.visitorCameraStream = null;
    }
    this.visitorCameraActive = false;
  }

  saveVisitorUpdates() {
    this.visitorUpdateMsg = '';
    this.visitorUpdateError = '';
    if (!this.foundPerson?.id) {
      this.visitorUpdateError = 'Search and select a visitor before saving updates.';
      return;
    }

    this.savingVisitorUpdate = true;
    const addressLine = this.visitorUpdateForm.addressLine.trim();
    const payload = {
      fullName: this.visitorUpdateForm.fullName.trim(),
      phoneNumber: this.visitorUpdateForm.phoneNumber.trim(),
      epicNumber: this.visitorUpdateForm.epicNumber.trim(),
      designation: this.visitorUpdateForm.designation.trim(),
      address: addressLine,
      fullAddress: addressLine,
      address1: addressLine,
      addressLine,
      district: this.visitorUpdateForm.district.trim(),
      constituency: this.visitorUpdateForm.constituency.trim(),
      booth: this.visitorUpdateForm.booth.trim(),
      boothVillage: this.visitorUpdateForm.boothVillage.trim(),
      village: this.visitorUpdateForm.village.trim(),
      location: this.visitorUpdateForm.location.trim(),
      city: this.visitorUpdateForm.city.trim(),
      state: this.visitorUpdateForm.state.trim(),
      pincode: this.visitorUpdateForm.pincode.trim(),
      briefProfile: this.visitorUpdateForm.briefProfile.trim(),
      livePhotoBase64: this.visitorUpdatePhoto || undefined,
    };

    this.visitorSearchService.update(this.foundPerson.id, payload).subscribe({
      next: updated => {
        this.foundPerson = {
          ...this.foundPerson,
          ...updated,
          photoBase64: this.visitorUpdatePhoto || this.foundPerson?.photoBase64,
          livePhotoBase64: this.visitorUpdatePhoto || this.foundPerson?.livePhotoBase64,
        } as Visitor;
        this.initVisitorUpdateForm(this.foundPerson);
        this.visitorUpdatePhoto = '';
        this.visitorUpdateMsg = 'Visitor details updated.';
        this.savingVisitorUpdate = false;
      },
      error: err => {
        this.visitorUpdateError = apiErrorMessage(err, 'Unable to update visitor details.');
        this.savingVisitorUpdate = false;
      }
    });
  }

  onPilotFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.pilotImportError = '';
    this.pilotImportResult = null;
    this.pilotFile = input.files?.[0] ?? null;
  }

  uploadPilotSheet(fileInput: HTMLInputElement) {
    this.pilotImportError = '';
    this.pilotImportResult = null;
    if (!this.pilotFile) {
      this.pilotImportError = 'Select an Excel file before importing.';
      return;
    }

    this.pilotImporting = true;
    this.appointmentService.importPilotAppointments(this.pilotFile).subscribe({
      next: result => {
        this.pilotImportResult = result;
        this.pilotImporting = false;
        this.pilotFile = null;
        fileInput.value = '';
      },
      error: err => {
        this.pilotImportError = apiErrorMessage(err, 'Unable to import the pilot Excel sheet.');
        this.pilotImporting = false;
      }
    });
  }

  resetPilotImport(fileInput: HTMLInputElement) {
    this.pilotFile = null;
    this.pilotImportResult = null;
    this.pilotImportError = '';
    fileInput.value = '';
  }

  private initVisitorUpdateForm(visitor: Visitor) {
    this.visitorUpdateForm = {
      fullName: visitor.fullName ?? '',
      phoneNumber: visitor.phoneNumber ?? '',
      epicNumber: visitor.epicNumber ?? '',
      designation: visitor.designation ?? '',
      addressLine: visitor.addressLine ?? visitor.address1 ?? visitor.fullAddress ?? visitor.address ?? '',
      district: visitor.district ?? '',
      constituency: visitor.constituency ?? '',
      booth: visitor.booth ?? '',
      boothVillage: visitor.boothVillage ?? visitor.booth ?? '',
      village: visitor.village ?? '',
      location: visitor.location ?? '',
      city: visitor.city ?? '',
      state: visitor.state ?? '',
      pincode: visitor.pincode ?? '',
      briefProfile: visitor.briefProfile ?? '',
    };
  }

  private emptyVisitorUpdateForm() {
    return {
      fullName: '',
      phoneNumber: '',
      epicNumber: '',
      designation: '',
      addressLine: '',
      district: '',
      constituency: '',
      booth: '',
      boothVillage: '',
      village: '',
      location: '',
      city: '',
      state: '',
      pincode: '',
      briefProfile: '',
    };
  }
}

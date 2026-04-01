import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScheduleEventService } from '../services/schedule-event.service';
import { ScheduleEvent, EventType } from '../models';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, provideNativeDateAdapter } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';
import { DragDropModule, CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';

@Component({
  selector: 'app-scheduling',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    MatFormFieldModule, 
    MatInputModule, 
    MatSelectModule, 
    MatButtonModule, 
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCardModule,
    DragDropModule
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './scheduling.component.html',
  styleUrls: ['./scheduling.component.scss'],
})
export class SchedulingComponent implements OnInit {
  events: ScheduleEvent[] = [];
  viewMode: 'day' | 'week' | 'month' = 'day';
  selectedEvent: ScheduleEvent | null = null;
  selectedDate: Date = new Date();
  showDialog = false;
  showAddDialog = false;
  loading = false;

  newEvent: Partial<ScheduleEvent> = {};

  hours = Array.from({ length: 13 }, (_, i) => `${String(i + 8).padStart(2,'0')}:00`);

  eventTypes = [
    { label: 'A1 – Cabinet / Union Minister / Media / Flight', value: 'A1' },
    { label: 'A2 – Event / Programme', value: 'A2' },
    { label: 'A3 – File Clearing / Birthday', value: 'A3' },
    { label: 'A4 – Individual Appointment', value: 'A4' },
    { label: 'B1 – Public Durbar', value: 'B1' },
    { label: 'B2 – Public Walk-in', value: 'B2' },
  ];

  locations = [
    { label: 'Shillong', value: 'SHILLONG' },
    { label: 'Tura', value: 'TURA' },
    { label: 'Delhi', value: 'DELHI' },
    { label: 'Others', value: 'OTHERS' },
  ];

  constructor(private scheduleEventService: ScheduleEventService) {}

  ngOnInit() {
    // Initialize with dummy data for demo purposes
    this.initializeDummyData();
    
    console.log('[SchedulingComponent] ✅ Initialized', this.events.length, 'dummy events');
    
    // Set initial date to April 1, 2026 for consistent demo
    this.selectedDate = new Date(2026, 3, 1); // Month is 0-indexed, so 3 = April
    
    // Log first 3 events to verify structure
    if (this.events.length > 0) {
      console.log('[SchedulingComponent] First 3 events:', 
        this.events.slice(0, 3).map(e => ({
          id: e.id,
          startTime: e.startTime,
          parsedDate: new Date(e.startTime).toDateString()
        }))
      );
    }
    
    console.log('[SchedulingComponent] Events for Apr 1:', this.getEventsForSelectedDate().length);
    
    this.loading = true;
    this.scheduleEventService.getAll().subscribe({
      next: events => {
        // For demo purposes, ALWAYS use component dummy data (April 2026) instead of API/mock service data (March 2024)
        // Only override if API returns fresh data with recent dates
        if (events && events.length > 0) {
          const firstEventDate = new Date(events[0].startTime);
          if (firstEventDate.getFullYear() === 2026) {
            console.log('[SchedulingComponent] ✅ API returned 2026 data - using it');
            this.events = events;
          } else {
            console.log('[SchedulingComponent] ⚠️ API returned old data from', firstEventDate.getFullYear(), '- keeping April 2026 dummy data');
          }
        } else {
          console.log('[SchedulingComponent] ✅ No API events - keeping dummy data');
        }
        // Dummy data already initialized, API data overrides if available
        this.loading = false; 
        console.log('[SchedulingComponent] Final total:', this.events.length, 'events');
        console.log('[SchedulingComponent] Final Apr 1 count:', this.getEventsForSelectedDate().length);
      },
      error: () => { 
        this.loading = false;
        console.log('[SchedulingComponent] ✅ API error - keeping', this.events.length, 'dummy events');
      }
    });
  }

  private initializeDummyData() {
    // Demo schedule events with AI-generated summaries - Comprehensive multi-day calendar
    // Using fixed dates in April 2026 for consistent demo calendar population
    const dates = {
      apr01: '2026-04-01',
      apr02: '2026-04-02',
      apr03: '2026-04-03',
      apr05: '2026-04-05',
      apr08: '2026-04-08',
      apr10: '2026-04-10',
      apr12: '2026-04-12',
      apr15: '2026-04-15',
      apr18: '2026-04-18',
      apr20: '2026-04-20',
      apr22: '2026-04-22',
      apr25: '2026-04-25',
      apr28: '2026-04-28',
      apr30: '2026-04-30'
    };
    

    
    this.events = [
      // APRIL 1 (Today)
      {
        id: 1,
        title: 'Weekly Cabinet Briefing - Policy Review & Budget Allocation Discussion',
        eventType: 'A1',
        startTime: `${dates.apr01}T09:00:00`,
        endTime: `${dates.apr01}T11:30:00`,
        location: 'SHILLONG',
        description: 'Strategic policy and budget review session with cabinet members at Conference Hall A, Secretariat',
        shortNotes: 'Strategic session covering quarterly budget review, infrastructure project approvals, and upcoming legislative priorities. Key decisions on education reform and healthcare expansion expected.',
        isConflict: false
      },
      {
        id: 2,
        title: 'File Clearing Session - Pending Scheme Applications & Departmental Approvals',
        eventType: 'A3',
        startTime: `${dates.apr01}T11:30:00`,
        endTime: `${dates.apr01}T13:00:00`,
        location: 'SHILLONG',
        description: 'Administrative file clearing and scheme application reviews at CM Office',
        shortNotes: 'Expedited review of 47 pending files including CMSDF applications, road construction proposals, and education grants. Focus on clearing backlog from West Garo Hills and East Khasi Hills districts.',
        isConflict: false
      },
      {
        id: 3,
        title: 'Public Durbar - Constituency Grievance Resolution & Direct Citizen Engagement',
        eventType: 'B1',
        startTime: `${dates.apr01}T14:00:00`,
        endTime: `${dates.apr01}T16:30:00`,
        location: 'SHILLONG',
        description: 'Open public interaction session for grievance resolution at Main Durbar Hall',
        shortNotes: 'Open public interaction session addressing water supply issues in Shillong East, land dispute resolutions, and healthcare accessibility concerns. Approximately 15 citizens scheduled for audience.',
        isConflict: false
      },
      {
        id: 4,
        title: 'Individual Appointment - Shri Ramesh Kumar - CM Care Fund Application Review',
        eventType: 'A4',
        startTime: `${dates.apr01}T16:30:00`,
        endTime: `${dates.apr01}T17:00:00`,
        location: 'SHILLONG',
        description: 'One-on-one meeting for CM Care medical assistance application at Meeting Room 3',
        shortNotes: 'One-on-one discussion regarding medical emergency assistance under CM Care scheme for cardiac surgery requiring ₹2.5L support. Applicant from Tura, West Garo Hills. Supporting medical documents verified by CMO.',
        isConflict: false,
        travelTimeMinutes: 0
      },
      {
        id: 5,
        title: 'State Development Event - Launch of Digital Meghalaya Initiative',
        eventType: 'A2',
        startTime: `${dates.apr01}T18:00:00`,
        endTime: `${dates.apr01}T19:30:00`,
        location: 'SHILLONG',
        description: 'Official launch ceremony for Digital Meghalaya e-governance platform at State Secretariat Auditorium',
        shortNotes: 'Inauguration ceremony for e-governance platform covering online citizen services, digital payment integration, and AI-powered grievance redressal system. Expected attendance: 200+ officials and stakeholders.',
        isConflict: false
      },
      
      // APRIL 2
      {
        id: 6,
        title: 'Delhi Flight - Union Cabinet Meeting & Central Government Coordination',
        eventType: 'A1',
        startTime: `${dates.apr02}T08:00:00`,
        endTime: `${dates.apr02}T09:30:00`,
        location: 'DELHI',
        description: 'Flight departure to Delhi for Union Cabinet meeting and inter-state coordination',
        shortNotes: 'Travel to Delhi for high-level discussions on NEC funding, special category status review, and infrastructure project approvals. Meeting with Union Home Minister and Finance Secretary scheduled.',
        isConflict: false,
        travelTimeMinutes: 150
      },
      {
        id: 7,
        title: 'Union Cabinet Briefing - NEC Funding & Special Status Discussion',
        eventType: 'A1',
        startTime: `${dates.apr02}T11:00:00`,
        endTime: `${dates.apr02}T13:30:00`,
        location: 'DELHI',
        description: 'High-level meeting with Union ministers on North East funding and state development projects',
        shortNotes: 'Critical meeting on North Eastern Council special funding allocation, infrastructure grants, and tourism development initiatives. Presentation on Meghalaya connectivity projects and smart city proposals.',
        isConflict: false
      },
      {
        id: 8,
        title: 'Return Flight - Delhi to Shillong',
        eventType: 'A1',
        startTime: `${dates.apr02}T16:00:00`,
        endTime: `${dates.apr02}T17:30:00`,
        location: 'DELHI',
        description: 'Return flight from Delhi to Shillong',
        shortNotes: 'Return journey after successful Union Cabinet meetings. Outcomes include ₹850 crore infrastructure package approval and fast-track clearance for 3 major road connectivity projects.',
        isConflict: false,
        travelTimeMinutes: 150
      },
      
      // APRIL 3
      {
        id: 9,
        title: 'District Review Meeting - Infrastructure Development Status (Tura)',
        eventType: 'A2',
        startTime: `${dates.apr03}T10:00:00`,
        endTime: `${dates.apr03}T12:00:00`,
        location: 'TURA',
        description: 'Quarterly review of district-level infrastructure projects in Garo Hills region',
        shortNotes: 'Comprehensive review of ongoing road construction, school building projects, and PHC upgrades across Garo Hills districts. Department heads presenting progress reports on 23 active projects.',
        isConflict: false,
        travelTimeMinutes: 180
      },
      {
        id: 10,
        title: 'Birthday Celebration & Community Engagement - Village Elder Felicitation',
        eventType: 'A3',
        startTime: `${dates.apr03}T13:00:00`,
        endTime: `${dates.apr03}T14:00:00`,
        location: 'TURA',
        description: 'Felicitation ceremony for community leader birthday celebration at Tura Community Hall',
        shortNotes: 'Birthday felicitation for 85-year-old village elder Shri Chennitha Sangma, noted social worker and freedom fighter. Community gathering with 200+ attendees expected.',
        isConflict: false
      },
      {
        id: 11,
        title: 'Walk-in Counter - Open Public Consultation (Tura)',
        eventType: 'B2',
        startTime: `${dates.apr03}T14:30:00`,
        endTime: `${dates.apr03}T16:30:00`,
        location: 'TURA',
        description: 'Open walk-in session for unscheduled public consultations at Deputy Commissioner Office',
        shortNotes: 'Open walk-in counter session for citizens without prior appointments. DEO-assisted registration and preliminary screening. Average 20-25 walk-ins expected from surrounding villages.',
        isConflict: false
      },
      {
        id: 12,
        title: 'Individual Appointment - Kong Evaristarisha Warjri - SHG Funding Proposal',
        eventType: 'A4',
        startTime: `${dates.apr03}T17:00:00`,
        endTime: `${dates.apr03}T17:30:00`,
        location: 'SHILLONG',
        description: 'Meeting with SHG leader for skill training center funding proposal',
        shortNotes: 'Discussion on ₹12 lakh funding proposal for women skill training center in Nongstoin covering 200+ SHG members. Focus on weaving, food processing, and handicrafts training programs.',
        isConflict: false,
        travelTimeMinutes: 180
      },
      
      // APRIL 5
      {
        id: 13,
        title: 'Media Event - Press Conference on Healthcare Expansion Initiative',
        eventType: 'A1',
        startTime: `${dates.apr05}T10:00:00`,
        endTime: `${dates.apr05}T11:30:00`,
        location: 'SHILLONG',
        description: 'Press conference announcing new healthcare infrastructure and mobile medical unit rollout',
        shortNotes: 'Major announcement on ₹450 crore healthcare expansion including 15 new PHCs, 50 mobile medical units, and telemedicine centers across remote areas. Expected media coverage from 25+ outlets.',
        isConflict: false
      },
      
      // APRIL 8
      {
        id: 14,
        title: 'File Clearing - Education Grants & School Infrastructure Approvals',
        eventType: 'A3',
        startTime: `${dates.apr08}T12:00:00`,
        endTime: `${dates.apr08}T13:30:00`,
        location: 'SHILLONG',
        description: 'Administrative review of education department proposals and school upgrade requests',
        shortNotes: 'Processing 32 school infrastructure upgrade proposals including library expansion, computer lab setups, and sports facility development. Total allocation: ₹85 lakhs across 11 districts.',
        isConflict: false
      },
      {
        id: 15,
        title: 'Public Durbar - Weekly Citizen Grievance Resolution Session',
        eventType: 'B1',
        startTime: `${dates.apr08}T14:00:00`,
        endTime: `${dates.apr08}T17:00:00`,
        location: 'SHILLONG',
        description: 'Weekly public durbar for direct citizen interaction and complaint resolution',
        shortNotes: 'Weekly grievance redressal session covering land disputes, pension issues, road connectivity problems, and scheme application queries. 25 citizens pre-registered for face-to-face audience.',
        isConflict: false
      },
      
      // APRIL 10
      {
        id: 16,
        title: 'State Programme - Independence Day Preparations & Security Review',
        eventType: 'A2',
        startTime: `${dates.apr10}T10:00:00`,
        endTime: `${dates.apr10}T12:00:00`,
        location: 'SHILLONG',
        description: 'Coordination meeting for upcoming Independence Day celebrations and security arrangements',
        shortNotes: 'Planning session covering venue arrangements, cultural programs, security deployment, and VIP coordination for state-level Independence Day celebrations. Expected gathering: 5000+ citizens.',
        isConflict: false
      },
      {
        id: 17,
        title: 'Individual Appointment - Dr. Carness Lyngdoh - Healthcare Equipment Proposal',
        eventType: 'A4',
        startTime: `${dates.apr10}T14:00:00`,
        endTime: `${dates.apr10}T14:30:00`,
        location: 'SHILLONG',
        description: 'Meeting with medical officer on emergency medical equipment procurement for district hospital',
        shortNotes: 'Discussion on urgent procurement of ICU equipment, ventilators, and diagnostic machines for East Khasi Hills Civil Hospital. Proposal amount: ₹35 lakhs under emergency medical fund.',
        isConflict: false
      },
      
      // APRIL 12
      {
        id: 18,
        title: 'Walk-in Counter - Open Public Consultation',
        eventType: 'B2',
        startTime: `${dates.apr12}T09:00:00`,
        endTime: `${dates.apr12}T11:00:00`,
        location: 'SHILLONG',
        description: 'Open walk-in session for unscheduled public consultations',
        shortNotes: 'Open walk-in counter session for citizens without prior appointments. DEO-assisted registration. Average 20-25 walk-ins expected.',
        isConflict: false
      },
      {
        id: 19,
        title: 'File Clearing - CMSDF Applications & Emergency Fund Approvals',
        eventType: 'A3',
        startTime: `${dates.apr12}T13:00:00`,
        endTime: `${dates.apr12}T15:00:00`,
        location: 'SHILLONG',
        description: 'Priority review of Chief Minister Special Development Fund applications',
        shortNotes: 'Fast-track processing of 18 CMSDF applications including community hall construction, water supply projects, and emergency medical assistance. Total fund allocation: ₹2.3 crores pending approval.',
        isConflict: false
      },
      
      // APRIL 15
      {
        id: 20,
        title: 'State Event - Technology Summit on AI in Governance',
        eventType: 'A2',
        startTime: `${dates.apr15}T10:00:00`,
        endTime: `${dates.apr15}T12:30:00`,
        location: 'SHILLONG',
        description: 'Technology summit focusing on AI adoption in public service delivery and e-governance',
        shortNotes: 'State technology summit showcasing AI-powered grievance redressal, chatbot citizen services, and predictive analytics for scheme delivery. Keynote by NIELIT Director and IIT experts. Expected participants: 150+ tech professionals.',
        isConflict: false
      },
      {
        id: 21,
        title: 'Cabinet Meeting - Legislative Session Planning',
        eventType: 'A1',
        startTime: `${dates.apr15}T14:00:00`,
        endTime: `${dates.apr15}T16:30:00`,
        location: 'SHILLONG',
        description: 'Strategic cabinet session on upcoming legislative agenda',
        shortNotes: 'Cabinet discussion on bills for upcoming legislative assembly session covering education reform, land revenue act amendments, and tourism infrastructure development.',
        isConflict: false
      },
      
      // APRIL 18
      {
        id: 22,
        title: 'District Tour - Jaintia Hills Development Review',
        eventType: 'A2',
        startTime: `${dates.apr18}T09:00:00`,
        endTime: `${dates.apr18}T12:00:00`,
        location: 'OTHERS',
        description: 'Field visit to Jaintia Hills for infrastructure and development program review',
        shortNotes: 'On-ground assessment of road connectivity projects, school infrastructure, and mining area rehabilitation. Meeting with local leaders and department officials.',
        isConflict: false,
        travelTimeMinutes: 120
      },
      {
        id: 23,
        title: 'Public Durbar - Jaintia Hills Citizens Meeting',
        eventType: 'B1',
        startTime: `${dates.apr18}T14:00:00`,
        endTime: `${dates.apr18}T16:30:00`,
        location: 'OTHERS',
        description: 'Public durbar session during district tour',
        shortNotes: 'Grievance resolution and direct interaction with citizens of Jaintia Hills districts. Focus on mining issues, environmental concerns, and livelihood support.',
        isConflict: false
      },
      
      // APRIL 20
      {
        id: 24,
        title: 'File Clearing - Departmental Approvals & Policy Reviews',
        eventType: 'A3',
        startTime: `${dates.apr20}T10:00:00`,
        endTime: `${dates.apr20}T12:00:00`,
        location: 'SHILLONG',
        description: 'Administrative file clearing session covering multiple departments',
        shortNotes: 'Processing pending files from PWD, Health, Education, and Finance departments. Total 35 files requiring immediate attention.',
        isConflict: false
      },
      
      // APRIL 22
      {
        id: 25,
        title: 'State Programme - World Earth Day Celebration',
        eventType: 'A2',
        startTime: `${dates.apr22}T10:00:00`,
        endTime: `${dates.apr22}T12:00:00`,
        location: 'SHILLONG',
        description: 'State-level Earth Day celebration and environmental awareness program',
        shortNotes: 'Tree plantation drive, environmental policy announcement, and awards for green initiatives. Expected participation: 1000+ students and environmental activists.',
        isConflict: false
      },
      {
        id: 26,
        title: 'Individual Appointment - Shri Balajied Syiemlieh - Agricultural Loan',
        eventType: 'A4',
        startTime: `${dates.apr22}T14:00:00`,
        endTime: `${dates.apr22}T14:30:00`,
        location: 'SHILLONG',
        description: 'Meeting with farmer for agricultural development loan proposal',
        shortNotes: 'Discussion on ₹25 lakh loan proposal for cold storage facility in Ri Bhoi district. Organic farming project with employment potential for 50+ workers.',
        isConflict: false
      },
      
      // APRIL 25
      {
        id: 27,
        title: 'Weekly Cabinet Review - Progress & Performance',
        eventType: 'A1',
        startTime: `${dates.apr25}T09:00:00`,
        endTime: `${dates.apr25}T11:30:00`,
        location: 'SHILLONG',
        description: 'Weekly cabinet review of departmental performance and ongoing projects',
        shortNotes: 'Review of Q1 performance metrics, budget utilization, and project implementation status across all departments.',
        isConflict: false
      },
      {
        id: 28,
        title: 'Public Durbar - Weekly Grievance Session',
        eventType: 'B1',
        startTime: `${dates.apr25}T14:00:00`,
        endTime: `${dates.apr25}T17:00:00`,
        location: 'SHILLONG',
        description: 'Weekly public durbar for citizen grievances and direct interaction',
        shortNotes: 'Regular grievance resolution session with 20+ pre-registered citizens covering various issues from healthcare to infrastructure.',
        isConflict: false
      },
      
      // APRIL 28
      {
        id: 29,
        title: 'File Clearing - Scheme Approvals & Fund Releases',
        eventType: 'A3',
        startTime: `${dates.apr28}T10:00:00`,
        endTime: `${dates.apr28}T12:00:00`,
        location: 'SHILLONG',
        description: 'Administrative session for scheme approvals and fund release authorizations',
        shortNotes: 'Processing government scheme applications and fund release orders. Total allocation: ₹5.2 crores across various welfare schemes.',
        isConflict: false
      },
      
      // APRIL 30
      {
        id: 30,
        title: 'Month-End Review - April Performance Assessment',
        eventType: 'A2',
        startTime: `${dates.apr30}T10:00:00`,
        endTime: `${dates.apr30}T13:00:00`,
        location: 'SHILLONG',
        description: 'Comprehensive monthly review meeting with all department heads',
        shortNotes: 'Month-end performance review covering budget utilization, project completion rates, scheme delivery metrics, and planning for May priorities.',
        isConflict: false
      },
      {
        id: 31,
        title: 'Walk-in Counter - Month-End Public Session',
        eventType: 'B2',
        startTime: `${dates.apr30}T14:00:00`,
        endTime: `${dates.apr30}T16:00:00`,
        location: 'SHILLONG',
        description: 'Month-end walk-in session for pending public consultations',
        shortNotes: 'Final public walk-in session of the month for citizens with urgent matters. DEO-assisted processing.',
        isConflict: false
      }
    ];
    
    console.log('[SchedulingComponent] Created', this.events.length, 'dummy events');
  }

  onDateSelected(date: Date | null) {
    if (date) {
      this.selectedDate = date;
      // Automatically switch to day view to show events
      this.viewMode = 'day';
      
      const eventsForDate = this.getEventsForSelectedDate();
      console.log(`[SchedulingComponent] Selected: ${date.toDateString()} - ${eventsForDate.length} event(s)`);
    }
  }

  getEventsForSelectedDate(): ScheduleEvent[] {
    const filtered = this.events.filter(event => {
      const eventDate = new Date(event.startTime);
      return this.isSameDay(eventDate, this.selectedDate);
    });
    
    // Debug log when no events found
    if (filtered.length === 0 && this.events.length > 0) {
      console.warn('[getEventsForSelectedDate] ❌ No events found!');
      console.log('  Selected date:', this.selectedDate.toDateString(), 
                  '| Y:', this.selectedDate.getFullYear(),
                  'M:', this.selectedDate.getMonth(),
                  'D:', this.selectedDate.getDate());
      console.log('  Sample event date:', new Date(this.events[0].startTime).toDateString(),
                  '| Y:', new Date(this.events[0].startTime).getFullYear(),
                  'M:', new Date(this.events[0].startTime).getMonth(),
                  'D:', new Date(this.events[0].startTime).getDate());
      console.log('  Event startTime string:', this.events[0].startTime);
    }
    
    return filtered;
  }

  isSameDay(date1: Date, date2: Date): boolean {
    return date1.getFullYear() === date2.getFullYear() &&
           date1.getMonth() === date2.getMonth() &&
           date1.getDate() === date2.getDate();
  }

  getEventClass(type: EventType): string {
    const m: Record<string,string> = { A1:'event-a1', A2:'event-a2', A3:'event-a3', A4:'event-a4', B1:'event-b1', B2:'event-b2' };
    return 'cal-slot ' + (m[type] ?? '');
  }

  getEventColor(type: EventType) {
    const m: Record<string,string> = { A1:'#1565c0', A2:'#2e7d32', A3:'#f57f17', A4:'#c62828', B1:'#4527a0', B2:'#006064' };
    return m[type] ?? '#666';
  }

  getStartHour(event: ScheduleEvent): number {
    return new Date(event.startTime).getHours();
  }

  getEventsForHour(hour: string): ScheduleEvent[] {
    const h = parseInt(hour);
    const selectedDateEvents = this.getEventsForSelectedDate();
    return selectedDateEvents.filter(e => new Date(e.startTime).getHours() === h);
  }

  openEvent(event: ScheduleEvent) { 
    this.selectedEvent = event; 
    this.showDialog = true; 
  }

  formatTime(dt: string) {
    return new Date(dt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  formatDate(date: Date): string {
    return date.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  addEvent() {
    if (this.newEvent.title) {
      this.scheduleEventService.create(this.newEvent).subscribe({
        next: created => {
          this.events.push(created);
          this.newEvent = {};
          this.showAddDialog = false;
        },
        error: () => {}
      });
    }
  }

  // Drag and drop handler
  onEventDrop(event: CdkDragDrop<ScheduleEvent[]>, targetHour: string) {
    const droppedEvent = event.item.data as ScheduleEvent;
    const targetHourNum = parseInt(targetHour);
    
    // Calculate new start and end times
    const eventDate = new Date(droppedEvent.startTime);
    const duration = new Date(droppedEvent.endTime).getTime() - new Date(droppedEvent.startTime).getTime();
    
    // Create new date with selected date and target hour
    const newStart = new Date(this.selectedDate);
    newStart.setHours(targetHourNum, 0, 0, 0);
    
    const newEnd = new Date(newStart.getTime() + duration);
    
    // Update the event
    droppedEvent.startTime = newStart.toISOString();
    droppedEvent.endTime = newEnd.toISOString();
    
    console.log('[SchedulingComponent] Event dropped to', targetHour, '- New time:', droppedEvent.startTime);
    
    // In a real app, you would call the service to update the event on the backend
    // this.scheduleEventService.update(droppedEvent.id, droppedEvent).subscribe();
  }

  // Check if a date has events (for calendar highlighting)
  dateHasEvents = (date: Date): boolean => {
    return this.events.some(event => {
      const eventDate = new Date(event.startTime);
      return this.isSameDay(eventDate, date);
    });
  }

  // Custom date class for calendar styling
  dateClass = (date: Date): string => {
    return this.dateHasEvents(date) ? 'has-events' : '';
  }
}

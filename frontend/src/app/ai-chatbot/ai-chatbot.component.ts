import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

interface ChatMessage {
  role: 'user' | 'bot';
  text: string;
  time: string;
}

@Component({
  selector: 'app-ai-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chatbot.component.html',
  styleUrls: ['./ai-chatbot.component.scss'],
})
export class AiChatbotComponent {
  isOpen = false;
  messages: ChatMessage[] = [
    {
      role: 'bot',
      text: 'Hello! I am MeghaBot 🤖 – your AI assistant for MeghaConnect.\n\nHow can I help you today? You can ask me about:\n• How to register as a visitor\n• How to book an appointment\n• Required documents for CMSDF\n• How to track application status',
      time: this.now(),
    },
  ];
  inputText = '';
  loading = false;

  private readonly FAQ: Record<string, string> = {
    register:
      'To register as a visitor:\n1. Go to the Registration page.\n2. Enter your EPIC (Voter ID) or Aadhaar number.\n3. Receive and verify OTP on your registered mobile.\n4. Capture a live photo for KYC.\n5. Fill in your designation and location.\n6. Submit to complete registration.',
    appointment:
      'To book an appointment with the CM Office:\n1. Log in to MeghaConnect with your mobile number and OTP.\n2. Click "Book New Appointment" on your dashboard.\n3. Fill in your personal details, agenda, scheme details (if applicable).\n4. Add associate visitors if needed.\n5. Upload required documents.\n6. Review and submit.\nYou will receive an Application ID. The CMO team will contact you to schedule.',
    cmsdf:
      'Documents required for CMSDF (CM Support and Development Fund):\n• EPIC / Voter ID scan\n• Application Letter\n• Plans & Estimates (up to 3 files)\n• Bank Account Details\n• MLA/MDC/Community Leader Approval Letter (if applicable)\nFor CM Care – also Hospital Documents and Eligibility Proof.',
    track:
      'To track your application status:\n1. Log in to MeghaConnect.\n2. Go to "My Portal" → "Appointment History" or "Active Schemes".\n3. Your application ID and current status are displayed.\nStatus flow: Submitted → CMO Review → Approver Review → HCM Pending → Scheduled/Accepted/Rejected.',
    documents:
      'General documents needed for appointment booking:\n• EPIC / Voter ID scan (mandatory)\n• Application Letter (mandatory)\n• Bank Account Details (mandatory)\n• Plans & Estimates (for project/scheme applications)\n• Additional documents may be required based on scheme type.',
  };

  constructor(private http: HttpClient) {}

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  sendMessage() {
    const text = this.inputText.trim();
    if (!text) return;

    this.messages.push({ role: 'user', text, time: this.now() });
    this.inputText = '';
    this.loading = true;

    // Try AI API first, fall back to FAQ
    this.http.post<{ answer: string }>('/api/ai/chatbot', { question: text }).pipe(
      catchError(() => of({ answer: this.getLocalAnswer(text) }))
    ).subscribe((res: { answer: string }) => {
      this.loading = false;
      this.messages.push({
        role: 'bot',
        text: res.answer || this.getLocalAnswer(text),
        time: this.now(),
      });
    });
  }

  private getLocalAnswer(question: string): string {
    const q = question.toLowerCase();
    if (q.includes('register') || q.includes('sign up') || q.includes('kyc')) {
      return this.FAQ['register'];
    }
    if (q.includes('appointment') || q.includes('book') || q.includes('cm')) {
      return this.FAQ['appointment'];
    }
    if (q.includes('cmsdf') || q.includes('scheme') || q.includes('document') || q.includes('upload')) {
      return this.FAQ['cmsdf'];
    }
    if (q.includes('track') || q.includes('status') || q.includes('application')) {
      return this.FAQ['track'];
    }
    if (q.includes('what document') || q.includes('required')) {
      return this.FAQ['documents'];
    }
    return "I'm sorry, I didn't understand that. You can ask me about:\n• How to register as a visitor\n• How to book an appointment\n• Required documents for CMSDF\n• How to track your application status";
  }

  private now(): string {
    return new Date().toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}

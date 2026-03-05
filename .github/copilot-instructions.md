# GitHub Copilot Coding Agent – Project Instructions

## Custom Agent Assignment

**All tasks in this repository must be assigned to and executed by `#agent-Narsingh`.**

When you receive any task or feature request, follow this workflow automatically:

### Step 1 – Agent Assignment Prompt

When a task is requested, the agent must confirm assignment with this prompt:

```
🤖 Task received.
📋 Assigning to: #agent-Narsingh (/.github/agents/agent-Narsingh.md)
📌 Task: <task description>
🔄 Auto-update: SRS.md will be updated after implementation.
✅ Proceeding...
```

### Step 2 – Implement the Task

Implement all requested changes following the instructions defined in `.github/agents/agent-Narsingh.md`.

### Step 3 – Auto-Update SRS.md

After completing any task, **automatically update `docs/SRS.md`** to reflect the changes:

1. Add or update the relevant section in `docs/SRS.md` that describes the new feature or change.
2. Update the **Document Control** table at the end of `docs/SRS.md` with:
   - A new version row (increment patch/minor version as appropriate)
   - Today's date
   - Author: `Agent Narsingh`
   - A brief description of what changed

### Critical UI/UX Rule: White Background Mandate

**ALL INPUT FIELDS, TEXTAREAS, AND DROPDOWNS MUST HAVE WHITE BACKGROUNDS**

This is a **MANDATORY** design requirement across the entire application:

| Element Type | Background | Text Color | Border |
|---|---|---|---|
| All input fields | `white` or `#FFFFFF` | `#1f2937` (dark grey) | `#d1d5db` (light grey) |
| Textareas | `white` or `#FFFFFF` | `#1f2937` | `#d1d5db` |
| Dropdowns/Selects | `white` or `#FFFFFF` | `#1f2937` | `#d1d5db` |
| PrimeNG p-select | `white` or `#FFFFFF` | `#1f2937` | `#d1d5db` |
| Tables (rows) | `white` or `#FFFFFF` | `#1f2937` | - |
| Dialog content | `white` or `#FFFFFF` | `#1f2937` | - |
| Card backgrounds | `white` or `#FFFFFF` | `#1f2937` | `#e5e7eb` |

**Implementation Requirements:**
- Use `background: white !important;` and `background-color: white !important;` for all form inputs
- Never use dark or black backgrounds for any input element
- Ensure text is always dark grey (`#1f2937`) for readability
- Global styles in `frontend/src/styles.scss` enforce this - do not override
- When creating new components, verify all inputs have white backgrounds
- Hover states may use light grey (`#F8FAFC`, `#F3F4F6`) but never dark backgrounds

**Enforcement:**
- All HTML component files must comply with this rule
- Any new input elements must explicitly set `background: white !important;`
- Existing components violating this rule must be fixed immediately
- This rule takes precedence over any PrimeNG theme defaults

### UI Framework Mandate: Angular Material for Forms & Tables, PrimeNG for Charts Only

**ALL forms, tables, and UI components MUST use Angular Material**

This is a **MANDATORY** architecture requirement:

| Component Type | Required Framework | Component Examples |
|---|---|---|
| Forms & Inputs | **Angular Material** | `mat-form-field`, `mat-input`, `mat-select`, `mat-datepicker` |
| Tables | **Angular Material** | `mat-table` with `matColumnDef` |
| Buttons | **Angular Material** | `mat-button`, `mat-raised-button`, `mat-icon-button` |
| Dialogs/Modals | **Angular Material** | `MatDialog`, `mat-dialog-content` |
| Dropdowns | **Angular Material** | `mat-select`, `mat-autocomplete` |
| Checkboxes/Radio | **Angular Material** | `mat-checkbox`, `mat-radio-button` |
| Navigation | **Angular Material** | `mat-toolbar`, `mat-sidenav`, `mat-menu` |
| Cards | **Angular Material** | `mat-card` |
| Tabs/Steppers | **Angular Material** | `mat-stepper`, `mat-tab-group` |
| Progress | **Angular Material** | `mat-progress-bar`, `mat-progress-spinner` |
| **Charts ONLY** | **PrimeNG** | `p-chart` (or use `chart.js` directly) |

**Banned for Forms/Tables/UI:**
- ❌ PrimeNG for forms/tables (`p-inputtext`, `p-select`, `p-datatable`) - DO NOT USE
- ❌ Bootstrap forms - DO NOT USE
- ❌ Plain HTML forms without Angular Material - DO NOT USE

**Implementation Rules:**
1. **Install Angular Material first** (if not already installed):
   ```bash
   cd frontend
   ng add @angular/material
   ```

2. **Always import Angular Material modules** in standalone components:
   ```typescript
   import { MatInputModule } from '@angular/material/input';
   import { MatSelectModule } from '@angular/material/select';
   import { MatButtonModule } from '@angular/material/button';
   import { MatFormFieldModule } from '@angular/material/form-field';
   import { MatTableModule } from '@angular/material/table';
   ```

3. **Use Angular Material component tags** in templates:
   ```html
   <mat-form-field appearance="outline">
     <mat-label>Name</mat-label>
     <input matInput [(ngModel)]="name" />
   </mat-form-field>
   
   <mat-form-field appearance="outline">
     <mat-label>District</mat-label>
     <mat-select [(ngModel)]="district">
       <mat-option *ngFor="let d of districts" [value]="d">{{d}}</mat-option>
     </mat-select>
   </mat-form-field>
   
   <button mat-raised-button color="primary" (click)="submit()">Submit</button>
   ```

4. **Tables must use mat-table**:
   ```html
   <table mat-table [dataSource]="data">
     <ng-container matColumnDef="name">
       <th mat-header-cell *matHeaderCellDef>Name</th>
       <td mat-cell *matCellDef="let item">{{item.name}}</td>
     </ng-container>
     <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
     <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
   </table>
   ```

5. **PrimeNG Usage - Charts Only**:
   - Keep PrimeNG installed ONLY for chart components
   - Use `p-chart` for data visualization if needed
   - Or use `chart.js` directly (already in package.json)

6. **Material Theme Configuration**:
   - Custom theme in `frontend/src/styles.scss`
   - Use Material Design color palette
   - Maintain white backgrounds for all input fields

**Rationale:**
- Angular Material provides better integration with Angular 19
- Material Design is a well-established design system
- Better accessibility (a11y) out of the box
- Consistent with Google's design principles
- PrimeNG retained only for specialized chart components
- Reduces dependency footprint for UI components

### Summary of Rules

| Rule | Behaviour |
|---|---|
| Task assignment | Always delegated to `#agent-Narsingh` |
| SRS update | Always updated in `docs/SRS.md` after every task |
| Agent instructions | Defined in `.github/agents/agent-Narsingh.md` |
| Commit message | Must reference the task and include `[agent-Narsingh]` tag |
| Input backgrounds | **MUST BE WHITE** - Never black or dark backgrounds |
| UI Framework | **MUST USE Angular Material** - PrimeNG only for charts |
| Forms & Tables | **Angular Material only** - mat-input, mat-select, mat-table, etc. |

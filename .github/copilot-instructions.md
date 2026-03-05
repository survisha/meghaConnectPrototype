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

### Critical UI/UX Rule: Button Visibility Mandate

**ALL BUTTONS MUST BE VISIBLE AND PROPERLY STYLED**

This is a **MANDATORY** design requirement to ensure all interactive elements are visible:

| Button Type | Framework | Background | Text Color | Border | Min Height |
|---|---|---|---|---|---|
| Primary raised | `mat-raised-button color="primary"` | `#1a237e` (primary blue) | `white` | None | `36px` |
| Accent raised | `mat-raised-button color="accent"` | `#00897b` (teal) | `white` | None | `36px` |
| Outlined/Stroked | `mat-stroked-button` or `mat-outlined-button` | `transparent` | `#1a237e` | `1.5px solid` | `36px` |
| Text button | `mat-button` | `transparent` | `#1a237e` | None | `36px` |
| Icon button | `mat-icon-button` | `transparent` | `#1a237e` | None | `40px` |
| Warn button | `mat-raised-button color="warn"` | `#dc2626` (red) | `white` | None | `36px` |

**Implementation Requirements:**
- **ALWAYS use Angular Material button directives**: `mat-raised-button`, `mat-stroked-button`, `mat-button`, `mat-icon-button`
- **NEVER use plain HTML `<button>` tags** without Material directives
- All buttons MUST have explicit `color` attribute: `color="primary"`, `color="accent"`, or `color="warn"`
- Buttons MUST import `MatButtonModule` in component imports
- Full-width buttons MUST use `class="full-width"` utility class
- Icon buttons MUST wrap icons in `<mat-icon>` tags

**Global Button Styles (`frontend/src/styles.scss`):**
```scss
// All Material buttons have:
- min-height: 36px (40px for icon buttons)
- padding: 0 16px
- font-weight: 500
- display: inline-flex with gap for icons
- Proper hover states with background color changes
- Disabled states with reduced opacity
- Box shadows for raised buttons
```

**Usage Examples:**
```html
<!-- Primary action button (visible blue background) -->
<button mat-raised-button color="primary" (click)="submit()">
  <mat-icon>check</mat-icon> Submit
</button>

<!-- Secondary action button (outlined) -->
<button mat-stroked-button (click)="cancel()">
  Cancel
</button>

<!-- Text button for tertiary actions -->
<button mat-button (click)="reset()">
  <i class="pi pi-refresh"></i> Reset
</button>

<!-- Icon-only button -->
<button mat-icon-button (click)="close()">
  <mat-icon>close</mat-icon>
</button>

<!-- Full-width button -->
<button mat-raised-button color="primary" class="full-width" (click)="save()">
  Save Changes
</button>

<!-- Disabled button -->
<button mat-raised-button color="primary" [disabled]="loading || !isValid">
  <span *ngIf="!loading">Submit</span>
  <span *ngIf="loading"><i class="pi pi-spin pi-spinner"></i> Processing...</span>
</button>
```

**Common Button Patterns:**
- **Form submit**: `mat-raised-button color="primary"` (visible blue)
- **Cancel/Back**: `mat-stroked-button` (outlined, no color attr = default)
- **Delete/Remove**: `mat-raised-button color="warn"` (red background)
- **Next/Continue**: `mat-raised-button color="primary"`
- **Previous**: `mat-stroked-button` with left arrow icon
- **Add/Create**: `mat-raised-button color="primary"` with plus icon
- **Close dialog**: `mat-icon-button` with `<mat-icon>close</mat-icon>`

**Enforcement:**
- ALL buttons MUST be visible in UI (proper background, text color, height)
- ANY button visibility issue MUST be reported immediately
- Global button styles in `frontend/src/styles.scss` enforce this - do NOT override without critical reason
- Component-specific button styles are FORBIDDEN unless for specialized layouts
- Buttons MUST maintain 36px minimum height for accessibility (touch targets)
- This rule takes precedence over any Angular Material theme defaults

**Testing Checklist:**
- [ ] All buttons render with visible background/border
- [ ] Button text is readable (white on primary blue, dark on outlined)
- [ ] Hover states work (background color change visible)
- [ ] Disabled buttons show visually distinct state
- [ ] Icons in buttons are properly aligned and visible
- [ ] Full-width buttons span container width
- [ ] Buttons are keyboard accessible (tab navigation)

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
| Button visibility | **MUST BE VISIBLE** - Always use Material directives (`mat-raised-button`, `mat-stroked-button`, etc.) |
| UI Framework | **MUST USE Angular Material** - PrimeNG only for charts |
| Forms & Tables | **Angular Material only** - mat-input, mat-select, mat-table, etc. |

# Task Assignment Prompt – How to Assign Tasks to Agent Narsingh

## Overview

Whenever you want to assign a task in this project, use the prompt template below.  
The GitHub Copilot Coding Agent will automatically:

1. **Route** the task to `#agent-Narsingh` (`.github/agents/agent-Narsingh.md`).
2. **Implement** the requested changes.
3. **Auto-update** `docs/SRS.md` with the new changes.

---

## Prompt Template

Copy and paste this prompt when assigning a task:

```
@agent #agent-Narsingh

Task: <describe your task here>

Requirements:
- <requirement 1>
- <requirement 2>

After completing the task:
- Update docs/SRS.md to reflect the new changes.
- Add an entry in the Document Control table in docs/SRS.md.
```

---

## Example Prompts

### Example 1 – Add a new feature

```
@agent #agent-Narsingh

Task: Add a new API endpoint for CM Scheme application status check.

Requirements:
- Endpoint: GET /api/v1/schemes/{schemeId}/status
- Returns: application status, last updated timestamp, and assigned officer name
- JWT-protected; accessible by CITIZEN and OFFICER roles

After completing the task:
- Update docs/SRS.md section 3 (CM Scheme Applications) to document the new endpoint.
- Add an entry in the Document Control table in docs/SRS.md.
```

### Example 2 – Fix a bug

```
@agent #agent-Narsingh

Task: Fix the visitor QR pass expiry validation bug.

Requirements:
- QR pass should expire exactly 24 hours after issue, not at midnight.
- Ensure the fix covers both web and mobile flows.

After completing the task:
- Update docs/SRS.md section 12 (QR Pass + Face Verification) if the logic changed.
- Add an entry in the Document Control table in docs/SRS.md.
```

### Example 3 – Update documentation

```
@agent #agent-Narsingh

Task: Update the SRS to include the new Grievance Priority Matrix.

Requirements:
- Add a table showing grievance priority levels (P1–P4) with SLA hours.
- Add it under section 4 (Grievance Management).

After completing the task:
- Update docs/SRS.md with the new section.
- Add an entry in the Document Control table in docs/SRS.md.
```

---

## What Happens When You Send the Prompt

```
📥 Task received
📋 Assigned to  : #agent-Narsingh
📁 Agent file   : .github/agents/agent-Narsingh.md
📌 Task         : <your task description>
🔄 SRS update   : docs/SRS.md will be updated automatically
✅ Starting work...
```

The agent will:
1. Read its instructions from `.github/agents/agent-Narsingh.md`.
2. Implement all requested changes in the repository.
3. Open (or update) a pull request with the changes.
4. Update `docs/SRS.md` – adding or editing the relevant section and appending a row to the Document Control table.

---

## Document Control (this file)

| Version | Date | Author | Changes |
|---|---|---|---|
| 1.0 | Mar 2026 | Agent Narsingh | Initial task-assignment prompt guide |

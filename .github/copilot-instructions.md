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

### Summary of Rules

| Rule | Behaviour |
|---|---|
| Task assignment | Always delegated to `#agent-Narsingh` |
| SRS update | Always updated in `docs/SRS.md` after every task |
| Agent instructions | Defined in `.github/agents/agent-Narsingh.md` |
| Commit message | Must reference the task and include `[agent-Narsingh]` tag |

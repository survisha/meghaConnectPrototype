# meghaConnectPrototype
I want to create the proto type for Meghalaya Entry-Exit and handling cm schemes UI and backend structure and DB schemas to show a demo for approval of the design

## Custom Agents

This repository uses [GitHub Copilot custom agents](https://docs.github.com/en/copilot/using-github-copilot/using-copilot-coding-agent/customizing-the-coding-agent-with-an-agent-md-file) stored in `.github/agents/`.

### agent-narsingh

**Agent Narsingh** is the dedicated coding assistant for this project. It specializes in:
- Meghalaya Entry-Exit system design
- CM schemes UI and frontend components
- Backend API structure
- Database schemas

#### How to assign a task to agent-narsingh

1. **Open a new GitHub Issue** (or a Pull Request) in this repository.
2. Add a clear task description in the issue body.
3. In the issue body or a comment, mention the agent using:

   ```
   @github-copilot use agent-narsingh
   ```

   or assign the Copilot agent to the issue and it will automatically pick up the `agent-narsingh` agent definition from `.github/agents/agent-narsingh.md`.

4. The agent will read its instructions from `.github/agents/agent-narsingh.md` and start working on the assigned task.

#### Example

```
Title: Create DB schema for Entry-Exit tracking

Body:
Design a PostgreSQL schema for recording citizen entry and exit events for Meghalaya border checkpoints.

@github-copilot use agent-narsingh
```

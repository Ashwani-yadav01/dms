# Agent Instructions

## Persistent Project Context - TRACK.md

`TRACK.md` is the project's persistent development memory.

The user does NOT manually maintain `TRACK.md`.

The agent is responsible for creating, reading, and updating `TRACK.md`.

### At the start of every session

Before investigating, planning, or implementing anything:

1. Check whether `TRACK.md` exists.
2. If it exists, read it completely.
3. Use it to understand:
   - Previously completed work
   - Current progress
   - Problems already identified
   - Problems already solved
   - Important implementation decisions
   - Known issues
   - Remaining work
4. After reading `TRACK.md`, inspect the actual source code relevant to the current task.

Do not assume that previous chat history is available.

The repository and `TRACK.md` should contain enough information to continue development across new sessions.

### If TRACK.md does not exist

Create `TRACK.md` as part of the project's initial tracking setup.

The initial file should contain:

- Project overview
- Current architecture/features discovered
- Known problems
- Current task
- Completed work
- Remaining work
- Important decisions

Do not invent information.

Only record information discovered from the repository or explicitly provided by the user.

### Updating TRACK.md

The agent MUST update `TRACK.md` after meaningful development work.

Update it when:

- A feature is implemented
- A bug is fixed
- A problem is investigated
- An architectural decision is made
- A task is partially completed
- Testing is completed
- A new important problem is discovered

Record what actually happened, not what was merely discussed.

For each meaningful task, record:

- Problem
- Status
- What was changed
- Files changed
- Important implementation decisions
- Testing performed
- Result
- Remaining issues

Use clear statuses such as:

```text
NOT STARTED
INVESTIGATING
WAITING FOR APPROVAL
IN PROGRESS
IMPLEMENTED
TESTED
PARTIAL
BLOCKED
```

Never mark planned work as completed.

If implementation is incomplete, mark it as `PARTIAL`.

If the task cannot continue, mark it as `BLOCKED` and explain why.

### Keep TRACK.md concise

`TRACK.md` is a context file, not a copy of the codebase.

Do NOT put:

- Large code blocks
- Entire files
- Huge logs
- Repeated explanations
- Unnecessary conversation history

Keep only information that will help a future agent continue the project correctly.

### Protect project history

Do not erase useful previous information when updating `TRACK.md`.

When updating it:

1. Preserve completed work.
2. Update the current status.
3. Add newly discovered information.
4. Remove information only when it is demonstrably obsolete or incorrect.

Before changing an existing entry, verify the current code when necessary.

---

# Backend Changes

Before modifying any backend file, you MUST:

1. Inspect and analyze the relevant code.
2. Explain the problem.
3. Explain what you are going to implement.
4. List the backend files you intend to change.
5. Explain the changes for each file.
6. Ask for my explicit permission.
7. STOP and wait for approval.

Do NOT modify, create, delete, rename, or configure backend files before receiving explicit permission.

This includes:

- Java source files
- Controllers
- Services
- Repositories
- Entities
- DTOs
- Configuration files
- `application.properties`
- `application.yml`
- `pom.xml`
- Database configuration
- Kafka configuration
- Redis configuration
- Security/authentication
- Docker/backend infrastructure
- Backend dependencies

You may freely:

- Read backend files
- Search backend files
- Analyze code
- Debug and identify problems
- Explain errors
- Suggest solutions
- Create an implementation plan

Statements such as "fix this", "implement this", or "make this work" do NOT grant permission to modify the backend.

### Required Backend Workflow

```text
Read TRACK.md
      ↓
Inspect relevant code
      ↓
Analyze
      ↓
Explain problem
      ↓
Implementation plan
      ↓
List files
      ↓
Ask permission
      ↓
WAIT
      ↓
Implement after approval
      ↓
Test
      ↓
Update TRACK.md
```

Before asking permission, use this format:

```text
Problem:
...

Implementation:
...

Files to change:
- ...

Expected result:
...

May I implement these backend changes?
```

Never modify backend files while merely investigating or debugging an issue.

---

# Frontend Changes

Frontend changes do NOT require explicit permission.

Before making a significant frontend implementation, briefly explain:

- What you are going to implement
- Which frontend files/components will be changed
- The expected result

Then proceed with the implementation.

After completing a significant frontend task:

1. Test the implementation.
2. Update `TRACK.md`.
3. Record the files changed and the result.

---

# General Development Rules

## Solve Problems Sequentially

Work on one clearly defined problem at a time.

Do not combine unrelated problems into one implementation.

When the user gives multiple problems:

1. Identify and list them.
2. Start with the first approved/current problem.
3. Finish and test it.
4. Update `TRACK.md`.
5. Only then move to the next problem.

Do not silently implement future problems.

## Do Not Invent Existing APIs

Before using an endpoint, service, DTO, database table, Kafka topic, Redis key, or component:

1. Search the repository.
2. Verify that it exists.
3. Understand how it works.
4. Reuse it where appropriate.

Never invent backend routes or existing functionality.

## Preserve Existing Architecture

Prefer minimal changes to the existing architecture.

Do not introduce new libraries, services, databases, communication patterns, or major architectural changes unless necessary.

If a major architectural change is necessary, explain it before implementation.

## Repository Is the Source of Truth

`TRACK.md` provides project history and context.

The actual repository code is the final source of truth.

If `TRACK.md` conflicts with the current code:

1. Inspect the code.
2. Determine the actual state.
3. Correct `TRACK.md`.
4. Do not blindly follow outdated tracking information.

## New Session Rule

A new session must be able to continue the project without relying on previous conversation history.

The minimum required context should come from:

```text
AGENTS.md
TRACK.md
actual repository code
```

Always read `TRACK.md` before beginning work.
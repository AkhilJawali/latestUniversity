# How to Generate a Timetable — Complete Guide

A practical, plain-language guide to what you must set up, in what order, what each
piece of data actually *means*, how the engine uses it, the common mistakes that block
generation, and what parts of the system are still unfinished.

Read Sections 1–2 to understand the model, then use Section 3 as an entity-by-entity
reference (with worked examples). Sections 4–8 cover running a generation and reading
the result. Section 9 lists current limitations. Section 10 is a pre-flight checklist.

---

## 1. The big picture

The engine takes your **master data** (courses, faculty, rooms, time slots, etc.),
breaks each course into individual **sessions** (one per lecture/tutorial/practical
hour block), then tries to place every session into a free
**(day + time-slot + room)** with a **faculty** who can teach it — without any clash.

```
Master data  ->  Generate  ->  Engine derives sessions  ->  Places them  ->  Draft timetable
```

A **session** is the atomic unit the engine schedules. Think of one row in a finished
timetable: "Monday, 09:00–10:00, Room A-101, Data Structures Lecture, Dr. Rao, Batch
CSE-2025." The engine's whole job is to produce a conflict-free set of these rows.

A **clash (hard constraint violation)** is any of these happening at the same time:
- the same **faculty** in two places,
- the same **room** used twice,
- the same **batch** of students expected in two sessions,
- a session in a **room too small** for the batch.

Three possible outcomes:
- **COMPLETE** — every session placed. You get a draft you can edit, approve, publish.
- **INFEASIBLE** — the engine could not fit all sessions (too many sessions vs. available
  slots/rooms, or conflicting constraints). It returns a report listing what couldn't be
  placed and why.
- **FAILED** — an unexpected error (missing rule, bad data). Check the server log.

---

## 2. What you must create — and the correct ORDER

Everything is scoped to a **campus**. A department belongs to a campus, and all the
config below (calendar, slot grid, working days, rules, rooms) must exist **for that
same campus**. Create in this order — each entity depends on the ones above it, so a
child cannot be created until its parent exists.

| # | Entity | Depends on | One-line purpose |
|---|--------|-----------|------------------|
| 1 | **Campus** | — | The physical site; root of all data |
| 2 | **Department** | Campus | Groups courses and faculty (e.g., CSE) |
| 3 | **Program** | Department | A degree, e.g., B.Tech CSE |
| 4 | **Batch** | Program | The cohort of students to schedule |
| 5 | **Section** (optional) | Batch | A sub-group of a batch |
| 6 | **Course** | Department | What gets scheduled; carries L-T-P hours |
| 7 | **Faculty** | Department | Who teaches |
| 8 | **Faculty Competency** | Faculty + Course | Which faculty *can* teach which course |
| 9 | **Room** | Campus | Where sessions are placed |
| 10 | **Working-Day Pattern** | Campus | Which weekdays are teaching days |
| 11 | **Time-Slot Grid + Slot Definitions** | Campus | The daily period grid |
| 12 | **Session-Derivation Rules** | Campus | Converts course hours into sessions |
| 13 | **Academic Calendar** | Campus | Confirms the semester exists |

Optional / advanced: soft-constraint weights, institution common slots, faculty
availability windows, resource blocks, assets/equipment.

---

## 3. Every entity explained (with examples)

Each entry below tells you **what it is**, **why the engine needs it**, its **key
fields**, and a **concrete example**. The hierarchy Campus → Department → Program →
Batch → Section is a strict parent-child tree.

### 3.1 Campus
**What it is:** A physical location of the institution (e.g., "Main Campus, Bengaluru").
Every other piece of data ultimately hangs off a campus.

**Why the engine needs it:** The calendar, slot grid, working days, derivation rules,
and rooms are all looked up **by campus**. The engine resolves the campus from the
department you generate for, then loads that campus's configuration.

**Key fields:** `name`, `code` (a short unique identifier).

**Example:**
```
Campus: name = "Main Campus", code = "MAIN"
```

### 3.2 Department
**What it is:** An academic unit inside a campus (e.g., Computer Science & Engineering).
It groups the courses that are taught and the faculty who teach them.

**Why the engine needs it:** You generate a timetable **per department** — the generate
request takes a `departmentId`. The engine also uses the department to find the campus.

**Key fields:** `name`, `code`, `campus` (parent).

**Example:**
```
Department: name = "Computer Science & Engineering", code = "CSE", campus = MAIN
```

### 3.3 Program
**What it is:** A degree program run by a department (e.g., "B.Tech in CSE"). It sits
between the department and the batches.

**Why the engine needs it:** Batches belong to programs, and programs belong to
departments. This is the link that lets the engine find "all the student groups under
this department."

**Key fields:** `name`, `department` (parent).

**Example:**
```
Program: name = "B.Tech CSE", department = CSE
```

### 3.4 Batch
**What it is:** A cohort of students who move through the program together — usually an
intake year (e.g., "CSE 2025 intake"). This is the group that attends sessions.

**Why the engine needs it:** A batch is one half of every assignment ("this course, for
this batch"). Its **strength** (number of students) must fit inside the room chosen for
its sessions — a hard constraint.

**Key fields:** `name`, `strength` (student count), `program` (parent).

**Example:**
```
Batch: name = "CSE-2025", strength = 60, program = "B.Tech CSE"
```

### 3.5 Section (optional)
**What it is:** A sub-division of a batch, used when a large batch is split into smaller
groups (e.g., "CSE-2025 Section A" of 30 students).

**Why the engine needs it:** If a section exists, the engine uses the section's
**sub-strength** instead of the full batch strength for room-capacity checks. If you
have no sections, the batch strength is used directly.

**Key fields:** `sectionIdentifier` (e.g., "A"), `subStrength`, `batch` (parent).

**Example:**
```
Section: sectionIdentifier = "A", subStrength = 30, batch = "CSE-2025"
```

### 3.6 Course
**What it is:** A subject to be taught (e.g., "Data Structures"). Crucially, a course
declares its **L-T-P split** — how many Lecture, Tutorial, and Practical hours it has
per week. This is what drives the number of sessions.

**Why the engine needs it:** Courses are the *what* being scheduled. The L-T-P hours are
fed into the derivation rules to produce sessions. `equipmentTags` (e.g., `["projector",
"lab-pc"]`) are matched against room equipment for practicals.

**Key fields:** `name`, `code`, `department`, `lectureHours`, `tutorialHours`,
`practicalHours`, `credits`, `courseType`, `equipmentTags`.

**Example:**
```
Course: name = "Data Structures", code = "CS201", department = CSE,
        lectureHours = 3, tutorialHours = 1, practicalHours = 2,
        credits = 4.0, courseType = "CORE", equipmentTags = ["lab-pc"]
```
This course wants 3 lecture hours + 1 tutorial hour + 2 practical hours each week.

### 3.7 Faculty
**What it is:** A teacher. Each faculty has a home department and a maximum weekly
teaching load.

**Why the engine needs it:** Every session must be taught by a faculty. The engine
ensures a faculty is never double-booked (in two sessions at the same time) and does
not exceed their **max weekly load**.

**Key fields:** `name`, `department` (home), `maxWeeklyLoad` (max teaching hours/week).

**Example:**
```
Faculty: name = "Dr. Rao", department = CSE, maxWeeklyLoad = 16
```

### 3.8 Faculty Competency
**What it is:** A link that declares "this faculty **can teach** this course." It is a
capability record, not an assignment.

**Why the engine needs it:** Before a course can be scheduled it must have at least one
competent faculty (this is a precondition, see Section 8). The engine picks a competent
faculty to attach to the course's sessions.

**Key fields:** `faculty`, `course`.

**Example:**
```
Competency: faculty = "Dr. Rao" can teach course = "Data Structures (CS201)"
```
> Note: "can teach" is different from "is assigned to teach." See the course-offering
> gap in Section 9.1 — right now the engine just takes the **first** competent faculty.

### 3.9 Room
**What it is:** A physical space where sessions happen (classroom, lab, seminar hall).

**Why the engine needs it:** Every session needs a room. The room's **capacity** must be
≥ the batch/section strength (hard constraint). For practicals, the room's equipment is
matched against the course's `equipmentTags`.

**Key fields:** `name` / `code`, `capacity`, `roomType`, `campus`, equipment.

**Example:**
```
Room: code = "A-101", capacity = 70, roomType = "CLASSROOM", campus = MAIN
```
Capacity 70 comfortably holds the 60-student CSE-2025 batch.

### 3.10 Working-Day Pattern
**What it is:** The set of weekdays that count as teaching days for the campus
(e.g., Monday–Friday, or a 6-day Monday–Saturday week).

**Why the engine needs it:** It defines the *days* axis of the timetable grid. No
sessions are placed on non-working days.

**Key fields:** pattern type / list of working days, `campus`.

**Example:**
```
Working-Day Pattern: campus = MAIN, days = [MON, TUE, WED, THU, FRI]
```

### 3.11 Time-Slot Grid + Slot Definitions
**What it is:** The **grid** is the campus's daily period template; the **slot
definitions** are the individual periods inside it, each with a start and end time.

**Why the engine needs it:** Slots are the *time* axis of the grid. A session is placed
into one slot on one working day. **A session's required duration must match a slot's
duration** — this is the most common cause of infeasibility (see Section 9 / the
`SLOT_DURATION_MISMATCH` reason).

**Key fields (grid):** `gridName`, `campus`.
**Key fields (slot):** `startTime`, `endTime`, `grid` (parent).

**Example:**
```
Grid: gridName = "Standard Day", campus = MAIN
Slots: 09:00–10:00, 10:00–11:00, 11:00–12:00, 12:00–13:00, 14:00–15:00, 15:00–16:00
```
Six 60-minute slots × 5 working days = 30 (day, slot) cells available per room.

### 3.12 Session-Derivation Rules
**What it is:** The rules that convert a course's L-T-P **hours** into a **count of
sessions** and set each session's **duration**. There is one rule per component type:
LECTURE (L), TUTORIAL (T), PRACTICAL (P).

**Why the engine needs it:** Courses store hours, but the engine schedules sessions. The
rule provides two things per component:
- `slotDurationMinutes` — how long each derived session is (must match a slot duration).
- `hoursPerSession` — how many course-hours one session covers. The session count is
  `ceil(hours / hoursPerSession)`.

You should define **all three** rules (L, T, P) for the campus even if a course only has
lectures — the engine looks up all three during derivation.

**Key fields:** `componentType` ("LECTURE"/"L", etc.), `slotDurationMinutes`,
`hoursPerSession`.

**Example rules:**
```
LECTURE  : slotDurationMinutes = 60, hoursPerSession = 1
TUTORIAL : slotDurationMinutes = 60, hoursPerSession = 1
PRACTICAL: slotDurationMinutes = 120, hoursPerSession = 2
```

**Worked derivation** for the CS201 course above (L=3, T=1, P=2):
- Lectures: `ceil(3 / 1)` = **3** lecture sessions, each 60 min.
- Tutorials: `ceil(1 / 1)` = **1** tutorial session, 60 min.
- Practicals: `ceil(2 / 2)` = **1** practical session, 120 min.
- **Total = 5 sessions** the engine must place for this one course + batch pair.

Tutorial sessions are derived without equipment requirements; lecture and practical
sessions carry the course's `equipmentTags`.

### 3.13 Academic Calendar
**What it is:** The record that a given semester exists for the campus (with its academic
year and semester identifier).

**Why the engine needs it:** The generate request's `semester` and `academicYear`
strings must **exactly match** a calendar row for the department's campus, or the
precondition check fails with a 422.

**Key fields:** `campus`, `academicYear`, `semesterIdentifier`.

**Example:**
```
Academic Calendar: campus = MAIN, academicYear = "2025-26", semesterIdentifier = "ODD"
```

---

## 4. How the engine turns data into a timetable

1. **Resolve campus** from the department in the request.
2. **Build "assignments"** — pairs of (course + batch) that need scheduling, with a
   faculty attached. (Currently via a heuristic — see Section 9.1.)
3. **Derive sessions** — using the derivation rules, each course's hours become sessions
   (see the worked example in 3.12). Practicals typically become longer blocks.
4. **Place sessions** — assign each session a **day + slot + room**, respecting all hard
   rules (no double-booking a faculty, room, or batch; room big enough; faculty free;
   session duration matches slot duration).
5. **Optimise** — improve "soft" quality (faculty time preferences, minimise gaps,
   spread sessions across the week).
6. **Save the draft** with a feasibility score, a quality score, and any violations.

---

## 5. The generate request (what you send from the frontend)

```json
{
  "departmentId": 110,
  "semester": "ODD",
  "academicYear": "2025-26"
}
```

**Critical:** `semester` and `academicYear` must **exactly match** an Academic Calendar
row for the department's campus. These are literal string matches:
- If the calendar says `semester_identifier = "ODD"`, sending `"1"` fails the precondition.
- If the calendar says `academic_year = "2025-26"`, sending `"2018-2019"` fails.

Generation runs **asynchronously** — the call returns a request ID; poll the status
endpoint until it reaches a terminal state (COMPLETE / INFEASIBLE / FAILED).

---

## 6. Ready-made test data (recommended first run)

A minimal, guaranteed-to-succeed dataset already exists:
`code/utms/src/main/resources/db/seed/seed_easy_generation.sql`

Load it, then generate with:
```json
{ "departmentId": 110, "semester": "ODD", "academicYear": "2025-26" }
```
It creates 1 department, 2 lecture-only courses, 2 faculty, 1 batch, 2 rooms, and 6
daily slots over Mon–Fri → 6 sessions that always fit. Use this to confirm the API
works, then build your own data following Section 2.

There is also a larger realistic dataset: `seed_demo_data.sql` (department 1 = CSE,
campus 1, semester `"ODD"`, year `"2024-25"`), but it has many courses/batches and may
come out **INFEASIBLE** with the current heuristic (see Limitations).

---

## 7. DO's and DON'Ts

### DO
- Create data in the order in Section 2 (parents before children).
- Keep the **first** test small (few courses, one batch) so it fits easily.
- Make **slot durations match the derivation-rule durations** (if rules say 60-min
  lectures, have 60-min teaching slots).
- Give rooms **capacity ≥ batch/section strength**.
- Ensure **all three derivation rules (L, T, P)** exist for the campus, even if a course
  only has lectures (the engine looks up all three).
- Make sure the **academic calendar** row matches your semester + year strings exactly.
- Confirm at least one **faculty competency** exists per course.

### DON'T
- Don't send a semester/year that has no matching calendar row.
- Don't overload: many courses × many batches × few slots/rooms = INFEASIBLE.
- Don't rely on labs/practicals for a first test — they add equipment and
  block-scheduling constraints. Start with lecture-only courses.
- Don't expect notifications, calendar feeds, or conflict-detection UI to work yet
  (not built — see Limitations).
- Don't forget to **rebuild and restart** the backend after any code change
  (`mvn clean package`).

---

## 8. Reading the outcome

- **COMPLETE** — open the draft to see placed sessions. You can then edit, approve, and publish.
- **INFEASIBLE** — open the Infeasibility Report. It lists each unplaced session and the
  blocking constraints. Typical fixes:
  - `SLOT_DURATION_MISMATCH` → add slots whose duration equals the rule duration (or
    change the rule so its `slotDurationMinutes` matches an existing slot).
  - `FACULTY_AVAILABILITY` → the faculty has no free slot; adjust their availability
    windows or add slots.
  - Too many sessions → add rooms/slots, or reduce course hours/batches.
- **FAILED** — check the backend log line `Generation failed: requestId=...` for the exception.

### Preconditions the engine checks first (422 if any fail)
Before generating, it verifies for the department's campus:
1. Academic calendar exists (campus + year + semester).
2. Time-slot grid exists.
3. Working-day pattern exists.
4. Session-derivation rules exist.
5. At least one course has a competent faculty.
6. At least one room exists.

If any fail you get a `422` with the specific check names — fix those and retry.

---

## 9. IMPORTANT — Current limitations (what is NOT finished)

Be aware the system is partially built. These affect timetable generation directly:

### 9.1 No "course offering" model (biggest gap)
There is **no data table** that says "batch B takes course C, taught by faculty F."
The database only has:
- Courses (belong to a department),
- Faculty competencies (which faculty *can* teach a course — not *assigned*),
- Batches (belong to programs, with no link to courses).

Because of this, the engine currently uses a **temporary heuristic** to build assignments:
- Each course is paired with **every batch** under the department's programs.
- Faculty = the **first** competent faculty for the course.
- Batch strength = the section's sub-strength, else the batch strength.

**Consequence:** for a real department with many courses and batches, this creates a very
large number of sessions and often comes out INFEASIBLE. It is an approximation to make
the pipeline usable, **not** the true "who teaches what to whom."
**Proper fix (not yet done):** add a `CourseOffering` entity (course + batch + section +
faculty + strength) and a screen to enter offerings; the engine would then read real
assignments instead of guessing.

### 9.2 Faculty workload limits are partial
Only **max weekly load** is real (from the faculty record). **Daily** and **consecutive**
hour caps use safe default values because the cadre-norm/compliance module is not built.

### 9.3 Resource blocks are ignored
Room/asset blocks (maintenance, events) are **not** applied during generation — the data
model stores them by date/time with no clean mapping to day + slot yet.

### 9.4 Downstream features not built
- **Notifications** — publishing emits an event but nothing delivers messages yet.
- **Calendar export / iCal feeds** — not implemented.
- **Real-time conflict detection while editing** — not implemented.
- **Exam scheduling, lab batch-splitting** — separate unfinished modules.
- Most **frontend screens** beyond generation/editing are pending.

### 9.5 Environment / build notes
- The backend must be **rebuilt (`mvn clean package`) and restarted** after code changes.
- Generation runs asynchronously — poll the status endpoint until a terminal state.

---

## 10. Quick checklist before you click "Generate"

- [ ] Department exists and is linked to a campus.
- [ ] That campus has: calendar (matching semester+year), slot grid with slots,
      working-day pattern, L/T/P derivation rules, rooms.
- [ ] Courses exist in the department with hours > 0.
- [ ] Each course has at least one competent faculty.
- [ ] At least one batch exists under the department's programs.
- [ ] Slot durations match the derivation-rule durations.
- [ ] Rooms have capacity ≥ batch/section strength.
- [ ] Backend rebuilt + restarted; semester/year strings match the calendar exactly.

If all boxes are ticked, generation should succeed (or return a readable infeasibility
report telling you exactly what to adjust).

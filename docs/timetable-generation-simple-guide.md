# How the Timetable Engine Works — Simple Guide & Demo Notes

> This guide explains **how the UTMS scheduling engine builds a timetable**, **how to work out how
> much of everything you need** (time slots, faculty, rooms, courses, batches, departments),
> **why it fails** and **how to demo it**. Everything is based on the current backend code
> (checked 11-Sep-2026). Current limitations are marked ⚠️.

---

## Contents

1. [The one-minute explanation](#1-the-one-minute-explanation)
2. [The most important idea: the batch is the bottleneck](#2-the-most-important-idea-the-batch-is-the-bottleneck)
3. [How the engine builds a timetable — step by step](#3-how-the-engine-builds-a-timetable--step-by-step)
4. [The 10 rules the engine never breaks](#4-the-10-rules-the-engine-never-breaks)
5. [Planning: how much of everything do I need?](#5-planning-how-much-of-everything-do-i-need)
6. [Worked planning example — from zero to a working timetable](#6-worked-planning-example--from-zero-to-a-working-timetable)
7. [Why it fails — the 6 kinds of failure](#7-why-it-fails--the-6-kinds-of-failure)
8. [Why the result looks the way it does](#8-why-the-result-looks-the-way-it-does)
9. [Real case: Department AU — why it showed 5 / 34](#9-real-case-department-au--why-it-showed-5--34)
10. [Demo script](#10-demo-script)
11. [Questions the audience may ask](#11-questions-the-audience-may-ask)
12. [Where to enter the data](#12-where-to-enter-the-data)
13. [Current limitations](#13-current-limitations)

---

## 1. The one-minute explanation

> *"The coordinator enters the courses, teachers, rooms and the daily time slots.
> The engine turns every course's weekly hours into individual classes — we call them sessions.
> Then it places each session into a day, a time slot and a room, like solving a puzzle,
> making sure no teacher, room or class is in two places at once, rooms are big enough,
> and teachers don't exceed their hours. If everything fits, we get a complete draft timetable.
> If not, it tells us what could not be placed, so we know what to fix."*

```
  INPUT                          ENGINE                                  OUTPUT
  ─────                          ──────                                  ──────
  Courses (L-T-P hours)   ┐
  Teachers + their hours  │      1. Check setup is complete
  Batch (class size)      │      2. Turn hours into sessions             Draft timetable
  Rooms (size, equipment) ├──►   3. List every possible place     ──►    + scores
  Time slots per day      │      4. Place sessions like a puzzle         + unplaced list
  Working days            │      5. Save the best result                 + reasons
  Session rules           ┘
```

---

## 2. The most important idea: the batch is the bottleneck

⚠️ Today the engine schedules **one batch per department** (the class of students).

Think of the batch as **one person**. One person can attend only **one class at a time**.

So the question "*will it fit?*" is mostly:

> **Does the batch's week have enough time slots for all its classes?**

This leads to three facts that surprise people:

| Fact | Why |
|---|---|
| **Adding more rooms does not create more time** | Only one class happens at a time, so one suitable room is enough |
| **Adding more teachers does not create more time** | Teachers only help when a teacher has too many hours |
| **Only adding time slots (or reducing course hours) creates more time** | The batch needs one free slot for every session |

Keep this picture in mind — almost every failure is explained by it.

---

## 3. How the engine builds a timetable — step by step

### Step 1 — Check the setup (instant)

Before starting, the engine checks that these exist. If anything is missing, it stops immediately
and lists **all** missing items:

| Check | Plain meaning |
|---|---|
| Academic calendar | a calendar for this campus with **exactly** this academic year and semester (e.g. `2025-26`, `ODD`) |
| Time-slot grid | the campus has daily time slots |
| Working-day pattern | the campus says which weekdays are working days |
| Session rules | the campus has rules for lecture / tutorial / practical length |
| Teachers | at least one course has a teacher who can teach it |
| Rooms | the campus has at least one room |

### Step 2 — Collect the data

The engine starts from the **department** and finds its **campus**.

| Comes from the **department** | Comes from the **campus** (shared by all departments on it) |
|---|---|
| Courses | Rooms |
| Batch (⚠️ the first one created — lowest ID) | Time slots |
| Teachers of those courses | Working days |
| | Session rules |
| | Academic calendar |

For each course it picks **one teacher**: ⚠️ among the teachers marked "can teach this course", the
one **created first** (lowest ID).

A course is **left out** if it has no teacher who can teach it, or if its L + T + P hours are 0.

### Step 3 — Turn course hours into sessions

Each course has weekly **L-T-P** hours (Lecture - Tutorial - Practical). The **session rules** say how
long one session is:

| Rule | Session length | Hours per session |
|---|---|---|
| Lecture | 60 minutes | 1 |
| Tutorial | 60 minutes | 1 |
| Practical | 120 minutes | 2 |

```
number of sessions = weekly hours ÷ hours per session   (round UP)
```

Example — course **CS101 with L-T-P = 3-1-2**:

```
Lectures   : 3 ÷ 1 = 3 sessions of 60 min
Tutorials  : 1 ÷ 1 = 1 session  of 60 min
Practicals : 2 ÷ 2 = 1 session  of 120 min
                     ─────────
                     5 sessions to place
```

If practical hours were 3: `3 ÷ 2 = 1.5 → 2 sessions` (the teacher then does 4 hours, not 3).

### Step 4 — List every possible place for every session

A **place** = one **day + time slot + room**. A session can go to a place only if:

- the time slot is a **teaching** slot (not break / lunch), and
- the slot length is **exactly** the session length (a 60-min lecture never goes into a 2-hour slot), and
- the room is **big enough** for the class, and
- the room has the **equipment** the course needs (e.g. `computer`), and
- the slot is not a blocked **common slot** (e.g. a university-wide hour).

If even **one** session has **no possible place at all**, the engine stops right here —
nothing can make it fit (see failure type 3 in section 7).

### Step 5 — Place the sessions (the puzzle)

The engine works like someone solving a **sudoku**:

1. It picks the session with the **fewest options** first (the hardest one).
2. It puts it into the **first** place that breaks no rule.
3. It moves to the next hardest session.
4. If some session has **no place left**, it **undoes** the last choice and tries the next option.
5. It repeats until **all sessions are placed** — or the **time limit (2 minutes)** is reached.

It always remembers its **best attempt so far** (the most sessions placed at the same time) —
that is the "**Sessions placed (best so far)**" number on the screen.

### Step 6 — Finish and save

| What happened | Status | What is saved |
|---|---|---|
| Every session placed | **COMPLETED** | full draft (then it shuffles sessions a little to improve the quality score) |
| 2 minutes passed first | **TIMED_OUT** | best attempt + list of sessions not placed |
| Proven impossible | **INFEASIBLE** | best attempt + list not placed + reasons |
| Stopped by the user | **CANCELLED** | best attempt |
| A setup error while running | **FAILED** | nothing — only an error message |

Each new run for the same department + semester creates a **new version** of the draft; older
unapproved drafts are marked **superseded**.

---

## 4. The 10 rules the engine never breaks

| # | Rule |
|---|---|
| 1 | A **teacher** is never in two places at the same time |
| 2 | A **room** never has two classes at the same time |
| 3 | The **batch** never has two classes at the same time |
| 4 | The room is **big enough** for the class |
| 5 | The room has **all the equipment** the course needs (for lectures and practicals; tutorials need none) |
| 6 | The session length **equals** the slot length, and only teaching slots are used |
| 7 | **Common slots** stay free |
| 8 | A teacher does not go over their **weekly hours** (their *Max Weekly Load*; if empty → 40 h) |
| 9 | A teacher does not teach more than **8 hours a day** |
| 10 | A teacher does not teach more than **3 hours back-to-back** |

**Back-to-back** = slots next to each other in the day. A **break** or **lunch** slot resets the count.

> Small detail: rules 8 and 9 are checked *before* placing a session, so the last session of the
> day/week can take a teacher slightly over (e.g. 7 h + a 2-hour practical = 9 h).

---

## 5. Planning: how much of everything do I need?

Always plan **from the courses outwards**:

```
Courses & their L-T-P  ──►  Sessions  ──►  Time slots  ──►  Teachers  ──►  Rooms
```

### 5.1 Departments

- You generate **one department at a time**. Each department is planned separately.
- Each department needs its **own**: program → batch → courses → teachers who can teach those courses.
- The **campus** things are created **once** and shared: time slots, working days, session rules,
  academic calendar, rooms.
- ⚠️ Two departments generated separately **don't know about each other** — they may both use the same
  room or the same teacher at the same time. For a clean demo, use **one department**.

### 5.2 Batches and sections

- ⚠️ Only **one batch per department** is scheduled: the one **created first**.
- **Class size** = the batch strength. If the batch has sections, the **first section's** size is used instead.
- So for now: **one department = one batch** for generation.

### 5.3 Courses → sessions

For each course, work out sessions using the rules (section 3, step 3), and group them **by length**:

| Course | L-T-P | 60-min sessions | 120-min sessions | Teacher hours |
|---|---|---|---|---|
| ... | ... | lectures + tutorials | practicals ÷ 2 (round up) | total |

Add up the columns. These totals drive everything else.

### 5.4 How many time slots to create

**The rule:** for each session length, the batch needs **one slot per session per week**.

```
slots per week   = slots of that length per day  ×  working days
slots needed/day = sessions of that length  ÷  working days   (round UP)  + a little spare
```

Example: 13 one-hour sessions, 5 working days →
`13 ÷ 5 = 2.6 → 3` one-hour slots per day minimum → create **4** (one spare) → 20 per week.

Tips:
- **Create slots for every session length** your rules use (60-min slots for lectures, 120-min for practicals).
  A spare 2-hour slot does **not** help a 1-hour lecture.
- **Leave spare slots** (roughly 10–20% more than needed). A week that is 100% full is very hard to
  solve and often runs out of time.
- Check lengths carefully: `18:00–19:59` is **119 minutes**, not 120 — it will never be used.
- A 6-day week gives 20% more slots than a 5-day week with the same daily grid.
- Put a **break** between long runs of slots — it helps teachers stay under 3 hours back-to-back.
- The grid is **per campus**, so size it for the department with the **most** weekly hours.

### 5.5 How many teachers (faculty) are needed

**The rule:** each course is taught by **one** teacher, and each teacher's total must fit their weekly hours.

```
teacher's hours = sum of hours of all courses they get
teacher's hours ≤ their Max Weekly Load
```

How to size:
1. **Minimum teachers ≈ total weekly hours ÷ typical Max Weekly Load** (round up).
2. Then **group courses into teachers** so every teacher stays within their load (a course can't be split).
3. Check the **daily limits**: a teacher can teach at most 8 h/day and 3 h back-to-back.
   With a typical grid (two 1-hour slots, break, two 1-hour slots, lunch, one 2-hour slot)
   a teacher can do about **6 h per day** → about 30 h per week on 5 days.

Remember how the teacher is chosen:
- ⚠️ If several teachers can teach a course, the **first-created** one always gets it.
- Adding a second teacher to an overloaded course **does not help** — unless you remove the first
  teacher's "can teach" mark for that course.
- To control who teaches what: mark **only** the intended teacher as able to teach that course.

Teachers **don't add time**: with one batch, only one class runs at a time anyway.
More teachers only help when someone has **too many hours**.

### 5.6 How many rooms

**The rule:** only one class runs at a time, so you need very few rooms.

- **1 room** with capacity ≥ class size (for all courses without equipment needs), plus
- **1 room for each different equipment need**, with that equipment **and** capacity ≥ class size
  (e.g. a computer lab for programming courses).

⚠️ The room **type** (classroom / lab) is **not checked** — only **capacity** and **equipment tags**.
If a course has tags, **its lectures also go to that equipped room**, not only its practicals.

### 5.7 Campus setup (create once)

| Item | How many | Notes |
|---|---|---|
| Working-day pattern | 1 per campus | 5-day = Mon–Fri; 6-day or alternate-Saturday = Mon–Sat (every Saturday) |
| Academic calendar | 1 per semester | the year and semester text must match what you type when generating |
| Time-slot grid | 1 per campus | sized as in 5.4 |
| Session rules | **exactly 3**: Lecture, Tutorial, Practical | all three are needed even if no course has tutorials |
| Common slots | optional | each one removes a slot from every day it is set on |

### 5.8 The planning checklist (the 4 golden checks)

| # | Check | Passes when |
|---|---|---|
| A | **Time** | for each session length: sessions needed ≤ slots per week (with spare) |
| B | **Teachers** | every teacher's hours ≤ their Max Weekly Load, and fit ~6 h per day |
| C | **Rooms** | a room ≥ class size, and for every equipment need a room with that equipment **and** enough seats |
| D | **Rules vs slots** | every rule length (60, 120 …) exists as a teaching slot length |

If all four pass, the timetable will almost always be generated.

---

## 6. Worked planning example — from zero to a working timetable

**Situation:** The CSE department wants a timetable for its first-year batch of **60 students**.
The courses are already decided:

| Course | Name | L-T-P | Needs equipment? |
|---|---|---|---|
| CS101 | Programming in C | 3-1-2 | computers |
| MA101 | Engineering Maths | 3-1-0 | — |
| PH101 | Physics | 3-0-2 | — |
| EN101 | English | 2-0-0 | — |

Session rules: Lecture 60 min, Tutorial 60 min, Practical 120 min (2 h).

### Step 1 — Sessions

| Course | 60-min sessions | 120-min sessions | Teacher hours |
|---|---|---|---|
| CS101 | 3 lectures + 1 tutorial = **4** | 2 ÷ 2 = **1** | 6 |
| MA101 | 3 + 1 = **4** | 0 | 4 |
| PH101 | 3 + 0 = **3** | 2 ÷ 2 = **1** | 5 |
| EN101 | **2** | 0 | 2 |
| **Total** | **13** | **2** | **17** |

**15 sessions** to place.

### Step 2 — Time slots

Working days: **Monday–Friday (5)**.

| Length | Needed per week | ÷ 5 days | Minimum per day | We create | Per week |
|---|---|---|---|---|---|
| 60 min | 13 | 2.6 | 3 | **4** (1 spare) | 20 |
| 120 min | 2 | 0.4 | 1 | **1** | 5 |

The daily grid:

| Time | Type |
|---|---|
| 09:00–10:00 | Teaching (60) |
| 10:00–11:00 | Teaching (60) |
| 11:00–11:15 | Break |
| 11:15–12:15 | Teaching (60) |
| 12:15–13:15 | Teaching (60) |
| 13:15–14:00 | Lunch |
| 14:00–16:00 | Teaching (120) |

✅ Check A: 13 ≤ 20 and 2 ≤ 5 — with spare.

### Step 3 — Teachers

Total 17 hours. If a typical load is 12 h/week → `17 ÷ 12 = 1.4 → at least 2 teachers`.
We use **3** so no one is stretched:

| Created in this order | Teacher | Can teach | Hours | Max Weekly Load |
|---|---|---|---|---|
| 1st | Dr. Anil Rao | CS101 | 6 | 12 ✅ |
| 2nd | Dr. Meera Iyer | MA101, PH101 | 4 + 5 = 9 | 12 ✅ |
| 3rd | Ms. Sara Khan | EN101 | 2 | 8 ✅ |

Daily limit: with this grid a teacher can do 2 h + 2 h + 2 h = **6 h per day** → nobody is close.
✅ Check B.

### Step 4 — Rooms

Class size 60.

| Need | Room |
|---|---|
| Normal room ≥ 60 | **LH-101**, classroom, 70 seats |
| Computers + ≥ 60 seats (for CS101) | **LAB-201**, lab, 65 seats, tag `computer` |

✅ Check C. (CS101's 3 lectures **and** its practical will be in LAB-201; its tutorial can be anywhere.)

### Step 5 — Rules vs slots

Lecture 60 ✅ (60-min slots exist) · Tutorial 60 ✅ · Practical 120 ✅ (14:00–16:00). ✅ Check D.

### Step 6 — Shopping list and result

| Create | Quantity |
|---|---|
| Campus | 1 |
| Department | 1 (CSE) |
| Program | 1 (B.Tech CSE) |
| Batch | 1 (2025, strength 60) |
| Courses | 4 |
| Teachers | 3, each marked "can teach" only their courses |
| Rooms | 2 (one with tag `computer`) |
| Working-day pattern | 1 (5-day) |
| Academic calendar | 1 (2025-26, ODD) |
| Time slots | 7 (5 teaching + break + lunch) |
| Session rules | 3 |

Generate with **CSE's department ID, `ODD`, `2025-26`** → expected **COMPLETED, 15 / 15**
(worked out from the rules above).

### Step 7 — If the department grows

| Change | New numbers | Does it still fit? | What to add |
|---|---|---|---|
| Add 2 courses of 3-1-0 | 60-min: 13 + 8 = **21** > 20 | ❌ | one more 60-min slot per day (→ 25) |
| Add a 3-0-2 lab course | 60-min 16 ≤ 20, 120-min 3 ≤ 5 | ✅ | nothing (maybe a teacher) |
| Batch grows to 80 students | LH-101 (70) and LAB-201 (65) too small | ❌ | rooms with ≥ 80 seats |
| Dr. Meera also gets EN101 | 9 + 2 = 11 ≤ 12 | ✅ | nothing |
| Switch to a 6-day week | 60-min 24/week, 120-min 6/week | ✅ more spare | — |

---

## 7. Why it fails — the 6 kinds of failure

| # | What you see | How long | Plain explanation | Typical causes | Fix |
|---|---|---|---|---|---|
| 1 | Error list, generation doesn't start | instant | **"Setup is incomplete"** | no calendar for that year/semester (or text typed differently, `Odd` vs `ODD`), no time slots, no working days, no session rules, no room, no course with a teacher | create the missing item |
| 2 | **FAILED** with a message | seconds | **"A setup mistake found while running"** | one of the 3 session rules missing or inactive (`No derivation rule for component type: TUTORIAL`) | add / activate the rule |
| 3 | **INFEASIBLE, 0 placed** | seconds | **"Some class has nowhere to go at all"** | no slot with the session's length; no room big enough; no room with that equipment **and** enough seats | add the slot / room, fix lengths or tags |
| 4 | **TIMED_OUT, few placed** | 2 min | **"The week is too small"** or **"a teacher has too many hours"** | Check A or B fails | add slots / reduce hours / spread courses to more teachers / raise load |
| 5 | **TIMED_OUT, almost all placed** (e.g. 33/34) | 2 min | **"It almost fits but there is no breathing room"** | week 95–100% full, tight teacher limits | add a few spare slots |
| 6 | **COMPLETED**, but a course is missing | normal | **"That course was never given to the engine"** | course has no teacher who can teach it, or 0 hours | mark a teacher, add hours |

### Why "TIMED_OUT" and not a clear "impossible"?

The engine tries combinations like a sudoku solver. For a **small** impossible problem it can try
everything quickly and say **INFEASIBLE**. For a **real-size** problem there are millions of combinations,
so it runs out of time before it can *prove* it's impossible → **TIMED_OUT**.
**Treat TIMED_OUT with a low count as "the data does not fit" and do the 4 golden checks.**

### Why is "best so far" sometimes very low (like 5 / 34)?

The engine only counts an attempt as "progress" while **every remaining session still has somewhere
to go**. The moment one teacher runs out of hours, all of that teacher's remaining sessions have
nowhere to go — so the engine undoes and never records a bigger number, even though other courses could
fit. So a very low number usually means **one teacher is out of hours** (or the week is far too small),
**not** that only 5 classes fit.

### About the reason labels in the INFEASIBLE report

| Label | Meaning |
|---|---|
| `SLOT_DURATION_MISMATCH` | no teaching slot has this session's length |
| `ROOM_CAPACITY` | no room is big enough |
| `EQUIPMENT_MISMATCH` | no room has the equipment |
| `FACULTY_AVAILABILITY` | ⚠️ added almost always — ignore it. If it is the **only** label, a room usually has the equipment but is too small while the big room lacks the equipment |
| `RESOURCE_CONTENTION`, `BATCH_CLASH`, … | classes competing for the same slots — do checks A and B |

---

## 8. Why the result looks the way it does

| You notice | Why |
|---|---|
| **Classes are packed at the start of the week and the start of the day; Friday is light** | The engine always tries the **earliest day, earliest slot, first room** first. The final shuffle only swaps sessions between the places already used |
| **Most classes are in the same room** | Rooms are tried in the order they were created; the first suitable room is used |
| **A programming course's lectures are in the lab** | The course has the equipment tag, and tags apply to lectures too |
| **Prof X was not used even though they can teach the course** | The first-created teacher who can teach the course always gets it |
| **Running it again gives the same timetable** | The search is the same each time; only the final shuffle can move sessions between the same places |
| **Feasibility score 100%** | all sessions placed (placed ÷ required) |
| **Quality score** | a soft score (e.g. balance across days); some parts are not built yet and count as perfect |

---

## 9. Real case: Department AU — why it showed 5 / 34

**What was needed** (one batch of 30, rules 60 / 60 / 120 min):

| Course | L-T-P | 60-min sessions | 120-min sessions | Hours |
|---|---|---|---|---|
| TDH | 7-3-4 | 10 | 2 | 14 |
| AE | 10-2-0 | 12 | 0 | 12 |
| DOM | 10-0-0 | 10 | 0 | 10 |
| **Total** | | **32** | **2** | **36** |

**First runs (#6–#8) → TIMED_OUT 5 / 34**

| Check | Numbers | Result |
|---|---|---|
| A — Time | 4 one-hour slots/day × 6 days = **24** < **32** | ❌ week too small |
| B — Teacher | one teacher (first-created) got all 3 courses = **36 h**, load **8 h** | ❌ teacher out of hours after ~7 h → that's why the count stuck at 5 |

**Run #9 → "COMPLETED 34 / 34"** — ⚠️ **not a real result**. It was made by the old engine code that had a
bug (fixed on 11-Sep-2026): the same room/batch/teacher was booked twice at the same time in 13 places.

**Today's data** (load raised to 40 h, one 1-hour slot and a `18:00–19:59` slot added):

| Check | Numbers | Result |
|---|---|---|
| A — Time | 5 one-hour slots/day × 6 = **30** < **32** | ❌ still 2 short |
| A — 2-hour | `18:00–19:59` is **119 min** → never used | ⚠️ fix to 20:00 |
| B — Teacher | 36 h ≤ 40 h, but ~7 h/day max × 6 = 42 h | ⚠️ very tight |

**Fix:** add at least 2 more one-hour teaching slots per week (e.g. a break then `17:15–18:15` every day
→ 36 slots), correct the 119-minute slot, and give the three courses to **three different teachers**.

---

## 10. Demo script

**Preparation**
- Enter the data from **section 6** (one department, one batch, 4 courses, 3 teachers, 2 rooms).
- Tip: to avoid waiting 2 minutes during the demo, ask a developer to set the time limit to 30 seconds
  (`utms.scheduling.engine.default-timeout-seconds: 30` in `application.yml`, then restart the backend).
- Keep a note of the original values so you can undo each "break".

**Part 1 — Explain (3 min)**
1. Say the **one-minute explanation** (section 1).
2. Show the courses and do the **session math** for CS101 on the board (3-1-2 → 5 sessions).
3. Show the totals: 13 one-hour + 2 two-hour sessions, and the grid: 20 + 5 slots → "it fits, with spare".
4. Explain "**the batch is one person**" (section 2).

**Part 2 — Success (3 min)**
1. Scheduling → Timetable Generation → enter department ID, `ODD`, `2025-26` → **Generate**.
2. Point at the progress phases while it runs.
3. Result: **COMPLETED 15 / 15**, feasibility 100%.
4. In the sessions list show: CS101 lectures in LAB-201, no clashes, classes packed early in the week (explain section 8).

**Part 3 — Break it and explain (8–10 min).** Change one thing, generate, explain, undo.

| # | Change | Result to expect | What to say |
|---|---|---|---|
| 1 | Type semester as `Odd` | instant error: academic calendar | "Setup check — the text must match the calendar exactly." |
| 2 | Practical rule 120 → 90 min | instant **INFEASIBLE, 0 placed**, reason `SLOT_DURATION_MISMATCH` | "Practicals are now 90 minutes but we have no 90-minute slot — they have nowhere to go." |
| 3 | LAB-201 seats 65 → 50 | instant **INFEASIBLE, 0 placed** | "The only room with computers is too small for 60 students." |
| 4 | MA101 lectures 3 → 11 | **TIMED_OUT** (or INFEASIBLE), not all placed | "21 one-hour classes but only 20 one-hour slots — the week is too small. Rooms or teachers won't fix this; only more slots will." |
| 5 | Dr. Meera's Max Weekly Load 12 → 6 | **TIMED_OUT**, her courses among the unplaced | "She needs 9 hours but is allowed 6. Fix: raise her load or give PH101 to another teacher." |

**Part 4 — Close (1 min)**
- "Before generating, the coordinator does 4 checks: **time, teachers, rooms, rule lengths**."
- "When it fails, the status tells us which kind of problem it is, and the unplaced list tells us where."

---

## 11. Questions the audience may ask

| Question | Answer |
|---|---|
| How many time slots should we create? | Enough that for each session length, slots per day × working days ≥ sessions per week, plus 10–20% spare |
| How many teachers do we need? | At least total weekly hours ÷ typical weekly load, then group courses so each teacher stays within their load |
| How many rooms? | One room big enough for the class, plus one equipped room per equipment need. More rooms don't create more time |
| Can it schedule several batches of a department together? | ⚠️ Not yet — only the first batch of the department |
| If two departments share a room or teacher, will they clash? | ⚠️ Possibly — each department is generated on its own and doesn't see the other's timetable |
| Does it avoid holidays? | It makes a **weekly** pattern; holidays and exam dates are not used in generation yet |
| Does it respect teacher availability or preferences (morning/afternoon)? | ⚠️ Not yet |
| Why is Friday almost empty? | It fills the earliest days and slots first (section 8) |
| Why did it take 2 minutes and then say TIMED_OUT? | The data didn't fit and there were too many combinations to prove it within the time limit |
| Can we lock some classes and regenerate the rest? | Yes — sessions can be locked on a draft and the rest regenerated (partial regeneration) |
| Can we move classes by hand after generation? | The drag-and-drop editor can check a move for conflicts, but ⚠️ saving moves is not available yet |
| What happens after the draft? | Review → approval (Coordinator → HOD → Dean/Registrar) → publish |
| Will it give the same result every time? | Yes, for the same data (small differences possible in which class sits in which already-used slot) |

---

## 12. Where to enter the data

Create in this order (later items need earlier ones):

| # | What | Screen | Fields that matter for generation |
|---|---|---|---|
| 1 | Campus | Master Data → Campus Hierarchy | — |
| 2 | Department | Campus row → View departments | — |
| 3 | Program | Department row → View programs | — |
| 4 | Batch | Program row → View batches | **strength** (create the scheduled batch first) |
| 5 | Section (optional) | Batch row → View sections | **sub-strength** |
| 6 | Courses | Master Data → Courses | **L, T, P** |
| 7 | Teachers | Master Data → Faculty | **Max weekly load**, **competencies** ("can teach") — create in the order you want priority |
| 8 | Rooms | Master Data → Rooms | **capacity**, **equipment tags** |
| 9 | Working days | Master Data → Academic Calendar → pattern | 5-day / 6-day |
| 10 | Academic calendar | Master Data → Academic Calendar | **academic year**, **semester** text |
| 11 | Time slots | Master Data → Time-Slot Grid | **start, end, type** (leave "day" empty = every day) |
| 12 | Session rules | Scheduling → Engine Configuration → Derivation Rules | **Lecture, Tutorial, Practical: minutes + hours per session** |
| 13 | Generate | Scheduling → Timetable Generation | **department ID**, **semester**, **academic year** |

Notes:
- **Department ID:** the tables don't show it. Click "View programs" on the department — the number in
  the browser address (`…/departments/12/programs`) is the ID.
- ⚠️ **Course equipment tags** can't be set on the Courses screen yet, and editing a course there clears
  them. A developer must set them in the database (write tags exactly as on the room, e.g. `computer`).
- Keep **session rule hours = minutes ÷ 60** (60 → 1, 120 → 2) to avoid confusion.

---

## 13. Current limitations

- Only **one batch per department** (the first created) is scheduled.
- Each course goes to the **first-created** teacher who can teach it.
- Departments are generated **independently** — shared rooms/teachers across departments are not checked.
- Teacher **availability** and **preferences**, **room blocks**, **holidays** and **exam dates** are not used.
- **Room type** is ignored; use equipment tags to force labs.
- Alternate Saturdays are treated as **every** Saturday.
- Timetables are **packed early** in the week/day; there is no real balancing yet.
- A big impossible problem shows **TIMED_OUT** rather than a clear reason.
- Daily limit (8 h), back-to-back limit (3 h) are fixed; only the weekly limit comes from the teacher's data.

# Timetable Generation — Simple Summary

A quick, plain-language view of what's built, what's left, and how generating a
timetable works end to end.

---

## 1. What the whole flow looks like

```
Master Data  →  Generate  →  Review & Edit  →  Approve  →  Publish  →  (Notify + Calendar)
```

1. **Master Data** — set up campuses, departments, courses, faculty, rooms, batches,
   academic calendar, time-slot grid. (The engine reads all of this.)
2. **Generate** — the engine places every session into slots/rooms without breaking
   hard rules (no double-booking, capacity, etc.).
3. **Review & Edit** — a coordinator views the draft, sees violations, drags sessions
   around, locks good ones, or re-generates part of it.
4. **Approve** — the draft goes up a multi-level chain (e.g. Coordinator → HOD →
   Registrar). Each level approves or rejects.
5. **Publish** — on final approval, the draft becomes the official timetable. Any
   older published one is replaced.
6. **Notify + Calendar** — affected faculty/students get told and their calendar
   feeds refresh. *(planned — see "Remaining")*

---

## 2. Step-by-step: how to generate a timetable (API)

| # | Action | Endpoint |
|---|--------|----------|
| 1 | Make sure master data exists | (master-data APIs) |
| 2 | Start generation | `POST /api/v1/timetables/generate` |
| 3 | Check progress | `GET /api/v1/timetables/generate/{requestId}/status` |
| 4 | If it failed, see why | `GET /api/v1/timetables/generate/{requestId}/infeasibility` |
| 5 | Open the draft | `GET /api/v1/timetables/{draftId}` |
| 6 | See sessions | `GET /api/v1/timetables/{draftId}/sessions` |
| 7 | See soft-rule violations | `GET /api/v1/timetables/{draftId}/violations` |
| 8 | See unplaced sessions | `GET /api/v1/timetables/{draftId}/unplaced` |
| 9 | Lock / unlock a session | `POST /.../sessions/{sessionId}/lock` \| `/unlock` |
| 10 | Re-generate part of it | `POST /api/v1/timetables/{draftId}/regenerate` |
| 11 | Send for approval | (approval-workflow APIs) |
| 12 | Publish the approved draft | `POST /api/v1/timetables/{draftId}/publish` |

---

## 3. What's DONE ✅

- **Scheduling engine** — generate a full draft, hard-rule enforcement, scores,
  partial re-generation, session lock/unlock. (A4-13/14 + engine stories)
- **Drag-and-drop editor (frontend)** — visual editing of the draft. (A4-15)
- **Approval workflow** — multi-level approve/reject with audit trail. (A4-19)
- **Publication** — publish approved draft, supersede the old one, audit, and fire a
  "published" event. (A4-21)

> Note: the above is **code-complete and inspection-checked**, but the Maven build,
> unit tests, and code review have **not** been run in this environment yet.

---

## 4. What's REMAINING ⏳

- **Conflict detection (real-time)** — instant clash checks while editing. (A4-8 line)
- **Notifications** — actually deliver the "timetable published" messages. (A4-37)
- **Calendar export/feed** — iCal feeds that refresh on publish. (A4-39)
- **Faculty workload checks** — min/max load validation.
- **Exam & lab scheduling** — exam seating, invigilation, lab batch-splitting.
- **Frontend for the rest** — coordinator dashboard, approval screens, student portal.
- **Build & test pass** — run `mvn clean package` + `mvn test`, then code review and
  end-to-end testing before this can be called "shippable".

---

## 5. One-line status

The core path **generate → edit → approve → publish** is wired end to end in code.
Everything around it (conflict alerts, notifications, calendars, exams, workload,
and full testing) is still to come.

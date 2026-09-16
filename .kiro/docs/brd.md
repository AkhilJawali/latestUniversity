# Business Requirements Document

## University Timetable Management System (UTMS)

**Class, Exam, Room, Faculty & Resource Scheduling — Multi-Campus, Multi-Department**

### Document Control

| Field | Details |
|---|---|
| Document Title | Business Requirements Document — University Timetable Management System |
| Version | 0.3 (Draft) |
| Status | Draft — For Review |
| Prepared For | [University / Client Name] |
| Prepared By | [Your Name / Technoboost Services Pvt. Ltd.] |
| Date | 10 July 2026 |
| Classification | Confidential |

### Version History

| Version | Date | Author | Description of Change |
|---|---|---|---|
| 0.1 | 10-Jul-2026 | [Author] | Initial draft for stakeholder review |
| 0.2 | 15-Jul-2026 | [Author] | Added hard/soft constraint and blocking model for rooms, labs and all schedulable assets |
| 0.3 | 15-Jul-2026 | [Author] | Updated current-state baseline and KPI baseline with discovery-validated findings; added institution-specific scheduling constraints |

## 1. Introduction & Purpose

This Business Requirements Document (BRD) defines the business needs, functional scope, and operating parameters for a University Timetable Management System (UTMS). The system is intended to serve as the single source of truth for all academic scheduling activity across the institution — class/lecture timetables, examination timetables, room and lab allocation, and faculty workload planning — spanning multiple campuses and departments.

Timetabling in a multi-campus, multi-program university is a constrained optimisation problem: it must simultaneously satisfy curriculum structure, faculty availability and workload norms, room/lab capacity, student elective choices, and accreditation-mandated teaching-hour rules, while remaining flexible enough for manual override by academic administrators. This document captures the business requirements that any solution — whether built in-house, configured on a COTS platform, or a hybrid — must satisfy.

### 1.1 Document Purpose

- Provide a single, agreed reference of business requirements for all stakeholders (Registrar's Office, Academic Council, HODs/Deans, IT, Faculty, Students).
- Serve as the basis for functional specification, solution design, vendor evaluation (build vs. buy), and UAT test-case creation.
- Establish the full set of scheduling parameters and constraints the system must account for.

### 1.2 Intended Audience

- Registrar / Academic Affairs leadership (business owner)
- Deans, HODs and Department Timetable Coordinators
- Faculty and teaching staff
- Students (as consumers of the published timetable)
- IT / Solution delivery team and implementation partner
- Accreditation & compliance office (NBA / NAAC / UGC liaison)

## 2. Business Objectives

- Eliminate manual, spreadsheet-driven timetabling and reduce scheduling turnaround from weeks to days.
- Minimise scheduling conflicts (faculty double-booking, room clashes, student elective clashes) to near zero.
- Ensure equitable and compliant faculty workload distribution as per NBA/NAAC/UGC norms.
- Provide a semi-automated engine that proposes optimal timetables while allowing administrators to review, adjust, and approve before publishing.
- Support simultaneous scheduling across multiple campuses and departments with shared and campus-specific resources.
- Improve room/lab utilisation and provide visibility into idle capacity for space-planning decisions.
- Provide real-time, role-based access to timetables for faculty and students, with automated notifications on changes.
- Maintain a complete audit trail of timetable versions, changes, and approvals for compliance and grievance handling.

## 3. Scope

### 3.1 In Scope

- Master data management: campuses, departments, programs, courses/subjects, faculty, rooms/labs, batches/sections, academic calendar.
- Class / lecture timetable generation (semi-automated, with manual override) across all campuses and departments.
- Examination timetable generation (internal, semester-end, supplementary/re-exams).
- Room, lab and shared-resource (auditorium, seminar hall, sports facility) allocation and clash detection.
- Faculty availability, workload computation, and leave/substitution handling.
- Elective and multi-program scheduling, including cross-department electives and choice-based credit system (CBCS) structures.
- Multi-campus and multi-department coordination, including shared faculty across campuses and inter-campus room borrowing.
- Approval workflows (Department Coordinator → HOD → Dean / Registrar).
- Conflict detection and resolution recommendations.
- Accreditation/compliance parameter tracking (NBA/NAAC/UGC teaching-hour and workload norms).
- Notifications (email/SMS/app) for publication, changes, and substitutions.
- Reporting & analytics: utilisation, workload distribution, conflict logs, compliance reports.
- Student self-service view for elective registration and personal timetable/calendar.

### 3.2 Out of Scope

- Fee management, admissions, and core HRMS/payroll functions.
- Learning Management System (LMS) content delivery, attendance capture hardware (biometric/RFID), and online proctoring.
- Integration with any existing ERP/SIS/HRMS in this phase — the system will operate as a standalone application with its own master data.
- Transport, hostel, and campus facility booking not related to academic scheduling.

## 4. Stakeholders & User Roles

| Role | Responsibility in the System |
|---|---|
| Registrar / Academic Affairs Head | System owner; defines academic calendar, approves final institution-wide timetable, resolves cross-department conflicts. |
| Dean (School/Faculty level) | Approves department-level timetables under their school; arbitrates inter-department clashes. |
| HOD (Head of Department) | Reviews and approves department timetable; allocates faculty to courses; manages department workload norms. |
| Department Timetable Coordinator | Primary system user; enters constraints, runs the scheduling engine, adjusts proposed timetable, submits for approval. |
| Faculty / Teaching Staff | Declares availability/preferences and leave; views assigned schedule; requests substitutions. |
| Student | Registers for electives (where applicable); views personal timetable and receives change notifications. |
| Examination Controller | Manages exam timetable generation, seating and invigilation duty allocation. |
| IT / System Administrator | Manages master data, user roles, system configuration, and integrations. |
| Accreditation & Compliance Officer | Defines and monitors NBA/NAAC/UGC workload and teaching-hour parameters; consumes compliance reports. |
| Visiting / Adjunct Faculty (external) | Limited-access users who declare availability and view their own assigned sessions. |

## 5. Current State & Problem Statement

Timetabling today is typically performed independently by each department using spreadsheets, with limited visibility into shared resources (auditoriums, common labs, cross-listed electives, faculty teaching across departments/campuses). This leads to:

- Last-minute clashes discovered only after publication, requiring manual firefighting.
- Uneven faculty workload distribution and difficulty proving compliance with accreditation norms during audits.
- No single, current version of the timetable — changes circulated over email/WhatsApp create version confusion.
- Significant coordinator effort spent on manual slot-fitting rather than academic planning; discovery validated a 4–6 week, Excel-based cycle per semester at school level, run end-to-end by a single coordinator, with institution-wide cycles extending to approximately 3 months.
- A prior open-source solver pilot (FET) was evaluated and rejected: soft constraints were hard-coded as hard constraints, causing runtime to compound exponentially — reinforcing the need for a semi-automated engine with explicit hard/soft constraint separation.
- Timetables are currently published as unstructured Excel over email through multiple pre-final iterations, and room inventory is maintained manually through physical room verification.
- Limited data on room/lab utilisation to inform future infrastructure and capacity decisions.

## 6. Business Requirements

### 6.1 Master Data Management

- Maintain hierarchical master data: Campus → Department → Program → Batch/Section → Course/Subject.
- Course master: credit hours, contact hours (L-T-P split), course type (core/elective/audit), prerequisite courses.
- Faculty master: qualification, designation, home department, max/min weekly teaching load, subjects competent to teach, campus(es) of association.
- Room/Lab master: capacity, room type, equipment tags, campus/building/floor location.
- Asset / schedulable-resource master: non-room schedulable assets with owning department, campus, and an availability/blocking calendar per resource.
- Academic calendar: semester start/end dates, holidays, exam windows, orientation/induction periods, per-campus calendar variation.
- Batch/section master: strength, program, elective basket enrolled.

### 6.2 Class / Lecture Timetable Generation

- System shall generate a proposed weekly timetable per batch/section based on course credit structure, faculty assignment, and room availability.
- Support configurable time-slot grids per campus.
- Support recurring weekly patterns as well as fortnightly/alternate-week patterns.
- Allow department coordinators to lock specific sessions before running the auto-suggestion engine.
- Provide drag-and-drop manual adjustment of the auto-generated draft with real-time conflict flagging.

### 6.3 Examination Timetable Generation

- Generate exam schedules factoring minimum gap between two exams for a student/batch, room seating capacity, and invigilator availability.
- Support seating-plan generation and invigilation duty roster generation with equitable distribution across faculty.
- Flag and prevent exam clashes for students registered in cross-department electives.

### 6.4 Room, Lab & Shared Resource Allocation

- Allocate rooms/labs based on batch strength vs. room capacity, required equipment, and proximity preferences.
- Support shared/common resource booking across departments with a first-come/priority-based reservation rule set.
- Support inter-campus room borrowing rules where a department may need to use another campus's facility.
- Provide real-time room-availability and utilisation views.
- Capture room, lab, and asset availability as hard or soft constraints.
- Apply the same hard/soft blocking model to any schedulable asset, not only rooms and labs.
- Provide a resource-blocking workflow with approval for blocks that impact already-published sessions.
- On activation of a hard block, the system shall automatically re-run conflict detection, list impacted sessions, propose alternative rooms/slots, and notify affected users once reallocation is confirmed.
- Maintain a complete audit trail for every block and report block frequency and duration per resource.

### 6.5 Faculty Availability & Workload Management

- Capture faculty time-preferences/unavailability as soft or hard constraints.
- Compute weekly/semester teaching load per faculty and validate against minimum/maximum norms.
- Support faculty teaching across multiple departments/campuses with combined workload visibility.
- Handle leave and ad-hoc substitution by proposing substitute faculty based on subject competency and availability.

### 6.6 Lab & Practical Session Scheduling

- Support batch-splitting for practical sessions with corresponding multi-slot, multi-room allocation.
- Support block scheduling for labs requiring 2–3 contiguous periods.
- Track lab-specific equipment/software prerequisites for allocation matching.

### 6.7 Elective & Multi-Program (CBCS) Handling

- Support student elective basket registration and generate elective-group timetables that avoid clashes with each student's core courses.
- Support minimum/maximum enrolment thresholds per elective, with waitlisting and automatic elective-group re-balancing.
- Support cross-department and cross-program electives.

### 6.8 Approval Workflow

- Configurable multi-level workflow: Department Coordinator drafts → HOD reviews/approves → Dean/Registrar final sign-off.
- Support review comments, rejection with reason, and version comparison between draft iterations.
- Maintain a full approval audit trail.

### 6.9 Conflict Detection & Resolution

- Real-time detection of faculty double-booking, room double-booking, student/batch clashes, and exceeding max daily/weekly teaching hours.
- Provide ranked alternative-slot suggestions when a conflict is detected.
- Maintain a conflict log for audit and continuous improvement.

### 6.10 Scheduling Engine (Semi-Automated)

- Engine shall auto-generate a first-draft timetable using a constraint-based algorithm considering hard constraints and soft constraints.
- Coordinators/HODs shall be able to review, override, and manually re-arrange the proposed draft before submission for approval.
- Provide a feasibility/quality score and a list of unresolved soft-constraint violations for each generated draft.
- Support re-running the engine on a subset of the timetable without disturbing already-approved sections.

### 6.11 Compliance & Accreditation Parameters

- Configure and validate against NBA/NAAC/UGC-prescribed norms.
- Generate compliance-ready reports for accreditation visits.
- Flag non-compliant assignments before publication.
- Exact numeric norms vary by accreditation body/state/university statute and must be configurable rather than hard-coded.

### 6.12 Multi-Campus / Multi-Department Coordination

- Support campus-specific calendars, time-slot grids, and holiday lists within one unified system.
- Support faculty and shared electives spanning campuses, with travel-time buffer rules.
- Provide a consolidated, institution-wide view for the Registrar alongside department/campus-scoped views.

### 6.13 Notifications & Communication

- Automated notifications (email/SMS/in-app) to affected faculty and students on publication, rescheduling, cancellation, or substitution.
- Digest/summary notification option in addition to real-time alerts for critical changes.

### 6.14 Calendar Export & Sync

- Provide one-click, automated calendar export (iCal/.ics feed) of the published timetable for every faculty member and student.
- Support a live subscription feed so that any subsequent change, substitution, or cancellation automatically reflects in personal calendars.
- Provide a bulk/admin-level calendar export option for coordinators.
- Log export/subscription status per user for adoption and troubleshooting.

### 6.15 Add/Drop & Enrolment Change Handling

- Any elective add/drop or section-change action shall automatically and immediately update the affected student's personal timetable, class roster, and headcount-driven room/capacity checks.
- Maintain a real-time, authoritative class-roster view per session.
- Where the institution's registration/enrolment record lives in a separate system, provide a scheduled or on-demand data-exchange to keep the two in sync.
- Flag and alert coordinators when add/drop volume for a session approaches room capacity.

### 6.16 Reporting & Analytics

- Room/lab utilisation reports by campus, building, and time-slot.
- Faculty workload distribution and compliance dashboards.
- Conflict-log and resolution-turnaround reports.
- Historical timetable archive with version comparison across semesters.

### 6.17 Student Self-Service

- Provide a single consolidated view of the full course catalogue and the tentative/published timetable together before the registration window opens.
- Elective registration window with real-time seat-availability display.
- Personal timetable view (web/mobile) with calendar export/subscription and change notifications.

## 7. Scheduling Parameters — Consolidated Reference

### 7.1 Structural / Curriculum Parameters

| Parameter | Description |
|---|---|
| Program & batch structure | Program duration, semesters, sections/batches per semester, batch strength. |
| Course credit structure (L-T-P) | Lecture/Tutorial/Practical hour split per course; determines number and length of weekly sessions. |
| Core vs. elective classification | Determines mandatory vs. flexible scheduling and clash-avoidance rules. |
| Prerequisite mapping | Ensures dependent courses are not scheduled in conflicting sequence across semesters. |
| Cross-listed / cross-department courses | Courses shared across programs/departments requiring synchronized slots. |

### 7.2 Faculty Parameters

| Parameter | Description |
|---|---|
| Subject competency mapping | Which courses a faculty member is qualified/approved to teach. |
| Min/max weekly teaching load | Institutional and accreditation-mandated workload bounds. |
| Availability / blocked slots | Research time, administrative duties, part-time/visiting constraints. |
| Multi-department / multi-campus load | Combined workload visibility where faculty teach across units. |
| Leave & substitution rules | Eligible substitute pool by subject competency and availability. |
| Preference weighting | Soft preferences such as preferred time-of-day, consecutive vs. spread sessions. |

### 7.3 Room / Infrastructure Parameters

| Parameter | Description |
|---|---|
| Room capacity | Must be greater than or equal to batch/group strength. |
| Room type & equipment | Lab vs. classroom vs. seminar hall vs. auditorium; specialised equipment/software needs. |
| Location / transit buffer | Minimise back-to-back session travel time between distant buildings/campuses. |
| Shared-resource priority rules | Booking priority for common facilities used by multiple departments. |
| Maintenance / blackout windows (hard blocks) | Rooms/labs/assets strictly unschedulable during maintenance, inspection, breakdown, or institutional-event reservations. |
| Soft resource holds | Tentative reservations or preferred-use windows the engine avoids by default but may override with an explicit warning and recorded justification. |
| Asset-level blocking scope | Hard/soft blocking applies to any schedulable asset. |
| Block authorisation & workflow | Roles permitted to raise/approve blocks, reason codes, recurrence patterns, and approval rules where already-published sessions are impacted. |

### 7.4 Time & Calendar Parameters

| Parameter | Description |
|---|---|
| Time-slot grid | Period duration, number of periods/day, break/lunch windows, configurable per campus. |
| Working days pattern | 5-day/6-day week, alternate Saturdays, campus-specific variations. |
| Academic calendar | Semester dates, holidays, exam windows, induction/orientation blocks. |
| Minimum gap rules | Minimum gap between two exams for the same student/batch; max consecutive teaching hours for faculty. |
| Calendar export format | iCal/.ics feed per user; live subscription vs. one-time download; refresh-on-change behaviour. |

### 7.5 Student / Batch Parameters

| Parameter | Description |
|---|---|
| Elective basket enrolment | Which electives each student/batch is registered into; min/max enrolment per elective. |
| Batch-splitting rules | Lab group sizes and split logic for large batches. |
| Cross-program overlap | Avoiding clashes for students taking courses across UG/PG/PhD offerings. |
| Add/drop reconciliation window | Cut-off timing and real-time roster update rule so enrolment always matches the published class list. |

### 7.6 Compliance Parameters

| Parameter | Description |
|---|---|
| Accreditation workload norms | NBA/NAAC/UGC-prescribed min/max teaching hours by faculty cadre. |
| Credit-to-contact-hour mapping | Statutory mapping used in compliance reporting. |
| Invigilation duty norms | Equitable distribution rules for exam invigilation duties. |

### 7.7 Institution-Specific Scheduling Constraints (Discovery-Validated)

| Parameter | Description |
|---|---|
| Mixed slot durations | A single time-slot grid must accommodate 1-hour lecture, 1.5-hour, and 3-hour practical slot lengths simultaneously. |
| Day-pattern saturation balancing | Where specific day patterns are saturated, the engine must balance load across alternative patterns rather than compounding clashes on preferred days. |
| Compressed practical windows | Practical/lab sessions are compressed into a narrow afternoon window; the engine must optimise lab-group rotation and room usage within that window. |
| Support-staff availability bounds | Lab sessions are bounded by lab technician/support-staff working hours and treated as a hard constraint on lab slot placement. |
| Institution-level common slots (CCC / UWE) | Core Common Curriculum and University-Wide Elective slots are institution-level hard constraints synchronized across all schools before school-level scheduling proceeds. |
| School-specific credit-to-contact-hour ratios | Credit-to-contact-hour conversion differs by school and must be configurable per school and enforced in workload and compliance computation. |

## 8. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | Auto-generate a full department timetable within 2 minutes; conflict checks return in under 2 seconds. |
| Scalability | Support multi-campus scale: 10,000+ students, 500+ faculty, 200+ rooms, concurrent use by 50+ coordinators. |
| Availability | 99.5% uptime during academic term; scheduled maintenance windows outside business hours. |
| Security & Access Control | Role-based access control aligned to system roles; data segregation by campus/department where applicable. |
| Auditability | Full change log: who changed what, when, and why, for every timetable version. |
| Usability | Drag-and-drop timetable editor; mobile-responsive student/faculty views; minimal training required for coordinators. |
| Data Retention | Retain historical timetables and compliance reports for a minimum of [X] years per institutional/accreditation policy. |
| Localization | Support institution-specific terminology and optionally regional language labels for student-facing views. |

## 9. Assumptions

- The system will be implemented as a standalone application with its own master data; no integration with an existing ERP/SIS/HRMS is in scope for this phase.
- Although standalone, the system will still expose calendar export/subscription feeds (iCal) to faculty and students, and a scheduled/on-demand data-exchange file or API for add/drop and enrolment reconciliation with any external LMS/registration system.
- Each department will nominate a Timetable Coordinator responsible for data entry and first-level review.
- Master data will be available in a clean, digitised form prior to implementation, or will be cleaned up as part of a data-migration phase.
- Accreditation norms will be provided by the Compliance Officer as configurable parameters rather than being assumed by the vendor/implementation team.
- Internet/network availability at all campuses is sufficient for a web-based application.

## 10. Constraints

- Academic calendar and semester start dates are fixed by the university and cannot be adjusted to ease scheduling.
- Certain heritage/legacy room and lab constraints reduce scheduling flexibility.
- Faculty shared across campuses introduces hard travel-time constraints that reduce available slot combinations.
- Semi-automated approach requires coordinator time for review and adjustment; full zero-touch automation is out of scope for this phase.

## 11. Dependencies

- Timely availability of finalised course structures for each program before each semester's scheduling cycle begins.
- Finalised faculty allocation to departments/courses from Academic Affairs.
- Room/lab inventory data kept current by campus facilities teams.
- Accreditation norm updates communicated promptly by the Compliance Officer whenever statutory rules change.

## 12. Success Criteria / KPIs

| KPI | Target |
|---|---|
| Scheduling cycle turnaround | Reduce from the discovery-validated baseline to ≤ 5 working days per semester. |
| Post-publication conflicts | < 1% of sessions require rescheduling after publication. |
| Manual calendar entry effort | Reduce faculty manual calendar entry to zero via automated export/subscription. |
| Add/drop-to-roster lag | Near-zero lag between an add/drop action and the roster/timetable reflecting it. |
| Faculty workload compliance | 100% of faculty assignments within accreditation-mandated bounds at publication. |
| Room utilisation | Improve average room utilisation by [X]% within two semesters of go-live. |
| User adoption | 100% of department coordinators using the system by semester 2. |
| Student-reported clashes | Zero elective/core clashes reported post-registration. |

## 13. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Poor-quality/incomplete master data at go-live | Dedicated data-cleansing and validation phase before first scheduling cycle. |
| Resistance from coordinators used to manual/spreadsheet methods | Change management, training, and running parallel manual + system cycles for one transition semester. |
| Accreditation norms vary/change across statutory bodies | Keep norms fully configurable, owned by the Compliance Officer, not hard-coded. |
| Complex multi-campus faculty sharing makes auto-generation slow or low-quality | Allow department-wise incremental generation and manual locking of pre-fixed sessions. |
| Standalone system creates duplicate data entry vs. other university systems | Design data model to be integration-ready for a future ERP/SIS connection. |

## 14. Open Items for Discussion

- Confirm exact NBA/NAAC/UGC workload norms applicable to this institution.
- Confirm number of campuses, departments, and approximate scale for sizing.
- Confirm whether exam seating-plan and invigilation-roster generation should be in Phase 1 or a later phase.
- Confirm data-retention duration for historical timetables and compliance reports.
- Confirm future integration roadmap so the standalone data model can be designed to be integration-ready.

## 15. Appendix A — Glossary

| Term | Definition |
|---|---|
| BRD | Business Requirements Document |
| CBCS | Choice-Based Credit System |
| L-T-P | Lecture-Tutorial-Practical (weekly hour split defining a course's contact hours) |
| HOD | Head of Department |
| NBA / NAAC | National Board of Accreditation / National Assessment and Accreditation Council (India) |
| UGC | University Grants Commission (India) |
| UAT | User Acceptance Testing |
| Soft/Hard constraint | Hard = must never be violated; Soft = preferred but can be relaxed |

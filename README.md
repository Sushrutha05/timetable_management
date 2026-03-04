# Academic Timetable Scheduling System

> An automated constraint-satisfaction engine for generating institution-wide academic timetables — managing faculty loads, room capacities, section conflicts, and semester parity in a single, transactional pass.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [High-Level Architecture](#2-high-level-architecture)
3. [Domain Model](#3-domain-model)
4. [Scheduling Engine Design](#4-scheduling-engine-design)
5. [API Design](#5-api-design)
6. [Database Schema](#6-database-schema)
7. [Security Model](#7-security-model)
8. [Tech Stack](#8-tech-stack)
9. [Local Development Setup](#9-local-development-setup)
10. [Project Structure](#10-project-structure)

---

## 1. System Overview

This system automates the assignment of `CourseOffering`s (a course assigned to a section and faculty) to `TimeSlot`s and `Room`s, while respecting a set of hard constraints:

| Constraint | Description |
|---|---|
| **No Room Double-Booking** | A room cannot host two classes at the same time |
| **No Faculty Double-Booking** | A faculty member cannot teach two classes simultaneously |
| **No Section Conflict** | A student section cannot have overlapping classes |
| **Faculty Load Limits** | Total scheduled hours must not exceed `DesignationConstraint.maxLectureHours + maxLabHours` |
| **Consecutive Class Gap** | A 55-minute gap is enforced between back-to-back classes for the same faculty |
| **Semester Break Isolation** | Break slots (e.g., 13:00–14:00 for Semesters 1–2) are never used for teaching |
| **Room-Type Matching** | Labs are assigned only to `LAB` rooms; Lectures/Tutorials to `CLASSROOM`/`THEORY` rooms |
| **Room Capacity** | Room capacity must meet or exceed the section's student count |

Generation is **section-first**: each section fully secures its lab slots before the next section begins, preventing partial slot starvation across sections.

---

## 2. High-Level Architecture

```
┌────────────────────────────────────────────┐
│              React SPA (Port 3000)          │
│  Admin Dashboard  │  Faculty View  │ Login  │
└──────────────────────┬─────────────────────┘
                       │ REST / JSON
                       ▼
┌────────────────────────────────────────────┐
│          Spring Boot API (Port 8080)        │
│                                            │
│  ┌──────────────┐   ┌────────────────────┐ │
│  │  Controller  │──▶│  Service Layer     │ │
│  │  Layer (14)  │   │  ┌─────────────┐   │ │
│  └──────────────┘   │  │  Generation │   │ │
│                     │  │  Engine     │   │ │
│  ┌──────────────┐   │  └─────────────┘   │ │
│  │  Spring      │   │  ┌─────────────┐   │ │
│  │  Security    │   │  │  Report     │   │ │
│  │  (JWT/Basic) │   │  │  Service    │   │ │
│  └──────────────┘   │  └─────────────┘   │ │
│                     └────────┬───────────┘ │
│                              │             │
│  ┌───────────────────────────▼───────────┐ │
│  │      Repository Layer (Spring Data)   │ │
│  └───────────────────────────┬───────────┘ │
└──────────────────────────────┼─────────────┘
                               │ JDBC / HikariCP
                               ▼
┌────────────────────────────────────────────┐
│          PostgreSQL (schema-versioned       │
│          via Flyway — 10 migrations)        │
└────────────────────────────────────────────┘
```

The backend is a **strict three-layer monolith** (Controller → Service → Repository). There is no shared state between layers and no service-to-controller call-backs, making the dependency graph acyclic and testable.

---

## 3. Domain Model

The core domain is built around 9 aggregate entities. The diagram below shows cardinality:

```
Department ─┬──< Section ──< CourseOffering >──┬── Course
            │                     │             │
            └──< Faculty >────────┘             └─< DepartmentCourse
                    │
                    ├──< FacultyPreference >── Course
                    └── DesignationConstraint (by designation)

CourseOffering ──< ScheduledClass >── TimeSlot
                                  └── Room
```

### Key Entities

| Entity | Responsibility |
|---|---|
| `Course` | Defines `lectureHours`, `tutorialHours`, `practicalHours`, and `courseType` |
| `CourseOffering` | Binds a `Course` to a `Faculty` and a `Section` — the unit the engine schedules |
| `Section` | A student cohort, scoped by department, semester, and year |
| `Faculty` | Linked to a `User` account; holds a `designation` that maps to load limits |
| `DesignationConstraint` | Per-designation caps on `maxLectureHours`, `maxLabHours`, `maxTotalHours` |
| `TimeSlot` | A named slot (`dayOfWeek`, `startTime`, `endTime`, `isBreak`, `semesterGroup`) |
| `Room` | Physical space with `type` (`LAB`, `CLASSROOM`) and `capacity` |
| `ScheduledClass` | The output record — binds an offering to a room and time slot |
| `TimetableMetadata` | Key-value store for publish state (`DRAFT` / `PUBLISHED`) |

---

## 4. Scheduling Engine Design

`TimetableGenerationService` is the core of the system. It implements a **greedy constraint-propagation algorithm** with deterministic ordering.

### Algorithm Overview

```
generateTimetable(parity)
  │
  ├─ 1. Delete existing classes for the target semester parity
  ├─ 2. Load all CourseOfferings, Rooms, and TimeSlots into memory
  ├─ 3. Partition TimeSlots into:
  │     • allByGroup   — all slots (break + teaching), keyed by semesterGroup × day
  │     • teachingByGroup — teaching-only slots, keyed by semesterGroup × day
  │
  ├─ 4. Sort sections:  semesterGroup < semester < sectionId
  │
  └─ 5. For each section (section-first strategy):
        ├─ Sort offerings: practicalHours DESC, creditHours DESC
        │
        ├─ For each offering:
        │   ├─ schedulePractical()  → greedy block assignment over allByGroup
        │   ├─ scheduleComponent("LECTURE") → greedy over teachingByGroup
        │   └─ scheduleComponent("TUTORIAL") → greedy over teachingByGroup
        │
        └─ Each scheduling call fills `occupied` and `facultyHours` incrementally
```

### Constraint Checks (per candidate slot)

Every candidate `TimeSlot` block is validated through a pipeline of guards before a room assignment is attempted:

1. **`isContiguous(block)`** — blocks must form a gapless sequence (no break slot inside a practical block)
2. **`isBreakForSemester(block, semester)`** — rejects slots overlapping institutional break windows
3. **`isSaturdayAfterLunch(block)`** — Saturday post-13:00 slots are excluded
4. **`hasConsecutiveFacultyClass(...)`** — ensures the 55-minute gap by checking the immediately adjacent slots in `daySlots`
5. **`resourcesFree(...)`** — atomic check: room not occupied + faculty not occupied + section not occupied + faculty within load limits

### Semester Parity

The engine partitions semesters into two groups to support **half-semester regeneration** without disrupting already-published schedules:

| Parity | Semesters |
|---|---|
| `ODD` | 1, 3, 5, 7 |
| `EVEN` | 2, 4, 6, 8 |

Each parity maps to a `semesterGroup` string (e.g., `SEM_1_2`, `SEM_3_4`) which indexes the pre-partitioned `TimeSlot` lookup maps.

### Fallback Faculty Assignment

If the primary faculty cannot be scheduled for a practical due to load limits or slot exhaustion, the engine iterates `FacultyPreference` entries (ordered by priority) and attempts substitution with an alternate faculty member before failing.

### Output

A successful run produces a list of `ScheduledClass` records, each atomically persisted within the enclosing `@Transactional` boundary. On failure, the entire transaction rolls back.

---

## 5. API Design

All admin endpoints are protected. Faculty-facing endpoints are read-only.

### Timetable Engine

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/admin/timetable/generate?parity=ODD\|EVEN` | Run the scheduling engine |
| `GET` | `/api/admin/timetable` | Retrieve the full current timetable |
| `GET` | `/api/admin/timetable/section/{sectionId}` | Section-specific view |
| `GET` | `/api/admin/timetable/faculty/{facultyId}` | Faculty-specific view |
| `POST` | `/api/admin/timetable/update-slot` | Move a class (drag-and-drop) with re-validation |
| `POST` | `/api/admin/timetable/publish` | Promote status from `DRAFT` → `PUBLISHED` |
| `GET` | `/api/admin/timetable/status` | Get current publish state |
| `GET` | `/api/admin/timetable/download/{sectionId}?type=xlsx` | Export timetable as Excel |

### Resource Management

| Domain | Base Path | Operations |
|---|---|---|
| Courses | `/api/admin/courses` | CRUD, department assignment |
| Faculty | `/api/admin/faculty` | CRUD, designation, department scoping |
| Rooms | `/api/admin/rooms` | CRUD |
| Sections | `/api/admin/sections` | CRUD |
| Time Slots | `/api/admin/time-slots` | CRUD, semester group tagging |
| Departments | `/api/admin/departments` | CRUD |
| Designations | `/api/admin/designations` | CRUD, hour limit configuration |
| Course Offerings | `/api/admin/offerings` | Assign faculty to course + section |
| Faculty Preferences | `/api/admin/preferences` | Priority-ranked course preferences |

### Auth

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Returns session / token |
| `GET` | `/api/faculty/timetable` | Faculty's own schedule (authenticated) |

---

## 6. Database Schema

Schema is version-controlled through **Flyway** (10 migrations, `V1` through `V10`).

```
designation_constraints   courses              rooms
──────────────────────    ──────────────────   ─────────────────
designation (PK)          course_id (PK)       room_id (PK)
max_lecture_hours         course_code          room_number
max_lab_hours             course_name          type
max_total_hours           credit_hours         capacity
priority_level            lecture_hours
                          tutorial_hours        time_slots
departments               practical_hours       ─────────────────
──────────────────────    course_type           time_slot_id (PK)
department_id (PK)                              day_of_week
name                      sections              start_time
                          ──────────────────    end_time
users                     section_id (PK)       is_break
──────────────────────    department_id (FK)    semester_group
user_id (PK)              name
email                     semester
password_hash             year
role                      student_count

faculty
──────────────────────
faculty_id (PK)
user_id (FK)
department_id (FK)
designation (FK → designation_constraints)
first_name / last_name

course_offerings          scheduled_classes     faculty_preferences
──────────────────────    ──────────────────    ─────────────────
offering_id (PK)          class_id (PK)         preference_id (PK)
course_id (FK)            offering_id (FK)       faculty_id (FK)
faculty_id (FK)           room_id (FK)           course_id (FK)
section_id (FK)           day_of_week            priority
                          start_time / end_time
```

---

## 7. Security Model

- **Spring Security** with form-based or token authentication.
- Role separation: `ADMIN` can generate, configure, and publish; `FACULTY` role has read-only access to their own timetable.
- Passwords are stored as hashed values (`password_hash`).
- A `requires_password_reset` flag forces credential rotation on first login.

---

## 8. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.5.7 |
| ORM | Hibernate / Spring Data JPA | 6.6 |
| Database | PostgreSQL | 12+ |
| Migrations | Flyway | 10.15 |
| Security | Spring Security | 6.5 |
| Excel Export | Apache POI | 5.3 |
| PDF Export | OpenPDF | 2.0 |
| CSV | Apache Commons CSV | 1.12 |
| Connection Pool | HikariCP | 6.3 |
| Build | Maven | 3.x |
| Frontend | React | 18 |
| Styling | Tailwind CSS | 3.x |
| Routing | React Router | 6.x |
| Test DB | H2 (in-memory) | 2.3 |

---

## 9. Local Development Setup

### Prerequisites

- Java 17+
- Maven 3.x
- PostgreSQL 12+
- Node.js 18+ and npm

### Backend

```bash
# 1. Create the database
psql -U postgres -c "CREATE DATABASE schedule_planner;"

# 2. Configure credentials
cp backend/src/main/resources/application.properties.example \
   backend/src/main/resources/application.properties
# Edit the datasource URL, username, and password

# 3. Start the API (Flyway runs migrations automatically)
cd backend
./mvnw spring-boot:run
# API available at http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm start
# UI available at http://localhost:3000
```

### Running Tests

```bash
cd backend
mvn test          # Integration + unit tests against H2 in-memory DB
mvn clean package # Full build + tests
```

---

## 10. Project Structure

```
mini_project/
├── backend/
│   ├── src/main/java/com/timetable/timetable_api/
│   │   ├── config/           # Security, CORS
│   │   ├── controller/       # 14 REST controllers
│   │   ├── dto/              # Request/response objects (no entity leakage)
│   │   ├── model/            # 14 JPA entities
│   │   ├── repository/       # Spring Data repositories
│   │   └── service/          # Business logic & scheduling engine
│   ├── src/main/resources/
│   │   └── db/migration/     # Flyway SQL migrations (V1–V10)
│   └── src/test/             # Integration tests (H2)
│
└── frontend/
    └── src/
        ├── components/       # Reusable UI components
        └── pages/            # Route-level page components
```

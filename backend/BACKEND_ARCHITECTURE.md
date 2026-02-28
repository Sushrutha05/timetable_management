# Timetable Scheduler - Backend Architecture

## Overview
The backend is a **Spring Boot 3.5.7** application serving as a Monolithic REST API for the Timetable Scheduling System. It manages all core business logic, including timetable generation algorithms, data management (CRUD), and reporting (PDF/Excel exports).

## 1. High-Level Architecture
The system follows a classic **Layered Architecture**:
1.  **Controller Layer** (`com.timetable.timetable_api.controller`): Handles HTTP requests.
2.  **Service Layer** (`com.timetable.timetable_api.service`): Implements business logic and algorithms.
3.  **Repository Layer** (`com.timetable.timetable_api.repository`): Data access via Spring Data JPA.
4.  **Database Layer**: PostgreSQL (Supabase).

## 2. Technology Stack
| Component | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.5.7 |
| **Build Tool** | Maven | 3.x |
| **Database** | PostgreSQL (Supabase) | 15+ |
| **Migration** | Flyway | 10.x |
| **Reporting** | OpenPDF, Apache POI | 2.x, 5.x |

## 3. Database Schema & Data Model
The database is hosted on **Supabase** and schema changes are managed by **Flyway**.

### Key Tables
*   **`courses`**: Stores course details including L:T:P breakdown (`lecture_hours`, `tutorial_hours`, `practical_hours`).
*   **`faculty`**: Stores faculty details and links to `users` for authentication.
*   **`designation_constraints`**: Defines workload limits (`max_total_hours`) for each designation (Professor, Assistant Prof, etc.).
*   **`time_slots`**: Master table defining all valid time slots (e.g., 09:00-10:00, 10:00-11:00).
*   **`scheduled_classes`**: The core output table linking `course_offerings`, `rooms`, `time_slots`.

### Migrations
*   `V1__init_schema.sql`: Sets up the entire initial schema.
*   `V2__add_constraints.sql`: Adds foreign keys, unique constraints, and indexes.

## 4. Key Modules

### A. Timetable Generation Engine
*   **Orchestrator**: `TimetableGenerationService`
*   **Algorithm**: Heuristic-based slot assignment. Uses constraints (Hard/Soft) to assign `CourseOfferings` to `TimeSlots` and `Rooms`.
*   **Constraints Handled**:
    *   No double booking of Faculty.
    *   No double booking of Rooms.
    *   Faculty max workload limits.
    *   Entity specific constraints (e.g. Lab sessions must be consecutive).

### B. Reporting Module
*   **PDF Export**: `PdfReportService` generates class-wise and faculty-wise timetables using OpenPDF.
*   **Excel Export**: `TimetableReportService` generates comprehensive spreadsheets using Apache POI.

## 5. Setup & Configuration

### Prerequisites
*   Java 17+
*   Maven
*   Supabase Account (or local PostgreSQL)

### Configuration (`application.properties`)
The application is configured to connect to Supabase. You must provide the following environment variables or update the file directly:
```properties
spring.datasource.url=jdbc:postgresql://[YOUR_HOST]:5432/postgres
spring.datasource.username=[YOUR_USER]
spring.datasource.password=[YOUR_PASSWORD]
spring.jpa.hibernate.ddl-auto=validate
```

### Running the Application
```bash
# Clean and Run
./mvnw clean spring-boot:run

# Run Tests
./mvnw test
```

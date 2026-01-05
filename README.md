# Timetable Management System

A comprehensive solution for automated academic timetable scheduling, facilitating the management of courses, faculty, rooms, and student sections.

## 🏗 System Architecture

The project follows a modern **Monolithic Client-Server Architecture**:

*   **Frontend**: A responsive Single Page Application (SPA) built with **React** and **Tailwind CSS**.
*   **Backend**: A robust RESTful API built with **Spring Boot** and **PostgreSQL**.

### 🔧 Tech Stack

**Backend**
*   **Java 17**: Core language.
*   **Spring Boot 3.5.7**: Framework for rapid application development.
*   **PostgreSQL**: Primary relational database.
*   **Spring Data JPA (Hibernate)**: ORM for database interactions.
*   **Flyway**: Database migration and version control.
*   **Spring Security**: Authentication and authorization.
*   **Apache POI**: Excel report generation.

**Frontend**
*   **React 18**: UI Library.
*   **Tailwind CSS**: Utility-first CSS framework for styling.
*   **React Router**: Client-side routing.
*   **Lucide React**: Icon set.

---

## ⚙️ Backend Engineering Brief

As a backend engineer, here is an overview of the core system design and components:

### Layered Architecture
The backend `timetable-api` strictly follows a layered architecture to ensure separation of concerns:

1.  **Controller Layer** (`com.timetable.timetable_api.controller`):
    *   Exposes REST endpoints.
    *   Handles HTTP validation and DTO mapping.
    *   *Key Controllers*: `TimetableGenerationController`, `FacultyManagementController`, `CourseManagementController`.

2.  **Service Layer** (`com.timetable.timetable_api.service`):
    *   Encapsulates all business logic.
    *   **Generation Engine**: `TimetableGenerationService` orchestrates the complex logic to assign classes to slots without conflicts.
    *   **Validation**: Ensures constraints (e.g., "Professor X cannot teach on Mondays") are respected.

3.  **Repository Layer** (`com.timetable.timetable_api.repository`):
    *   Interfaces with PostgreSQL via Spring Data JPA.
    *   Abstracts SQL queries using method name parsing and JPQL.

### Key Domain Modules
*   **Timetable Generation**: The heart of the system. Takes inputs (Courses, Faculty Preferences, Room Availability) and produces a valid `ScheduledClass` set.
*   **Resource Management**: CRUD capabilities for `Faculty`, `Rooms`, `Courses`, and `Sections`.
*   **Reporting**: Generates exportable formats (Excel/CSV) for administrative use (via `TimetableReportService`).

---

## 🚀 Getting Started

### Prerequisites
*   **Java 17+** installed.
*   **Node.js 16+** and **npm** installed.
*   **PostgreSQL** installed and running.

### 1. Database Setup
Create a PostgreSQL database named `schedule_planner`.
```sql
CREATE DATABASE schedule_planner;
```
*Note: The application uses Flyway, so tables will be automatically created on the first run.*

### 2. Backend Setup
Navigate to the backend directory:
```bash
cd backend
```

Update database credentials in `src/main/resources/application.properties` if they differ from defaults:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/schedule_planner
spring.datasource.username=your_username
spring.datasource.password=your_password
```

Run the application:
```bash
./mvnw spring-boot:run
```
The API will start on `http://localhost:8080`.

### 3. Frontend Setup
Navigate to the frontend directory:
```bash
cd frontend
```

Install dependencies:
```bash
npm install
```

Start the development server:
```bash
npm start
```
The application will open at `http://localhost:3000`.

---

## 📂 Project Structure

```text
mini_project/
├── backend/                # Spring Boot API
│   ├── src/main/java       # Source code
│   └── src/main/resources  # Config & Migrations
└── frontend/               # React Application
    ├── src/                # Components & Pages
    └── public/             # Static assets
```

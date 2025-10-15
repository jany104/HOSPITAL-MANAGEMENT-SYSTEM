# CareSphere Hospital Management System – Deep-Dive Guide

_Last updated: October 1, 2025_

---

## 1. Executive Summary
- **Purpose:** Streamline day-to-day hospital reception, admissions, billing, and staff coordination through a desktop application.
- **Platform:** Java Swing desktop client backed by a MySQL relational database (JDBC via MySQL Connector/J 8.0.28).
- **Core capabilities:**
  - Authentication for reception operators.
  - Real-time operational dashboard (patient census, room availability, ambulance readiness).
  - CRUD workflows for patients, rooms, staff, departments, and ambulances.
  - Financial touchpoints (deposits, room pricing, pending amounts) tightly coupled with admissions lifecycle.

This document equips every team member with architecture knowledge, module responsibilities, data design, and likely viva questions so anyone can represent the project confidently.

---

## 2. Technology Stack & Key Dependencies
| Layer | Technology | Notes |
| --- | --- | --- |
| UI Layer | **Java Swing** | Custom-styled components (`UIComponents`, `UITheme`, `IllustrationPanel`) give a modern look-and-feel. |
| Application Core | Java SE (8+) | Object-oriented design, event-driven controllers per module.
| Persistence | JDBC (`conn` helper) | Central connection manager loads credentials from `db.properties` and uses MySQL Connector/J. |
| Database | MySQL 8.x | Schema under `sql/schema.sql`, sample records in `sql/seed-data.sql`. |
| Build & Run | PowerShell script `scripts/build-and-run.ps1` | Compiles `.java` sources, copies illustration assets, launches `hospital.management.system.Login`. |
| Assets | SVG/PNG illustrations | Integrated via `IllustrationPanel` to enrich the UI. |

Supporting jars (bundled in project root):
- `mysql-connector-java-8.0.28.jar` – JDBC driver.
- `ResultSet2xml*.jar` – Legacy utilities (not used directly in current code but kept for reference).

---

## 3. High-Level Architecture

```text
+----------------------+       +----------------------+       +-------------------------+
|  Presentation Layer  | <---> |   Application Logic  | <---> |   MySQL Database (JDBC) |
|  (Java Swing Frames) |       |  (per-module classes) |       |  login, Patient_Info…   |
+----------------------+       +----------------------+       +-------------------------+
         ^                            ^                                   ^
         |                            |                                   |
         | UIComponents/UITheme       | conn (connection helper)          | schema.sql / seed-data.sql
```

### Core Concepts
1. **Page Container Pattern:** Each functional area is a `JFrame` that wraps its content in `UIComponents.pageContainer`, yielding consistent headers, cards, and illustrations.
2. **Centralized Styling:** Fonts, colors, gradients, and widgets are defined once in `UITheme` and `UIComponents`, ensuring branding consistency.
3. **Database Access Flow:**
   - Each module obtains a connection via the `conn` class (implements `AutoCloseable`).
   - SQL queries are executed using `PreparedStatement` (parameterized to reduce SQL injection risk).
   - Result sets are adapted to Swing tables through `ResultSetTableModelBuilder` (read-only models).
4. **Domain Separation:** Separate frames/classes encapsulate patient management, staff directory, rooms, etc., which keeps logic manageable and testable.

---

## 4. Database Design Cheat Sheet

| Table | Key Columns | Purpose |
| --- | --- | --- |
| `login` | `ID` (PK), `PW` | Stores operator credentials checked during login. |
| `department` | `Department` (PK), `Phone_Number` | Directory of hospital departments with contact numbers. |
| `room` | `room_no` (PK), `Availability`, `Price`, `Bed_Type` | Master list of rooms, used for admissions and occupancy tracking. |
| `Patient_Info` | `number` (PK), `Room_Number` (FK → `room.room_no`), `Deposite` | Tracks admitted patients, their rooms, and deposits. |
| `EMP_INFO` | `Aadhar_Number` (PK), `Salary`, `Gmail` | HR roster for hospital staff. |
| `Ambulance` | Composite key (`Name`, `Car_Name`) | Monitors ambulance assignments and availability. |

> **Referential Integrity:** `Patient_Info.Room_Number` enforces a foreign key to `room.room_no`, ensuring that admissions only reference valid rooms.

**Seed Data Tips**
- Use `sql/schema.sql` to create the schema and service account (`team_member`).
- Populate baseline data via `sql/seed-data.sql` before demo day to guarantee dashboards show non-empty tables.

---

## 5. Module-by-Module Walkthrough & Talking Points

### 5.1 Authentication (`Login.java`)
- Modern left/right split layout with gradient hero panel and form card.
- Validates credentials against `login` table using parameterized SQL.
- On success, launches the `Reception` workspace and disposes of the login window.
- **Teacher may ask:** _"How do you prevent password leaks?"_
  - Mention `JPasswordField`, zeroing out char arrays in `finally`, and the plan to hash passwords server-side in future iterations.

### 5.2 Reception Dashboard (`Reception.java`)
- Serves as the operational home screen after login.
- **Live KPIs:**
  - Active patients (`Patient_Info` count).
  - Available rooms (`room.Availability = 'Available'`).
  - Ambulances ready (`Ambulance.Available = 'Yes'`).
- Uses Swing `Timer` for:
  - Updating KPIs every 60 seconds.
  - Refreshing the hero clock every second.
- Navigation grid of animated cards, each opening a functional module.
- **Talking point:** Dashboard gracefully handles DB outages (shows muted error label with guidance).

### 5.3 Patient Admission (`NEW_PATIENT.java`)
- Guided form collected in `AdmissionForm` record.
- Validations: required fields, numeric deposit parsing (`BigDecimal`), room availability.
- Transactional save: insert patient + mark room occupied within one connection.
- **Edge cases handled:** empty room list, invalid deposit, commit/rollback pattern (ready for expansion).
- **Potential question:** _"How do you guarantee that rooms aren’t double-booked?"_
  - Explain database transaction and `Availability` flip to 'Occupied'.

### 5.4 Patient Directory (`ALL_Patient_Info.java`)
- Displays live table using `ResultSetTableModelBuilder` for zero-boilerplate table population.
- Includes client-side filtering via `TableRowSorter` + regex filter over multiple columns.
- Refresh button re-queries the database.

### 5.5 Room Management (`Room.java` & `SearchRoom.java`)
- `Room.java`: read-only list of all rooms; quick refresh and close actions.
- `SearchRoom.java`: adds availability filter with `JComboBox`, reuses `ResultSetTableModelBuilder`. Demonstrates parameterized query for filters.

### 5.6 Patient Discharge (`patient_discharge.java`)
- Auto-populates patient drop-down from admissions.
- Shows read-only stay summary (room, in/out timestamps) before discharge.
- On confirmation, transactionally removes patient record and marks room `Available`.
- **Teacher angle:** highlight safe guard prompts and ACID properties.

### 5.7 Update Patient Details (`update_patient_details.java`)
- Allows modifying room, check-in time, and deposit for selected patient.
- Recalculates pending amount from `room.Price` − `Deposite`.
- Uses a disabled `pendingField` to prevent manual tampering.

### 5.8 Employee Directory (`Employee_info.java`)
- Table with full staff roster, plus Add/Edit/Delete flows via `StaffFormDialog`.
- Input validation in dialog (positive salary/age, trimmed text).
- CRUD operations via prepared statements keyed on `Aadhar_Number`.
- Double-click table row to edit quickly.

### 5.9 Departments (`Department.java`)
- Read-only view of departments with contact numbers.
- Good module for junior team member to rehearse (lightweight but demonstrates table loading utility).

### 5.10 Ambulance Fleet (`Ambulance.java`)
- Table view showing driver, vehicle, status, and location.
- In a future sprint, we can add dispatch toggles or GPS integration.

### 5.11 Shared UI Infrastructure
- **`UITheme`** centralizes colors and fonts (ensures consistent typography like Segoe UI).
- **`UIComponents`** offers reusable widgets (buttons, form rows, cards, stacked layouts, info badges) to reduce duplication.
- **`IllustrationPanel`** dynamically renders SVG/PNG illustrations per module; fallback loading ensures the app starts even if assets are missing.
- **`GradientPanel`** paints hero backgrounds with antialiased gradients.
- **`ResultSetTableModelBuilder`** converts any `ResultSet` to a non-editable `DefaultTableModel` for quick table population.

### 5.12 Database Connector (`conn.java`)
- Loads `db.properties` if present; otherwise defaults to local credentials.
- Implements `AutoCloseable`, so try-with-resources automatically frees connections and statements.
- Throws human-readable errors if JDBC driver missing or credentials invalid.

---

## 6. End-to-End Workflow (Demo Script)
1. **Login** with seeded credentials (e.g., `admin` / `admin123`).
2. **Reception Overview** opens; highlight live stats and navigation cards.
3. **Register New Patient:** fill form, show room list filtered to available rooms.
4. Return to **Reception** and hit **Refresh** to see KPI updates.
5. Check **Patient Directory** to confirm record; search by name.
6. Visit **Room Availability** to show occupancy change.
7. **Update Patient Details** (modify deposit) and show recalculated pending amount.
8. **Discharge Patient** to free room and remove record.
9. Open **Employee Directory**; add staff, edit existing entry, explain data validation.
10. Showcase **Ambulance** and **Department** lists for operational completeness.

---

## 7. Error Handling & Validation Highlights
- **UI-level checks:** Format validation for numeric fields (deposit, salary), required field prompts, disabled controls when no data available.
- **Database safeguards:** try-with-resources, transactions for multi-table updates, prepared statements to mitigate SQL injection.
- **Resilience:** Dashboard surfaces data fetch errors instead of crashing; `conn` wraps low-level SQL exceptions with actionable messages.
- **Improvements to mention if asked:** Password hashing, audit logging, moving business logic into stored procedures or service layer.

---

## 8. Security Considerations
- Credentials currently stored in plaintext in `login` table (for demo). Emphasize recommendation to hash passwords and enforce password policies.
- `db.properties` supports overriding default credentials; store it outside version control in production.
- JDBC connection uses MySQL account with full rights for simplicity; in production, apply least-privilege principle per module.

---

## 9. Build, Run, and Environment Setup

### Prerequisites
- Java Development Kit (JDK) 8 or newer.
- MySQL Server 8.x with user permissions outlined in `schema.sql`.
- PowerShell (already available on Windows for running helper script).

### Database Setup
```powershell
# From MySQL shell
SOURCE sql/schema.sql;
SOURCE sql/seed-data.sql;
```

### Compile & Launch
```powershell
# From project root
scripts\build-and-run.ps1
```
- The script compiles sources, copies illustration assets, ensures the MySQL connector is on the classpath, and starts the login screen.
- To use a different connector path, pass `-ConnectorPath "C:\path\to\mysql-connector.jar"`.

### Optional PDF Export of This Doc
1. Install [Pandoc](https://pandoc.org/) and a PDF engine (e.g., `wkhtmltopdf` or TeX distribution).
2. From project root:
   ```powershell
   pandoc docs\project-overview.md -o docs\project-overview.pdf
   ```

---

## 10. Testing & Quality Gates
- **Manual QA:**
  - Admissions: verify room switches, deposit validation, success toast.
  - Discharge: ensure patient removed and room availability flips back.
  - Staff CRUD: confirm insert/update/delete propagate immediately.
  - Dashboard timers: simulate DB disconnect to observe graceful error state.
- **Suggested automated tests (future work):** JUnit tests for `ResultSetTableModelBuilder`, integration tests using Testcontainers for MySQL.

---

## 11. Viva Preparation – Likely Questions & Suggested Answers
| Topic | Sample Question | Key Points to Cover |
| --- | --- | --- |
| Architecture | "Why choose Java Swing?" | Quick desktop deployment, full control over UI, fits lab environment without web server overhead. |
| Data Integrity | "How do you avoid inconsistent room states?" | Transactions when admitting/discharging, foreign key constraints, status flags in `room` table. |
| Security | "How would you harden authentication?" | Hash passwords, salt, implement role-based access, lock after failed attempts, environment-based configs. |
| Scalability | "Can multiple receptionists use it simultaneously?" | Yes—MySQL handles concurrent connections; UI caches minimal state, always re-queries database. |
| Extensibility | "What new modules are easiest to add?" | Modules follow consistent pattern. Example: add Pharmacy inventory by replicating table view + CRUD dialog. |
| Error Scenarios | "What if the DB is offline?" | Dashboard shows warning; most flows catch SQL exceptions and display user-friendly alerts. |

Encourage each teammate to take ownership of a module section from §5 and rehearse the talking points.

---

## 12. Roadmap & Enhancements
- **Authentication:** Integrate password hashing + role-based permissions.
- **Reporting:** Export admissions/staff data to CSV/PDF, integrate charting libraries.
- **Notifications:** Email/SMS triggers on ambulance readiness or room assignments.
- **Testing:** Add automated regression suite; adopt Maven/Gradle for structured builds.
- **Deployment:** Package as executable JAR with bundled dependencies and installer script.

---

## 13. Quick Reference
- **Default admin:** `admin` / `admin123`
- **Backup credential:** `team_member` / `strongpass123`
- **DB props override:** place `db.properties` alongside JAR with keys `db.url`, `db.user`, `db.password`.
- **Code entry point:** `hospital.management.system.Login`.
- **Main resources:**
  - Source: `src/hospital/management/system/*.java`
  - Illustrations: `src/hospital/management/system/illustrations/`
  - Database scripts: `sql/`
  - Helper script: `scripts/build-and-run.ps1`

Use this guide as your single source of truth when explaining the project to faculty, peers, or evaluators.

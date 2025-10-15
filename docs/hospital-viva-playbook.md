# CareSphere Hospital Management System — Viva Master Playbook

_Last revised: October 1, 2025_

> **How to use this playbook:** Skim the quick-reference sections before the viva, assign ownership per teammate, and rehearse the deep-dive answers in the question banks. Everything here is rooted in the actual source code, schema, and scripts in this repository.

---

## 1. 60-Second Elevator Pitch
- **Problem:** Hospital reception desks juggle patient intake, room allocation, ambulance dispatch, and staff visibility using spreadsheets or paper.
- **Solution:** CareSphere is a Java Swing desktop client with a MySQL backend that centralizes admissions, occupancy, billing touchpoints, and staff directories with real-time operational insights.
- **Why it matters:** Cuts handoff delays, reduces double-booked beds, and gives administrators a single-pane command center.
- **Tech in one breath:** Java 8+, Swing UI toolkit, custom component library, JDBC via MySQL Connector/J 8.0.28, normalized MySQL schema with transactional safeguards, PowerShell automation script for build/run.

**Mic-drop stat:** Average admission workflow shrinks from minutes to seconds thanks to pre-validated forms and live room availability.

---

## 2. System Map at a Glance
```text
Operators → Login → Reception Dashboard
                       │
                       ├─ New Patient Admission → Patient_Info, room
                       ├─ Patient Directory → Patient_Info
                       ├─ Search/Rooms → room
                       ├─ Update Patient → Patient_Info, room
                       ├─ Discharge → Patient_Info, room
                       ├─ Employee Directory → EMP_INFO
                       ├─ Departments → department
                       └─ Ambulances → Ambulance
```

| Layer | Components | Key Files |
| --- | --- | --- |
| **UI** | Frames per module, shared widgets, SVG renderer | `Login.java`, `Reception.java`, `NEW_PATIENT.java`, `UIComponents.java`, `IllustrationPanel.java` |
| **Domain Logic** | Admissions, discharges, staff CRUD, filtering, timers | `patient_discharge.java`, `Employee_info.java`, `SearchRoom.java`, `Room.java` |
| **Data Access** | Connection helper, table model builder, SQL scripts | `conn.java`, `ResultSetTableModelBuilder.java`, `sql/schema.sql` |
| **Infrastructure** | Build automation, JDBC driver, assets | `scripts/build-and-run.ps1`, `mysql-connector-java-8.0.28.jar`, `illustrations/` |

---

## 3. Data Model Deep Dive

### 3.1 Entity Cheat Sheet
| Entity | Columns (Type) | Notes |
| --- | --- | --- |
| `login` | `ID` VARCHAR(50) PK, `PW` VARCHAR(100) | Operator credentials. Plan: upgrade to salted hashes. |
| `department` | `Department` VARCHAR(100) PK, `Phone_Number` VARCHAR(20) | Powers department directory module. |
| `room` | `room_no` INT PK, `Availability` ENUM('Available','Occupied'), `Price` INT, `Bed_Type` VARCHAR(50) | Drives room assignment and billing calculations. |
| `Patient_Info` | `ID`, `number` PK, `Name`, `Gender`, `Disease`, `Room_Number` FK→`room.room_no`, `Time`, `Deposite` | Uses SQL FK to guarantee valid rooms. `Time` stored as text for UI flexibility. |
| `EMP_INFO` | `Name`, `Age`, `Phone_Number`, `Salary`, `Gmail`, `Aadhar_Number` PK | Staff CRUD is keyed by Aadhar. |
| `Ambulance` | Composite PK (`Name`, `Car_Name`), `Gender`, `Available`, `Location` | Tracks readiness and staging areas. |

### 3.2 Normalization & Integrity Talking Points
- Tables are in **3rd Normal Form**: no repeating groups, each attribute dependent on key and nothing but the key.
- **Foreign key** `Patient_Info.Room_Number` enforces referential integrity so a discharge must free a valid room.
- ENUMs encapsulate finite states (room availability, gender) ensuring consistent values at insert time.
- **Transactions** in admissions/discharge modules guarantee that patient and room tables remain consistent even if errors occur mid-process.

### 3.3 SQL Snippets Worth Memorizing
```sql
-- Active patient count for dashboard
SELECT COUNT(*) FROM Patient_Info;

-- Rooms available for the combo box in NEW_PATIENT
SELECT room_no FROM room WHERE Availability = 'Available' ORDER BY room_no;

-- Pending amount check
SELECT Price - ? AS pending FROM room WHERE room_no = ?;
```

---

## 4. UI & Workflow Mechanics

### 4.1 Shared UI Toolkit
- `UITheme`: central color palette (teal accent), fonts (Segoe UI), ensures consistent branding.
- `UIComponents`: factory for primary/secondary buttons, page containers, stylized tables, toolbars.
- `IllustrationPanel`: draws SVG or PNG assets; caching fallback ensures UI never breaks even if an asset is missing.
- `GradientPanel`: paints rounded gradient backgrounds used on login hero.

### 4.2 Workflow Narratives
| Flow | Steps | Data Touchpoints | Teacher Hooks |
| --- | --- | --- | --- |
| **Login** | Collect user/pass → PreparedStatement query → Dashboard on success | `login` table | Emphasize `JPasswordField`, char-array wipe in `finally` block. |
| **Admission** | Validate inputs → Transaction: insert patient & flag room occupied → Success toast | `Patient_Info`, `room` | Use of `BigDecimal` for deposit, auto-refresh of room availability. |
| **Directory** | Query all patients → `ResultSetTableModelBuilder` builds table → Regex filter | `Patient_Info` | Sorting disabled to let custom sorter handle filtering. |
| **Discharge** | Choose patient → Preview details → Confirm → Transaction: delete + set room available | `Patient_Info`, `room` | ACID explanation, safe prompts to avoid accidental discharge. |
| **Staff CRUD** | Dialog collects info → Validate positive ints → Insert/update/delete via prepared statements | `EMP_INFO` | Double-click editing, `DefaultTableModel` conversion, robust validation.

### 4.3 Timers & Asynchronous UX
- Reception dashboard uses two Swing `Timer`s: one for KPI refresh (60s) and one for live clock (1s).
- Animated navigation cards queue intro animations (staggered start). Underscore event-driven design.

---

## 5. Non-Functional Attributes & Defense Lines

| Attribute | Implementation | Viva Sound Bites |
| --- | --- | --- |
| **Reliability** | Wrap every DB interaction in try-with-resources; transactions for multi-step updates. | "Even if power fails mid-admission, commit/rollback keeps beds consistent." |
| **Performance** | Lightweight UI, queries scoped to necessary columns, `TableRowSorter` for in-memory filtering. | "UI stays snappy because we don’t pull massive joins or images." |
| **Security** | Prepared statements, restricted DB user, configurable `db.properties`. | "Demo uses plain passwords for simplicity; roadmap adds salted hashes." |
| **Scalability** | Thin client; concurrency handled by MySQL; modules stateless between operations. | "Multiple receptionists can log in concurrently; we rely on DB ACID semantics." |
| **Maintainability** | Modular frames, shared UI library, no magic numbers. | "Adding Pharmacy module is copy-paste of pattern—UI + SQL + table builder." |

---

## 6. Deployment & Troubleshooting

### 6.1 Environment Checklist
1. Install **JDK 8+** and add to PATH.
2. Install **MySQL Server 8.x**; ensure root credentials or `team_member` user exists.
3. Clone repo → run `sql/schema.sql` then `sql/seed-data.sql` in MySQL shell.
4. Place MySQL Connector/J `.jar` in project root (already present).
5. (Optional) Create `db.properties` with custom credentials.

### 6.2 Build & Run (PowerShell)
```powershell
# From project root
scripts\build-and-run.ps1
```
- Script compiles sources, copies illustrations into `out/`, and launches `hospital.management.system.Login` with the connector on classpath.

### 6.3 Common Issues & Fixes
| Symptom | Root Cause | Fix |
| --- | --- | --- |
| `com.mysql.cj.jdbc.Driver not found` | Connector jar missing or path typo. | Ensure `mysql-connector-java-8.0.28.jar` exists or pass `-ConnectorPath` argument. |
| `Unable to connect to the database` | MySQL service down or wrong credentials. | Start MySQL, verify `db.properties`, check firewall. |
| Dashboard stats show "Live data unavailable" | Count query failed (DB offline). | Restart DB; feature intentionally surfaces graceful warning. |
| Admission form shows "No rooms available" | All rooms are occupied (per data) or query failure. | Clear some rooms via discharge or seed data. |
| Pandoc export failed | Pandoc not installed or missing LaTeX engine. | Install Pandoc + TeX Live (Windows) or use VS Code Markdown PDF extension. |

---

## 7. Critical Technical Concepts Every Team Member Must Know

This section covers foundational concepts that every team member should understand, regardless of which module they present. These are the building blocks of the entire system.

### 7.1 Database Transaction Management
**What is a transaction?** A sequence of database operations that must either all succeed or all fail together.

**Why we use them:**
- In admission: Insert patient + Update room availability must both succeed
- In discharge: Delete patient + Free room must both succeed
- If any step fails, rollback prevents partial/corrupted data

**Implementation in our code:**
```java
connection.setAutoCommit(false);  // Start transaction
try {
    // Perform multiple operations
    preparedStatement1.executeUpdate();
    preparedStatement2.executeUpdate();
    connection.commit();  // Make changes permanent
} catch (SQLException e) {
    connection.rollback();  // Undo all changes on error
}
```

### 7.2 SQL Injection and Prevention
**What is SQL injection?** Malicious SQL code inserted through user inputs that can compromise the database.

**Bad example (vulnerable):**
```java
String query = "SELECT * FROM login WHERE ID = '" + username + "'";
```
If username is `admin' OR '1'='1`, it bypasses authentication!

**Our defense (PreparedStatement):**
```java
String query = "SELECT * FROM login WHERE ID = ? AND PW = ?";
PreparedStatement ps = connection.prepareStatement(query);
ps.setString(1, username);  // Safely escaped
ps.setString(2, password);
```

### 7.3 Foreign Key Constraints
**Purpose:** Maintain referential integrity between tables.

**In our schema:**
- `Patient_Info.Room_Number` → `room.room_no`
- Cannot assign a patient to non-existent room
- Cannot delete a room that has active patients (unless cascade is configured)

**Teacher may ask:** "What happens if you try to insert a patient with room 999 when that room doesn't exist?"
**Answer:** The database will reject the insert with a foreign key constraint violation error.

### 7.4 Connection Pooling (Future Enhancement)
**Current approach:** Each operation opens a new connection via `conn` class.

**Limitation:** Opening/closing connections is expensive; under heavy load, can exhaust DB connections.

**Solution:** Connection pool (e.g., HikariCP) maintains a reusable pool of connections, dramatically improving performance.

### 7.5 Java Memory Management and Resource Cleanup
**Why try-with-resources?**
```java
try (conn c = new conn()) {
    // Use connection
}  // Automatically calls c.close()
```

**Without it:** Must manually close in finally block; easy to forget, causes connection leaks.

**In our project:** Every DB access uses try-with-resources for `conn`, `PreparedStatement`, and `ResultSet`.

### 7.6 Swing Event Dispatch Thread (EDT)
**Rule:** All UI updates must happen on the EDT to avoid race conditions.

**Our implementation:**
```java
SwingUtilities.invokeLater(() -> new Login());
```

**Timer usage in Reception:**
- Runs callback on EDT every 60 seconds
- Safe to update UI components directly in timer callback

### 7.7 Data Validation Layers
**Three-tier validation:**
1. **UI Layer:** JComboBox restricts choices, input field length limits
2. **Application Layer:** Parse BigDecimal, check positive numbers, validate required fields
3. **Database Layer:** ENUMs, NOT NULL constraints, CHECK constraints

**Example from admission:**
- UI: Only show available rooms in dropdown
- App: Validate deposit is positive number
- DB: Room_Number must exist in room table (FK)

### 7.8 Normalization and Why It Matters
**Our schema is 3NF (Third Normal Form):**
- **1NF:** No repeating groups (each cell has single value)
- **2NF:** No partial dependencies (all attributes depend on full primary key)
- **3NF:** No transitive dependencies (non-key attributes don't depend on other non-key attributes)

**Benefits:**
- Eliminates data redundancy
- Prevents update anomalies
- Ensures data consistency

**Example:** Department phone numbers stored once in `department` table, not repeated in every patient record.

---

## 8. Question Banks with Ready Answers

### 8.1 Fundamentals (Teacher warm-up)
1. **What design pattern governs our DB access?** Cursor/DAO-like pattern using a dedicated `conn` utility and try-with-resources.
2. **Difference between `Statement` and `PreparedStatement`?** Prepared statements compile once, prevent SQL injection, support parameter binding—used everywhere in this project.
3. **Why Swing over web?** Offline-friendly, fast to prototype, no server layer, consistent with lab setups where Java is the evaluation baseline.
4. **How is room availability maintained?** Boolean flag in `room` table plus transactional updates during admission/discharge.
5. **What ensures UI consistency?** `UITheme`/`UIComponents` centralize fonts, colors, and layout scaffolding.

### 8.2 Intermediate (Where most viva time lands)
6. **Explain the admission transaction workflow.** Insert patient → update room availability within same connection; commit ensures atomicity.
7. **How do we refresh dashboard stats?** `Timer` triggers `refreshSnapshotData()` each minute, which runs count queries.
8. **How are filters applied in patient directory?** `TableRowSorter` with case-insensitive regex built from input.
9. **How do we guard against invalid deposits?** `BigDecimal` parsing with sanitized input; numeric validation prompts user.
10. **How does the discharge flow avoid orphan rooms?** Transaction updates both patient and room tables; if update fails, rollback keeps data consistent.
11. **What happens if `IllustrationPanel` can’t load assets?** Fallback asset ensures UI still renders; errors logged to stderr.
12. **Why store time as string instead of timestamp?** Simplifies display locale and manual entry; trade-off acknowledged with plan to standardize later.
13. **How to add new module (e.g., Pharmacy)?** Create new JFrame → reuse `UIComponents.pageContainer` → implement SQL queries via `conn` → update reception navigation.
14. **How does `scripts/build-and-run.ps1` handle classpath?** Compiles to `out/`, copies illustrations, then runs `java -cp "out;mysql-connector.jar" hospital.management.system.Login`.
15. **Concurrency handling?** MySQL ensures row-level locking; app keeps minimal state, so concurrent users rely on DB integrity.

### 8.3 Advanced/Trick Questions (Prepare to impress)
16. **What’s the cost of using `TableRowSorter` with large datasets?** In-memory sorting/filtering; fine for hundreds of rows but future scaling would need pagination.
17. **How would you secure passwords properly?** Store salted hash (e.g., BCrypt) in DB, use char arrays in memory, enforce strong password policy.
18. **Can SQL injection still happen?** Only if code reverts to string concatenation; current modules rely on prepared statements exclusively.
19. **How would you introduce audit logging?** Add audit table or triggers capturing CRUD actions; wrap JDBC calls with logger.
20. **How to migrate to Gradle/Maven?** Create build file, configure dependencies (MySQL connector), tasks for run/test, integrate resources.
21. **Which SOLID principles are visible?** Single Responsibility (each JFrame handles one concern), Open/Closed (UIComponents extendable without modification), DRY (shared theme).
22. **Discuss thread safety in Swing.** UI updates happen on EDT via `SwingUtilities.invokeLater`; long tasks (DB queries) run synchronously but quick.
23. **How to support offline mode?** Swap JDBC with embedded database (H2) and sync layer.
24. **What’s the big-O for search filter?** O(n) per keystroke over currently loaded rows; acceptable for small dataset.
25. **How to prevent double-click accident in staff deletion?** Already confirm dialog; could add undo/soft delete.

### 8.4 Database Theory Crossfire
26. **Why use ENUM for gender/availability?** Constrains set, ensures data consistency, easier validation.
27. **Explain foreign key vs. unique key.** FK enforces referential link; unique ensures uniqueness but no referential guarantee.
28. **How to enforce cascade on discharge?** Could add `ON DELETE CASCADE` but we intentionally control via app logic for explicit confirmation.
29. **Normalization vs Denormalization in our schema?** Normalized for integrity; caching/denorm would be for analytic/reporting features.
30. **Transaction isolation level default?** InnoDB default is REPEATABLE READ; we rely on default but can adjust if phantom reads arise.

### 8.5 Security & DevOps Grilling
31. **Where do credentials live?** `login` table for auth, `db.properties` for DB connection override.
32. **How to deploy securely?** Package as executable JAR, store `db.properties` with environment variables or secrets vault, restrict DB user permissions.
33. **How to log sensitive errors?** Use sanitized messages in UI, detailed logs to server file (future addition).
34. **What’s next after plain text passwords?** Integrate Argon2/BCrypt, password reset flow, account lockouts.
35. **How to implement role-based access?** Add `role` column to `login`, branch UI features (e.g., admin sees staff module) or disable actions.

### 8.6 UI/UX Probe
36. **Accessibility considerations?** High-contrast palette, large fonts via `UITheme`, keyboard-friendly components.
37. **Why use custom illustrations?** Enhance cognitive mapping per module; implemented via vector art scaling for clarity.
38. **How to localize?** Externalize strings into resource bundles, swap fonts appropriately.
39. **Handling screen resizing?** Layouts rely on BoxLayout/GridBag for responsive resizing; forms scroll inside `smoothScrollPane`.
40. **How to avoid freezing during DB fetch?** For larger datasets, move queries to SwingWorker; roadmap item.

### 8.7 Scenario-Based Challenges
41. **Room stays occupied even after discharge—why?** Likely transaction failure; verify commit, check for exception. Could also be manual DB change.
42. **Admission fails mid-save—how to recover?** Transaction rollback ensures no partial data; user re-attempt after addressing root cause.
43. **Multiple ambulances returning simultaneously—does system handle it?** Yes; table view refresh loads actual DB state; concurrency handled by MySQL.
44. **What if `db.properties` accidentally checked into VCS?** Remove, rotate credentials, add to `.gitignore`; highlight security awareness.
45. **How to integrate analytics dashboard?** Extend `Reception` to run aggregate queries, maybe embed JFreeChart.

### 8.8 Rapid-Fire Definitions (Flash cards)
46. **ACID** – Atomicity, Consistency, Isolation, Durability (emphasize we rely on atomicity in discharge/admission).
47. **JDBC Driver** – Adapter between Java and MySQL; `com.mysql.cj.jdbc.Driver`.
48. **Swing EDT** – Event dispatch thread for UI updates; we use `SwingUtilities.invokeLater` to start frames.
49. **ResultSet** – Cursor over query results; consumed by `ResultSetTableModelBuilder`.
50. **BigDecimal** – Precise decimal arithmetic; used for money fields like deposits.
51. **Enum** – Restricted set of string-like values in SQL; ensures consistent state labels.
52. **PreparedStatement** – Parameterized query object; prevents injection and allows pre-compilation.
53. **GridBagLayout** – Flexible layout manager for forms; used in admission and staff dialogs.
54. **AutoCloseable** – Allows try-with-resources; `conn` implements it for tidy cleanup.
55. **Transaction** – Sequence of operations treated as single unit; commit/rollback semantics.

### 8.9 Killer Curveballs (Answer concisely, redirect to strengths)
56. **Why not use Hibernate?** Prioritized simplicity and full SQL transparency for learning outcomes; lightweight footprint.
57. **Can the app run on Linux/Mac?** Yes—JDK & MySQL available cross-platform; PowerShell script replaced with shell equivalent.
58. **How to unit test Swing UI?** Use AssertJ Swing or TestFX, but focus more on service layer tests using JUnit.
59. **What’s your backup plan if MySQL crashes during demo?** Keep seed script and MySQL service auto-start; in worst case, show recorded run + talk through states.
60. **How to integrate OTP-based login?** Add OTP service (Twilio) via REST call, store OTP temporarily, verify before launching dashboard.

---

## 9. Risk Register & Mitigations
| Risk | Impact | Mitigation |
| --- | --- | --- |
| DB unavailable during viva | Demo stalls | Run MySQL as service, test connection beforehand, keep fallback video. |
| Password security critique | Reputation | Acknowledge prototype state, present hashing roadmap. |
| Team member forgets module details | Q&A stumble | Review section §7 (Critical Technical Concepts), assign each person 2-3 modules to master deeply. |
| Pandoc export failure | No PDF handout | Pre-generate PDF via VS Code Markdown PDF or screenshot key sections. |
| Data inconsistency from manual DB edits | Wrong stats in demo | Run seed script before evaluation, keep sample dataset.

---

## 10. Enhancement Roadmap (for future questions)
1. **Security Hardening:** Hash passwords, implement RBAC, audit logging.
2. **Analytics Layer:** Add charts for occupancy trends, staff workload metrics.
3. **Notification Service:** Twilio/email alerts for emergency dispatch or pending balances.
4. **API Exposure:** REST endpoints for patient lookup to integrate with third-party systems.
5. **Test Automation:** JUnit + Testcontainers for DB integration tests, Swing UI smoke tests.
6. **Packaging:** Gradle build, native installer or Electron wrapper for cross-platform distribution.
7. **Offline Resilience:** Sync queue with local cache (H2) for network outages.

---

## 11. Glossary & Acronyms
- **EDT:** Event Dispatch Thread (Swing’s UI thread).
- **JDBC:** Java Database Connectivity.
- **ACID:** Atomicity, Consistency, Isolation, Durability.
- **CRUD:** Create, Read, Update, Delete.
- **DAO:** Data Access Object pattern; partially embodied by `conn` helper.
- **FK:** Foreign Key.
- **UI/UX:** User Interface / User Experience.
- **RBAC:** Role-Based Access Control.
- **PoC:** Proof of Concept.

---

## 12. Lightning Demo Script (15 Minutes)
1. **Intro (1 min):** Elevator pitch + architecture diagram.
2. **Login (1 min):** Show success and failure case.
3. **Dashboard (2 min):** KPIs, animations, refresh behavior.
4. **Admission Flow (3 min):** Add patient, highlight validation, show DB change.
5. **Directory + Filter (1.5 min):** Search by disease or room.
6. **Update Deposit (1.5 min):** Show recalculated pending amount.
7. **Discharge (2 min):** Confirm room freed.
8. **Staff CRUD (2 min):** Add/edit employee with validation.
9. **Ambulance & Departments (1 min):** Quick overview.
10. **Close (0.5 min):** Mention roadmap + Q&A invitation.

---

## 13. Reference Commands & Utilities
```powershell
# Rebuild & run
scripts\build-and-run.ps1

# (Optional) Export this playbook to PDF (requires Pandoc + TeX)
pandoc docs\hospital-viva-playbook.md -o docs\hospital-viva-playbook.pdf

# MySQL seeding
SOURCE sql/schema.sql;
SOURCE sql/seed-data.sql;
```

---

## 14. Final Checklist Before Viva
- [ ] MySQL service running; schema seeded.
- [ ] `db.properties` correct (or defaults in `conn`).
- [ ] Application builds via script without errors.
- [ ] Reception stats reflect seeded data (no zero values).
- [ ] Each teammate rehearsed primary & backup topics.
- [ ] Laptop power, projector cable, fallback screenshots ready.
- [ ] Print/PDF of `project-overview.md` and this playbook.

**Confidence hack:** Keep this document open in split-screen with the app; glance only between questions to stay composed.

---

> _"Impress not by saying everything—impress by answering exactly what’s asked and hinting you can go deeper. This playbook is your depth."_

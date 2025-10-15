# CareSphere Hospital Management System – Ultimate Handbook

_Last updated: October 1, 2025_

> This single handbook fuses the executive overview, viva master playbook, and team study guide. Skim the quick hits when you're short on time, then dive into the deep dives, cheatsheets, and question banks as you rehearse.

---

## Table of Contents
1. [60-second orientation](#1-60-second-orientation)
2. [Architecture snapshot](#2-architecture-snapshot)
3. [Technology stack & assets](#3-technology-stack--assets)
4. [System map & end-to-end flow](#4-system-map--end-to-end-flow)
5. [Database deep dive](#5-database-deep-dive)
6. [Core concepts made simple](#6-core-concepts-made-simple)
7. [Module-by-module narratives](#7-module-by-module-narratives)
8. [Code mechanics reference](#8-code-mechanics-reference)
9. [Build, run, and demo operations](#9-build-run-and-demo-operations)
10. [Viva master question bank](#10-viva-master-question-bank)
11. [Troubleshooting, risks & safeguards](#11-troubleshooting-risks--safeguards)
12. [Cheat sheets & quick recall](#12-cheat-sheets--quick-recall)
13. [Enhancement roadmap](#13-enhancement-roadmap)
14. [Final prep & confidence boosters](#14-final-prep--confidence-boosters)

---

## 1. 60-second orientation
- **Problem:** Reception teams juggle admissions, room assignments, billing touchpoints, staff coordination, and ambulance readiness with spreadsheets or paper trails.
- **Solution:** CareSphere is a Java Swing desktop client with a MySQL backbone that centralizes admissions, occupancy, staff rosters, departmental contacts, and ambulance tracking with live KPIs.
- **Why it matters:** Reduces double-bookings, shortens admission time from minutes to seconds, and gives administrators a single-pane command center.
- **Mic-drop stat:** Admissions drop from manual minutes to guided seconds thanks to validated forms and live room availability.
- **One-breath tech stack:** Java 8+, Swing UI toolkit, custom component library, JDBC via MySQL Connector/J 8.0.28, normalized MySQL schema with transactional safeguards, PowerShell automation script for build/run.

---

## 2. Architecture snapshot

```text
+----------------------+       +----------------------+       +-------------------------+
|  Presentation Layer  | <---> |   Application Logic  | <---> |   MySQL Database (JDBC) |
|  (Java Swing Frames) |       |  (per-module classes) |       |  login, Patient_Info…   |
+----------------------+       +----------------------+       +-------------------------+
         ^                            ^                                   ^
         |                            |                                   |
         | UIComponents/UITheme       | conn (connection helper)          | schema.sql / seed-data.sql
```

### Core architectural ideas
1. **Page container pattern:** Every functional area is a `JFrame` wrapped in `UIComponents.pageContainer` for consistent headers, cards, and illustrations.
2. **Centralized styling:** `UITheme` and `UIComponents` define fonts, colors, gradients, and reusable widgets so branding stays consistent.
3. **Database access flow:** Modules use the `conn` helper (implements `AutoCloseable`) and parameterized `PreparedStatement`s; results flow into tables through `ResultSetTableModelBuilder`.
4. **Domain separation:** Dedicated classes handle patients, staff, rooms, departments, ambulances, and shared UI infrastructure to keep logic focused and testable.

---

## 3. Technology stack & assets

| Layer | Technology | Notes |
| --- | --- | --- |
| UI Layer | **Java Swing** | Modern look via `UIComponents`, `UITheme`, `IllustrationPanel`.
| Application Core | Java SE (8+) | Event-driven controllers per module.
| Persistence | JDBC (`conn` helper) | Loads credentials from `db.properties`, uses MySQL Connector/J 8.0.28.
| Database | MySQL 8.x | Schema in `sql/schema.sql`, seed data in `sql/seed-data.sql`.
| Build & Run | PowerShell script `scripts/build-and-run.ps1` | Compiles sources, copies illustrations, launches `hospital.management.system.Login`.
| Assets | SVG/PNG illustrations | Found in `src/hospital/management/system/illustrations/`.

**Bundled jars:**
- `mysql-connector-java-8.0.28.jar` – JDBC driver.
- `ResultSet2xml*.jar` – Legacy utilities retained for reference.

---

## 4. System map & end-to-end flow

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

### Demo script (10-step tour)
1. **Login** with seeded credentials (e.g., `admin` / `admin123`).
2. **Reception dashboard** lights up live KPIs (patients, rooms, ambulances) and navigation cards.
3. **Register new patient** through validated form with available-room dropdown.
4. Refresh the **dashboard** to watch KPIs update.
5. Open **Patient Directory** and search by name/disease.
6. Visit **Room Availability** to see occupancy flip.
7. **Update Patient Details** to adjust deposit and recalc pending amount.
8. **Discharge** the patient; confirm room freed.
9. Navigate through **Employee Directory** (CRUD), **Ambulances**, and **Departments**.
10. Close with roadmap highlights (security, analytics, packaging).

---

## 5. Database deep dive

### 5.1 Entity cheat sheet
| Table | Key Columns | Purpose |
| --- | --- | --- |
| `login` | `ID` (PK), `PW` | Operator credentials (planned upgrade: salted hashes).
| `department` | `Department` (PK), `Phone_Number` | Directory of hospital departments.
| `room` | `room_no` (PK), `Availability`, `Price`, `Bed_Type` | Master list of rooms for admissions and billing.
| `Patient_Info` | `number` (PK), `Room_Number` (FK → `room.room_no`), `Deposite` | Tracks admitted patients, rooms, deposits, and timestamps.
| `EMP_INFO` | `Aadhar_Number` (PK), `Salary`, `Gmail` | Staff roster keyed on Aadhar number.
| `Ambulance` | Composite key (`Name`, `Car_Name`) | Monitors ambulance drivers, vehicles, availability, and locations.

### 5.2 Normalization & integrity talking points
- Tables sit in **Third Normal Form**: no repeating groups, attributes fully depend on the key, and no transitive dependencies.
- **Foreign key** `Patient_Info.Room_Number → room.room_no` guarantees that admissions reference valid rooms.
- ENUMs (`Availability`, `Gender`) constrain inputs to allowed states.
- **Transactions** (admission, discharge) keep patient and room tables consistent even on failure.

### 5.3 SQL snippets worth memorizing
```sql
-- Active patient count for dashboard
SELECT COUNT(*) FROM Patient_Info;

-- Rooms for admission dropdown
SELECT room_no FROM room WHERE Availability = 'Available' ORDER BY room_no;

-- Pending amount calculation
SELECT Price - ? AS pending FROM room WHERE room_no = ?;
```

---

## 6. Core concepts made simple

### 6.1 Essentials in story form
- **Program as recipe:** The app is a step-by-step instruction list: show login → validate credentials → open dashboard → navigate modules.
- **Java Swing = LEGO set:** `JFrame` forms the shell, `JButton` acts as an interactive door, `JTable` organizes data, `JComboBox` offers curated choices.
- **Database as smart librarian:** MySQL shelves tables (`login`, `Patient_Info`, `room`, etc.) so data stays organized, searchable, and durable.
- **JDBC as bridge:** Java asks questions, JDBC ferries them across, MySQL answers.
- **Transactions as safety net:** Insert patient + mark room occupied succeed together or not at all; discharge reverses the pairing safely.
- **SQL injection cautionary tale:** Prepared statements act like savvy guards; they refuse string concatenation tricks.
- **Normalization notebook:** Store department phones once (`department` table) and reference them instead of copying.
- **Foreign keys as key rings:** You can only assign rooms that exist; MySQL enforces reality.
- **Swing Event Dispatch Thread (EDT):** Keep UI updates on the EDT; longer tasks stay quick so the interface never freezes.
- **Hashing roadmap:** Plain passwords today; BCrypt/Argon2 tomorrow for one-way secret codes.
- **OOP principles:** `Login` inherits from `JFrame`, `conn` encapsulates JDBC details, `IllustrationPanel` leverages polymorphism to draw varied art.

### 6.2 Quick glossary
| Term | Fast definition |
| --- | --- |
| **ACID** | Atomicity, Consistency, Isolation, Durability—foundation of MySQL transactions.
| **PreparedStatement** | Parameterized SQL query that blocks injection and caches execution plans.
| **EDT** | Swing's UI thread; all rendering happens here via `SwingUtilities.invokeLater`.
| **AutoCloseable** | Interface enabling try-with-resources cleanup (used by `conn`).
| **BigDecimal** | Exact decimal arithmetic for money fields like deposits and salaries.
| **ENUM** | SQL type limiting values to a defined set (e.g., `Availability`).

---

## 7. Module-by-module narratives

| # | Module | Highlights | Teacher hooks |
| --- | --- | --- | --- |
| 1 | **Login (`Login.java`)** | Gradient hero panel, secure credential check via prepared statement, `JPasswordField` with char-array wipe. | "How do you prevent password leaks?" → mention masking, clearing arrays, hashing plan.
| 2 | **Reception (`Reception.java`)** | Live KPIs, animated navigation cards, Swing timers for clock + stats, graceful DB outage messaging. | Ask about resilience when DB offline.
| 3 | **New Patient Admission (`NEW_PATIENT.java`)** | Available-room dropdown, `BigDecimal` validation, transaction to insert patient + occupy room, success toast. | Explain double-booking avoidance via transactions and room flag.
| 4 | **Patient Directory (`ALL_Patient_Info.java`)** | `ResultSetTableModelBuilder` for tables, `TableRowSorter` regex filter, refresh button. | Discuss client-side filtering vs server-side.
| 5 | **Room Management (`Room.java`, `SearchRoom.java`)** | Read-only overview plus availability filter using parameterized SQL. | Suggest "room_history" enhancement for audit trails.
| 6 | **Patient Discharge (`patient_discharge.java`)** | Dropdown of active patients, summary preview, transactional delete + room release, confirmation dialog. | Highlight ACID properties.
| 7 | **Update Patient Details (`update_patient_details.java`)** | Modify room/time/deposit, auto recalculates pending amount, disabled field to prevent tampering. | Mention derived field strategy.
| 8 | **Employee Directory (`Employee_info.java`)** | CRUD dialog with validation, double-click editing, keyed on Aadhar. | Talk about validation layers and PK choice.
| 9 | **Departments (`Department.java`)** | Lightweight read-only contact list. | Entry-level module for team rotation.
| 10 | **Ambulances (`Ambulance.java`)** | Fleet readiness overview, location column, future dispatch toggles. | Discuss GPS integration roadmap.
| 11 | **Shared UI Infrastructure** | `UITheme`, `UIComponents`, `IllustrationPanel`, `GradientPanel`, `ResultSetTableModelBuilder`. | Show how DRY and consistency are achieved.
| 12 | **Database Connector (`conn.java`)** | Loads `db.properties`, default credentials fallback, try-with-resources, human-friendly SQL errors. | Mention pooling roadmap (HikariCP).

---

## 8. Code mechanics reference

### 8.1 Connection lifecycle (`conn.java`)
```java
Properties props = loadProperties();
Class.forName("com.mysql.cj.jdbc.Driver");
connection = DriverManager.getConnection(
    props.getProperty("db.url"),
    props.getProperty("db.user"),
    props.getProperty("db.password")
);
```
- Implements `AutoCloseable` to fit try-with-resources.
- Defaults handle absence of `db.properties`; production deploy stores file outside version control.

### 8.2 Authentication flow (`Login.java`)
```java
String query = "SELECT * FROM login WHERE ID = ? AND PW = ?";
PreparedStatement ps = connection.prepareStatement(query);
ps.setString(1, username.trim());
ps.setString(2, password);
ResultSet rs = ps.executeQuery();
if (rs.next()) {
    SwingUtilities.invokeLater(Reception::new);
    dispose();
} else {
    JOptionPane.showMessageDialog(this, "Invalid credentials");
}
Arrays.fill(passwordChars, '\\0');
```

### 8.3 Admission transaction (`NEW_PATIENT.java`)
```java
try (conn c = new conn()) {
    c.connection.setAutoCommit(false);
    try {
        insertPatient.executeUpdate();
        occupyRoom.executeUpdate();
        c.connection.commit();
    } catch (SQLException ex) {
        c.connection.rollback();
        throw ex;
    }
}
```

### 8.4 Table filtering (`ALL_Patient_Info.java`)
```java
TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
table.setRowSorter(sorter);
String expression = Pattern.quote(query.trim());
sorter.setRowFilter(RowFilter.regexFilter("(?i)" + expression));
```

### 8.5 ResultSet to table (`ResultSetTableModelBuilder.java`)
```java
return new DefaultTableModel(rows, columnNames) {
    @Override public boolean isCellEditable(int row, int column) {
        return false;
    }
};
```

---

## 9. Build, run, and demo operations

### 9.1 Environment checklist
1. Install **JDK 8+** and add to PATH.
2. Install **MySQL Server 8.x**; ensure user in `schema.sql` exists (e.g., `team_member`).
3. Run `sql/schema.sql` then `sql/seed-data.sql` in MySQL shell.
4. Place `mysql-connector-java-8.0.28.jar` (already bundled) in project root or provide custom path to the script.
5. (Optional) Create `db.properties` next to the JAR to override credentials.

### 9.2 Build & launch
```powershell
scripts\build-and-run.ps1
```
- Compiles Java sources into `out/`.
- Copies illustration assets.
- Launches `hospital.management.system.Login` with connector on the classpath.
- Pass `-ConnectorPath` if using a different JAR location.

### 9.3 Database seeding
```powershell
# Inside MySQL shell
SOURCE sql/schema.sql;
SOURCE sql/seed-data.sql;
```

### 9.4 Optional PDF export
1. Install [Pandoc](https://pandoc.org/) and a PDF engine (TeX Live or wkhtmltopdf).
2. Export this handbook:
   ```powershell
   pandoc docs\caresphere-ultimate-handbook.md -o docs\caresphere-ultimate-handbook.pdf
   ```

---

## 10. Viva master question bank

### 10.1 Fundamentals
1. **Purpose of the project?** Centralizes admissions, occupancy, staff, departments, and ambulances in one desktop app.
2. **Why Java Swing?** Offline-friendly, no server dependencies, quick to prototype, full UI control.
3. **Why MySQL?** Open-source, ACID-compliant, excellent JDBC support, handles concurrency.
4. **How many tables?** Six: `login`, `Patient_Info`, `room`, `EMP_INFO`, `department`, `Ambulance`.
5. **Define JDBC.** Java Database Connectivity API for executing SQL from Java.

### 10.2 Intermediate depth
6. **Prevent SQL injection?** Prepared statements with placeholders.
7. **Describe admission transaction.** Insert patient + mark room occupied within one transaction; rollback on failure.
8. **Dashboard refresh logic?** Swing `Timer` triggers count queries every 60 seconds.
9. **Filter implementation?** `TableRowSorter` with case-insensitive regex built from search text.
10. **Prevent invalid deposits?** `BigDecimal` parsing, numeric validation, user prompts.
11. **Discharge consistency?** Transaction removes patient and frees room atomically.
12. **Illustration fallback?** `IllustrationPanel` logs failure, uses placeholder asset.
13. **Time stored as text?** Simplicity for formatting; roadmap includes standardized timestamp.
14. **Add new module?** Clone module pattern: new JFrame, `UIComponents.pageContainer`, SQL via `conn`, link from Reception.
15. **Concurrency handling?** MySQL manages locks; UI re-queries for fresh data.

### 10.3 Advanced & trick shots
16. **`TableRowSorter` scalability?** In-memory; fine for hundreds of rows, would need pagination for thousands.
17. **Secure passwords properly?** Store salted hashes (BCrypt/Argon2), enforce policies.
18. **Remaining SQL injection risk?** Only if string concatenation sneaks back; code uses prepared statements exclusively.
19. **Introduce audit logging?** Add audit table or triggers; log CRUD actions with timestamps.
20. **Migrate to Gradle/Maven?** Define dependencies, compile/run tasks, resources handling.
21. **SOLID principles?** SRP in module frames, Open/Closed via extensible UI components, DRY through shared theming.
22. **Swing thread safety?** EDT handles UI; long tasks kept short, future `SwingWorker` for heavier operations.
23. **Offline support?** Swap JDBC with embedded H2, add sync queue.
24. **Filter complexity?** O(n) per keystroke over loaded rows.
25. **Prevent accidental staff deletion?** Already confirm; consider undo/soft delete.

### 10.4 Database theory crossfire
26. **Why ENUMs?** Enforces consistent values at insertion.
27. **Foreign key vs unique key?** FK maintains relational link; unique ensures uniqueness without referential guarantee.
28. **Cascade on discharge?** Could add `ON DELETE CASCADE`, but current app handles explicitly for confirmation control.
29. **Normalization vs denormalization?** Normalized for integrity; denorm reserved for analytics.
30. **Default isolation level?** InnoDB's REPEATABLE READ; acceptable for current workload.

### 10.5 Security & DevOps
31. **Where credentials live?** `login` table for operators, `db.properties` for DB overrides.
32. **Secure deployment?** Bundle executable JAR, store secrets outside repo, restrict DB privileges.
33. **Logging sensitive errors?** UI shows friendly messages; detailed logs planned for server-side storage.
34. **Next step after plaintext passwords?** Hashing, lockouts, password policies.
35. **Implement RBAC?** Add `role` column, branch UI features, enforce permissions server-side.

### 10.6 UI/UX focus
36. **Accessibility choices?** High-contrast palette, large fonts via `UITheme`, keyboard-friendly controls.
37. **Custom illustrations rationale?** Improves cognitive mapping; vector assets scale cleanly.
38. **Localization strategy?** Externalize strings to resource bundles; adjust fonts per locale.
39. **Handling resize?** BoxLayout/GridBag layouts adapt; forms scroll via custom scroll pane.
40. **Avoid freezing during DB fetch?** Queries kept lightweight; plan to move heavy calls to `SwingWorker`.

### 10.7 Scenario drills
41. **Room stays occupied after discharge?** Likely transaction failure or exception; check logs and rollback.
42. **Admission fails mid-save?** Rollback prevents partial data; user retries post fix.
43. **Concurrent ambulance updates?** MySQL handles it; UI refresh pulls actual state.
44. **`db.properties` committed accidentally?** Remove, rotate credentials, add to ignore list.
45. **Analytics dashboard idea?** Extend Reception with aggregate queries, incorporate charting library.

### 10.8 Rapid-fire definitions
46. **ACID** – Atomicity, Consistency, Isolation, Durability.
47. **JDBC driver** – Adapter bridging Java and MySQL (`com.mysql.cj.jdbc.Driver`).
48. **Swing EDT** – UI thread invoked via `SwingUtilities.invokeLater`.
49. **ResultSet** – Cursor over query results consumed by table builder.
50. **BigDecimal** – Exact decimal for currency.
51. **ENUM** – Restricted SQL value set.
52. **PreparedStatement** – Precompiled SQL with placeholders.
53. **GridBagLayout** – Flexible layout manager used in forms.
54. **AutoCloseable** – Enables try-with-resources cleanup.
55. **Transaction** – All-or-nothing group of database operations.

### 10.9 Killer curveballs
56. **Why not Hibernate?** We favored lightweight transparency; JDBC suits the learning goals.
57. **Run on Linux/Mac?** Yes—install JDK & MySQL, swap PowerShell script for shell equivalent.
58. **Unit test Swing UI?** Use AssertJ Swing/TestFX; priority on service layer tests.
59. **MySQL crash mid-demo?** Keep service auto-started, seed data ready, fallback video/screenshots.
60. **OTP-based login?** Integrate OTP provider (Twilio), store temporary codes, validate before dashboard.

---

## 11. Troubleshooting, risks & safeguards

### 11.1 Common issues & fixes
| Symptom | Root cause | Fix |
| --- | --- | --- |
| `com.mysql.cj.jdbc.Driver not found` | Connector JAR missing/path typo | Ensure `mysql-connector-java-8.0.28.jar` exists or pass `-ConnectorPath` to script.
| "Unable to connect to the database" | MySQL down or wrong credentials | Start MySQL service, verify `db.properties`, check firewall.
| Dashboard shows "Live data unavailable" | Count query failed (DB offline) | Restart DB; UI intentionally degrades gracefully.
| Admission says "No rooms available" | All rooms occupied or query failed | Discharge a patient or reseed room data.
| Pandoc export fails | Missing Pandoc/TeX | Install dependencies or use VS Code PDF extension.

### 11.2 Risk register
| Risk | Impact | Mitigation |
| --- | --- | --- |
| DB unavailable during viva | Demo stalls | Start MySQL early, keep fallback recording.
| Password security critique | Viva challenge | Acknowledge prototype state, outline hashing roadmap.
| Team member forgets module details | Q&A stumble | Assign sections, rehearse using §10.
| Pandoc/PDF hiccups | No handout | Pre-generate PDF or capture annotated screenshots.
| Manual DB edits cause inconsistency | Wrong dashboard stats | Rerun seed script; enforce least-privilege access.

---

## 12. Cheat sheets & quick recall

### 12.1 Credentials & file map
```
Login
  Username: admin
  Password: admin123

Database (default)
  Host: localhost
  Port: 3306
  Schema: hospital_management_system
  User: team_member (see schema.sql) or root
  Password: check db.properties or schema seed

Key files
  Schema: sql/schema.sql
  Seed data: sql/seed-data.sql
  Build script: scripts/build-and-run.ps1
  Entry point: hospital.management.system.Login
```

### 12.2 SQL crib notes
```sql
SELECT COUNT(*) FROM Patient_Info; -- Dashboard patients
SELECT room_no FROM room WHERE Availability = 'Available';
DELETE FROM Patient_Info WHERE number = ?; -- Discharge
UPDATE room SET Availability = 'Available' WHERE room_no = ?;
SELECT * FROM Patient_Info WHERE Name LIKE '%keyword%';
```

### 12.3 Swing & JDBC kit
```
JFrame          → Window shell
JButton         → Clickable action
JTextField      → Single-line text input
JPasswordField  → Hidden password input
JComboBox       → Dropdown selector
JTable          → Tabular data view
Timer           → Scheduled task (dashboard refresh)

Class.forName("com.mysql.cj.jdbc.Driver");
Connection conn = DriverManager.getConnection(url, user, pass);
PreparedStatement ps = conn.prepareStatement("SELECT ...");
ResultSet rs = ps.executeQuery();
```

### 12.4 Transaction template
```java
try (conn c = new conn()) {
    c.connection.setAutoCommit(false);
    try {
        statement1.executeUpdate();
        statement2.executeUpdate();
        c.connection.commit();
    } catch (SQLException e) {
        c.connection.rollback();
        throw e;
    }
}
```

### 12.5 Module responsibility grid
| Module | Primary function | Key SQL |
| --- | --- | --- |
| Login | Authenticate operators | `SELECT * FROM login WHERE ID=? AND PW=?`
| Reception | KPIs + navigation | `SELECT COUNT(*) FROM ...`
| NEW_PATIENT | Admit patients | Insert into `Patient_Info`, update `room`
| ALL_Patient_Info | Patient directory | `SELECT * FROM Patient_Info`
| Room | Room overview | `SELECT * FROM room`
| SearchRoom | Availability filter | `SELECT * FROM room WHERE Availability=?`
| patient_discharge | Discharge flow | Delete from `Patient_Info`, update `room`
| update_patient_details | Modify records | `UPDATE Patient_Info SET ...`
| Employee_info | Staff CRUD | `INSERT/UPDATE/DELETE EMP_INFO`
| Department | Department list | `SELECT * FROM department`
| Ambulance | Fleet tracking | `SELECT * FROM Ambulance`

### 12.6 Validation checklist
- **Admission:** fields filled, positive deposit, room selected, (future) unique ID.
- **Employee CRUD:** non-empty name, positive age/salary, valid phone/email, unique Aadhar.
- **Login:** non-empty fields, credentials match database record.

### 12.7 Error translation
| Message | Meaning | Action |
| --- | --- | --- |
| "Foreign key constraint violated" | Invalid/non-existent room assigned | Pick valid room; ensure room seeded.
| "Duplicate entry" | Primary key already exists | Use different patient/employee ID.
| "Live data unavailable" | Dashboard query failed | Investigate DB connectivity.

---

## 13. Enhancement roadmap
1. **Security hardening:** Hash passwords, introduce RBAC, add audit logging.
2. **Analytics layer:** Occupancy trend charts, staff workload metrics.
3. **Notifications:** Email/SMS alerts for ambulance readiness or pending balances.
4. **API exposure:** REST endpoints for third-party integrations.
5. **Automated testing:** JUnit + Testcontainers, eventual Swing smoke tests.
6. **Packaging:** Gradle/Maven build, bundled executable JAR or installer.
7. **Offline resilience:** Local cache (H2) with sync queue for outages.

---

## 14. Final prep & confidence boosters

### 14.1 30-minute countdown routine
1. **Read aloud (5 min each):** Elevator pitch, assigned modules, database tables, top 10 viva questions.
2. **Quick quiz (5 min):** Define transaction, explain injection defense, name foreign key, list admission validations.
3. **Demo run-through (10 min):** Login → Dashboard → Admit → Update → Discharge → Employee CRUD.
4. **Final checks (5 min):** MySQL running, data seeded, app launches, backup plan ready.

### 14.2 Live demo checklist
- [ ] MySQL service running; schema & seeds applied.
- [ ] `db.properties` validated or defaults confirmed.
- [ ] Reception KPIs show non-zero values.
- [ ] Each teammate rehearsed primary + backup topics.
- [ ] Laptops charged, projector cable ready, fallback screenshots or video queued.

### 14.3 Confidence playbook
- Open strong: “Our system replaces manual paperwork with a guided, transactional desktop app…”
- If challenged: acknowledge current state, describe the enhancement path.
- Close with strengths: transactions, validation, modular UI, future roadmap.
- Remember: you built it—you know it best.

**🎓 Study together, quiz each other, and walk into the viva confident. You've got this! 🚀**

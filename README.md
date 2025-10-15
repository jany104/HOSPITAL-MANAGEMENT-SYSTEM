# Hospital Management System

```
purpose    desktop admissions suite for hospital front desks
contains   swing ui · mysql schema · seed data · pdf discharge receipts
focus      keep operators fast, keep configuration local
```

---

### essentials
`stack`        Java 17 · Swing · MySQL 8 · PowerShell automation  
`entry`        scripts/build-and-run.ps1 (compiles, runs)  
`data`         sql/schema.sql · sql/seed-data.sql  
`config`       copy db-template.properties → db.properties (personal credentials)  
`privacy`      db.properties is gitignored by design

### launch script
```
git clone https://github.com/Surya-T-S/HOSPITAL-MANAGEMENT-SYSTEM.git
cd "Hospital management system"

# mysql console
SOURCE sql/schema.sql;
SOURCE sql/seed-data.sql;

# local credentials
copy db-template.properties db.properties

powershell -ExecutionPolicy Bypass -File scripts/build-and-run.ps1
```

### collaboration cues
- branch from `main`, develop, and keep the build script green
- describe visible UI or data changes in pull requests

_“Wherever the art of Medicine is loved, there is also a love of Humanity.” — Hippocrates_


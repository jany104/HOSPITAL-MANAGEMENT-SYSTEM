INSERT INTO login (ID, PW) VALUES
    ('admin', 'admin123'),
    ('team_member', 'strongpass123')
ON DUPLICATE KEY UPDATE PW = VALUES(PW);

INSERT INTO department (Department, Phone_Number) VALUES
    ('Cardiology', '0123456789'),
    ('Neurology', '0987654321'),
    ('Orthopedics', '0223344556'),
    ('Pediatrics', '0112233445'),
    ('Oncology', '0115544332'),
    ('Emergency Medicine', '0116677889'),
    ('Dermatology', '0117766554')
ON DUPLICATE KEY UPDATE Phone_Number = VALUES(Phone_Number);

INSERT INTO room (room_no, Availability, Price, Bed_Type) VALUES
    (101, 'Available', 1200, 'Single'),
    (102, 'Available', 1500, 'Double'),
    (103, 'Occupied', 1000, 'Single'),
    (104, 'Available', 1300, 'Single'),
    (201, 'Available', 1800, 'Suite'),
    (202, 'Available', 1600, 'Double'),
    (203, 'Available', 2100, 'Suite'),
    (301, 'Available', 1400, 'Single'),
    (302, 'Available', 1700, 'Double'),
    (401, 'Available', 2400, 'Deluxe')
ON DUPLICATE KEY UPDATE Availability = VALUES(Availability), Price = VALUES(Price), Bed_Type = VALUES(Bed_Type);

INSERT INTO EMP_INFO (Name, Age, Phone_Number, Salary, Gmail, Aadhar_Number) VALUES
    ('Dr. Arjun Mehta', 45, '5551234567', 95000, 'arjun.mehta@example.com', '111122223333'),
    ('Dr. Naina Kapoor', 38, '5559876543', 98000, 'naina.kapoor@example.com', '444455556666'),
    ('Dr. Suresh Rao', 52, '5556547890', 102000, 'suresh.rao@example.com', '777788889999'),
    ('Dr. Riya Banerjee', 41, '5553214567', 91000, 'riya.banerjee@example.com', '222233334444'),
    ('Dr. Kabir Singh', 36, '5557891234', 88000, 'kabir.singh@example.com', '555566667777'),
    ('Dr. Meera Joshi', 47, '5552135467', 99000, 'meera.joshi@example.com', '888899990000')
ON DUPLICATE KEY UPDATE Name = VALUES(Name), Age = VALUES(Age), Phone_Number = VALUES(Phone_Number), Salary = VALUES(Salary), Gmail = VALUES(Gmail);

INSERT INTO Ambulance (Name, Gender, Car_Name, Available, Location) VALUES
    ('Alex Driver', 'Male', 'Mercedes Sprinter', 'Yes', 'Gate A'),
    ('Priya Patel', 'Female', 'Ford Transit', 'No', 'On Call')
ON DUPLICATE KEY UPDATE Available = VALUES(Available), Location = VALUES(Location);

INSERT INTO Patient_Info (ID, number, Name, Gender, Disease, Room_Number, Time, Deposite, Admission_Reason) VALUES
    ('Aadhar Card', '999988887777', 'Rahul Kumar', 'Male', 'Cardiology', 103, '2025-09-01 10:00', 1000, 'Scheduled angiography and observation'),
    ('Voter Id', 'ABC1234567', 'Anita Sharma', 'Female', 'Neurology', 101, '2025-09-10 09:30', 1500, 'Migraine workup and pain management'),
    ('Passport', 'MNO4567890', 'Priya Nair', 'Female', 'Orthopedics', 202, '2025-09-18 14:20', 1600, 'ACL reconstruction post-operative care'),
    ('Driving License', 'DL09X1234', 'Sameer Bhatia', 'Male', 'Emergency Medicine', 301, '2025-09-22 22:15', 1400, 'Trauma observation and neurologic monitoring')
ON DUPLICATE KEY UPDATE Name = VALUES(Name), Gender = VALUES(Gender), Disease = VALUES(Disease), Room_Number = VALUES(Room_Number), Time = VALUES(Time), Deposite = VALUES(Deposite), Admission_Reason = VALUES(Admission_Reason);

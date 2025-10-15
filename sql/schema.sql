CREATE DATABASE IF NOT EXISTS hospital_management_system;
USE hospital_management_system;

CREATE TABLE IF NOT EXISTS login (
    ID VARCHAR(50) NOT NULL,
    PW VARCHAR(100) NOT NULL,
    PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS department (
    Department VARCHAR(100) NOT NULL,
    Phone_Number VARCHAR(20) NOT NULL,
    PRIMARY KEY (Department)
);

CREATE TABLE IF NOT EXISTS room (
    room_no INT NOT NULL,
    Availability ENUM('Available', 'Occupied') NOT NULL DEFAULT 'Available',
    Price INT NOT NULL,
    Bed_Type VARCHAR(50) NOT NULL,
    PRIMARY KEY (room_no)
);

CREATE TABLE IF NOT EXISTS Patient_Info (
    ID VARCHAR(100) NOT NULL,
    number VARCHAR(100) NOT NULL,
    Name VARCHAR(100) NOT NULL,
    Gender ENUM('Male', 'Female', 'Other') NOT NULL,
    Disease VARCHAR(100),
    Room_Number INT NOT NULL,
    Time VARCHAR(100) NOT NULL,
    Deposite INT NOT NULL,
    Admission_Reason TEXT,
    PRIMARY KEY (number),
    CONSTRAINT fk_patient_room FOREIGN KEY (Room_Number) REFERENCES room (room_no)
);

CREATE TABLE IF NOT EXISTS EMP_INFO (
    Name VARCHAR(100) NOT NULL,
    Age INT NOT NULL,
    Phone_Number VARCHAR(20) NOT NULL,
    Salary INT NOT NULL,
    Gmail VARCHAR(100) NOT NULL,
    Aadhar_Number VARCHAR(20) NOT NULL,
    PRIMARY KEY (Aadhar_Number)
);

CREATE TABLE IF NOT EXISTS Ambulance (
    Name VARCHAR(100) NOT NULL,
    Gender ENUM('Male', 'Female', 'Other') NOT NULL,
    Car_Name VARCHAR(100) NOT NULL,
    Available ENUM('Yes', 'No') NOT NULL,
    Location VARCHAR(100) NOT NULL,
    PRIMARY KEY (Name, Car_Name)
);

CREATE USER 'team_member'@'localhost' IDENTIFIED BY 'StrongPass!23';
GRANT ALL ON hospital_management_system.* TO 'team_member'@'localhost';
FLUSH PRIVILEGES;

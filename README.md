# Clinic Management System

A dependency-free Java console application for managing a clinic's patients, doctors,
appointments, and invoices. Data is kept in memory and is cleared when the application
exits.

## Requirements

- Java 17 or newer

## Compile and run

From the project root:

```powershell
New-Item -ItemType Directory -Force out
javac -d out src\ClinicManagementSystem.java
java -cp out ClinicManagementSystem
```

## Features

- Register and list patients and doctors
- Schedule appointments and prevent overlapping bookings for the same patient or doctor
- Mark appointments completed or cancelled
- Create invoices using a doctor's consultation fee and additional charges
- Mark invoices as paid and view collected and outstanding totals

Appointment dates use `yyyy-MM-dd`; times use the 24-hour `HH:mm` format.

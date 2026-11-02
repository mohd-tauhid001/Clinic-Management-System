import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class ClinicManagementSystem {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter APPOINTMENT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Scanner scanner = new Scanner(System.in);
    private final Map<Integer, Patient> patients = new LinkedHashMap<>();
    private final Map<Integer, Doctor> doctors = new LinkedHashMap<>();
    private final Map<Integer, Appointment> appointments = new LinkedHashMap<>();
    private final Map<Integer, Invoice> invoices = new LinkedHashMap<>();
    private int nextPatientId = 1;
    private int nextDoctorId = 1;
    private int nextAppointmentId = 1;
    private int nextInvoiceId = 1;

    public static void main(String[] args) {
        ClinicManagementSystem system = new ClinicManagementSystem();
        try {
            system.run();
        } catch (InputClosedException exception) {
            System.out.println("\nInput closed. Exiting Clinic Management System.");
        }
    }

    private void run() {
        boolean running = true;
        while (running) {
            printMenu();
            switch (readInt("Choose an option: ", 0, 10)) {
                case 1 -> addPatient();
                case 2 -> listPatients();
                case 3 -> addDoctor();
                case 4 -> listDoctors();
                case 5 -> scheduleAppointment();
                case 6 -> listAppointments();
                case 7 -> updateAppointmentStatus();
                case 8 -> createInvoice();
                case 9 -> listInvoices();
                case 10 -> printSummary();
                case 0 -> {
                    System.out.println("Goodbye.");
                    running = false;
                }
                default -> throw new IllegalStateException("Unexpected menu option");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== Clinic Management System ===");
        System.out.println("1. Register patient");
        System.out.println("2. List patients");
        System.out.println("3. Register doctor");
        System.out.println("4. List doctors");
        System.out.println("5. Schedule appointment");
        System.out.println("6. List appointments");
        System.out.println("7. Update appointment status");
        System.out.println("8. Create invoice");
        System.out.println("9. List invoices");
        System.out.println("10. Clinic summary");
        System.out.println("0. Exit");
    }

    private void addPatient() {
        System.out.println("\n--- Register Patient ---");
        String name = readNonBlank("Patient name: ");
        int age = readInt("Age: ", 1, 120);
        String phone = readNonBlank("Phone: ");
        Patient patient = new Patient(nextPatientId++, name, age, phone);
        patients.put(patient.id, patient);
        System.out.println("Patient registered with ID " + patient.id + ".");
    }

    private void listPatients() {
        System.out.println("\n--- Patients ---");
        if (patients.isEmpty()) {
            System.out.println("No patients registered.");
            return;
        }
        System.out.printf("%-6s %-24s %-6s %s%n", "ID", "NAME", "AGE", "PHONE");
        for (Patient patient : patients.values()) {
            System.out.printf("%-6d %-24s %-6d %s%n",
                    patient.id, patient.name, patient.age, patient.phone);
        }
    }

    private void addDoctor() {
        System.out.println("\n--- Register Doctor ---");
        String name = readNonBlank("Doctor name: ");
        String specialization = readNonBlank("Specialization: ");
        BigDecimal consultationFee = readAmount("Consultation fee: ");
        Doctor doctor = new Doctor(nextDoctorId++, name, specialization, consultationFee);
        doctors.put(doctor.id, doctor);
        System.out.println("Doctor registered with ID " + doctor.id + ".");
    }

    private void listDoctors() {
        System.out.println("\n--- Doctors ---");
        if (doctors.isEmpty()) {
            System.out.println("No doctors registered.");
            return;
        }
        System.out.printf("%-6s %-24s %-24s %s%n",
                "ID", "NAME", "SPECIALIZATION", "CONSULTATION FEE");
        for (Doctor doctor : doctors.values()) {
            System.out.printf("%-6d %-24s %-24s %s%n",
                    doctor.id, doctor.name, doctor.specialization, formatAmount(doctor.consultationFee));
        }
    }

    private void scheduleAppointment() {
        if (!requireRecords(patients, "patients") || !requireRecords(doctors, "doctors")) {
            return;
        }

        System.out.println("\n--- Schedule Appointment ---");
        listPatients();
        Patient patient = patients.get(readInt("Patient ID: ", 1, Integer.MAX_VALUE));
        if (patient == null) {
            System.out.println("Patient not found.");
            return;
        }

        listDoctors();
        Doctor doctor = doctors.get(readInt("Doctor ID: ", 1, Integer.MAX_VALUE));
        if (doctor == null) {
            System.out.println("Doctor not found.");
            return;
        }

        LocalDate date = readDate("Appointment date (yyyy-MM-dd): ");
        LocalTime time = readTime("Appointment time (HH:mm): ");
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        if (dateTime.isBefore(LocalDateTime.now())) {
            System.out.println("Appointment must be in the future.");
            return;
        }
        boolean slotTaken = appointments.values().stream()
                .anyMatch(appointment -> appointment.status != AppointmentStatus.CANCELLED
                        && appointment.dateTime.equals(dateTime)
                        && (appointment.doctor.id == doctor.id || appointment.patient.id == patient.id));
        if (slotTaken) {
            System.out.println("That time slot is already booked for this doctor or patient.");
            return;
        }

        Appointment appointment = new Appointment(
                nextAppointmentId++, patient, doctor, dateTime);
        appointments.put(appointment.id, appointment);
        System.out.println("Appointment scheduled with ID " + appointment.id + ".");
    }

    private void listAppointments() {
        System.out.println("\n--- Appointments ---");
        if (appointments.isEmpty()) {
            System.out.println("No appointments scheduled.");
            return;
        }
        System.out.printf("%-6s %-22s %-22s %-18s %s%n",
                "ID", "PATIENT", "DOCTOR", "DATE / TIME", "STATUS");
        for (Appointment appointment : appointments.values()) {
            System.out.printf("%-6d %-22s %-22s %-18s %s%n",
                    appointment.id, appointment.patient.name, appointment.doctor.name,
                    appointment.dateTime.format(APPOINTMENT_FORMAT), appointment.status);
        }
    }

    private void updateAppointmentStatus() {
        if (!requireRecords(appointments, "appointments")) {
            return;
        }
        listAppointments();
        Appointment appointment = appointments.get(
                readInt("Appointment ID: ", 1, Integer.MAX_VALUE));
        if (appointment == null) {
            System.out.println("Appointment not found.");
            return;
        }
        if (appointment.status != AppointmentStatus.SCHEDULED) {
            System.out.println("Only scheduled appointments can be updated.");
            return;
        }

        System.out.println("1. Mark completed");
        System.out.println("2. Cancel");
        int choice = readInt("Choose an action: ", 1, 2);
        appointment.status = choice == 1
                ? AppointmentStatus.COMPLETED : AppointmentStatus.CANCELLED;
        System.out.println("Appointment marked " + appointment.status + ".");
    }

    private void createInvoice() {
        if (!requireRecords(appointments, "appointments")) {
            return;
        }
        listAppointments();
        Appointment appointment = appointments.get(
                readInt("Appointment ID to bill: ", 1, Integer.MAX_VALUE));
        if (appointment == null) {
            System.out.println("Appointment not found.");
            return;
        }
        if (appointment.status == AppointmentStatus.CANCELLED) {
            System.out.println("A cancelled appointment cannot be invoiced.");
            return;
        }
        boolean alreadyInvoiced = invoices.values().stream()
                .anyMatch(invoice -> invoice.appointment.id == appointment.id);
        if (alreadyInvoiced) {
            System.out.println("This appointment already has an invoice.");
            return;
        }

        BigDecimal additionalCharges = readAmount("Additional charges (enter 0 if none): ");
        Invoice invoice = new Invoice(
                nextInvoiceId++, appointment, appointment.doctor.consultationFee,
                additionalCharges);
        invoices.put(invoice.id, invoice);
        System.out.println("Invoice created with ID " + invoice.id
                + ". Total: " + formatAmount(invoice.total));
    }

    private void listInvoices() {
        System.out.println("\n--- Invoices ---");
        if (invoices.isEmpty()) {
            System.out.println("No invoices created.");
            return;
        }
        System.out.printf("%-6s %-9s %-20s %-20s %-12s %-12s %s%n",
                "ID", "APPT ID", "PATIENT", "DOCTOR", "TOTAL", "STATUS", "ACTION");
        for (Invoice invoice : invoices.values()) {
            String action = invoice.paid ? "" : "Enter " + invoice.id + " to mark paid";
            System.out.printf("%-6d %-9d %-20s %-20s %-12s %-12s %s%n",
                    invoice.id, invoice.appointment.id, invoice.appointment.patient.name,
                    invoice.appointment.doctor.name, formatAmount(invoice.total),
                    invoice.paid ? "PAID" : "UNPAID", action);
        }
        if (invoices.values().stream().anyMatch(invoice -> !invoice.paid)) {
            String answer = readLine("Mark an unpaid invoice as paid? Enter invoice ID, or 0 to return: ");
            int invoiceId = parseInt(answer, 0, Integer.MAX_VALUE, "Invoice ID");
            if (invoiceId != 0) {
                Invoice invoice = invoices.get(invoiceId);
                if (invoice == null) {
                    System.out.println("Invoice not found.");
                } else if (invoice.paid) {
                    System.out.println("Invoice is already paid.");
                } else {
                    invoice.paid = true;
                    System.out.println("Invoice " + invoice.id + " marked paid.");
                }
            }
        }
    }

    private void printSummary() {
        long scheduled = appointments.values().stream()
                .filter(appointment -> appointment.status == AppointmentStatus.SCHEDULED).count();
        long completed = appointments.values().stream()
                .filter(appointment -> appointment.status == AppointmentStatus.COMPLETED).count();
        long cancelled = appointments.values().stream()
                .filter(appointment -> appointment.status == AppointmentStatus.CANCELLED).count();
        BigDecimal collected = invoices.values().stream()
                .filter(invoice -> invoice.paid)
                .map(invoice -> invoice.total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = invoices.values().stream()
                .filter(invoice -> !invoice.paid)
                .map(invoice -> invoice.total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("\n--- Clinic Summary ---");
        System.out.println("Patients: " + patients.size());
        System.out.println("Doctors: " + doctors.size());
        System.out.println("Appointments: " + appointments.size()
                + " (scheduled: " + scheduled + ", completed: " + completed
                + ", cancelled: " + cancelled + ")");
        System.out.println("Invoices: " + invoices.size());
        System.out.println("Collected: " + formatAmount(collected));
        System.out.println("Outstanding: " + formatAmount(outstanding));
    }

    private boolean requireRecords(Map<?, ?> records, String label) {
        if (records.isEmpty()) {
            System.out.println("Register at least one " + label + " first.");
            return false;
        }
        return true;
    }

    private String readNonBlank(String prompt) {
        while (true) {
            String value = readLine(prompt).trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field cannot be blank.");
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        return scanner.nextLine();
    }

    private int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            String value = readLine(prompt);
            try {
                return parseInt(value, minimum, maximum, "Please enter a whole number");
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    private int parseInt(String value, int minimum, int maximum, String errorMessage) {
        try {
            int number = Integer.parseInt(value.trim());
            if (number >= minimum && number <= maximum) {
                return number;
            }
        } catch (NumberFormatException ignored) {
            // Report invalid input through the same message as out-of-range values.
        }
        throw new IllegalArgumentException(errorMessage + " between "
                + minimum + " and " + maximum + ".");
    }

    private BigDecimal readAmount(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                BigDecimal amount = new BigDecimal(value.trim());
                if (amount.signum() >= 0) {
                    return amount.setScale(2, java.math.RoundingMode.HALF_UP);
                }
            } catch (NumberFormatException ignored) {
                // Report malformed and negative amounts with the same actionable prompt.
            }
            System.out.println("Enter a valid non-negative amount.");
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return LocalDate.parse(value.trim(), DATE_FORMAT);
            } catch (DateTimeParseException exception) {
                System.out.println("Enter a valid date in yyyy-MM-dd format.");
            }
        }
    }

    private LocalTime readTime(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return LocalTime.parse(value.trim(), TIME_FORMAT);
            } catch (DateTimeParseException exception) {
                System.out.println("Enter a valid time in HH:mm format (24-hour clock).");
            }
        }
    }

    private String formatAmount(BigDecimal amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    private static final class Patient {
        private final int id;
        private final String name;
        private final int age;
        private final String phone;

        private Patient(int id, String name, int age, String phone) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.phone = phone;
        }
    }

    private static final class Doctor {
        private final int id;
        private final String name;
        private final String specialization;
        private final BigDecimal consultationFee;

        private Doctor(int id, String name, String specialization, BigDecimal consultationFee) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
            this.consultationFee = consultationFee;
        }
    }

    private enum AppointmentStatus {
        SCHEDULED,
        COMPLETED,
        CANCELLED
    }

    private static final class Appointment {
        private final int id;
        private final Patient patient;
        private final Doctor doctor;
        private final LocalDateTime dateTime;
        private AppointmentStatus status = AppointmentStatus.SCHEDULED;

        private Appointment(int id, Patient patient, Doctor doctor, LocalDateTime dateTime) {
            this.id = id;
            this.patient = patient;
            this.doctor = doctor;
            this.dateTime = dateTime;
        }
    }

    private static final class Invoice {
        private final int id;
        private final Appointment appointment;
        private final BigDecimal consultationFee;
        private final BigDecimal additionalCharges;
        private final BigDecimal total;
        private boolean paid;

        private Invoice(int id, Appointment appointment, BigDecimal consultationFee,
                        BigDecimal additionalCharges) {
            this.id = id;
            this.appointment = appointment;
            this.consultationFee = consultationFee;
            this.additionalCharges = additionalCharges;
            this.total = consultationFee.add(additionalCharges);
        }
    }

    private static final class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}

package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class patient_discharge extends JFrame {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH);

    private final JComboBox<String> patientSelector;
    private final JLabel roomValue;
    private final JLabel inTimeValue;
    private final JLabel outTimeValue;
    private PatientSummary currentSummary;
    private LocalDateTime latestCheckoutTime;

    public patient_discharge() {
        super("Discharge patient");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(920, 540);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);
        patientSelector = UIComponents.comboBox();

        roomValue = UIComponents.subtitle("—");
        inTimeValue = UIComponents.subtitle("—");
        latestCheckoutTime = LocalDateTime.now();
        outTimeValue = UIComponents.subtitle(latestCheckoutTime.format(DATE_TIME_FORMATTER));

        JPanel layout = UIComponents.pageContainer(
                "Discharge checkout",
                "Finalize stay details and release the room",
                buildSummaryCard(),
                IllustrationPanel.IllustrationStyle.DISCHARGE
        );

        add(layout, BorderLayout.CENTER);

        populatePatients();
        loadSelectionDetails();

        patientSelector.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            loadSelectionDetails();
        });

        setVisible(true);
    }

    private JPanel buildSummaryCard() {
        JPanel container = new JPanel(new BorderLayout(0, 16));
        container.setOpaque(false);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        addRow(grid, gbc, 0, "Patient ID", patientSelector);
        addRow(grid, gbc, 1, "Room number", roomValue);
        addRow(grid, gbc, 2, "Check-in", inTimeValue);
        addRow(grid, gbc, 3, "Check-out", outTimeValue);

        JButton refreshButton = UIComponents.secondaryButton("Refresh details");
        refreshButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            loadSelectionDetails();
        });

        JButton dischargeButton = UIComponents.primaryButton("Confirm discharge");
        dischargeButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            discharge();
        });

        JButton cancelButton = UIComponents.secondaryButton("Cancel");
        cancelButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            dispose();
        });

        JPanel actions = UIComponents.toolbar(12, cancelButton, refreshButton, dischargeButton);
        actions.setBorder(new EmptyBorder(8, 0, 0, 0));

        container.add(grid, BorderLayout.CENTER);
        container.add(actions, BorderLayout.SOUTH);

        return container;
    }

    private long calculateStayDays(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            return 1;
        }
        Duration duration = Duration.between(checkIn, checkOut);
        if (duration.isNegative()) {
            return 1;
        }
        long hours = duration.toHours();
        long days = (hours + 23) / 24;
        return Math.max(days, 1);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, Component valueComponent) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(UIComponents.formLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(valueComponent, gbc);
    }

    private void populatePatients() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        try (conn c = new conn(); ResultSet patients = c.statement.executeQuery("select number from Patient_Info order by number")) {
            while (patients.next()) {
                model.addElement(patients.getString("number"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load patient list: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        patientSelector.setModel(model);
        if (model.getSize() == 0) {
            patientSelector.addItem("No active patients");
            patientSelector.setEnabled(false);
        }
    }

    private void loadSelectionDetails() {
        if (!patientSelector.isEnabled()) {
            currentSummary = null;
            resetSummaryDisplay();
            return;
        }

        String selected = Objects.toString(patientSelector.getSelectedItem(), "");
        if (selected.isBlank()) {
            currentSummary = null;
            resetSummaryDisplay();
            return;
        }

        PatientSummary summary;
        try {
            summary = fetchSummary(selected);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load patient details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            currentSummary = null;
            resetSummaryDisplay();
            return;
        }

        currentSummary = summary;
        if (summary == null) {
            resetSummaryDisplay();
            return;
        }

        roomValue.setText(summary.roomNumber());
        inTimeValue.setText(formatDisplayTimestamp(summary.admittedAt(), summary.admissionRaw()));
        refreshCheckoutTimestamp();
    }

    private void refreshCheckoutTimestamp() {
        latestCheckoutTime = LocalDateTime.now();
        outTimeValue.setText(latestCheckoutTime.format(DATE_TIME_FORMATTER));
    }

    private void resetSummaryDisplay() {
        roomValue.setText("—");
        inTimeValue.setText("—");
        refreshCheckoutTimestamp();
    }

    private String formatDisplayTimestamp(LocalDateTime value, String fallback) {
        if (value != null) {
            return value.format(DATE_TIME_FORMATTER);
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return "—";
    }

    private PatientSummary fetchSummary(String patientNumber) throws SQLException {
        try (conn c = new conn(); PreparedStatement ps = c.connection.prepareStatement(
            "select p.number, p.Name, p.Room_Number, p.Time, p.Deposite, r.Price, r.Bed_Type " +
            "from Patient_Info p join room r on r.room_no = p.Room_Number where p.number = ?")) {
            ps.setString(1, patientNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String admissionRaw = rs.getString("Time");
                LocalDateTime admittedAt = parseAdmissionTime(admissionRaw);
                return new PatientSummary(
                    rs.getString("number"),
                    rs.getString("Name"),
                    rs.getString("Room_Number"),
                    rs.getString("Bed_Type"),
                    admittedAt,
                    admissionRaw,
                    rs.getInt("Deposite"),
                    rs.getInt("Price")
                );
            }
        }
    }

    private LocalDateTime parseAdmissionTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw, DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void discharge() {
        if (!patientSelector.isEnabled()) {
            JOptionPane.showMessageDialog(this, "There are no patients to discharge", "Nothing to do", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String patientNumber = Objects.toString(patientSelector.getSelectedItem(), "");
        String room = roomValue.getText();
        if (patientNumber.isBlank() || room.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a patient before discharging", "Missing selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PatientSummary summary = currentSummary;
        if (summary == null || !patientNumber.equals(summary.patientNumber())) {
            try {
                summary = fetchSummary(patientNumber);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Unable to load patient details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentSummary = summary;
        }

        if (summary == null) {
            JOptionPane.showMessageDialog(this, "Selected patient record is no longer available.", "Missing record", JOptionPane.WARNING_MESSAGE);
            return;
        }

        refreshCheckoutTimestamp();
        LocalDateTime checkoutTime = latestCheckoutTime;
        LocalDateTime checkInTime = summary.admittedAt() != null ? summary.admittedAt() : parseAdmissionTime(summary.admissionRaw());
        if (checkInTime == null) {
            checkInTime = checkoutTime;
        }

        long stayDays = calculateStayDays(checkInTime, checkoutTime);
        int roomRate = Math.max(summary.roomRate(), 0);
        long roomChargeLong = stayDays * (long) roomRate;
        int roomCharge = roomChargeLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) roomChargeLong;
        int deposit = Math.max(summary.deposit(), 0);
        int balance = roomCharge - deposit;

        StringBuilder confirmation = new StringBuilder();
        confirmation.append("Discharge patient ").append(patientNumber);
        if (summary.patientName() != null && !summary.patientName().isBlank()) {
            confirmation.append(" (\"").append(summary.patientName()).append("\")");
        }
        confirmation.append(" from room ").append(room).append("?\n\n");
        confirmation.append("Stay length: ").append(stayDays).append(" day").append(stayDays == 1 ? "" : "s").append('\n');
        confirmation.append("Room charges: INR ").append(roomCharge).append('\n');
        confirmation.append("Deposit recorded: INR ").append(deposit).append('\n');
        confirmation.append(balance >= 0 ? "Balance due: INR " : "Refund owed: INR ").append(Math.abs(balance)).append('\n');

        int confirm = JOptionPane.showConfirmDialog(this,
                confirmation.toString(),
                "Confirm discharge",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (conn c = new conn()) {
            c.connection.setAutoCommit(false);

            try (PreparedStatement deletePatient = c.connection.prepareStatement("delete from Patient_Info where number = ?")) {
                deletePatient.setString(1, patientNumber);
                deletePatient.executeUpdate();
            }

            try (PreparedStatement freeRoom = c.connection.prepareStatement("update room set Availability = 'Available' where room_no = ?")) {
                freeRoom.setString(1, room);
                freeRoom.executeUpdate();
            }

            c.connection.commit();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to discharge patient: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path receiptPath = null;
        try {
            PdfReceiptWriter.DischargeDetails details = new PdfReceiptWriter.DischargeDetails(
                summary.patientNumber(),
                summary.patientName(),
                summary.roomNumber(),
                summary.bedType(),
                checkInTime,
                checkoutTime,
                stayDays,
                roomRate,
                roomCharge,
                deposit,
                balance
            );
            receiptPath = PdfReceiptWriter.writeDischargeSummary(Path.of("exports", "receipts"), details);
        } catch (IOException ioEx) {
            JOptionPane.showMessageDialog(this,
                    "Patient discharged, but the receipt could not be saved:\n" + ioEx.getMessage(),
                    "Receipt warning",
                    JOptionPane.WARNING_MESSAGE);
        }

        StringBuilder message = new StringBuilder("Patient discharged.");
        if (receiptPath != null) {
            message.append("\nReceipt saved to:\n").append(receiptPath.toAbsolutePath());
        }
        JOptionPane.showMessageDialog(this, message.toString(), "Success", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private record PatientSummary(
            String patientNumber,
            String patientName,
            String roomNumber,
            String bedType,
            LocalDateTime admittedAt,
            String admissionRaw,
            int deposit,
            int roomRate) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(patient_discharge::new);
    }
}

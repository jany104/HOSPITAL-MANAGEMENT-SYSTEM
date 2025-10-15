package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class PatientDetailsDialog extends JDialog {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH);
    private static final List<DateTimeFormatter> SUPPORTED_INPUT_FORMATS = List.of(
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    );

    private PatientDetailsDialog(Window owner, String patientNumber) {
        super(owner, "Patient profile", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        boolean ready = initialize(patientNumber);
        if (!ready) {
            dispose();
        }
    }

    static void showFor(Component parent, String patientNumber) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        PatientDetailsDialog dialog = new PatientDetailsDialog(owner, patientNumber);
        if (dialog.isDisplayable()) {
            dialog.setVisible(true);
        }
    }

    private boolean initialize(String patientNumber) {
        PatientRecord record;
        try {
            record = fetchRecord(patientNumber);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load patient details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (record == null) {
            JOptionPane.showMessageDialog(this, "No patient record found for ID " + patientNumber + ".", "Not found", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        setContentPane(buildContent(record));
        pack();
        setLocationRelativeTo(getOwner());
        return true;
    }

    private JComponent buildContent(PatientRecord record) {
        JPanel container = new JPanel(new BorderLayout(0, 18));
        container.setBorder(new EmptyBorder(24, 24, 24, 24));
        container.setOpaque(false);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel nameLabel = UIComponents.heading(nonEmpty(record.name(), "Unknown patient"));
        header.add(nameLabel);

        List<String> meta = new ArrayList<>();
        if (!record.department().isBlank() && !"—".equals(record.department())) {
            meta.add(record.department());
        }
        if (!record.roomNumber().isBlank() && !"—".equals(record.roomNumber())) {
            meta.add("Room " + record.roomNumber());
        }
        if (!meta.isEmpty()) {
            JLabel info = UIComponents.subtitle(String.join(" • ", meta));
            info.setBorder(new EmptyBorder(6, 0, 0, 0));
            header.add(info);
        }

        container.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 0, 8, 18);

        addRow(grid, gbc, "Patient ID", record.idNumber());
        addRow(grid, gbc, "ID type", record.idType());
        addRow(grid, gbc, "Gender", record.gender());
    addRow(grid, gbc, "Department", record.department());
    addRow(grid, gbc, "Admission reason", record.reason());
    addRow(grid, gbc, "Bed type", record.bedType());
        addRow(grid, gbc, "Room", record.roomNumber());
    addRow(grid, gbc, "Room rate (per day)", formatCurrency(record.roomRate()));
        addRow(grid, gbc, "Deposit", formatCurrency(record.deposit()));
        addRow(grid, gbc, "Check-in", formatDateTime(record.checkIn(), record.checkInRaw()));

        JPanel card = UIComponents.surfaceCard(grid);
        container.add(card, BorderLayout.CENTER);

        JButton closeButton = UIComponents.primaryButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel actions = UIComponents.buttonBar(closeButton);
        container.add(actions, BorderLayout.SOUTH);

        return container;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
        JLabel labelComponent = UIComponents.formLabel(label);
        panel.add(labelComponent, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel valueComponent = new JLabel(nonEmpty(value, "—"));
        valueComponent.setFont(UITheme.mediumFont(14f));
        valueComponent.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(valueComponent, gbc);

        gbc.gridx = 0;
        gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
    }

    private PatientRecord fetchRecord(String patientNumber) throws SQLException {
        try (conn c = new conn()) {
            try {
                return executeFetch(c, patientNumber, true);
            } catch (SQLException ex) {
                if (isMissingAdmissionReasonColumn(ex)) {
                    return executeFetch(c, patientNumber, false);
                }
                throw ex;
            }
        }
    }

    private PatientRecord executeFetch(conn connection, String patientNumber, boolean includeReason) throws SQLException {
        String reasonSelect = includeReason ? "p.Admission_Reason as Admission_Reason" : "NULL as Admission_Reason";
        String query = "select p.ID, p.number, p.Name, p.Gender, p.Disease, p.Room_Number, p.Time, p.Deposite, " + reasonSelect + ", r.Price, r.Bed_Type " +
                "from Patient_Info p left join room r on r.room_no = p.Room_Number where p.number = ?";
        try (PreparedStatement ps = connection.connection.prepareStatement(query)) {
            ps.setString(1, patientNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                LocalDateTime checkIn = parseTimestamp(rs.getString("Time"));
                int deposit = safeInt(rs, "Deposite");
                int price = safeInt(rs, "Price");
                String reason = includeReason ? nonEmpty(rs.getString("Admission_Reason"), "—") : "—";
                return new PatientRecord(
                        nonEmpty(rs.getString("ID"), "—"),
                        nonEmpty(rs.getString("number"), ""),
                        nonEmpty(rs.getString("Name"), ""),
                        nonEmpty(rs.getString("Gender"), "—"),
                        nonEmpty(rs.getString("Disease"), "—"),
                        reason,
                        nonEmpty(rs.getString("Room_Number"), "—"),
                        checkIn,
                        nonEmpty(rs.getString("Time"), ""),
                        deposit,
                        price,
                        nonEmpty(rs.getString("Bed_Type"), "—")
                );
            }
        }
    }

    private boolean isMissingAdmissionReasonColumn(SQLException ex) {
        for (SQLException current = ex; current != null; current = current.getNextException()) {
            String state = current.getSQLState();
            int code = current.getErrorCode();
            if ("42S22".equals(state) || code == 1054) {
                return true;
            }
            String message = Objects.toString(current.getMessage(), "").toLowerCase(Locale.ENGLISH);
            if (message.contains("unknown column") && message.contains("admission_reason")) {
                return true;
            }
        }
        return false;
    }

    private int safeInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? 0 : value;
    }

    private LocalDateTime parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : SUPPORTED_INPUT_FORMATS) {
            try {
                return LocalDateTime.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String formatDateTime(LocalDateTime value, String fallback) {
        if (value != null) {
            return value.format(DISPLAY_FORMAT);
        }
        return nonEmpty(fallback, "—");
    }

    private String formatCurrency(int amount) {
        return String.format(Locale.ENGLISH, "₹%,d", amount);
    }

    private String nonEmpty(String value, String fallback) {
        String trimmed = Objects.toString(value, "").trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private record PatientRecord(
            String idType,
            String idNumber,
            String name,
            String gender,
            String department,
            String reason,
            String roomNumber,
            LocalDateTime checkIn,
            String checkInRaw,
            int deposit,
            int roomRate,
            String bedType) {
    }
}

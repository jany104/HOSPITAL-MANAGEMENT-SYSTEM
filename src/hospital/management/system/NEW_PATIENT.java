package hospital.management.system;

import javax.swing.*;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Map;

public class NEW_PATIENT extends JFrame {

    private final JComboBox<String> idTypeField;
    private final JTextField idNumberField;
    private final JTextField nameField;
    private final JComboBox<String> genderField;
    private final JComboBox<String> departmentField;
    private final JComboBox<String> roomField;
    private final JTextField depositField;
    private final JTextField reasonField;
    private final JLabel admissionTimeLabel;
    private final JLabel roomRateValue;
    private final Map<String, RoomDetails> roomDetails = new HashMap<>();

    private static final Locale INDIA_LOCALE = new Locale("en", "IN");

    public NEW_PATIENT() {
        super("Register new patient");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(940, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);

        idTypeField = UIComponents.comboBox("Aadhar Card", "Voter Id", "Driving License", "Passport");
        idNumberField = UIComponents.textField(20);
        nameField = UIComponents.textField(20);
        genderField = UIComponents.comboBox("Male", "Female", "Non-binary", "Prefer not to say");
        departmentField = UIComponents.comboBox();
        departmentField.setPrototypeDisplayValue("Cardiothoracic Surgery");
        roomField = UIComponents.comboBox();
        roomField.setPrototypeDisplayValue("999");
        departmentField.setMaximumRowCount(8);
        roomField.setMaximumRowCount(10);
        depositField = UIComponents.textField(20);
        depositField.setEditable(false);
        depositField.setFocusable(false);
        depositField.setBackground(UITheme.SURFACE);
        admissionTimeLabel = UIComponents.subtitle(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        reasonField = UIComponents.textField(28);
        reasonField.setColumns(28);
        roomRateValue = UIComponents.subtitle("—");
        roomRateValue.setFont(UITheme.mediumFont(17f));
        roomRateValue.setForeground(UITheme.PRIMARY_DARK);

        roomField.addActionListener(event -> updateSelectedRoomDetails());

        JPanel layout = UIComponents.pageContainer(
            "New admission",
            "Capture core details and assign an available room",
            buildFormPanel(),
            IllustrationPanel.IllustrationStyle.ADMISSION
        );

        add(layout, BorderLayout.CENTER);

        populateDepartments();
        populateRooms();
        setVisible(true);
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new BorderLayout(0, 16));
        form.setOpaque(false);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(4, 0, 8, 4));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        addFormRow(grid, gbc, 0, "ID type", idTypeField);
        addFormRow(grid, gbc, 1, "ID number", idNumberField);
        addFormRow(grid, gbc, 2, "Patient name", nameField);
        addFormRow(grid, gbc, 3, "Gender", genderField);
        addFormRow(grid, gbc, 4, "Department", departmentField);
        addFormRow(grid, gbc, 5, "Room", roomField);
        addFormRow(grid, gbc, 6, "Room rate (₹)", roomRateValue);
        addFormRow(grid, gbc, 7, "Admission reason", reasonField);
        addFormRow(grid, gbc, 8, "Admission time", admissionTimeLabel);
        addFormRow(grid, gbc, 9, "Deposit (₹)", depositField);

        JButton submitButton = UIComponents.primaryButton("Save admission");
        submitButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            savePatient();
        });

        JButton cancelButton = UIComponents.secondaryButton("Cancel");
        cancelButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            dispose();
        });

        JPanel actions = UIComponents.toolbar(12, cancelButton, submitButton);
        actions.setBorder(new EmptyBorder(8, 0, 0, 0));

        JScrollPane scrollPane = UIComponents.smoothScrollPane(grid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        form.add(scrollPane, BorderLayout.CENTER);
        form.add(actions, BorderLayout.SOUTH);

        return form;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component component) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(UIComponents.formLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private void updateSelectedRoomDetails() {
        if (!roomField.isEnabled()) {
            roomRateValue.setText("—");
            depositField.setText("");
            return;
        }

        String selected = sanitizeInput(Objects.toString(roomField.getSelectedItem(), ""));
        RoomDetails details = roomDetails.get(selected);
        if (details == null) {
            roomRateValue.setText("—");
            depositField.setText("");
            return;
        }

        roomRateValue.setText(formatRoomDetails(details));
        depositField.setText(String.valueOf(details.price()));
    }

    private String formatRoomDetails(RoomDetails details) {
        StringBuilder builder = new StringBuilder(formatCurrency(details.price()));
        builder.append(" per day");
        if (details.bedType() != null && !details.bedType().isBlank()) {
            builder.append(" • ").append(details.bedType());
        }
        return builder.toString();
    }

    private String formatCurrency(int amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(INDIA_LOCALE);
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(amount);
    }

    private void populateDepartments() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        try (conn c = new conn(); ResultSet departments = c.statement.executeQuery("select Department from department order by Department")) {
            while (departments.next()) {
                model.addElement(departments.getString("Department"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load departments: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        int count = model.getSize();
        if (count == 0) {
            departmentField.setModel(new DefaultComboBoxModel<>(new String[]{"No departments configured"}));
            departmentField.setEnabled(false);
        } else {
            departmentField.setModel(model);
            departmentField.setEnabled(true);
            departmentField.setSelectedIndex(0);
        }
    }

    private void populateRooms() {
        roomDetails.clear();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        int availableCount = 0;
        try (conn c = new conn(); ResultSet rooms = c.statement.executeQuery("select room_no, Price, Bed_Type from room where Availability = 'Available' order by room_no")) {
            while (rooms.next()) {
                String roomNumber = rooms.getString("room_no");
                int price = rooms.getInt("Price");
                String bedType = rooms.getString("Bed_Type");
                model.addElement(roomNumber);
                roomDetails.put(roomNumber, new RoomDetails(price, bedType));
                availableCount++;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load rooms: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (availableCount == 0) {
            model.addElement("No rooms available");
        }

        roomField.setModel(model);
        roomField.setEnabled(availableCount > 0);
        if (availableCount > 0) {
            roomField.setSelectedIndex(0);
        }

        updateSelectedRoomDetails();
    }

    private AdmissionForm collectFormValues() {
        List<String> missingLabels = new ArrayList<>();
        List<JComponent> missingComponents = new ArrayList<>();

        String idNumber = sanitizeInput(idNumberField.getText());
        if (idNumber.isEmpty()) {
            missingLabels.add("ID number");
            missingComponents.add(idNumberField);
        }

        String patientName = sanitizeInput(nameField.getText());
        if (patientName.isEmpty()) {
            missingLabels.add("Patient name");
            missingComponents.add(nameField);
        }

        String department = sanitizeInput(Objects.toString(departmentField.getSelectedItem(), ""));
        if (department.isEmpty() || !departmentField.isEnabled()) {
            missingLabels.add("Department");
            missingComponents.add(departmentField);
        }

        String reason = sanitizeInput(reasonField.getText());
        if (reason.isEmpty()) {
            missingLabels.add("Admission reason");
            missingComponents.add(reasonField);
        }

        if (!missingLabels.isEmpty()) {
            String message = "Please complete: " + String.join(", ", missingLabels) + ".";
            JOptionPane.showMessageDialog(this, message, "Missing information", JOptionPane.WARNING_MESSAGE);
            JComponent first = missingComponents.get(0);
            first.requestFocusInWindow();
            if (first instanceof JTextField textField) {
                textField.selectAll();
            }
            return null;
        }

        String roomValue = sanitizeInput(Objects.toString(roomField.getSelectedItem(), ""));
        if (roomValue.isEmpty() || "No rooms available".equalsIgnoreCase(roomValue)) {
            JOptionPane.showMessageDialog(this, "Select an available room before saving.", "Room required", JOptionPane.WARNING_MESSAGE);
            roomField.requestFocusInWindow();
            return null;
        }

        RoomDetails selectedRoom = roomDetails.get(roomValue);
        if (selectedRoom == null) {
            JOptionPane.showMessageDialog(this, "Selected room information is unavailable.", "Room required", JOptionPane.WARNING_MESSAGE);
            roomField.requestFocusInWindow();
            return null;
        }

        String idType = sanitizeInput(Objects.toString(idTypeField.getSelectedItem(), ""));
        String gender = sanitizeInput(Objects.toString(genderField.getSelectedItem(), ""));
        String admissionTime = admissionTimeLabel.getText();
        String depositValue = String.valueOf(selectedRoom.price());

        return new AdmissionForm(idType, idNumber, patientName, gender, department, roomValue, depositValue, admissionTime, reason);
    }

    private static String sanitizeInput(String value) {
        if (value == null) {
            return "";
        }
        return value.strip();
    }

    private void savePatient() {
        if (!departmentField.isEnabled()) {
            JOptionPane.showMessageDialog(this, "Departments are not configured yet. Add departments before admitting patients.", "Setup required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!roomField.isEnabled()) {
            JOptionPane.showMessageDialog(this, "No rooms are currently available", "Admission blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AdmissionForm form = collectFormValues();
        if (form == null) {
            return;
        }

        try (conn c = new conn()) {
            c.connection.setAutoCommit(false);

            try (PreparedStatement insert = c.connection.prepareStatement(
                    "insert into Patient_Info (ID, number, Name, Gender, Disease, Room_Number, Time, Deposite, Admission_Reason) values (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, form.idType());
                insert.setString(2, form.idNumber());
                insert.setString(3, form.name());
                insert.setString(4, form.gender());
                insert.setString(5, form.department());
                insert.setString(6, form.room());
                insert.setString(7, form.admissionTime());
                insert.setString(8, form.deposit());
                insert.setString(9, form.reason());
                insert.executeUpdate();
            }

            try (PreparedStatement updateRoom = c.connection.prepareStatement("update room set Availability = 'Occupied' where room_no = ?")) {
                updateRoom.setString(1, form.room());
                updateRoom.executeUpdate();
            }

            c.connection.commit();
            JOptionPane.showMessageDialog(this, "Patient added successfully", "Admission saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to save admission: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private record RoomDetails(int price, String bedType) {
    }

    private record AdmissionForm(String idType, String idNumber, String name,
                                 String gender, String department, String room,
                                 String deposit, String admissionTime, String reason) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NEW_PATIENT::new);
    }
}

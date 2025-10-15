package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class Employee_info extends JFrame {

    private final JTable table;
    private JButton editButton;
    private JButton deleteButton;

    public Employee_info(){
        super("Employee directory");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1040, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);
        table = UIComponents.styledTable();
        table.setAutoCreateRowSorter(true);

        JPanel layout = UIComponents.pageContainer(
                "Team members",
                "HR overview for every hospital employee",
                buildTablePanel(),
                IllustrationPanel.IllustrationStyle.STAFF
        );

        add(layout, BorderLayout.CENTER);

        loadEmployees();
        updateActionStates();
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event != null && event.getValueIsAdjusting()) {
                return;
            }
            updateActionStates();
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e != null && e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editSelectedStaff();
                }
            }
        });

        setVisible(true);

    }

    private void loadEmployees() {
        try (conn c = new conn()) {
            ResultSet resultSet = c.statement.executeQuery("select * from EMP_INFO");
            table.setModel(ResultSetTableModelBuilder.buildTableModel(resultSet));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to fetch employee records: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            table.clearSelection();
            updateActionStates();
        }
    }

    private JPanel buildTablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        JPanel tableCard = UIComponents.surfaceCard(UIComponents.tableScroll(table));
        wrapper.add(tableCard, BorderLayout.CENTER);

        JButton addButton = UIComponents.primaryButton("Add staff");
        addButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            addStaff();
        });

        editButton = UIComponents.secondaryButton("Edit selected");
        editButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            editSelectedStaff();
        });

        deleteButton = UIComponents.secondaryButton("Delete selected");
        deleteButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            deleteSelectedStaff();
        });

        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        JButton refreshButton = UIComponents.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            loadEmployees();
        });

        JButton closeButton = UIComponents.secondaryButton("Close");
        closeButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            dispose();
        });

        JPanel actions = UIComponents.buttonBar(addButton, editButton, deleteButton, refreshButton, closeButton);
        actions.setBorder(new EmptyBorder(8, 24, 0, 24));
        wrapper.add(actions, BorderLayout.SOUTH);

        return wrapper;
    }

    private void updateActionStates() {
        boolean hasSelection = table.getSelectedRow() >= 0;
        if (editButton != null) {
            editButton.setEnabled(hasSelection);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(hasSelection);
        }
    }

    private void addStaff() {
        StaffFormDialog dialog = new StaffFormDialog("Add staff member", null, true);
        dialog.setVisible(true);
        StaffMember staffMember = dialog.getResult();
        if (staffMember != null) {
            try {
                insertStaff(staffMember);
                loadEmployees();
                JOptionPane.showMessageDialog(this, "Staff member added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unable to add staff member: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editSelectedStaff() {
        StaffMember selected = getSelectedStaff();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member to edit.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StaffFormDialog dialog = new StaffFormDialog("Edit staff member", selected, false);
        dialog.setVisible(true);
        StaffMember updated = dialog.getResult();
        if (updated != null) {
            try {
                updateStaff(updated);
                loadEmployees();
                JOptionPane.showMessageDialog(this, "Staff member updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unable to update staff member: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedStaff() {
        StaffMember selected = getSelectedStaff();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a staff member to delete.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int option = JOptionPane.showConfirmDialog(this,
                "Remove " + selected.name() + " from the staff directory?",
                "Confirm deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (option == JOptionPane.YES_OPTION) {
            try {
                deleteStaff(selected.aadharNumber());
                loadEmployees();
                JOptionPane.showMessageDialog(this, "Staff member removed.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unable to delete staff member: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private StaffMember getSelectedStaff() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (!(table.getModel() instanceof DefaultTableModel model)) {
            return null;
        }
        return buildStaffFromModel(model, modelRow);
    }

    private StaffMember buildStaffFromModel(DefaultTableModel model, int row) {
        String name = valueAsString(model, row, "Name");
        int age = valueAsInt(model, row, "Age");
        String phone = valueAsString(model, row, "Phone_Number");
        int salary = valueAsInt(model, row, "Salary");
        String email = valueAsString(model, row, "Gmail");
        String aadhar = valueAsString(model, row, "Aadhar_Number");
        return new StaffMember(name, age, phone, salary, email, aadhar);
    }

    private String valueAsString(DefaultTableModel model, int row, String columnName) {
        Object value = getModelValue(model, row, columnName);
        return Objects.toString(value, "").trim();
    }

    private int valueAsInt(DefaultTableModel model, int row, String columnName) {
        Object value = getModelValue(model, row, columnName);
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = Objects.toString(value, "").trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Object getModelValue(DefaultTableModel model, int row, String columnName) {
        int columnIndex = findColumnIndex(model, columnName);
        if (columnIndex < 0 || row < 0 || row >= model.getRowCount()) {
            return null;
        }
        return model.getValueAt(row, columnIndex);
    }

    private int findColumnIndex(DefaultTableModel model, String columnName) {
        int columnIndex = model.findColumn(columnName);
        if (columnIndex >= 0) {
            return columnIndex;
        }
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(model.getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    private void insertStaff(StaffMember staff) throws SQLException {
        try (conn c = new conn();
             PreparedStatement statement = c.connection.prepareStatement(
                     "insert into EMP_INFO (Name, Age, Phone_Number, Salary, Gmail, Aadhar_Number) values (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, staff.name());
            statement.setInt(2, staff.age());
            statement.setString(3, staff.phoneNumber());
            statement.setInt(4, staff.salary());
            statement.setString(5, staff.email());
            statement.setString(6, staff.aadharNumber());
            statement.executeUpdate();
        }
    }

    private void updateStaff(StaffMember staff) throws SQLException {
        try (conn c = new conn();
             PreparedStatement statement = c.connection.prepareStatement(
                     "update EMP_INFO set Name = ?, Age = ?, Phone_Number = ?, Salary = ?, Gmail = ? where Aadhar_Number = ?")) {
            statement.setString(1, staff.name());
            statement.setInt(2, staff.age());
            statement.setString(3, staff.phoneNumber());
            statement.setInt(4, staff.salary());
            statement.setString(5, staff.email());
            statement.setString(6, staff.aadharNumber());
            statement.executeUpdate();
        }
    }

    private void deleteStaff(String aadharNumber) throws SQLException {
        try (conn c = new conn();
             PreparedStatement statement = c.connection.prepareStatement(
                     "delete from EMP_INFO where Aadhar_Number = ?")) {
            statement.setString(1, aadharNumber);
            statement.executeUpdate();
        }
    }

    private static String sanitize(String input) {
        return input == null ? "" : input.trim();
    }

    private static int parsePositiveInt(String value, String fieldName) {
        String sanitized = sanitize(value);
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            int parsed = Integer.parseInt(sanitized);
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than zero.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    private static int parseNonNegativeInt(String value, String fieldName) {
        String sanitized = sanitize(value);
        if (sanitized.isEmpty()) {
            return 0;
        }
        try {
            int parsed = Integer.parseInt(sanitized);
            if (parsed < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    private record StaffMember(String name, int age, String phoneNumber, int salary, String email, String aadharNumber) {
    }

    private final class StaffFormDialog extends JDialog {

        private final JTextField nameField;
        private final JTextField ageField;
        private final JTextField phoneField;
        private final JTextField salaryField;
        private final JTextField emailField;
        private final JTextField aadharField;
        private final boolean allowAadharEdit;
        private final StaffMember original;
        private StaffMember result;

        StaffFormDialog(String title, StaffMember initial, boolean allowAadharEdit) {
            super(Employee_info.this, title, true);
            this.allowAadharEdit = allowAadharEdit;
            this.original = initial;

            getContentPane().setBackground(UITheme.BACKGROUND);
            setLayout(new BorderLayout());

            nameField = UIComponents.textField(20);
            ageField = UIComponents.textField(20);
            phoneField = UIComponents.textField(20);
            salaryField = UIComponents.textField(20);
            emailField = UIComponents.textField(20);
            aadharField = UIComponents.textField(20);

            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 0, 8, 12);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.weightx = 1;

            int row = 0;
            UIComponents.addFormRow(form, gbc, row++, "Name", nameField);
            UIComponents.addFormRow(form, gbc, row++, "Age", ageField);
            UIComponents.addFormRow(form, gbc, row++, "Phone number", phoneField);
            UIComponents.addFormRow(form, gbc, row++, "Salary (₹)", salaryField);
            UIComponents.addFormRow(form, gbc, row++, "Email", emailField);
            UIComponents.addFormRow(form, gbc, row, "Aadhar number", aadharField);

            if (initial != null) {
                nameField.setText(initial.name());
                ageField.setText(Integer.toString(initial.age()));
                phoneField.setText(initial.phoneNumber());
                salaryField.setText(Integer.toString(initial.salary()));
                emailField.setText(initial.email());
                aadharField.setText(initial.aadharNumber());
            }

            aadharField.setEditable(allowAadharEdit);
            if (!allowAadharEdit) {
                aadharField.setForeground(UITheme.MUTED);
            }

            JScrollPane formScroll = UIComponents.smoothScrollPane(form);
            formScroll.setBorder(null);
            formScroll.setOpaque(false);
            formScroll.getViewport().setOpaque(false);
            formScroll.setPreferredSize(new Dimension(420, 320));

            JPanel card = UIComponents.surfaceCard(formScroll);

            JButton cancelButton = UIComponents.secondaryButton("Cancel");
            cancelButton.addActionListener(e -> {
                if (e != null) {
                    e.getActionCommand();
                }
                result = null;
                dispose();
            });

            JButton saveButton = UIComponents.primaryButton("Save");
            saveButton.addActionListener(e -> {
                if (e != null) {
                    e.getActionCommand();
                }
                trySave();
            });

            JPanel actions = UIComponents.toolbar(12, cancelButton, saveButton);
            actions.setBorder(new EmptyBorder(16, 0, 0, 0));
            card.add(actions, BorderLayout.SOUTH);

            JPanel content = new JPanel(new BorderLayout());
            content.setOpaque(false);
            content.setBorder(new EmptyBorder(24, 24, 24, 24));
            content.add(card, BorderLayout.CENTER);

            add(content, BorderLayout.CENTER);

            getRootPane().setDefaultButton(saveButton);
            pack();
            setMinimumSize(new Dimension(Math.max(480, getWidth()), getHeight()));
            setResizable(false);
            setLocationRelativeTo(Employee_info.this);
        }

        private void trySave() {
            try {
                result = buildStaffMember();
                dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input", JOptionPane.ERROR_MESSAGE);
            }
        }

        private StaffMember buildStaffMember() {
            String name = sanitize(nameField.getText());
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name is required.");
            }

            int age = parsePositiveInt(ageField.getText(), "Age");

            String phone = sanitize(phoneField.getText());
            if (phone.isEmpty()) {
                throw new IllegalArgumentException("Phone number is required.");
            }

            int salary = parseNonNegativeInt(salaryField.getText(), "Salary");

            String email = sanitize(emailField.getText());
            if (email.isEmpty() || !email.contains("@")) {
                throw new IllegalArgumentException("Enter a valid email address.");
            }

            String aadhar = sanitize(aadharField.getText());
            if (aadhar.isEmpty()) {
                throw new IllegalArgumentException("Aadhar number is required.");
            }
            if (!allowAadharEdit && original != null) {
                aadhar = original.aadharNumber();
            }

            return new StaffMember(name, age, phone, salary, email, aadhar);
        }

        StaffMember getResult() {
            return result;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Employee_info::new);
    }
}

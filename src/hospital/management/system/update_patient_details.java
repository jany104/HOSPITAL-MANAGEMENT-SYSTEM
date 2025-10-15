package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class update_patient_details extends JFrame {

    private final JComboBox<String> patientSelector;
    private final JTextField roomField;
    private final JTextField checkInField;
    private final JTextField depositField;
    private final JTextField pendingField;

    public update_patient_details() {
        super("Update patient details");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(940, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);
        patientSelector = UIComponents.comboBox();
        roomField = UIComponents.textField(20);
        checkInField = UIComponents.textField(20);
        depositField = UIComponents.textField(20);
        pendingField = UIComponents.textField(20);
        pendingField.setEditable(false);
        pendingField.setBackground(UITheme.SURFACE);

        JPanel layout = UIComponents.pageContainer(
                "Adjust admission",
                "Review and update room assignments or collected amounts",
                buildFormPanel(),
                IllustrationPanel.IllustrationStyle.UPDATE
        );

        add(layout, BorderLayout.CENTER);

        populatePatients();
        populateDetails();

        patientSelector.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            populateDetails();
        });

        setVisible(true);
    }

    private JPanel buildFormPanel() {
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

        addRow(grid, gbc, 0, "Patient", patientSelector);
        addRow(grid, gbc, 1, "Room number", roomField);
        addRow(grid, gbc, 2, "Check-in", checkInField);
        addRow(grid, gbc, 3, "Deposit paid (₹)", depositField);
        addRow(grid, gbc, 4, "Pending amount (₹)", pendingField);

        JButton recalcButton = UIComponents.secondaryButton("Recalculate");
        recalcButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            populateDetails();
        });

        JButton saveButton = UIComponents.primaryButton("Save changes");
        saveButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            persistChanges();
        });

        JButton cancelButton = UIComponents.secondaryButton("Cancel");
        cancelButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            dispose();
        });

        JPanel actions = UIComponents.toolbar(12, cancelButton, recalcButton, saveButton);
        actions.setBorder(new EmptyBorder(8, 0, 0, 0));

        container.add(grid, BorderLayout.CENTER);
        container.add(actions, BorderLayout.SOUTH);

        return container;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(UIComponents.formLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void populatePatients() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        try (conn c = new conn(); ResultSet patients = c.statement.executeQuery("select Name from Patient_Info order by Name")) {
            while (patients.next()) {
                model.addElement(patients.getString("Name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load patients: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        patientSelector.setModel(model);
        if (model.getSize() == 0) {
            patientSelector.addItem("No active patients");
            patientSelector.setEnabled(false);
        }
    }

    private void populateDetails() {
        if (!patientSelector.isEnabled()) {
            roomField.setText("");
            checkInField.setText("");
            depositField.setText("");
            pendingField.setText("");
            return;
        }

        String patientName = Objects.toString(patientSelector.getSelectedItem(), "");
        if (patientName.isEmpty()) {
            return;
        }

        try (conn c = new conn(); PreparedStatement ps = c.connection.prepareStatement("select Room_Number, Time, Deposite from Patient_Info where Name = ?")) {
            ps.setString(1, patientName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    roomField.setText(rs.getString("Room_Number"));
                    checkInField.setText(rs.getString("Time"));
                    depositField.setText(rs.getString("Deposite"));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load patient record: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        recalculatePending();
    }

    private void recalculatePending() {
        String room = roomField.getText().trim();
        String deposited = depositField.getText().trim();
        if (room.isEmpty() || deposited.isEmpty()) {
            pendingField.setText("");
            return;
        }

        try (conn c = new conn(); PreparedStatement ps = c.connection.prepareStatement("select Price from room where room_no = ?")) {
            ps.setString(1, room);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int price = rs.getInt("Price");
                    int paid = Integer.parseInt(deposited);
                    int pending = Math.max(0, price - paid);
                    pendingField.setText(String.valueOf(pending));
                } else {
                    pendingField.setText("—");
                }
            }
        } catch (SQLException | NumberFormatException ex) {
            pendingField.setText("—");
        }
    }

    private void persistChanges() {
        if (!patientSelector.isEnabled()) {
            JOptionPane.showMessageDialog(this, "There are no patient records to update", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String patientName = Objects.toString(patientSelector.getSelectedItem(), "");
        String room = roomField.getText().trim();
        String checkIn = checkInField.getText().trim();
        String deposit = depositField.getText().trim();

        if (patientName.isEmpty() || room.isEmpty() || checkIn.isEmpty() || deposit.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete all editable fields", "Missing information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (conn c = new conn(); PreparedStatement ps = c.connection.prepareStatement(
                "update Patient_Info set Room_Number = ?, Time = ?, Deposite = ? where Name = ?")) {
            ps.setString(1, room);
            ps.setString(2, checkIn);
            ps.setString(3, deposit);
            ps.setString(4, patientName);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Details updated successfully", "Saved", JOptionPane.INFORMATION_MESSAGE);
            recalculatePending();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to update patient: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(update_patient_details::new);
    }
}

package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.regex.Pattern;

public class ALL_Patient_Info extends JFrame {

    private final JTable table;
    private TableRowSorter<TableModel> sorter;
    private final JTextField filterField;
    private final JButton inspectButton;

    public ALL_Patient_Info() {
        super("Patient directory");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    getContentPane().setBackground(UITheme.BACKGROUND);
        table = UIComponents.styledTable();
    table.setAutoCreateRowSorter(false);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    openSelectedPatient();
                }
            }
        });
        sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        filterField = UIComponents.textField(20);
        filterField.setToolTipText("Search by patient name, ID, department, or room.");
        filterField.getDocument().addDocumentListener(createFilterListener());

        inspectButton = UIComponents.secondaryButton("Inspect patient");
        inspectButton.setEnabled(false);
        inspectButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            openSelectedPatient();
        });

        table.getSelectionModel().addListSelectionListener(e ->
            inspectButton.setEnabled(table.getSelectedRow() >= 0)
        );

        loadPatients();

        JPanel layout = UIComponents.pageContainer(
                "Patient directory",
                "Browse and search every admitted patient",
                buildTablePanel(),
                IllustrationPanel.IllustrationStyle.PATIENT
        );

        add(layout, BorderLayout.CENTER);

        setVisible(true);
    }

    private void loadPatients() {
        try (conn c = new conn();
             ResultSet resultSet = c.statement.executeQuery("select * from Patient_Info")) {
            TableModel model = ResultSetTableModelBuilder.buildTableModel(resultSet);
            table.setModel(model);
            if (sorter == null) {
                sorter = new TableRowSorter<>(model);
                table.setRowSorter(sorter);
            } else {
                sorter.setModel(model);
            }
            configureColumns();
            applyFilter(filterField.getText());
            inspectButton.setEnabled(table.getSelectedRow() >= 0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to fetch patient records: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DocumentListener createFilterListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter(filterField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter(filterField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter(filterField.getText());
            }
        };
    }

    private void applyFilter(String query) {
        if (sorter == null) {
            return;
        }
        if (query == null || query.isBlank()) {
            sorter.setRowFilter(null);
            return;
        }
        String expression = Pattern.quote(query.trim());
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + expression));
    }

    private void openSelectedPatient() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a patient first.", "No selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        TableModel model = table.getModel();
        int numberIndex = findColumnIndex(model, "number");
        if (numberIndex < 0) {
            JOptionPane.showMessageDialog(this, "Patient identifier column is missing.", "Unable to inspect", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String patientNumber = sanitizeCell(model.getValueAt(modelRow, numberIndex));
        if (patientNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selected patient record has no identifier.", "Unable to inspect", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PatientDetailsDialog.showFor(this, patientNumber);
    }

    private void configureColumns() {
        TableColumnModel columnModel = table.getColumnModel();
        if (columnModel.getColumnCount() == 0) {
            return;
        }

        int nameIndex = getColumnIndex(columnModel, "Name");
        if (nameIndex > 0) {
            columnModel.moveColumn(nameIndex, 0);
        }

        adjustColumn(columnModel, "Name", null, 220);
        adjustColumn(columnModel, "number", "Patient ID", 150);
        adjustColumn(columnModel, "Disease", "Department", 180);
        adjustColumn(columnModel, "Room_Number", "Room", 110);
        adjustColumn(columnModel, "Time", "Check-in time", 180);
        adjustColumn(columnModel, "Deposite", "Deposit (₹)", 140);

        table.getTableHeader().repaint();
    }

    private void adjustColumn(TableColumnModel columnModel, String identifier, String headerLabel, int preferredWidth) {
        int index = getColumnIndex(columnModel, identifier);
        if (index < 0) {
            return;
        }
        TableColumn column = columnModel.getColumn(index);
        if (headerLabel != null) {
            column.setHeaderValue(headerLabel);
        }
        column.setPreferredWidth(preferredWidth);
    }

    private int getColumnIndex(TableColumnModel columnModel, String identifier) {
        try {
            return columnModel.getColumnIndex(identifier);
        } catch (IllegalArgumentException ex) {
            return -1;
        }
    }

    private int findColumnIndex(TableModel model, String columnName) {
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(model.getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    private String sanitizeCell(Object value) {
        return Objects.toString(value, "").trim();
    }

    private JPanel buildTablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        JPanel tableCard = UIComponents.surfaceCard();
        tableCard.setLayout(new BorderLayout(0, 16));

        JPanel filterRow = new JPanel();
        filterRow.setOpaque(false);
        filterRow.setLayout(new BoxLayout(filterRow, BoxLayout.X_AXIS));
        filterRow.setBorder(new EmptyBorder(0, 4, 0, 4));

        JLabel filterLabel = UIComponents.formLabel("Quick search");
        filterRow.add(filterLabel);
        filterRow.add(Box.createHorizontalStrut(12));

        filterField.setMaximumSize(new Dimension(Integer.MAX_VALUE, filterField.getPreferredSize().height));
        filterRow.add(filterField);
    filterRow.add(Box.createHorizontalGlue());

        tableCard.add(filterRow, BorderLayout.NORTH);
        tableCard.add(UIComponents.tableScroll(table), BorderLayout.CENTER);
        wrapper.add(tableCard, BorderLayout.CENTER);

        JButton refreshButton = UIComponents.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            loadPatients();
        });

        JButton closeButton = UIComponents.secondaryButton("Close");
        closeButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            dispose();
        });

        JPanel actions = UIComponents.buttonBar(refreshButton, inspectButton, closeButton);
        actions.setBorder(new EmptyBorder(8, 24, 0, 24));
        wrapper.add(actions, BorderLayout.SOUTH);

        return wrapper;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ALL_Patient_Info::new);
    }
}

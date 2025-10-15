package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SearchRoom extends JFrame {

    private final JComboBox<String> statusFilter;
    private final JTable table;

    public SearchRoom(){
        super("Search rooms");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(940, 580);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);
        statusFilter = UIComponents.comboBox("Available", "Occupied");

        table = UIComponents.styledTable();
        table.setAutoCreateRowSorter(true);
        loadAllRooms();

        JPanel layout = UIComponents.pageContainer(
                "Search rooms",
                "Pick an availability status to narrow the list",
                buildSearchPanel(),
        IllustrationPanel.IllustrationStyle.SEARCH
        );

        add(layout, BorderLayout.CENTER);

        setVisible(true);
    }

    private void loadAllRooms() {
        try (conn c = new conn(); ResultSet resultSet = c.statement.executeQuery("select * from room")) {
            table.setModel(ResultSetTableModelBuilder.buildTableModel(resultSet));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to load rooms: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        String selected = (String) statusFilter.getSelectedItem();
        if (selected == null) {
            loadAllRooms();
            return;
        }
        String query = "select * from room where Availability = ?";
        try (conn c = new conn(); PreparedStatement ps = c.connection.prepareStatement(query)) {
            ps.setString(1, selected);
            try (ResultSet resultSet = ps.executeQuery()) {
                table.setModel(ResultSetTableModelBuilder.buildTableModel(resultSet));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to filter rooms: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SearchRoom::new);
    }

    private JPanel buildSearchPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        JButton searchButton = UIComponents.primaryButton("Apply filter");
        searchButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            applyFilter();
        });

        JButton resetButton = UIComponents.secondaryButton("Reset");
        resetButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            statusFilter.setSelectedIndex(0);
            loadAllRooms();
        });

    JPanel filterPanel = new JPanel();
    filterPanel.setOpaque(false);
    filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));
    filterPanel.add(UIComponents.formLabel("Status"));
    filterPanel.add(Box.createHorizontalStrut(12));
    filterPanel.add(statusFilter);
    filterPanel.add(Box.createHorizontalStrut(12));
    filterPanel.add(searchButton);
    filterPanel.add(Box.createHorizontalStrut(12));
    filterPanel.add(resetButton);
    filterPanel.add(Box.createHorizontalGlue());
        wrapper.add(filterPanel, BorderLayout.NORTH);

        JPanel tableCard = UIComponents.surfaceCard(UIComponents.tableScroll(table));
        wrapper.add(tableCard, BorderLayout.CENTER);

        JButton closeButton = UIComponents.secondaryButton("Close");
        closeButton.addActionListener(event -> {
            if (event != null) {
                event.getActionCommand();
            }
            dispose();
        });

        JPanel actions = UIComponents.buttonBar(closeButton);
        actions.setBorder(new EmptyBorder(8, 24, 0, 24));
        wrapper.add(actions, BorderLayout.SOUTH);

        return wrapper;
    }
}

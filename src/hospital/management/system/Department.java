package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.ResultSet;

public class Department extends JFrame {

    private final JTable table;

    public Department(){
        super("Hospital departments");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(820, 540);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);
        table = UIComponents.styledTable();
        table.setAutoCreateRowSorter(true);
        loadDepartments();

        JPanel layout = UIComponents.pageContainer(
                "Departments",
                "Contact numbers for each specialty",
                buildTablePanel(),
        IllustrationPanel.IllustrationStyle.DEPARTMENT
        );

        add(layout, BorderLayout.CENTER);

        setVisible(true);

    }

    private void loadDepartments() {
        try (conn c = new conn()) {
            ResultSet resultSet = c.statement.executeQuery("select * from department");
            table.setModel(ResultSetTableModelBuilder.buildTableModel(resultSet));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to fetch department list: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel buildTablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        JPanel tableCard = UIComponents.surfaceCard(UIComponents.tableScroll(table));
        wrapper.add(tableCard, BorderLayout.CENTER);

        JButton refreshButton = UIComponents.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            loadDepartments();
        });

        JButton closeButton = UIComponents.secondaryButton("Close");
        closeButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            dispose();
        });

        JPanel actions = UIComponents.buttonBar(refreshButton, closeButton);
        actions.setBorder(new EmptyBorder(8, 24, 0, 24));
        wrapper.add(actions, BorderLayout.SOUTH);

        return wrapper;
    }

    public static void main(String[] args) {
         SwingUtilities.invokeLater(Department::new);
    }
}

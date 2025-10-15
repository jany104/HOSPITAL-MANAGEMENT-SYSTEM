package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.ResultSet;

public class Ambulance extends JFrame{

    private final JTable table;

    public Ambulance(){
        super("Hospital ambulances");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);
        table = UIComponents.styledTable();
        table.setAutoCreateRowSorter(true);
        loadAmbulances();

        JPanel layout = UIComponents.pageContainer(
                "Emergency fleet",
                "Monitor ambulance availability and locations",
                buildTablePanel(),
                IllustrationPanel.IllustrationStyle.AMBULANCE
        );

        add(layout, BorderLayout.CENTER);

        setVisible(true);
    }

    private void loadAmbulances() {
        try (conn c = new conn()) {
            ResultSet resultSet = c.statement.executeQuery("select * from Ambulance");
            table.setModel(ResultSetTableModelBuilder.buildTableModel(resultSet));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to fetch ambulance records: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            loadAmbulances();
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
        SwingUtilities.invokeLater(Ambulance::new);
    }
}

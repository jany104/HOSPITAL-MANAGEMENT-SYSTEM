package hospital.management.system;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Arrays;

public final class UIComponents {

    private UIComponents() {
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UITheme.mediumFont(16f));
        button.setForeground(Color.WHITE);
        button.setBackground(UITheme.PRIMARY);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 26, 12, 26));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UITheme.regularFont(15f));
        button.setForeground(UITheme.TEXT_PRIMARY);
        button.setBackground(UITheme.BACKGROUND);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 22, 10, 22)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        return button;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.headingFont(26f));
        label.setForeground(UITheme.TEXT_PRIMARY);
        return label;
    }

    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.regularFont(15f));
        label.setForeground(UITheme.TEXT_SECONDARY);
        return label;
    }

    public static JPanel header(String title, String subtitle) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(24, 32, 20, 32));
        wrapper.add(heading(title));
        wrapper.add(Box.createVerticalStrut(6));
        wrapper.add(subtitle(subtitle));
        return wrapper;
    }

    public static JTextField textField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(UITheme.regularFont(16f));
        field.setForeground(UITheme.TEXT_PRIMARY);
        field.setBackground(UITheme.SURFACE);
        field.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        return field;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        field.setFont(UITheme.regularFont(16f));
        field.setForeground(UITheme.TEXT_PRIMARY);
        field.setBackground(UITheme.SURFACE);
        field.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        return field;
    }

    public static JComboBox<String> comboBox(String... values) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        Arrays.stream(values).forEach(model::addElement);
        JComboBox<String> comboBox = new JComboBox<>(model);
        comboBox.setFont(UITheme.regularFont(15f));
        comboBox.setBackground(UITheme.SURFACE);
        comboBox.setForeground(UITheme.TEXT_PRIMARY);
        comboBox.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(4, 6, 4, 6)));
        comboBox.setMaximumRowCount(6);
        return comboBox;
    }

    public static JTable styledTable() {
        JTable table = new JTable();
        table.setFont(UITheme.regularFont(14f));
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setBackground(UITheme.SURFACE);
        table.setRowHeight(24);
        table.setGridColor(UITheme.BORDER);
        table.setSelectionBackground(UITheme.PRIMARY_LIGHT);
        table.setSelectionForeground(UITheme.TEXT_PRIMARY);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setBackground(UITheme.PRIMARY_DARK);
        header.setForeground(Color.WHITE);
        header.setFont(UITheme.mediumFont(15f));
        header.setBorder(new EmptyBorder(8, 10, 8, 10));

        DefaultTableCellRenderer stripedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    component.setBackground(row % 2 == 0 ? UITheme.SURFACE : UITheme.BACKGROUND);
                    component.setForeground(UITheme.TEXT_PRIMARY);
                }
                if (component instanceof JComponent jComponent) {
                    jComponent.setBorder(new EmptyBorder(0, 8, 0, 8));
                }
                return component;
            }
        };
        table.setDefaultRenderer(Object.class, stripedRenderer);
        return table;
    }

    public static JScrollPane smoothScrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.getViewport().setBackground(UITheme.SURFACE);
        scrollPane.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(0, 0, 0, 0)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.getVerticalScrollBar().setBlockIncrement(120);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(22);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(120);
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().putClientProperty("JScrollBar.fastWheelScrolling", Boolean.TRUE);
        scrollPane.getHorizontalScrollBar().putClientProperty("JScrollBar.fastWheelScrolling", Boolean.TRUE);
        return scrollPane;
    }

    public static JScrollPane tableScroll(JTable table) {
        JScrollPane scrollPane = smoothScrollPane(table);
        scrollPane.getViewport().setBackground(UITheme.SURFACE);
        return scrollPane;
    }

    public static JPanel buttonBar(Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 24, 24, 24));
        panel.add(Box.createHorizontalGlue());
        for (int i = 0; i < components.length; i++) {
            panel.add(components[i]);
            if (i < components.length - 1) {
                panel.add(Box.createHorizontalStrut(12));
            }
        }
        return panel;
    }

    public static JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.mediumFont(14f));
        label.setForeground(UITheme.TEXT_PRIMARY);
        return label;
    }

    public static JPanel surfaceCard() {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(24, 24, 24, 24)));
        panel.setLayout(new BorderLayout());
        panel.setOpaque(true);
        return panel;
    }

    public static JPanel surfaceCard(Component center) {
        JPanel panel = surfaceCard();
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel twoColumnLayout(Component left, Component right) {
        JPanel container = new JPanel(new BorderLayout(32, 0));
        container.setOpaque(false);
        container.add(left, BorderLayout.CENTER);
        container.add(right, BorderLayout.EAST);
        return container;
    }

    public static JPanel verticalStack(Component... children) {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        for (int i = 0; i < children.length; i++) {
            stack.add(children[i]);
            if (i < children.length - 1) {
                stack.add(Box.createVerticalStrut(16));
            }
        }
        return stack;
    }

    public static JPanel pageContainer(String title, String subtitle, JComponent body, IllustrationPanel.IllustrationStyle illustrationStyle) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(0, 32, 32, 32));

        JPanel header = UIComponents.header(title, subtitle);
        container.add(header, BorderLayout.NORTH);

        JPanel layout = new JPanel(new BorderLayout(32, 0));
        layout.setOpaque(false);
        layout.add(surfaceCard(body), BorderLayout.CENTER);
        if (illustrationStyle != null) {
            layout.add(new IllustrationPanel(illustrationStyle), BorderLayout.EAST);
        }
        container.add(layout, BorderLayout.CENTER);

        return container;
    }

    public static JPanel toolbar(int gap, Component... components) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        for (int i = 0; i < components.length; i++) {
            if (components[i] != null) {
                panel.add(components[i]);
                if (i < components.length - 1) {
                    panel.add(Box.createHorizontalStrut(gap));
                }
            }
        }
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    public static JPanel infoBadge(String labelText) {
        JLabel label = new JLabel(labelText.toUpperCase());
        label.setFont(UITheme.mediumFont(11f));
        label.setForeground(UITheme.PRIMARY_DARK);

        JPanel badge = new JPanel();
        badge.setOpaque(true);
        badge.setBackground(UITheme.PRIMARY_LIGHT);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        badge.add(label);
        return badge;
    }

    public static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.insets = new Insets(6, 0, 6, 12);
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(formLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }
}

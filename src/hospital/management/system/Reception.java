package hospital.management.system;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Reception extends JFrame {

    private static final int SNAPSHOT_REFRESH_INTERVAL_MS = 60_000;
    private static final int CLOCK_REFRESH_INTERVAL_MS = 1_000;
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a");
    private static final DateTimeFormatter SNAPSHOT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("hh:mm:ss a");

    private JLabel activePatientsValueLabel;
    private JLabel availableRoomsValueLabel;
    private JLabel ambulancesReadyValueLabel;
    private JLabel snapshotTimestampLabel;
    private JLabel snapshotErrorLabel;
    private JLabel heroClockLabel;
    private Timer statsRefreshTimer;
    private Timer heroClockTimer;

    public Reception() {
        super("Reception workspace");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BACKGROUND);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        container.add(buildHeroSection(), BorderLayout.NORTH);

        SnapshotStats stats = loadSnapshotStats();

        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(new EmptyBorder(0, 32, 32, 32));

        mainContent.add(buildStatsSection(stats));
        mainContent.add(Box.createVerticalStrut(24));
        mainContent.add(buildNavigationSection());

        container.add(mainContent, BorderLayout.CENTER);

        JScrollPane scrollPane = UIComponents.smoothScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        startDynamicUpdates();

        setVisible(true);
    }

    private JPanel buildHeroSection() {
        JPanel hero = new JPanel(new BorderLayout());
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(24, 32, 18, 32));

    JLabel title = UIComponents.heading("Reception operations");
    title.setFont(UITheme.headingFont(30f));
        JLabel subtitle = UIComponents.subtitle("Everything you need to coordinate admissions and services");

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(subtitle);
        titleBlock.add(Box.createVerticalStrut(6));

        heroClockLabel = new JLabel(formatHeroClock(LocalDateTime.now()));
        heroClockLabel.setFont(UITheme.mediumFont(13f));
        heroClockLabel.setForeground(UITheme.PRIMARY_DARK);
        heroClockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBlock.add(heroClockLabel);

        JPanel heroMeta = new JPanel();
        heroMeta.setOpaque(false);
        heroMeta.setLayout(new BoxLayout(heroMeta, BoxLayout.X_AXIS));
        heroMeta.add(UIComponents.infoBadge("reception"));
        heroMeta.add(Box.createHorizontalStrut(12));
        heroMeta.add(UIComponents.infoBadge("live ops"));

        hero.add(titleBlock, BorderLayout.WEST);
        hero.add(heroMeta, BorderLayout.EAST);

        return hero;
    }

    private JComponent buildStatsSection(SnapshotStats stats) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel caption = new JLabel("Operational snapshot");
        caption.setFont(UITheme.mediumFont(16f));
        caption.setForeground(UITheme.TEXT_PRIMARY);
        headerRow.add(caption, BorderLayout.WEST);

        JPanel headerMeta = new JPanel();
        headerMeta.setOpaque(false);
        headerMeta.setLayout(new BoxLayout(headerMeta, BoxLayout.X_AXIS));

        snapshotTimestampLabel = new JLabel("Updated –");
        snapshotTimestampLabel.setFont(UITheme.regularFont(13f));
        snapshotTimestampLabel.setForeground(UITheme.MUTED);

        JButton refreshButton = UIComponents.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            refreshSnapshotData();
        });

        headerMeta.add(snapshotTimestampLabel);
        headerMeta.add(Box.createHorizontalStrut(12));
        headerMeta.add(refreshButton);

        headerRow.add(headerMeta, BorderLayout.EAST);

        wrapper.add(headerRow);
        wrapper.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(1, 3, 18, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        activePatientsValueLabel = createValueLabel(stats.activePatients());
        grid.add(createStatCard("Active patients", activePatientsValueLabel, "Currently admitted"));

        availableRoomsValueLabel = createValueLabel(stats.availableRooms());
        grid.add(createStatCard("Available rooms", availableRoomsValueLabel, "Ready for assignment"));

        ambulancesReadyValueLabel = createValueLabel(stats.ambulancesReady());
        grid.add(createStatCard("Ambulances ready", ambulancesReadyValueLabel, "Cleared for dispatch"));

        wrapper.add(grid);

        snapshotErrorLabel = new JLabel();
        snapshotErrorLabel.setFont(UITheme.regularFont(13f));
        snapshotErrorLabel.setForeground(UITheme.MUTED);
        snapshotErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        snapshotErrorLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        snapshotErrorLabel.setVisible(false);
        wrapper.add(snapshotErrorLabel);

        applySnapshot(stats, LocalDateTime.now());

        return wrapper;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String description) {
        JPanel card = UIComponents.surfaceCard();
        card.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(18, 20, 18, 20)));
        card.setLayout(new BorderLayout(0, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.mediumFont(14f));
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setFont(UITheme.regularFont(13f));
        descriptionLabel.setForeground(UITheme.TEXT_SECONDARY);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(descriptionLabel, BorderLayout.SOUTH);
        return card;
    }

    private JLabel createValueLabel(int initialValue) {
        JLabel label = new JLabel(formatCount(initialValue));
        label.setFont(UITheme.headingFont(32f));
        label.setForeground(UITheme.PRIMARY_DARK);
        return label;
    }

    private void startDynamicUpdates() {
        refreshSnapshotData();
        statsRefreshTimer = new Timer(SNAPSHOT_REFRESH_INTERVAL_MS, event -> {
            if (event != null) {
                event.getWhen();
            }
            refreshSnapshotData();
        });
        statsRefreshTimer.start();

        updateHeroClock();
        heroClockTimer = new Timer(CLOCK_REFRESH_INTERVAL_MS, event -> {
            if (event != null) {
                event.getWhen();
            }
            updateHeroClock();
        });
        heroClockTimer.start();
    }

    private void refreshSnapshotData() {
        SnapshotStats stats = loadSnapshotStats();
        applySnapshot(stats, LocalDateTime.now());
    }

    private void applySnapshot(SnapshotStats stats, LocalDateTime timestamp) {
        if (activePatientsValueLabel != null) {
            activePatientsValueLabel.setText(formatCount(stats.activePatients()));
        }
        if (availableRoomsValueLabel != null) {
            availableRoomsValueLabel.setText(formatCount(stats.availableRooms()));
        }
        if (ambulancesReadyValueLabel != null) {
            ambulancesReadyValueLabel.setText(formatCount(stats.ambulancesReady()));
        }
        if (snapshotTimestampLabel != null) {
            snapshotTimestampLabel.setText("Updated " + formatSnapshotTimestamp(timestamp));
        }
        if (snapshotErrorLabel != null) {
            if (stats.hasError()) {
                String message = stats.errorMessage() != null ? stats.errorMessage() : "Check database connection.";
                snapshotErrorLabel.setText("Live data unavailable: " + message);
                snapshotErrorLabel.setVisible(true);
            } else {
                snapshotErrorLabel.setVisible(false);
            }
        }
    }

    private void updateHeroClock() {
        if (heroClockLabel != null) {
            heroClockLabel.setText(formatHeroClock(LocalDateTime.now()));
        }
    }

    private String formatHeroClock(LocalDateTime time) {
        return time.format(CLOCK_FORMAT);
    }

    private String formatSnapshotTimestamp(LocalDateTime time) {
        return time.format(SNAPSHOT_TIMESTAMP_FORMAT);
    }

    @Override
    public void dispose() {
        if (statsRefreshTimer != null && statsRefreshTimer.isRunning()) {
            statsRefreshTimer.stop();
        }
        if (heroClockTimer != null && heroClockTimer.isRunning()) {
            heroClockTimer.stop();
        }
        super.dispose();
    }

    private JPanel buildNavigationSection() {
        JPanel wrapper = new JPanel(new BorderLayout(24, 0));
        wrapper.setOpaque(false);

        JPanel navigationCard = UIComponents.surfaceCard();
        navigationCard.setLayout(new BorderLayout(0, 18));

        JPanel navigationHeader = new JPanel();
        navigationHeader.setOpaque(false);
        navigationHeader.setLayout(new BoxLayout(navigationHeader, BoxLayout.Y_AXIS));

        JLabel headerTitle = new JLabel("Workspace shortcuts");
        headerTitle.setFont(UITheme.mediumFont(16f));
        headerTitle.setForeground(UITheme.TEXT_PRIMARY);
        headerTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headerSubtitle = new JLabel("Open a module to begin");
        headerSubtitle.setFont(UITheme.regularFont(13f));
        headerSubtitle.setForeground(UITheme.TEXT_SECONDARY);
        headerSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        navigationHeader.add(headerTitle);
        navigationHeader.add(Box.createVerticalStrut(4));
        navigationHeader.add(headerSubtitle);
        navigationHeader.setBorder(new EmptyBorder(0, 0, 0, 0));

        navigationCard.add(navigationHeader, BorderLayout.NORTH);
        navigationCard.add(buildNavigationGrid(), BorderLayout.CENTER);

        wrapper.add(navigationCard, BorderLayout.CENTER);

    IllustrationPanel illustration = new IllustrationPanel(IllustrationPanel.IllustrationStyle.HERO);
        illustration.setPreferredSize(new Dimension(280, 360));
        wrapper.add(illustration, BorderLayout.EAST);

        return wrapper;
    }

    private JPanel buildNavigationGrid() {
        JPanel navigation = new JPanel(new GridLayout(5, 2, 18, 18));
        navigation.setOpaque(false);

        int order = 0;
        navigation.add(createNavCard("Register patient", "Record arrival details and allocate rooms", IllustrationPanel.IllustrationStyle.ADMISSION, NEW_PATIENT::new, order++));
        navigation.add(createNavCard("Patient directory", "Review or update active patients", IllustrationPanel.IllustrationStyle.PATIENT, ALL_Patient_Info::new, order++));
    navigation.add(createNavCard("Room availability", "Monitor occupancy in real-time", IllustrationPanel.IllustrationStyle.ROOM, Room::new, order++));
    navigation.add(createNavCard("Search rooms", "Locate rooms by status or number", IllustrationPanel.IllustrationStyle.SEARCH, SearchRoom::new, order++));
        navigation.add(createNavCard("Discharge", "Close stays and free capacity", IllustrationPanel.IllustrationStyle.DISCHARGE, patient_discharge::new, order++));
        navigation.add(createNavCard("Update details", "Modify patient records and deposits", IllustrationPanel.IllustrationStyle.UPDATE, update_patient_details::new, order++));
        navigation.add(createNavCard("Manage staff", "Add, update, or remove team members", IllustrationPanel.IllustrationStyle.STAFF, Employee_info::new, order++));
    navigation.add(createNavCard("Departments", "Connect with hospital specialties", IllustrationPanel.IllustrationStyle.DEPARTMENT, Department::new, order++));
        navigation.add(createNavCard("Ambulances", "Track emergency vehicle readiness", IllustrationPanel.IllustrationStyle.AMBULANCE, Ambulance::new, order++));
    navigation.add(createNavCard("Log out", "Return to operator login", IllustrationPanel.IllustrationStyle.LOGOUT, this::logout, order));

        return navigation;
    }

    private AnimatedCard createNavCard(String title, String subtitle, IllustrationPanel.IllustrationStyle style, Runnable action, int order) {
        AnimatedCard card = new AnimatedCard();
        card.setLayout(new BorderLayout(24, 0));
        card.setPreferredSize(new Dimension(0, 200));
        card.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(28, 28, 28, 28)));
        card.scheduleIntro(order * 60);

        JPanel illustrationWrapper = new JPanel(new BorderLayout());
        illustrationWrapper.setOpaque(false);

        IllustrationPanel illustration = new IllustrationPanel(style);
        illustration.setPreferredSize(new Dimension(190, 190));
        illustration.setMaximumSize(new Dimension(210, Integer.MAX_VALUE));
        illustrationWrapper.add(illustration, BorderLayout.CENTER);

        card.add(illustrationWrapper, BorderLayout.EAST);

        JLabel heading = UIComponents.heading(title);
        JLabel subheading = UIComponents.subtitle(subtitle);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBorder(new EmptyBorder(0, 0, 0, 18));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        subheading.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(heading);
        text.add(Box.createVerticalStrut(6));
        text.add(subheading);

        JPanel ctaRow = new JPanel();
        ctaRow.setOpaque(false);
        ctaRow.setLayout(new BoxLayout(ctaRow, BoxLayout.X_AXIS));
        ctaRow.setBorder(new EmptyBorder(16, 0, 0, 0));

        JLabel actionLabel = new JLabel("Open workspace");
        actionLabel.setFont(UITheme.mediumFont(13f));
        actionLabel.setForeground(UITheme.PRIMARY_DARK);

        JLabel arrowLabel = new JLabel("→");
        arrowLabel.setFont(UITheme.headingFont(18f));
        arrowLabel.setForeground(UITheme.PRIMARY);
        arrowLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        ctaRow.add(actionLabel);
        ctaRow.add(arrowLabel);
        ctaRow.add(Box.createHorizontalGlue());

        ctaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(ctaRow);

        card.add(text, BorderLayout.CENTER);

        java.awt.event.MouseAdapter hoverAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.triggerHover(true);
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!isPointerInside(card, e)) {
                    card.triggerHover(false);
                    card.setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        card.addMouseListener(hoverAdapter);
        illustrationWrapper.addMouseListener(hoverAdapter);
        text.addMouseListener(hoverAdapter);
        ctaRow.addMouseListener(hoverAdapter);
        actionLabel.addMouseListener(hoverAdapter);
        arrowLabel.addMouseListener(hoverAdapter);

        java.awt.event.MouseAdapter clickAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                action.run();
            }
        };

        card.addMouseListener(clickAdapter);
        illustrationWrapper.addMouseListener(clickAdapter);
        text.addMouseListener(clickAdapter);
        ctaRow.addMouseListener(clickAdapter);
        actionLabel.addMouseListener(clickAdapter);
        arrowLabel.addMouseListener(clickAdapter);

        return card;
    }

    private SnapshotStats loadSnapshotStats() {
        int patientCount = 0;
        int availableRooms = 0;
        int ambulancesReady = 0;
        String errorMessage = null;

        try (conn c = new conn()) {
            patientCount = countQuery(c, "select count(*) from Patient_Info");
            availableRooms = countQuery(c, "select count(*) from room where Availability = 'Available'");
            ambulancesReady = countQuery(c, "select count(*) from Ambulance where Available = 'Yes'");
        } catch (Exception ex) {
            errorMessage = "Check database connection.";
            System.err.println("Unable to load snapshot stats: " + ex.getMessage());
        }

        return new SnapshotStats(patientCount, availableRooms, ambulancesReady, errorMessage);
    }

    private int countQuery(conn connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    private String formatCount(int value) {
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        return formatter.format(Math.max(value, 0));
    }

    private void logout() {
        dispose();
        new Login();
    }

    private static class AnimatedCard extends JPanel {
        private float hoverProgress = 0f;
        private float hoverTarget = 0f;
        private float introProgress = 0f;
        private float introTarget = 1f;
        private final Timer animationTimer;

        AnimatedCard() {
            setOpaque(false);
            animationTimer = new Timer(16, event -> {
                if (event != null) {
                    event.getWhen();
                }
                stepAnimation();
            });
        }

        void triggerHover(boolean hovering) {
            hoverTarget = hovering ? 1f : 0f;
            startAnimation();
        }

        void scheduleIntro(int delayMs) {
            introProgress = 0f;
            introTarget = 1f;
            Timer startTimer = new Timer(Math.max(0, delayMs), e -> {
                startAnimation();
                ((Timer) e.getSource()).stop();
            });
            startTimer.setRepeats(false);
            startTimer.start();
        }

        private void startAnimation() {
            if (!animationTimer.isRunning()) {
                animationTimer.start();
            }
        }

        private void stepAnimation() {
            boolean continueAnimation = false;
            hoverProgress = approach(hoverProgress, hoverTarget);
            introProgress = approach(introProgress, introTarget);
            if (Math.abs(hoverProgress - hoverTarget) > 0.001f) {
                continueAnimation = true;
            }
            if (Math.abs(introProgress - introTarget) > 0.001f) {
                continueAnimation = true;
            }
            if (!continueAnimation) {
                animationTimer.stop();
            }
            repaint();
        }

        private float approach(float value, float target) {
            float delta = target - value;
            if (Math.abs(delta) < 0.01f) {
                return target;
            }
            return value + delta * 0.18f;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float easedHover = (float) Math.pow(Math.max(0f, Math.min(1f, hoverProgress)), 0.6);
            Color background = interpolate(UITheme.SURFACE, UITheme.PRIMARY_LIGHT, easedHover);
            g2.setColor(background);
            int arc = 28;
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            if (easedHover > 0f) {
                g2.setComposite(AlphaComposite.SrcOver.derive(0.08f * easedHover));
                g2.setColor(UITheme.PRIMARY_DARK);
                g2.fillRoundRect(6, 6, Math.max(0, getWidth() - 12), Math.max(0, getHeight() - 12), arc - 6, arc - 6);
            }

            g2.dispose();

            Graphics2D contentGraphics = (Graphics2D) g.create();
            float translateY = (1f - Math.max(0f, Math.min(1f, introProgress))) * 24f;
            float alpha = Math.max(0f, Math.min(1f, introProgress));
            contentGraphics.translate(0, translateY);
            contentGraphics.setComposite(AlphaComposite.SrcOver.derive(alpha));
            super.paintComponent(contentGraphics);
            contentGraphics.dispose();
        }

        private Color interpolate(Color start, Color end, float factor) {
            factor = Math.max(0f, Math.min(1f, factor));
            int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * factor);
            int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * factor);
            int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * factor);
            int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * factor);
            return new Color(r, g, b, a);
        }
    }

    private static boolean isPointerInside(AnimatedCard card, java.awt.event.MouseEvent event) {
        Component source = event.getComponent();
        Point point = SwingUtilities.convertPoint(source, event.getPoint(), card);
        return card.contains(point);
    }

    private record SnapshotStats(int activePatients, int availableRooms, int ambulancesReady, String errorMessage) {
        boolean hasError() {
            return errorMessage != null && !errorMessage.isBlank();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Reception::new);
    }
}

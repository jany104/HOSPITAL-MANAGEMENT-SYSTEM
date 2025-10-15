package hospital.management.system;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

public class Login extends JFrame implements ActionListener {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton signInButton;
    private final JButton clearButton;

    public Login() {
        super("CareSphere Hospital Management");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 540);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new BorderLayout());

        GradientPanel heroPanel = new GradientPanel(UITheme.PRIMARY_DARK, UITheme.PRIMARY);
        heroPanel.setPreferredSize(new Dimension(360, 540));
        heroPanel.setLayout(new BorderLayout());
        heroPanel.setBorder(new EmptyBorder(36, 36, 36, 36));

        JPanel heroHeader = new JPanel();
        heroHeader.setOpaque(false);
        heroHeader.setLayout(new BoxLayout(heroHeader, BoxLayout.Y_AXIS));

        JLabel heroTitle = new JLabel("CareSphere Hospital");
        heroTitle.setFont(UITheme.headingFont(30f));
        heroTitle.setForeground(Color.WHITE);
        heroTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel heroSubtitle = new JLabel("Coordinated systems for exceptional patient care");
        heroSubtitle.setFont(UITheme.regularFont(16f));
        heroSubtitle.setForeground(new Color(255, 255, 255, 210));

        JPanel heroHighlights = new JPanel();
        heroHighlights.setOpaque(false);
        heroHighlights.setLayout(new BoxLayout(heroHighlights, BoxLayout.Y_AXIS));
        heroHighlights.setBorder(new EmptyBorder(16, 0, 0, 0));

        heroHighlights.add(createHeroBullet("Real-time bed availability"));
        heroHighlights.add(Box.createVerticalStrut(10));
        heroHighlights.add(createHeroBullet("Staff coordination dashboards"));
        heroHighlights.add(Box.createVerticalStrut(10));
        heroHighlights.add(createHeroBullet("Secure patient record access"));

        heroHeader.add(heroTitle);
        heroHeader.add(heroSubtitle);
        heroHeader.add(heroHighlights);

        heroPanel.add(heroHeader, BorderLayout.NORTH);
        heroPanel.add(new IllustrationPanel(IllustrationPanel.IllustrationStyle.HOSPITAL), BorderLayout.CENTER);

        JPanel rightContainer = new JPanel(new GridBagLayout());
        rightContainer.setOpaque(false);
        rightContainer.setBorder(new EmptyBorder(40, 48, 40, 48));

        JPanel formCard = UIComponents.surfaceCard();
        formCard.setPreferredSize(new Dimension(420, 0));

        JPanel cardContent = new JPanel();
        cardContent.setOpaque(false);
        cardContent.setLayout(new BoxLayout(cardContent, BoxLayout.Y_AXIS));

        JLabel cardTitle = UIComponents.heading("Operator sign in");
        JLabel cardSubtitle = UIComponents.subtitle("Use your assigned CareSphere credentials");
        cardSubtitle.setBorder(new EmptyBorder(4, 0, 16, 0));

        cardContent.add(cardTitle);
        cardContent.add(cardSubtitle);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        usernameField = UIComponents.textField(20);
        passwordField = UIComponents.passwordField(20);

        gbc.gridy = 0;
        formPanel.add(UIComponents.formLabel("Username"), gbc);

        gbc.gridy = 1;
        formPanel.add(usernameField, gbc);

        gbc.gridy = 2;
        formPanel.add(UIComponents.formLabel("Password"), gbc);

        gbc.gridy = 3;
        formPanel.add(passwordField, gbc);

        gbc.gridy = 4;
        JCheckBox rememberMe = new JCheckBox("Remember me on this device");
        rememberMe.setFont(UITheme.regularFont(13f));
        rememberMe.setForeground(UITheme.TEXT_SECONDARY);
        rememberMe.setOpaque(false);
        formPanel.add(rememberMe, gbc);

        cardContent.add(formPanel);

        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));
        buttonRow.setBorder(new EmptyBorder(20, 0, 0, 0));

        signInButton = UIComponents.primaryButton("Sign in");
        signInButton.addActionListener(this);
        clearButton = UIComponents.secondaryButton("Clear");
        clearButton.addActionListener(this);
        JButton exitButton = UIComponents.secondaryButton("Exit");
        exitButton.addActionListener(e -> {
            if (e != null) {
                e.getActionCommand();
            }
            System.exit(0);
        });

        buttonRow.add(signInButton);
        buttonRow.add(Box.createHorizontalStrut(12));
        buttonRow.add(clearButton);
        buttonRow.add(Box.createHorizontalStrut(12));
        buttonRow.add(exitButton);

        cardContent.add(buttonRow);

        JLabel supportLabel = new JLabel("Need access? Contact administration");
        supportLabel.setFont(UITheme.regularFont(12f));
        supportLabel.setForeground(UITheme.TEXT_SECONDARY);
        supportLabel.setBorder(new EmptyBorder(18, 0, 0, 0));
        cardContent.add(supportLabel);

        formCard.add(cardContent, BorderLayout.CENTER);

        GridBagConstraints cardConstraints = new GridBagConstraints();
        cardConstraints.gridx = 0;
        cardConstraints.gridy = 0;
        cardConstraints.weightx = 1;
        cardConstraints.weighty = 1;
        cardConstraints.fill = GridBagConstraints.BOTH;
        rightContainer.add(formCard, cardConstraints);

        add(heroPanel, BorderLayout.WEST);
        add(rightContainer, BorderLayout.CENTER);

        usernameField.requestFocusInWindow();
        setVisible(true);
    }

    private JLabel createHeroBullet(String text) {
        JLabel label = new JLabel("\u2022 " + text);
        label.setFont(UITheme.regularFont(14f));
        label.setForeground(new Color(255, 255, 255, 220));
        return label;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == signInButton) {
            performLogin();
        } else if (source == clearButton) {
            usernameField.setText("");
            passwordField.setText("");
            usernameField.requestFocusInWindow();
        }
    }

    private void performLogin() {
        char[] passwordChars = null;
        try (conn c = new conn()) {
            String user = usernameField.getText().trim();
            passwordChars = passwordField.getPassword();
            String pass = new String(passwordChars);

            String query = "select * from login where ID = ? and PW = ?";
            try (PreparedStatement preparedStatement = c.connection.prepareStatement(query)) {
                preparedStatement.setString(1, user);
                preparedStatement.setString(2, pass);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        new Reception();
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid credentials", "Authentication failed", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to verify credentials: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (passwordChars != null) {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Login::new);
    }
}

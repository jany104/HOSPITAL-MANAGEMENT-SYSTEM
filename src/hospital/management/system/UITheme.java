package hospital.management.system;

import java.awt.*;

public final class UITheme {

    public static final Color PRIMARY = new Color(34, 102, 141);
    public static final Color PRIMARY_DARK = new Color(23, 71, 104);
    public static final Color PRIMARY_LIGHT = new Color(211, 229, 241);
    public static final Color ACCENT = new Color(46, 160, 135);
    public static final Color SURFACE = new Color(252, 253, 255);
    public static final Color BACKGROUND = new Color(242, 246, 250);
    public static final Color ELEVATED = new Color(255, 255, 255, 245);
    public static final Color BORDER = new Color(214, 220, 228);
    public static final Color MUTED = new Color(94, 112, 133);
    public static final Color TEXT_PRIMARY = new Color(22, 37, 56);
    public static final Color TEXT_SECONDARY = new Color(96, 112, 133);

    private static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private UITheme() {
    }

    public static Font regularFont(float size) {
        return BASE_FONT.deriveFont(Font.PLAIN, size);
    }

    public static Font mediumFont(float size) {
        return BASE_FONT.deriveFont(Font.BOLD, size);
    }

    public static Font semiboldFont(float size) {
        return BASE_FONT.deriveFont(Font.BOLD, size + 1f);
    }

    public static Font headingFont(float size) {
        return BASE_FONT.deriveFont(Font.BOLD, size);
    }

    public static Font monospace(float size) {
        return new Font(Font.MONOSPACED, Font.PLAIN, (int) size);
    }
}

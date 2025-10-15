package hospital.management.system;

import javax.swing.*;
import java.awt.*;

public class GradientPanel extends JPanel {

    private final Color start;
    private final Color end;

    public GradientPanel(Color start, Color end) {
        this.start = start;
        this.end = end;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gradientPaint = new GradientPaint(0, 0, start, getWidth(), getHeight(), end);
        g2.setPaint(gradientPaint);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
        g2.dispose();
    }
}

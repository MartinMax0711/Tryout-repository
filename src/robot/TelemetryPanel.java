package robot;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Supplier;

class TelemetryPanel extends JPanel {

    private static final Color BG    = new Color(0x0D, 0x10, 0x14);
    private static final Color LABEL = new Color(0x76, 0x83, 0x93);
    private static final Color VALUE = new Color(0xE6, 0xEC, 0xF3);
    private static final Color TITLE = new Color(0x8A, 0xB4, 0xF8);
    private static final Color BAR_BG = new Color(0x1B, 0x21, 0x29);

    private final Robot robot;
    private final Supplier<String> status;

    TelemetryPanel(Robot robot, Supplier<String> status) {
        this.robot = robot;
        this.status = status;
        setBackground(BG);
        setPreferredSize(new Dimension(320, 720));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int x = 18;
        int y = 34;
        int w = getWidth() - 36;

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g2.setColor(TITLE);
        g2.drawString("ROBOT TELEMETRY", x, y);
        y += 10;
        g2.setColor(new Color(0x1E, 0x25, 0x2E));
        g2.drawLine(x, y, x + w, y);
        y += 26;

        y = row(g2, x, y, w, "Alliance", String.valueOf(Constants.alliance));
        y = row(g2, x, y, w, "Status", status.get());
        y = row(g2, x, y, w, "Runtime", String.format("%.2f s", robot.getRuntime()));
        y = row(g2, x, y, w, "Position X", String.format("%.2f in", robot.getX()));
        y = row(g2, x, y, w, "Position Y", String.format("%.2f in", robot.getY()));
        y = row(g2, x, y, w, "Heading", String.format("%.1f°", robot.getHeadingDeg()));
        y = row(g2, x, y, w, "Speed", String.format("%.1f in/s", robot.getSpeed()));
        y = row(g2, x, y, w, "Turn rate", String.format("%.1f °/s", robot.getTurnRateDegPerSec()));

        y += 16;
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g2.setColor(TITLE);
        g2.drawString("MOTORS", x, y);
        y += 14;

        for (Motor m : robot.motors) {
            y = motorRow(g2, x, y, w, m);
        }

        y += 12;
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g2.setColor(TITLE);
        g2.drawString("LOG", x, y);
        y += 16;

        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        List<String> lines = robot.getLogLines();
        for (String line : lines) {
            if (y > getHeight() - 12) break;
            g2.setColor(VALUE);
            g2.drawString(line, x, y);
            y += 15;
        }

        g2.dispose();
    }

    private int row(Graphics2D g2, int x, int y, int w, String label, String value) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g2.setColor(LABEL);
        g2.drawString(label, x, y);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        g2.setColor(VALUE);
        int tw = g2.getFontMetrics().stringWidth(value);
        g2.drawString(value, x + w - tw, y);
        return y + 22;
    }

    private int motorRow(Graphics2D g2, int x, int y, int w, Motor m) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g2.setColor(LABEL);
        g2.drawString(m.getName(), x, y + 10);

        String info = String.format("%+.2f  %7.0f tick  %6.1f in",
                m.getPower(), m.getTicks(), m.getDistance());
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        g2.setColor(VALUE);
        int tw = g2.getFontMetrics().stringWidth(info);
        g2.drawString(info, x + w - tw, y + 10);

        int barY = y + 16;
        int barH = 7;
        g2.setColor(BAR_BG);
        g2.fill(new Rectangle2D.Double(x, barY, w, barH));
        double mid = x + w / 2.0;
        double len = Math.abs(m.getPower()) * (w / 2.0);
        g2.setColor(FieldPanel.powerColor(m.getPower() >= 0 ? 1 : -1));
        if (m.getPower() >= 0) {
            g2.fill(new Rectangle2D.Double(mid, barY, len, barH));
        } else {
            g2.fill(new Rectangle2D.Double(mid - len, barY, len, barH));
        }
        g2.setColor(new Color(0x3A, 0x44, 0x52));
        g2.drawLine((int) mid, barY - 1, (int) mid, barY + barH + 1);

        return y + 34;
    }
}

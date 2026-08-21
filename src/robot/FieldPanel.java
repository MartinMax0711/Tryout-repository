package robot;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * 空白场地的画面 / the blank field view.
 * 你不需要修改这个文件。/ You do not need to edit this file.
 */
class FieldPanel extends JPanel {

    private static final Color BG        = new Color(0x11, 0x14, 0x18);
    private static final Color GRID      = new Color(0x22, 0x28, 0x30);
    private static final Color GRID_MAIN = new Color(0x2E, 0x38, 0x44);
    private static final Color BORDER    = new Color(0x55, 0x62, 0x72);
    private static final Color TRAIL     = new Color(0x3D, 0x8B, 0xFD, 0x99);
    private static final Color BODY      = new Color(0x1E, 0x2A, 0x38);
    private static final Color BODY_EDGE = new Color(0x8A, 0xB4, 0xF8);

    private final Robot robot;

    FieldPanel(Robot robot) {
        this.robot = robot;
        setBackground(BG);
        setPreferredSize(new Dimension(720, 720));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double size = Math.min(getWidth(), getHeight()) - 24;
        double scale = size / Robot.FIELD_SIZE_CM;
        double ox = (getWidth() - size) / 2.0;
        double oy = (getHeight() - size) / 2.0;

        drawGrid(g2, ox, oy, size, scale);
        drawTrail(g2, ox, oy, size, scale);
        drawRobot(g2, ox, oy, size, scale);

        g2.dispose();
    }

    /** 场地坐标 (cm) -> 屏幕坐标 (px) */
    private double sx(double xCm, double ox, double scale) { return ox + xCm * scale; }
    private double sy(double yCm, double oy, double size, double scale) { return oy + size - yCm * scale; }

    private void drawGrid(Graphics2D g2, double ox, double oy, double size, double scale) {
        g2.setColor(new Color(0x16, 0x1A, 0x20));
        g2.fill(new Rectangle2D.Double(ox, oy, size, size));

        for (int cm = 0; cm <= (int) Robot.FIELD_SIZE_CM; cm += 25) {
            boolean main = cm % 100 == 0;
            g2.setColor(main ? GRID_MAIN : GRID);
            g2.setStroke(new BasicStroke(main ? 1.4f : 0.8f));
            double px = sx(cm, ox, scale);
            double py = sy(cm, oy, size, scale);
            g2.drawLine((int) px, (int) oy, (int) px, (int) (oy + size));
            g2.drawLine((int) ox, (int) py, (int) (ox + size), (int) py);
        }

        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new Rectangle2D.Double(ox, oy, size, size));

        g2.setColor(new Color(0x5A, 0x66, 0x76));
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        for (int cm = 0; cm <= (int) Robot.FIELD_SIZE_CM; cm += 100) {
            if (cm < Robot.FIELD_SIZE_CM) {          // 最后一个标签会顶到边框 / would clip
                g2.drawString(String.valueOf(cm), (float) (sx(cm, ox, scale) + 3),
                        (float) (oy + size - 4));
            }
            if (cm > 0) {
                g2.drawString(String.valueOf(cm), (float) (ox + 4),
                        (float) (sy(cm, oy, size, scale) - 3));
            }
        }
    }

    private void drawTrail(Graphics2D g2, double ox, double oy, double size, double scale) {
        List<double[]> trail = robot.getTrail();
        if (trail.size() < 2) return;
        Path2D.Double path = new Path2D.Double();
        double[] p0 = trail.get(0);
        path.moveTo(sx(p0[0], ox, scale), sy(p0[1], oy, size, scale));
        for (int i = 1; i < trail.size(); i++) {
            double[] p = trail.get(i);
            path.lineTo(sx(p[0], ox, scale), sy(p[1], oy, size, scale));
        }
        g2.setColor(TRAIL);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private void drawRobot(Graphics2D g2, double ox, double oy, double size, double scale) {
        double cx = sx(robot.getX(), ox, scale);
        double cy = sy(robot.getY(), oy, size, scale);

        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(-robot.getHeading());   // 屏幕 y 向下，所以取负 / screen y points down

        double L = Robot.BODY_LENGTH_CM * scale;
        double W = Robot.BODY_WIDTH_CM * scale;

        // 车身 / chassis
        g2.setColor(BODY);
        g2.fill(new Rectangle2D.Double(-L / 2, -W / 2, L, W));
        g2.setColor(BODY_EDGE);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new Rectangle2D.Double(-L / 2, -W / 2, L, W));

        // 车头方向 / front marker
        Path2D.Double nose = new Path2D.Double();
        nose.moveTo(L / 2 - 2, -W * 0.18);
        nose.lineTo(L / 2 + W * 0.20, 0);
        nose.lineTo(L / 2 - 2, W * 0.18);
        nose.closePath();
        g2.setColor(BODY_EDGE);
        g2.fill(nose);

        // 四个轮子，颜色跟着功率变 / four wheels, coloured by motor power
        double wl = 13 * scale, ww = 5.5 * scale;
        drawWheel(g2, robot.frontLeft,   15 * scale, -16 * scale, wl, ww);
        drawWheel(g2, robot.frontRight,  15 * scale,  16 * scale, wl, ww);
        drawWheel(g2, robot.backLeft,   -15 * scale, -16 * scale, wl, ww);
        drawWheel(g2, robot.backRight,  -15 * scale,  16 * scale, wl, ww);

        g2.setTransform(old);
    }

    /** 注意：车体坐标 y 向左为正，屏幕里要取负 / body +y is left, screen y is down */
    private void drawWheel(Graphics2D g2, Motor motor, double bx, double byLeft,
                           double len, double wid) {
        double px = bx, py = byLeft;
        Rectangle2D.Double r = new Rectangle2D.Double(px - len / 2, py - wid / 2, len, wid);
        g2.setColor(powerColor(motor.getPower()));
        g2.fill(r);
        g2.setColor(new Color(0x0B, 0x0E, 0x12));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(r);
    }

    static Color powerColor(double power) {
        double p = Math.max(-1, Math.min(1, power));
        if (p >= 0) {
            return blend(new Color(0x3A, 0x40, 0x48), new Color(0x35, 0xD0, 0x7F), p);
        }
        return blend(new Color(0x3A, 0x40, 0x48), new Color(0xE5, 0x5A, 0x4E), -p);
    }

    private static Color blend(Color a, Color b, double t) {
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }
}

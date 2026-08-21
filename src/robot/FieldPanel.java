package robot;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

class FieldPanel extends JPanel {

    private static final Color BG        = new Color(0x11, 0x14, 0x18);
    private static final Color TILE      = new Color(0x18, 0x1D, 0x24);
    private static final Color TILE_EDGE = new Color(0x27, 0x2F, 0x3A);
    private static final Color BORDER    = new Color(0x5C, 0x6A, 0x7A);
    private static final Color BODY      = new Color(0x1E, 0x2A, 0x38);
    private static final Color RED_A     = new Color(0xF0, 0x5A, 0x5A);
    private static final Color BLUE_A    = new Color(0x5A, 0x93, 0xF0);
    private static final Color WAYPOINT  = new Color(0xC8, 0xD2, 0xE0);

    private final Robot robot;

    FieldPanel(Robot robot) {
        this.robot = robot;
        setBackground(BG);
        setPreferredSize(new Dimension(740, 740));
    }

    private Color allianceColor() {
        return Constants.alliance == Alliance.BLUE ? BLUE_A : RED_A;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double size = Math.min(getWidth(), getHeight()) - 28;
        double scale = size / Constants.FIELD_SIZE;
        double ox = (getWidth() - size) / 2.0;
        double oy = (getHeight() - size) / 2.0;

        drawField(g2, ox, oy, size, scale);
        MyPaths paths = MyPaths.current;
        if (paths != null) {
            drawPreview(g2, paths, ox, oy, size, scale);
            drawWaypoints(g2, paths, ox, oy, size, scale);
        }
        drawTrail(g2, ox, oy, size, scale);
        drawRobot(g2, ox, oy, size, scale);

        g2.dispose();
    }

    private double sx(double xIn, double ox, double scale) { return ox + xIn * scale; }
    private double sy(double yIn, double oy, double size, double scale) { return oy + size - yIn * scale; }

    private void drawField(Graphics2D g2, double ox, double oy, double size, double scale) {
        g2.setColor(TILE);
        g2.fill(new Rectangle2D.Double(ox, oy, size, size));

        g2.setColor(TILE_EDGE);
        g2.setStroke(new BasicStroke(1f));
        for (double in = Constants.TILE; in < Constants.FIELD_SIZE; in += Constants.TILE) {
            double px = sx(in, ox, scale), py = sy(in, oy, size, scale);
            g2.draw(new java.awt.geom.Line2D.Double(px, oy, px, oy + size));
            g2.draw(new java.awt.geom.Line2D.Double(ox, py, ox + size, py));
        }

        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(new Rectangle2D.Double(ox, oy, size, size));

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g2.setColor(new Color(0x5A, 0x66, 0x76));
        for (int in = 0; in <= (int) Constants.FIELD_SIZE; in += 24) {
            if (in < Constants.FIELD_SIZE) {
                g2.drawString(String.valueOf(in), (float) (sx(in, ox, scale) + 3),
                        (float) (oy + size - 4));
            }
            if (in > 0) {
                g2.drawString(String.valueOf(in), (float) (ox + 4),
                        (float) (sy(in, oy, size, scale) - 3));
            }
        }
    }

    private void drawPreview(Graphics2D g2, MyPaths paths, double ox, double oy,
                             double size, double scale) {
        Color c = allianceColor();
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Path path : paths.all()) {
            BezierCurve curve = path.getCurve();
            Path2D.Double p2 = new Path2D.Double();
            for (int i = 0; i <= 60; i++) {
                Pose pt = curve.pointAt(i / 60.0);
                double px = sx(pt.x, ox, scale), py = sy(pt.y, oy, size, scale);
                if (i == 0) p2.moveTo(px, py); else p2.lineTo(px, py);
            }
            g2.draw(p2);
        }
    }

    private void drawWaypoints(Graphics2D g2, MyPaths paths, double ox, double oy,
                               double size, double scale) {
        Pose[] poses = paths.labelledPoses();
        String[] labels = paths.labels();
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        for (int i = 0; i < poses.length; i++) {
            Pose p = poses[i];
            double px = sx(p.x, ox, scale), py = sy(p.y, oy, size, scale);
            boolean goal = labels[i].equals("GOAL");

            if (goal) {
                g2.setColor(allianceColor());
                g2.fill(new Ellipse2D.Double(px - 7, py - 7, 14, 14));
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(new Ellipse2D.Double(px - 7, py - 7, 14, 14));
            } else {
                g2.setColor(WAYPOINT);
                g2.fill(new Ellipse2D.Double(px - 2.6, py - 2.6, 5.2, 5.2));
            }
            g2.setColor(goal ? Color.WHITE : new Color(0x9A, 0xA6, 0xB4));
            g2.drawString(labels[i], (float) (px + 7), (float) (py + 4));
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
        Color c = allianceColor();
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 230));
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private void drawRobot(Graphics2D g2, double ox, double oy, double size, double scale) {
        double cx = sx(robot.getX(), ox, scale);
        double cy = sy(robot.getY(), oy, size, scale);

        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(-robot.getHeading());

        double L = Constants.ROBOT_LENGTH * scale;
        double W = Constants.ROBOT_WIDTH * scale;
        double prot = Constants.ROBOT_FRONT_PROTRUSION * scale;

        if (prot > 0) {
            g2.setColor(new Color(0x33, 0x3E, 0x4C));
            g2.fill(new Rectangle2D.Double(L / 2, -W * 0.30, prot, W * 0.60));
            g2.setColor(allianceColor());
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(new Rectangle2D.Double(L / 2, -W * 0.30, prot, W * 0.60));
        }

        g2.setColor(BODY);
        g2.fill(new Rectangle2D.Double(-L / 2, -W / 2, L, W));
        g2.setColor(allianceColor());
        g2.setStroke(new BasicStroke(2.2f));
        g2.draw(new Rectangle2D.Double(-L / 2, -W / 2, L, W));

        Path2D.Double nose = new Path2D.Double();
        nose.moveTo(L / 2 - W * 0.22, -W * 0.16);
        nose.lineTo(L / 2 - 1, 0);
        nose.lineTo(L / 2 - W * 0.22, W * 0.16);
        nose.closePath();
        g2.setColor(allianceColor());
        g2.fill(nose);

        double wl = 4.0 * scale, ww = 2.0 * scale;
        double lx = Constants.HALF_WHEELBASE * scale, ly = Constants.HALF_TRACK * scale;
        drawWheel(g2, robot.frontLeft,   lx, -ly, wl, ww);
        drawWheel(g2, robot.frontRight,  lx,  ly, wl, ww);
        drawWheel(g2, robot.backLeft,   -lx, -ly, wl, ww);
        drawWheel(g2, robot.backRight,  -lx,  ly, wl, ww);

        g2.setTransform(old);
    }

    private void drawWheel(Graphics2D g2, Motor motor, double bx, double byScreen,
                           double len, double wid) {
        Rectangle2D.Double r = new Rectangle2D.Double(bx - len / 2, byScreen - wid / 2, len, wid);
        g2.setColor(powerColor(motor.getPower()));
        g2.fill(r);
        g2.setColor(new Color(0x0B, 0x0E, 0x12));
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(r);
    }

    static Color powerColor(double power) {
        double p = Math.max(-1, Math.min(1, power));
        if (p >= 0) return blend(new Color(0x3A, 0x40, 0x48), new Color(0x35, 0xD0, 0x7F), p);
        return blend(new Color(0x3A, 0x40, 0x48), new Color(0xE5, 0x5A, 0x4E), -p);
    }

    private static Color blend(Color a, Color b, double t) {
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }
}

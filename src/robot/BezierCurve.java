package robot;

public class BezierCurve {

    private final Pose[] points;
    private final double length;

    public BezierCurve(Pose... points) {
        if (points.length < 2) throw new IllegalArgumentException("need at least 2 points");
        this.points = points.clone();
        this.length = approximateLength();
    }

    public Pose pointAt(double t) {
        t = clamp01(t);
        double[] xs = new double[points.length];
        double[] ys = new double[points.length];
        for (int i = 0; i < points.length; i++) {
            xs[i] = points[i].x;
            ys[i] = points[i].y;
        }
        for (int n = points.length - 1; n > 0; n--) {
            for (int i = 0; i < n; i++) {
                xs[i] = xs[i] + (xs[i + 1] - xs[i]) * t;
                ys[i] = ys[i] + (ys[i + 1] - ys[i]) * t;
            }
        }
        return new Pose(xs[0], ys[0]);
    }

    public double tangentAt(double t) {
        double d = 1e-3;
        Pose a = pointAt(Math.max(0, t - d));
        Pose b = pointAt(Math.min(1, t + d));
        return Math.atan2(b.y - a.y, b.x - a.x);
    }

    public double closestT(double x, double y) {
        double bestT = 0, bestD = Double.MAX_VALUE;
        for (int i = 0; i <= 200; i++) {
            double t = i / 200.0;
            Pose p = pointAt(t);
            double d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
            if (d < bestD) { bestD = d; bestT = t; }
        }
        double step = 1 / 200.0;
        for (int pass = 0; pass < 4; pass++) {
            step /= 4;
            for (int i = -3; i <= 3; i++) {
                double t = clamp01(bestT + i * step);
                Pose p = pointAt(t);
                double d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
                if (d < bestD) { bestD = d; bestT = t; }
            }
        }
        return bestT;
    }

    public double advance(double t, double distance) {
        double step = 0.005;
        double travelled = 0;
        Pose prev = pointAt(t);
        double cur = t;
        while (cur < 1.0 && travelled < distance) {
            cur = Math.min(1.0, cur + step);
            Pose next = pointAt(cur);
            travelled += Math.hypot(next.x - prev.x, next.y - prev.y);
            prev = next;
        }
        return cur;
    }

    public double getLength() { return length; }
    public Pose start() { return points[0]; }
    public Pose end() { return points[points.length - 1]; }
    public Pose[] getPoints() { return points.clone(); }

    private double approximateLength() {
        double total = 0;
        Pose prev = pointAt(0);
        for (int i = 1; i <= 100; i++) {
            Pose p = pointAt(i / 100.0);
            total += Math.hypot(p.x - prev.x, p.y - prev.y);
            prev = p;
        }
        return total;
    }

    private static double clamp01(double t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }
}

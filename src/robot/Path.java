package robot;

public class Path {

    public enum HeadingMode { CONSTANT, LINEAR, TANGENT, AIM }

    private final BezierCurve curve;
    private HeadingMode mode = HeadingMode.TANGENT;
    private double startHeading;
    private double endHeading;
    private Pose aimPoint;
    private double maxPower = 0.85;
    private String name = "";

    private Path(BezierCurve curve) {
        this.curve = curve;
        this.startHeading = curve.start().heading;
        this.endHeading = curve.end().heading;
    }

    public static Path line(Pose start, Pose end) {
        return new Path(new BezierCurve(start, end));
    }

    public static Path curve(Pose start, Pose control, Pose end) {
        return new Path(new BezierCurve(start, control, end));
    }

    public static Path curve(Pose start, Pose control1, Pose control2, Pose end) {
        return new Path(new BezierCurve(start, control1, control2, end));
    }

    public Path constantHeading(double heading) {
        this.mode = HeadingMode.CONSTANT;
        this.startHeading = heading;
        this.endHeading = heading;
        return this;
    }

    public Path linearHeading(double from, double to) {
        this.mode = HeadingMode.LINEAR;
        this.startHeading = from;
        this.endHeading = to;
        return this;
    }

    public Path tangentHeading() {
        this.mode = HeadingMode.TANGENT;
        return this;
    }

    public Path aimAt(Pose target) {
        this.mode = HeadingMode.AIM;
        this.aimPoint = target;
        return this;
    }

    public Path maxPower(double power) {
        this.maxPower = Math.max(0.05, Math.min(1.0, power));
        return this;
    }

    public Path name(String name) {
        this.name = name;
        return this;
    }

    public double headingAt(double t) {
        switch (mode) {
            case CONSTANT: return startHeading;
            case LINEAR:   return startHeading + Robot.normalizeRad(endHeading - startHeading) * clamp01(t);
            case AIM: {
                Pose p = curve.pointAt(t);
                return Math.atan2(aimPoint.y - p.y, aimPoint.x - p.x);
            }
            case TANGENT:
            default:       return curve.tangentAt(t);
        }
    }

    public Pose poseAt(double t) {
        Pose p = curve.pointAt(t);
        return new Pose(p.x, p.y, headingAt(t));
    }

    public Pose endPose() {
        return poseAt(1.0);
    }

    public BezierCurve getCurve() { return curve; }
    public double getMaxPower()   { return maxPower; }
    public String getName()       { return name; }

    private static double clamp01(double t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }
}

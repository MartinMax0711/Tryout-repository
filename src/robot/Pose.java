package robot;

public class Pose {

    public final double x;
    public final double y;
    public final double heading;

    public Pose(double x, double y) {
        this(x, y, 0.0);
    }

    public Pose(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    public Pose mirrorIfBlue() {
        if (Constants.alliance != Alliance.BLUE) return this;
        return new Pose(Constants.FIELD_SIZE - x, y, Math.PI - heading);
    }

    public Pose withHeading(double heading) {
        return new Pose(x, y, heading);
    }

    public Pose plus(double dx, double dy) {
        return new Pose(x + dx, y + dy, heading);
    }

    public double distanceTo(Pose other) {
        return Math.hypot(other.x - x, other.y - y);
    }

    public double headingTo(Pose other) {
        return Math.atan2(other.y - y, other.x - x);
    }

    public double getHeadingDeg() {
        return Math.toDegrees(heading);
    }

    public static double calculateAimHeading(Pose from, Pose target) {
        return from.headingTo(target);
    }

    @Override
    public String toString() {
        return String.format("Pose(%.2f, %.2f, %.1f°)", x, y, Math.toDegrees(heading));
    }
}

package robot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Robot {

    public static final double FIELD_SIZE = Constants.FIELD_SIZE;
    private static final double K = Constants.HALF_WHEELBASE + Constants.HALF_TRACK;

    public final Motor frontLeft  = new Motor("frontLeft");
    public final Motor frontRight = new Motor("frontRight");
    public final Motor backLeft   = new Motor("backLeft");
    public final Motor backRight  = new Motor("backRight");
    public final Motor[] motors = { frontLeft, frontRight, backLeft, backRight };

    private final Object lock = new Object();
    private Pose initialPose = new Pose(FIELD_SIZE / 2, FIELD_SIZE / 2, 0);
    private double x = initialPose.x;
    private double y = initialPose.y;
    private double heading = initialPose.heading;
    private double vx, vy, omega;
    private double runtime = 0.0;

    private final List<double[]> trail = new ArrayList<>();
    private final Deque<String> logLines = new ArrayDeque<>();

    public Robot() {
        synchronized (lock) {
            trail.add(new double[]{x, y});
        }
    }

    public void followPath(Path path) throws InterruptedException {
        BezierCurve curve = path.getCurve();
        double maxPower = path.getMaxPower();

        final double LOOKAHEAD = 8.0;
        final double DECEL = 22.0;
        final double MIN_POWER = 0.12;
        final double HANDOFF = 3.0;

        long deadline = System.currentTimeMillis()
                + 4000 + (long) (curve.getLength() * 120);

        while (true) {
            if (Thread.interrupted()) throw new InterruptedException();

            double px = getX(), py = getY(), ph = getHeading();
            Pose end = curve.end();
            double distEnd = Math.hypot(end.x - px, end.y - py);
            double t = curve.closestT(px, py);

            if ((t > 0.90 && distEnd < HANDOFF) || System.currentTimeMillis() > deadline) break;

            Pose target = curve.pointAt(curve.advance(t, LOOKAHEAD));
            double dx = target.x - px, dy = target.y - py;
            double norm = Math.hypot(dx, dy);
            if (norm < 1e-6) break;

            double power = Math.max(MIN_POWER, Math.min(maxPower, distEnd / DECEL * maxPower));
            double vfx = dx / norm * power, vfy = dy / norm * power;

            double cos = Math.cos(ph), sin = Math.sin(ph);
            double fwd  =  vfx * cos + vfy * sin;
            double left = -vfx * sin + vfy * cos;

            double targetH = path.headingAt(Math.min(1.0, t + 0.15));
            double turn = shape(normalizeRad(targetH - ph) / Math.toRadians(45), maxPower, 0);

            drive(fwd, left, turn);
            Thread.sleep(5);
        }

        goToPose(path.endPose(), Math.min(maxPower, 0.55));
    }

    public void followPaths(Path... paths) throws InterruptedException {
        for (Path p : paths) followPath(p);
    }

    public void goToPose(Pose target, double maxPower) throws InterruptedException {
        final double FINAL_POS_TOL = 0.25;
        final double FINAL_ANG_TOL = Math.toRadians(0.3);

        long deadline = System.currentTimeMillis() + 6000
                + (long) (Math.hypot(target.x - getX(), target.y - getY()) * 120);

        for (int pass = 0; pass < 3; pass++) {
            boolean fast = (pass == 0);
            posePass(target,
                    fast ? maxPower : Math.min(maxPower, 0.22),
                    fast ? 0.10 : 0.035,
                    fast ? 0.40 : 0.12,
                    fast ? Math.toRadians(0.5) : Math.toRadians(0.12),
                    deadline);
            stop();
            Thread.sleep(120);

            double dist = Math.hypot(target.x - getX(), target.y - getY());
            double he = Math.abs(normalizeRad(target.heading - getHeading()));
            if ((dist < FINAL_POS_TOL && he < FINAL_ANG_TOL)
                    || System.currentTimeMillis() > deadline) break;
        }
    }

    public void goToPose(Pose target) throws InterruptedException {
        goToPose(target, 0.6);
    }

    private void posePass(Pose target, double maxPower, double minPower,
                          double posTol, double angTol, long deadline)
            throws InterruptedException {
        final double POS_RAMP = 10.0;
        final double ANG_RAMP = Math.toRadians(45);

        while (true) {
            if (Thread.interrupted()) throw new InterruptedException();

            double px = getX(), py = getY(), ph = getHeading();
            double dx = target.x - px, dy = target.y - py;
            double dist = Math.hypot(dx, dy);
            double he = normalizeRad(target.heading - ph);

            if ((dist < posTol && Math.abs(he) < angTol)
                    || System.currentTimeMillis() > deadline) return;

            double power = dist > 1e-6
                    ? shape(dist / POS_RAMP, maxPower, minPower) : 0;
            double vfx = dist > 1e-6 ? dx / dist * power : 0;
            double vfy = dist > 1e-6 ? dy / dist * power : 0;

            double cos = Math.cos(ph), sin = Math.sin(ph);
            double fwd  =  vfx * cos + vfy * sin;
            double left = -vfx * sin + vfy * cos;
            double turn = shape(he / ANG_RAMP, maxPower, minPower);

            drive(fwd, left, turn);
            Thread.sleep(5);
        }
    }

    public void forward(double inches) throws InterruptedException      { move(inches, 0, 0, 0.6); }
    public void backward(double inches) throws InterruptedException     { move(-inches, 0, 0, 0.6); }
    public void strafeLeft(double inches) throws InterruptedException    { move(0, inches, 0, 0.6); }
    public void strafeRight(double inches) throws InterruptedException   { move(0, -inches, 0, 0.6); }
    public void turnLeft(double degrees) throws InterruptedException     { move(0, 0, Math.toRadians(degrees), 0.5); }
    public void turnRight(double degrees) throws InterruptedException    { move(0, 0, -Math.toRadians(degrees), 0.5); }

    public void turnTo(double headingDegrees) throws InterruptedException {
        move(0, 0, normalizeRad(Math.toRadians(headingDegrees) - getHeading()), 0.5);
    }

    public void goTo(double targetX, double targetY) throws InterruptedException {
        goToPose(new Pose(targetX, targetY, getHeading()), 0.6);
    }

    public void stop() {
        for (Motor m : motors) m.stop();
    }

    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    public void waitSeconds(double seconds) throws InterruptedException {
        Thread.sleep(Math.max(0, (long) (seconds * 1000)));
    }

    public void drive(double forward, double strafe, double turn) {
        double fl = forward - strafe - turn;
        double fr = forward + strafe + turn;
        double bl = forward + strafe - turn;
        double br = forward - strafe + turn;
        double max = Math.max(1.0, Math.max(Math.abs(fl),
                Math.max(Math.abs(fr), Math.max(Math.abs(bl), Math.abs(br)))));
        frontLeft.setPower(fl / max);
        frontRight.setPower(fr / max);
        backLeft.setPower(bl / max);
        backRight.setPower(br / max);
    }

    public void driveFieldCentric(double fieldX, double fieldY, double turn) {
        double h = getHeading();
        double cos = Math.cos(h), sin = Math.sin(h);
        drive(fieldX * cos + fieldY * sin, -fieldX * sin + fieldY * cos, turn);
    }

    public void setMotorPowers(double fl, double fr, double bl, double br) {
        frontLeft.setPower(fl);
        frontRight.setPower(fr);
        backLeft.setPower(bl);
        backRight.setPower(br);
    }

    public void resetEncoders() {
        for (Motor m : motors) m.resetEncoder();
    }

    public double getX() { synchronized (lock) { return x; } }
    public double getY() { synchronized (lock) { return y; } }
    public double getHeading() { synchronized (lock) { return heading; } }
    public double getHeadingDeg() { return Math.toDegrees(normalizeRad(getHeading())); }

    public Pose getPose() {
        synchronized (lock) { return new Pose(x, y, heading); }
    }

    public void setPose(Pose pose) {
        synchronized (lock) {
            initialPose = pose;
            x = pose.x;
            y = pose.y;
            heading = pose.heading;
            vx = vy = omega = 0;
            trail.clear();
            trail.add(new double[]{x, y});
        }
    }

    public double getSpeed() {
        synchronized (lock) { return Math.hypot(vx, vy); }
    }

    public double getTurnRateDegPerSec() {
        synchronized (lock) { return Math.toDegrees(omega); }
    }

    public double getRuntime() { synchronized (lock) { return runtime; } }

    public void log(String message) {
        System.out.printf("[%6.2fs] %s%n", getRuntime(), message);
        synchronized (logLines) {
            logLines.addLast(String.format("%6.2fs  %s", getRuntime(), message));
            while (logLines.size() > 16) logLines.removeFirst();
        }
    }

    public List<String> getLogLines() {
        synchronized (logLines) { return new ArrayList<>(logLines); }
    }

    public List<double[]> getTrail() {
        synchronized (lock) { return new ArrayList<>(trail); }
    }

    private void move(double forwardIn, double strafeIn, double turnRad, double maxPower)
            throws InterruptedException {
        final double FINAL_POS_TOL = 0.1;
        final double FINAL_ANG_TOL = Math.toRadians(0.2);

        double[] start = wheelDistances();
        long deadline = System.currentTimeMillis()
                + 5000 + (long) ((Math.abs(forwardIn) + Math.abs(strafeIn)) * 120)
                + (long) (Math.abs(Math.toDegrees(turnRad)) * 25);

        for (int pass = 0; pass < 3; pass++) {
            boolean fast = (pass == 0);
            runPass(start, forwardIn, strafeIn, turnRad,
                    fast ? maxPower : Math.min(maxPower, 0.22),
                    fast ? 0.10 : 0.035,
                    fast ? 0.20 : 0.06,
                    fast ? Math.toRadians(0.4) : Math.toRadians(0.12),
                    deadline);
            stop();
            Thread.sleep(120);

            double[] e = remainingError(start, forwardIn, strafeIn, turnRad);
            boolean good = Math.abs(e[0]) < FINAL_POS_TOL && Math.abs(e[1]) < FINAL_POS_TOL
                    && Math.abs(e[2]) < FINAL_ANG_TOL;
            if (good || System.currentTimeMillis() > deadline) break;
        }
    }

    private void runPass(double[] start, double forwardIn, double strafeIn, double turnRad,
                         double maxPower, double minPower, double posTol, double angTol,
                         long deadline) throws InterruptedException {
        final double POS_RAMP = 10.0;
        final double ANG_RAMP = Math.toRadians(50);

        while (true) {
            if (Thread.interrupted()) throw new InterruptedException();

            double[] e = remainingError(start, forwardIn, strafeIn, turnRad);
            boolean atTarget = Math.abs(e[0]) < posTol && Math.abs(e[1]) < posTol
                    && Math.abs(e[2]) < angTol;
            if (atTarget || System.currentTimeMillis() > deadline) return;

            drive(shape(e[0] / POS_RAMP, maxPower, minPower),
                  shape(e[1] / POS_RAMP, maxPower, minPower),
                  shape(e[2] / ANG_RAMP, maxPower, minPower));
            Thread.sleep(5);
        }
    }

    private double[] remainingError(double[] start, double forwardIn, double strafeIn,
                                    double turnRad) {
        double[] d = wheelDistances();
        double dfl = d[0] - start[0], dfr = d[1] - start[1];
        double dbl = d[2] - start[2], dbr = d[3] - start[3];

        double doneFwd    = (dfl + dfr + dbl + dbr) / 4.0;
        double doneStrafe = (-dfl + dfr + dbl - dbr) / 4.0;
        double doneTurn   = (-dfl + dfr - dbl + dbr) / (4.0 * K);

        return new double[]{forwardIn - doneFwd, strafeIn - doneStrafe, turnRad - doneTurn};
    }

    private static double shape(double raw, double maxPower, double minPower) {
        if (Math.abs(raw) < 1e-4) return 0;
        double p = Math.max(-maxPower, Math.min(maxPower, raw));
        if (Math.abs(p) < minPower) p = Math.signum(p) * minPower;
        return p;
    }

    private double[] wheelDistances() {
        return new double[]{
                frontLeft.getDistance(), frontRight.getDistance(),
                backLeft.getDistance(),  backRight.getDistance()
        };
    }

    void step(double dt) {
        for (Motor m : motors) m.update(dt);

        double fl = frontLeft.getSpeed();
        double fr = frontRight.getSpeed();
        double bl = backLeft.getSpeed();
        double br = backRight.getSpeed();

        double bodyVx = (fl + fr + bl + br) / 4.0;
        double bodyVy = (-fl + fr + bl - br) / 4.0;
        double bodyW  = (-fl + fr - bl + br) / (4.0 * K);

        synchronized (lock) {
            vx = bodyVx; vy = bodyVy; omega = bodyW;
            double cos = Math.cos(heading), sin = Math.sin(heading);
            x += (bodyVx * cos - bodyVy * sin) * dt;
            y += (bodyVx * sin + bodyVy * cos) * dt;
            heading += bodyW * dt;
            runtime += dt;

            double hx = Math.abs(Constants.ROBOT_LENGTH / 2 * cos)
                    + Math.abs(Constants.ROBOT_WIDTH / 2 * sin);
            double hy = Math.abs(Constants.ROBOT_LENGTH / 2 * sin)
                    + Math.abs(Constants.ROBOT_WIDTH / 2 * cos);
            x = Math.max(hx, Math.min(FIELD_SIZE - hx, x));
            y = Math.max(hy, Math.min(FIELD_SIZE - hy, y));

            double[] last = trail.get(trail.size() - 1);
            if (Math.hypot(x - last[0], y - last[1]) > 0.4) {
                trail.add(new double[]{x, y});
                if (trail.size() > 4000) trail.remove(0);
            }
        }
    }

    void reset() {
        for (Motor m : motors) m.hardReset();
        synchronized (lock) {
            x = initialPose.x;
            y = initialPose.y;
            heading = initialPose.heading;
            vx = vy = omega = 0;
            runtime = 0;
            trail.clear();
            trail.add(new double[]{x, y});
        }
        synchronized (logLines) { logLines.clear(); }
    }

    public static double normalizeRad(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
}

package robot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 四电机机器人（麦克纳姆轮：可以前后、左右平移、原地旋转）。
 * Four-motor mecanum robot: it can drive, strafe sideways and turn in place.
 *
 * 你不需要修改这个文件，只要在 MyProgram.java 里调用这里的方法。
 * You do not need to edit this file — just call these methods from MyProgram.java.
 *
 * 坐标系 / Coordinate system:
 *   场地 400cm x 400cm，原点在左下角，x 向右，y 向上。
 *   heading = 0 度表示机器人朝右（+x），逆时针为正。
 */
public class Robot {

    // ---- 场地和车身尺寸 / field and chassis dimensions ----
    public static final double FIELD_SIZE_CM = 400.0;
    public static final double BODY_LENGTH_CM = 40.0;   // 前后长
    public static final double BODY_WIDTH_CM  = 40.0;   // 左右宽
    private static final double HALF_WHEELBASE = 15.0;  // 前后轮距的一半 lx
    private static final double HALF_TRACK     = 16.0;  // 左右轮距的一半 ly
    private static final double K = HALF_WHEELBASE + HALF_TRACK;

    // ---- 四个电机，已经帮你定义好了 / the four motors, already defined for you ----
    public final Motor frontLeft  = new Motor("frontLeft");
    public final Motor frontRight = new Motor("frontRight");
    public final Motor backLeft   = new Motor("backLeft");
    public final Motor backRight  = new Motor("backRight");
    public final Motor[] motors = { frontLeft, frontRight, backLeft, backRight };

    // ---- 位姿 / pose ----
    private final Object lock = new Object();
    private double x = FIELD_SIZE_CM / 2;
    private double y = FIELD_SIZE_CM / 2;
    private double heading = 0.0;               // 弧度 / radians
    private double vx, vy, omega;               // 车身速度（车体坐标系）
    private double runtime = 0.0;

    private final List<double[]> trail = new ArrayList<>();
    private final Deque<String> logLines = new ArrayDeque<>();

    public Robot() {
        synchronized (lock) {
            trail.add(new double[]{x, y});
        }
    }

    // =====================================================================
    //  简单指令（会等到动作做完才返回）/ blocking commands
    // =====================================================================

    /** 向前走 cm 厘米 / drive forward */
    public void forward(double cm) throws InterruptedException {
        move(cm, 0, 0, 0.6);
    }

    /** 向后走 cm 厘米 / drive backward */
    public void backward(double cm) throws InterruptedException {
        move(-cm, 0, 0, 0.6);
    }

    /** 向左平移 cm 厘米 / strafe left */
    public void strafeLeft(double cm) throws InterruptedException {
        move(0, cm, 0, 0.6);
    }

    /** 向右平移 cm 厘米 / strafe right */
    public void strafeRight(double cm) throws InterruptedException {
        move(0, -cm, 0, 0.6);
    }

    /** 原地左转（逆时针）degrees 度 / turn left (counter-clockwise) */
    public void turnLeft(double degrees) throws InterruptedException {
        move(0, 0, Math.toRadians(degrees), 0.5);
    }

    /** 原地右转（顺时针）degrees 度 / turn right (clockwise) */
    public void turnRight(double degrees) throws InterruptedException {
        move(0, 0, -Math.toRadians(degrees), 0.5);
    }

    /** 转到某个绝对角度（度）/ turn to an absolute heading in degrees */
    public void turnTo(double headingDegrees) throws InterruptedException {
        double diff = normalizeRad(Math.toRadians(headingDegrees) - getHeading());
        move(0, 0, diff, 0.5);
    }

    /** 先转向再开到场地上的某个点 / turn toward a field point and drive to it */
    public void goTo(double targetX, double targetY) throws InterruptedException {
        double dx = targetX - getX();
        double dy = targetY - getY();
        double dist = Math.hypot(dx, dy);
        if (dist < 0.5) return;
        turnTo(Math.toDegrees(Math.atan2(dy, dx)));
        forward(dist);
    }

    /** 停车 / stop all four motors */
    public void stop() {
        for (Motor m : motors) m.stop();
    }

    /** 等待若干毫秒 / sleep for milliseconds */
    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    /** 等待若干秒 / sleep for seconds */
    public void waitSeconds(double seconds) throws InterruptedException {
        Thread.sleep(Math.max(0, (long) (seconds * 1000)));
    }

    // =====================================================================
    //  连续控制（马上返回，电机保持这个速度）/ non-blocking control
    // =====================================================================

    /**
     * 同时给三个方向的速度，立刻返回。/ set a continuous velocity command.
     * @param forward 前进 -1..1，正数向前
     * @param strafe  平移 -1..1，正数向左
     * @param turn    旋转 -1..1，正数逆时针
     */
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

    /** 直接分别给四个电机功率 / set the four motor powers directly */
    public void setMotorPowers(double fl, double fr, double bl, double br) {
        frontLeft.setPower(fl);
        frontRight.setPower(fr);
        backLeft.setPower(bl);
        backRight.setPower(br);
    }

    /** 四个电机编码器清零 / reset all four encoders */
    public void resetEncoders() {
        for (Motor m : motors) m.resetEncoder();
    }

    // =====================================================================
    //  读取状态 / sensors and state
    // =====================================================================

    public double getX() { synchronized (lock) { return x; } }
    public double getY() { synchronized (lock) { return y; } }

    /** 朝向，弧度 / heading in radians */
    public double getHeading() { synchronized (lock) { return heading; } }

    /** 朝向，度（-180..180）/ heading in degrees */
    public double getHeadingDeg() { return Math.toDegrees(normalizeRad(getHeading())); }

    /** 当前速度 cm/s / current ground speed */
    public double getSpeedCmPerSec() {
        synchronized (lock) { return Math.hypot(vx, vy); }
    }

    /** 当前旋转速度 度/秒 / current turn rate in degrees per second */
    public double getTurnRateDegPerSec() {
        synchronized (lock) { return Math.toDegrees(omega); }
    }

    /** 程序运行了多少秒 / seconds since the program started */
    public double getRuntime() { synchronized (lock) { return runtime; } }

    /** 在屏幕右侧和控制台打印一条信息 / print a line to the telemetry panel and console */
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

    // =====================================================================
    //  内部实现 / internals — 一般不需要看
    // =====================================================================

    /**
     * 用编码器闭环走一个相对位移（车体坐标系），走完再做一次慢速修正。
     * Closed-loop relative move using the wheel encoders, plus a slow correction pass.
     */
    private void move(double forwardCm, double strafeCm, double turnRad, double maxPower)
            throws InterruptedException {
        final double FINAL_POS_TOL = 0.15;                   // cm
        final double FINAL_ANG_TOL = Math.toRadians(0.2);    // rad

        double[] start = wheelDistances();
        long deadline = System.currentTimeMillis()
                + 5000 + (long) ((Math.abs(forwardCm) + Math.abs(strafeCm)) * 60)
                + (long) (Math.abs(Math.toDegrees(turnRad)) * 25);

        for (int pass = 0; pass < 3; pass++) {
            boolean fast = (pass == 0);
            runPass(start, forwardCm, strafeCm, turnRad,
                    fast ? maxPower : Math.min(maxPower, 0.22),   // 修正用低速
                    fast ? 0.10 : 0.035,                          // 最小功率
                    fast ? 0.30 : 0.10,                           // 位置容差 cm
                    fast ? Math.toRadians(0.4) : Math.toRadians(0.12),
                    deadline);
            stop();
            Thread.sleep(120);   // 让车身停稳 / let the chassis settle

            double[] e = remainingError(start, forwardCm, strafeCm, turnRad);
            boolean good = Math.abs(e[0]) < FINAL_POS_TOL && Math.abs(e[1]) < FINAL_POS_TOL
                    && Math.abs(e[2]) < FINAL_ANG_TOL;
            if (good || System.currentTimeMillis() > deadline) break;
        }
    }

    /** 一轮闭环控制，直到到位或超时 / one closed-loop pass until on target or timed out */
    private void runPass(double[] start, double forwardCm, double strafeCm, double turnRad,
                         double maxPower, double minPower, double posTol, double angTol,
                         long deadline) throws InterruptedException {
        final double POS_RAMP = 25.0;                  // cm，进入这个范围开始减速
        final double ANG_RAMP = Math.toRadians(50);

        while (true) {
            if (Thread.interrupted()) throw new InterruptedException();

            double[] e = remainingError(start, forwardCm, strafeCm, turnRad);
            boolean atTarget = Math.abs(e[0]) < posTol && Math.abs(e[1]) < posTol
                    && Math.abs(e[2]) < angTol;
            if (atTarget || System.currentTimeMillis() > deadline) return;

            drive(shape(e[0] / POS_RAMP, maxPower, minPower),
                  shape(e[1] / POS_RAMP, maxPower, minPower),
                  shape(e[2] / ANG_RAMP, maxPower, minPower));
            Thread.sleep(5);
        }
    }

    /** 还差多少 / how much of the move is left: {forward cm, strafe cm, turn rad} */
    private double[] remainingError(double[] start, double forwardCm, double strafeCm,
                                    double turnRad) {
        double[] d = wheelDistances();
        double dfl = d[0] - start[0], dfr = d[1] - start[1];
        double dbl = d[2] - start[2], dbr = d[3] - start[3];

        double doneFwd    = (dfl + dfr + dbl + dbr) / 4.0;
        double doneStrafe = (-dfl + dfr + dbl - dbr) / 4.0;
        double doneTurn   = (-dfl + dfr - dbl + dbr) / (4.0 * K);

        return new double[]{forwardCm - doneFwd, strafeCm - doneStrafe, turnRad - doneTurn};
    }

    /** 把误差变成功率：限幅 + 最小启动功率 / clamp the error into a usable power */
    private static double shape(double raw, double maxPower, double minPower) {
        if (Math.abs(raw) < 1e-4) return 0;
        double p = Math.max(-maxPower, Math.min(maxPower, raw));
        if (Math.abs(p) < minPower) p = Math.signum(p) * minPower;
        return p;
    }

    private double[] wheelDistances() {
        return new double[]{
                frontLeft.getDistanceCm(), frontRight.getDistanceCm(),
                backLeft.getDistanceCm(),  backRight.getDistanceCm()
        };
    }

    /** 仿真的一步 / one physics step, called by Simulation */
    void step(double dt) {
        for (Motor m : motors) m.update(dt);

        double fl = frontLeft.getSpeedCmPerSec();
        double fr = frontRight.getSpeedCmPerSec();
        double bl = backLeft.getSpeedCmPerSec();
        double br = backRight.getSpeedCmPerSec();

        // 麦克纳姆正运动学 / mecanum forward kinematics
        double bodyVx = (fl + fr + bl + br) / 4.0;          // 前进
        double bodyVy = (-fl + fr + bl - br) / 4.0;         // 向左
        double bodyW  = (-fl + fr - bl + br) / (4.0 * K);   // 逆时针

        synchronized (lock) {
            vx = bodyVx; vy = bodyVy; omega = bodyW;
            double cos = Math.cos(heading), sin = Math.sin(heading);
            x += (bodyVx * cos - bodyVy * sin) * dt;
            y += (bodyVx * sin + bodyVy * cos) * dt;
            heading += bodyW * dt;
            runtime += dt;

            // 撞墙就停在墙边 / keep the robot inside the field
            double hx = Math.abs(BODY_LENGTH_CM / 2 * cos) + Math.abs(BODY_WIDTH_CM / 2 * sin);
            double hy = Math.abs(BODY_LENGTH_CM / 2 * sin) + Math.abs(BODY_WIDTH_CM / 2 * cos);
            x = Math.max(hx, Math.min(FIELD_SIZE_CM - hx, x));
            y = Math.max(hy, Math.min(FIELD_SIZE_CM - hy, y));

            double[] last = trail.get(trail.size() - 1);
            if (Math.hypot(x - last[0], y - last[1]) > 1.0) {
                trail.add(new double[]{x, y});
                if (trail.size() > 4000) trail.remove(0);
            }
        }
    }

    /** 复位到场地中央 / reset the robot back to the middle of the field */
    void reset() {
        for (Motor m : motors) m.hardReset();
        synchronized (lock) {
            x = FIELD_SIZE_CM / 2;
            y = FIELD_SIZE_CM / 2;
            heading = 0;
            vx = vy = omega = 0;
            runtime = 0;
            trail.clear();
            trail.add(new double[]{x, y});
        }
        synchronized (logLines) { logLines.clear(); }
    }

    static double normalizeRad(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
}

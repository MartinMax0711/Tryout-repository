package robot;

public class Motor {

    public static final double TICKS_PER_REV = 537.7;
    public static final double MAX_REV_PER_SEC = 5.2;
    public static final double WHEEL_DIAMETER = 4.0;
    public static final double WHEEL_CIRCUMFERENCE = Math.PI * WHEEL_DIAMETER;

    private static final double RESPONSE_TIME = 0.08;

    private final String name;

    private volatile double power = 0.0;
    private volatile boolean reversed = false;

    private double revPerSec = 0.0;
    private double ticks = 0.0;
    private double tickOffset = 0.0;

    Motor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPower(double power) {
        if (Double.isNaN(power)) power = 0;
        this.power = Math.max(-1.0, Math.min(1.0, power));
    }

    public double getPower() {
        return power;
    }

    public void stop() {
        setPower(0);
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public boolean isReversed() {
        return reversed;
    }

    public synchronized double getTicks() {
        return ticks - tickOffset;
    }

    public synchronized double getDistance() {
        return getTicks() / TICKS_PER_REV * WHEEL_CIRCUMFERENCE;
    }

    public synchronized double getSpeed() {
        return revPerSec * WHEEL_CIRCUMFERENCE;
    }

    public synchronized void resetEncoder() {
        tickOffset = ticks;
    }

    synchronized void update(double dt) {
        double target = (reversed ? -power : power) * MAX_REV_PER_SEC;
        double alpha = Math.min(1.0, dt / RESPONSE_TIME);
        revPerSec += (target - revPerSec) * alpha;
        ticks += revPerSec * TICKS_PER_REV * dt;
    }

    synchronized void hardReset() {
        power = 0;
        revPerSec = 0;
        ticks = 0;
        tickOffset = 0;
    }
}

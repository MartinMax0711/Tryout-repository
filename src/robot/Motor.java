package robot;

/**
 * 一个电机（自带编码器）。
 * A single motor with a built-in encoder.
 *
 * 你不需要修改这个文件。/ You do not need to edit this file.
 */
public class Motor {

    /** 编码器每转的 tick 数 / encoder ticks per revolution */
    public static final double TICKS_PER_REV = 1000.0;
    /** 电机满功率转速（转/秒）/ free speed in revolutions per second */
    public static final double MAX_REV_PER_SEC = 3.0;
    /** 轮子直径 cm / wheel diameter */
    public static final double WHEEL_DIAMETER_CM = 10.0;
    public static final double WHEEL_CIRCUMFERENCE_CM = Math.PI * WHEEL_DIAMETER_CM;

    /** 电机响应时间常数（秒），模拟加速过程 / first-order response time */
    private static final double RESPONSE_TIME = 0.08;

    private final String name;

    private volatile double power = 0.0;      // -1.0 .. 1.0
    private volatile boolean reversed = false;

    private double revPerSec = 0.0;           // 当前真实转速
    private double ticks = 0.0;               // 累计 tick
    private double tickOffset = 0.0;          // resetEncoder 的基准

    Motor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** 设定功率，-1.0（全速后退）到 1.0（全速前进）/ set power from -1.0 to 1.0 */
    public void setPower(double power) {
        if (Double.isNaN(power)) power = 0;
        this.power = Math.max(-1.0, Math.min(1.0, power));
    }

    public double getPower() {
        return power;
    }

    /** 停止这个电机 / stop this motor */
    public void stop() {
        setPower(0);
    }

    /** 反转这个电机的方向（装反了的时候用）/ flip this motor's direction */
    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public boolean isReversed() {
        return reversed;
    }

    /** 编码器读数 / encoder reading in ticks */
    public synchronized double getTicks() {
        return ticks - tickOffset;
    }

    /** 轮子走过的距离 cm / distance travelled by this wheel */
    public synchronized double getDistanceCm() {
        return getTicks() / TICKS_PER_REV * WHEEL_CIRCUMFERENCE_CM;
    }

    /** 当前轮速 cm/s / current wheel speed */
    public synchronized double getSpeedCmPerSec() {
        return revPerSec * WHEEL_CIRCUMFERENCE_CM;
    }

    /** 编码器清零 / reset the encoder to zero */
    public synchronized void resetEncoder() {
        tickOffset = ticks;
    }

    // ---- 仿真内部使用 / used by the simulation loop ----

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

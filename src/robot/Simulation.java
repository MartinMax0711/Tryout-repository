package robot;

/**
 * 物理仿真循环：每秒 100 步推进机器人。
 * The physics loop — advances the robot 100 times per second.
 *
 * 你不需要修改这个文件。/ You do not need to edit this file.
 */
class Simulation implements Runnable {

    private static final double DT = 0.01;          // 10ms 一步
    private final Robot robot;
    private volatile boolean running = true;

    Simulation(Robot robot) {
        this.robot = robot;
    }

    @Override
    public void run() {
        long next = System.nanoTime();
        while (running) {
            robot.step(DT);
            next += (long) (DT * 1_000_000_000L);
            long sleep = next - System.nanoTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else {
                next = System.nanoTime();   // 落后了就重新对时 / re-sync if we fell behind
            }
        }
    }

    void shutdown() {
        running = false;
    }
}

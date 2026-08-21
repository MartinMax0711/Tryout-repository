package robot;

class Simulation implements Runnable {

    private static final double DT = 0.01;
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
                next = System.nanoTime();
            }
        }
    }

    void shutdown() {
        running = false;
    }
}

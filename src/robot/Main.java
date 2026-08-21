package robot;

/**
 * 程序入口 / entry point.
 * 运行这个类，窗口打开后 MyProgram.run() 会自动开始执行。
 * Run this class — the window opens and MyProgram.run() starts automatically.
 *
 * 加参数 --headless 可以不开窗口直接跑 / pass --headless to run without a window.
 */
public class Main {

    public static void main(String[] args) {
        Robot robot = new Robot();
        Platform platform = new Platform(robot);

        boolean headless = args.length > 0 && args[0].equals("--headless");
        if (headless) {
            platform.runHeadless();
        } else {
            platform.start();
        }
    }
}

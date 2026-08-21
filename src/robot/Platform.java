package robot;

import javax.swing.*;
import java.awt.*;

/**
 * 平台本体：开窗口、跑物理仿真、执行你在 MyProgram 里写的代码。
 * The platform: opens the window, runs the physics loop and executes MyProgram.
 *
 * 你不需要修改这个文件。/ You do not need to edit this file.
 */
public class Platform {

    private final Robot robot;
    private final Simulation simulation;

    private Thread simThread;
    private Thread programThread;
    private volatile String status = "READY";

    private JFrame frame;
    private FieldPanel fieldPanel;
    private TelemetryPanel telemetryPanel;

    public Platform(Robot robot) {
        this.robot = robot;
        this.simulation = new Simulation(robot);
    }

    /** 开机：显示窗口，然后自动执行 MyProgram.run() / boot up and auto-run MyProgram */
    public void start() {
        simThread = new Thread(simulation, "simulation");
        simThread.setDaemon(true);
        simThread.start();

        SwingUtilities.invokeLater(() -> {
            buildWindow();
            startProgram();
        });
    }

    private void buildWindow() {
        frame = new JFrame("Robot Platform  ·  4 Motors  ·  400 x 400 cm");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        fieldPanel = new FieldPanel(robot);
        telemetryPanel = new TelemetryPanel(robot, () -> status);

        JButton runButton = new JButton("▶  运行 / Run");
        JButton stopButton = new JButton("■  停止 / Stop");
        runButton.setFocusable(false);
        stopButton.setFocusable(false);
        runButton.addActionListener(e -> startProgram());
        stopButton.addActionListener(e -> stopProgram("STOPPED"));

        JLabel hint = new JLabel("  代码写在 MyProgram.java  ·  edit MyProgram.java");
        hint.setForeground(new Color(0x76, 0x83, 0x93));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(new Color(0x0D, 0x10, 0x14));
        bar.add(runButton);
        bar.add(stopButton);
        bar.add(hint);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(0x0D, 0x10, 0x14));
        root.add(bar, BorderLayout.NORTH);
        root.add(fieldPanel, BorderLayout.CENTER);
        root.add(telemetryPanel, BorderLayout.EAST);

        frame.setContentPane(root);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Timer(16, e -> {
            fieldPanel.repaint();
            telemetryPanel.repaint();
        }).start();
    }

    /** 从头开始跑一次你的程序 / reset the robot and run your program from the top */
    public void startProgram() {
        stopProgram(null);
        robot.reset();

        programThread = new Thread(() -> {
            status = "RUNNING";
            try {
                MyProgram.run(robot);
                status = "FINISHED";
            } catch (InterruptedException e) {
                status = "STOPPED";
            } catch (Throwable t) {
                status = "ERROR: " + t.getClass().getSimpleName();
                t.printStackTrace();
            } finally {
                robot.stop();
            }
        }, "my-program");
        programThread.setDaemon(true);
        programThread.start();
    }

    private void stopProgram(String newStatus) {
        Thread t = programThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
            try {
                t.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        robot.stop();
        if (newStatus != null) status = newStatus;
    }

    public String getStatus() {
        return status;
    }

    /**
     * 没有窗口的模式（跑测试或者在服务器上用）。
     * Headless mode — runs the program without opening a window.
     */
    public void runHeadless() {
        simThread = new Thread(simulation, "simulation");
        simThread.setDaemon(true);
        simThread.start();
        status = "RUNNING";
        try {
            MyProgram.run(robot);
            status = "FINISHED";
        } catch (InterruptedException e) {
            status = "STOPPED";
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            status = "ERROR: " + t.getClass().getSimpleName();
            t.printStackTrace();
        } finally {
            robot.stop();
            simulation.shutdown();
        }
        System.out.printf("%n%s  ->  x=%.1f cm  y=%.1f cm  heading=%.1f°  runtime=%.2fs%n",
                status, robot.getX(), robot.getY(), robot.getHeadingDeg(), robot.getRuntime());
    }
}

package robot;

import javax.swing.*;
import java.awt.*;

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
        frame = new JFrame("Robot Platform  ·  4 Motors  ·  144 x 144 in");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        fieldPanel = new FieldPanel(robot);
        telemetryPanel = new TelemetryPanel(robot, () -> status);

        JButton runButton = new JButton("\u25B6  Run");
        JButton stopButton = new JButton("\u25A0  Stop");
        runButton.setFocusable(false);
        stopButton.setFocusable(false);
        runButton.addActionListener(e -> startProgram());
        stopButton.addActionListener(e -> stopProgram("STOPPED"));

        JComboBox<Alliance> allianceBox = new JComboBox<>(Alliance.values());
        allianceBox.setSelectedItem(Constants.alliance);
        allianceBox.setFocusable(false);
        allianceBox.addActionListener(e -> {
            Constants.alliance = (Alliance) allianceBox.getSelectedItem();
            startProgram();
        });

        JLabel hint = new JLabel("  Paths: MyPaths.java   \u00B7   Actions: MyProgram.java");
        hint.setForeground(new Color(0x76, 0x83, 0x93));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(new Color(0x0D, 0x10, 0x14));
        bar.add(runButton);
        bar.add(stopButton);
        bar.add(allianceBox);
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
        System.out.printf("%n%s  ->  x=%.2f in  y=%.2f in  heading=%.1f°  runtime=%.2fs%n",
                status, robot.getX(), robot.getY(), robot.getHeadingDeg(), robot.getRuntime());
    }
}

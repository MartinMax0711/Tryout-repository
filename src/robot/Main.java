package robot;

public class Main {

    public static void main(String[] args) {
        for (String a : args) {
            if (a.equalsIgnoreCase("--blue")) Constants.alliance = Alliance.BLUE;
            if (a.equalsIgnoreCase("--red")) Constants.alliance = Alliance.RED;
        }
        Robot robot = new Robot();
        Platform platform = new Platform(robot);

        boolean headless = java.util.Arrays.asList(args).contains("--headless");
        if (headless) {
            platform.runHeadless();
        } else {
            platform.start();
        }
    }
}

package robot;

public class MyProgram {

    public static void run(Robot robot) throws InterruptedException {

        MyPaths p = new MyPaths();

        robot.setPose(p.startPosition);
        robot.log(p.alliance + " auto start");

        robot.followPath(p.startToShoot());
        shoot(robot, "preload");

        robot.followPath(p.toFirstRow());
        intake(robot, "row 1");
        robot.followPath(p.backToShoot(p.firstRow));
        shoot(robot, "row 1");

        robot.followPath(p.toSecondRow());
        intake(robot, "row 2");
        robot.followPath(p.backToShoot(p.secondRow));
        shoot(robot, "row 2");

        robot.followPath(p.toThirdRow());
        robot.followPath(p.alongThirdRow());
        intake(robot, "row 3");
        robot.followPath(p.backToShoot(p.thirdRowEndPoint));
        shoot(robot, "row 3");

        robot.followPath(p.park());
        robot.log("auto done  " + robot.getPose());

    }

    private static void shoot(Robot robot, String what) throws InterruptedException {
        robot.log("Shoot: " + what);
        robot.waitSeconds(0.8);
    }

    private static void intake(Robot robot, String what) throws InterruptedException {
        robot.log("Intake: " + what);
        robot.waitSeconds(0.5);
    }
}

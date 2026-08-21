package robot;

import java.util.ArrayList;
import java.util.List;

import static robot.Constants.ROBOT_FRONT_PROTRUSION;
import static robot.Constants.ROBOT_LENGTH;
import static robot.Constants.ROBOT_WIDTH;

public class MyPaths {

    public static volatile MyPaths current;

    public final Alliance alliance = Constants.alliance;

    public final Pose startPosition;
    public final Pose shootPosition;
    public final Pose firstRowControlPoint;
    public final Pose firstRow;
    public final Pose secondRowControlPoint;
    public final Pose secondRow;
    public final Pose thirdRow;
    public final Pose thirdRowControlPoint;
    public final Pose thirdRowEndPoint;
    public final Pose endLocation;
    public final Pose innerEndLocation;
    public final Pose goalLocation;

    public final double pickupHeading;
    public final double aimHeading;

    public MyPaths() {
        startPosition = new Pose(130.046 - ROBOT_WIDTH / Math.sqrt(2.0) / 2,
                                 130.046 - ROBOT_WIDTH / Math.sqrt(2.0) / 2,
                                 Math.toRadians(126.0)).mirrorIfBlue();
        shootPosition = new Pose(84.8704156479217, 78.42542787286067).mirrorIfBlue();
        firstRowControlPoint = new Pose(91.99073396778475, 83.2123552123552).mirrorIfBlue();
        firstRow = new Pose(144 - 6 - ROBOT_LENGTH / 2 - ROBOT_FRONT_PROTRUSION, 83.51).mirrorIfBlue();
        secondRowControlPoint = new Pose(83.14671814671814, 54.622779922779934).mirrorIfBlue();
        secondRow = new Pose(144 - ROBOT_LENGTH / 2 - ROBOT_FRONT_PROTRUSION - 6, 54.7 + 2.0).mirrorIfBlue();
        thirdRow = new Pose(98.30656370656376, 35.80328185328186).mirrorIfBlue();
        thirdRowControlPoint = new Pose(83.03667953667953, 35.43976833976834).mirrorIfBlue();
        thirdRowEndPoint = new Pose(144 - ROBOT_LENGTH / 2 - ROBOT_FRONT_PROTRUSION - 6, 35.43976833976834).mirrorIfBlue();

        endLocation = new Pose(100.0, 53.0).mirrorIfBlue();
        innerEndLocation = new Pose(86.0, 132.0).mirrorIfBlue();
        pickupHeading = (Constants.alliance == Alliance.RED) ? 0.0 : Math.toRadians(180.0);

        goalLocation = new Pose(138.0, 144.0).mirrorIfBlue();
        aimHeading = Pose.calculateAimHeading(shootPosition, goalLocation);

        current = this;
    }

    public Path startToShoot() {
        return Path.line(startPosition, shoot())
                .linearHeading(startPosition.heading, aimHeading)
                .maxPower(0.8)
                .name("start → shoot");
    }

    public Path toFirstRow() {
        return Path.curve(shoot(), firstRowControlPoint, firstRow)
                .linearHeading(aimHeading, pickupHeading)
                .maxPower(0.75)
                .name("→ row 1");
    }

    public Path toSecondRow() {
        return Path.curve(shoot(), secondRowControlPoint, secondRow)
                .linearHeading(aimHeading, pickupHeading)
                .maxPower(0.75)
                .name("→ row 2");
    }

    public Path toThirdRow() {
        return Path.curve(shoot(), thirdRowControlPoint, thirdRow)
                .linearHeading(aimHeading, pickupHeading)
                .maxPower(0.75)
                .name("→ row 3");
    }

    public Path alongThirdRow() {
        return Path.line(thirdRow, thirdRowEndPoint)
                .constantHeading(pickupHeading)
                .maxPower(0.45)
                .name("row 3 →");
    }

    public Path backToShoot(Pose from) {
        return Path.line(from, shoot())
                .linearHeading(pickupHeading, aimHeading)
                .maxPower(0.8)
                .name("→ shoot");
    }

    public Path park() {
        return Path.line(shoot(), endLocation.withHeading(aimHeading))
                .constantHeading(aimHeading)
                .maxPower(0.7)
                .name("→ park");
    }

    public Pose shoot() {
        return shootPosition.withHeading(aimHeading);
    }

    public List<Path> all() {
        List<Path> paths = new ArrayList<>();
        paths.add(startToShoot());
        paths.add(toFirstRow());
        paths.add(backToShoot(firstRow));
        paths.add(toSecondRow());
        paths.add(backToShoot(secondRow));
        paths.add(toThirdRow());
        paths.add(alongThirdRow());
        paths.add(backToShoot(thirdRowEndPoint));
        paths.add(park());
        return paths;
    }

    public Pose[] labelledPoses() {
        return new Pose[]{
                startPosition, shootPosition, firstRow, secondRow,
                thirdRow, thirdRowEndPoint, endLocation, innerEndLocation, goalLocation
        };
    }

    public String[] labels() {
        return new String[]{"start", "shoot", "row1", "row2", "row3", "row3 end",
                "park", "inner park", "GOAL"};
    }
}

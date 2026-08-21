# Robot Platform — Four-Motor Mecanum Simulator (FTC 144 × 144)

A 144 × 144 inch field with a **four-motor** mecanum robot and Pedro-Pathing-style path
following: `Pose`, Bezier curves, `mirrorIfBlue()`, and heading interpolation.

Poses go in `MyPaths.java`, actions go in `MyProgram.java`. Run it and the robot drives.

---

## Running it

```bash
./run.sh
```

That is all it takes — the script compiles `src/` and opens the window. The program starts
automatically as soon as the window appears.

| Flag | What it does |
|---|---|
| `./run.sh --blue` | Start on the blue alliance (also switchable in the window) |
| `./run.sh --headless` | No window; prints every step and the final pose to the terminal |

If the script will not execute, grant permission once:

```bash
chmod +x run.sh
```

**IDE:** open this folder as a project in IntelliJ or VS Code and run `src/robot/Main.java`.
No Gradle, no dependencies — plain Java 21 and Swing.

### In the window

- **▶ Run** — reset to the start pose and run the program again
- **■ Stop** — stop all motors immediately
- **RED / BLUE** — switch alliance; it re-runs, so `mirrorIfBlue()` takes effect right away
- **Field** — faint lines are a preview of every path in `MyPaths`; the bright line is the
  trail the robot has actually driven
- **Side panel** — pose, speed, the four motor powers and encoders, and your `robot.log()` output

After editing `MyProgram.java` or `MyPaths.java`, close the window and run `./run.sh` again.
The ▶ button re-runs the program but does not recompile.

---

## Your paths — [`MyPaths.java`](src/robot/MyPaths.java)

Your pose definitions are in there as-is; Kotlin to Java is just `new` and a semicolon:

```java
startPosition = new Pose(130.046 - ROBOT_WIDTH / Math.sqrt(2.0) / 2,
                         130.046 - ROBOT_WIDTH / Math.sqrt(2.0) / 2,
                         Math.toRadians(126.0)).mirrorIfBlue();
shootPosition = new Pose(84.8704156479217, 78.42542787286067).mirrorIfBlue();
firstRow      = new Pose(144 - 6 - ROBOT_LENGTH / 2 - ROBOT_FRONT_PROTRUSION, 83.51).mirrorIfBlue();
...
pickupHeading = (Constants.alliance == Alliance.RED) ? 0.0 : Math.toRadians(180.0);
goalLocation  = new Pose(138.0, 144.0).mirrorIfBlue();
aimHeading    = Pose.calculateAimHeading(shootPosition, goalLocation);
```

Below the poses, the same file builds the paths that use them — `startToShoot()`,
`toFirstRow()`, `toSecondRow()`, `toThirdRow()`, `alongThirdRow()`, `backToShoot(from)`
and `park()`. Every path returned by `all()` is drawn on the field as a preview line.

### How `mirrorIfBlue()` is implemented

Inferred from your own code — `pickupHeading` is 0° on red and 180° on blue, and the goal
sits at (138, 144) — this mirrors across the field's **vertical centre line**:
`x → 144 - x` and `heading → 180° - heading`.

If your team uses **180° rotational symmetry** instead, change the one line in
[`Pose.java`](src/robot/Pose.java):

```java
return new Pose(FIELD_SIZE - x, FIELD_SIZE - y, heading + Math.PI);
```

### Robot dimensions — [`Constants.java`](src/robot/Constants.java)

`ROBOT_WIDTH = 18`, `ROBOT_LENGTH = 18`, `ROBOT_FRONT_PROTRUSION = 2` (inches). Your pose
formulas depend on these — with the current values `firstRow.x` works out to 127. Set them
to your real robot.

---

## Your actions — [`MyProgram.java`](src/robot/MyProgram.java)

```java
MyPaths p = new MyPaths();

robot.setPose(p.startPosition);
robot.followPath(p.startToShoot());
shoot(robot, "preload");

robot.followPath(p.toFirstRow());
intake(robot, "row 1");
robot.followPath(p.backToShoot(p.firstRow));
shoot(robot, "row 1");
...
```

`shoot()` and `intake()` at the bottom of the file are empty stubs — drop your mechanism
code into them.

---

## Building paths

```java
Path.line(a, b)                       // straight line
Path.curve(a, control, b)             // one control point
Path.curve(a, c1, c2, b)              // two control points
```

Then pick how the heading behaves along it:

| Method | Behaviour |
|---|---|
| `.constantHeading(h)` | Hold one heading the whole way |
| `.linearHeading(from, to)` | Interpolate smoothly between two headings |
| `.tangentHeading()` | Always face along the path |
| `.aimAt(goalLocation)` | Always face a field point — useful while shooting |
| `.maxPower(0.6)` | Cap the drive power |

Follow them with `robot.followPath(path)` (returns when the path is done),
`robot.followPaths(a, b, c)`, or `robot.goToPose(pose)`.

The follower uses an 8 inch lookahead, a P controller onto the end pose, and one slow
correction pass. Measured end-of-path error across all nine paths: **under 0.15 in and
under 0.3°**, on both alliances.

---

## The four motors

| Field | Position |
|---|---|
| `robot.frontLeft` | Front left |
| `robot.frontRight` | Front right |
| `robot.backLeft` | Back left |
| `robot.backRight` | Back right |

`setPower(-1..1)` · `getPower()` · `getTicks()` · `getDistance()` · `resetEncoder()` ·
`setReversed(true)` · `stop()`

Specs: 537.7 ticks per revolution, 5.2 rev/s free speed, 4 in wheels (about 65 in/s top
speed), with a 0.08 s motor response time constant.

---

## Manual driving

| Command | Description |
|---|---|
| `robot.forward(in)` / `backward` / `strafeLeft` / `strafeRight` | Relative moves, in inches |
| `robot.turnLeft(deg)` / `turnRight(deg)` / `turnTo(deg)` | Turning |
| `robot.goTo(x, y)` | Drive to a point holding the current heading |
| `robot.drive(forward, strafe, turn)` | -1..1 each, returns immediately |
| `robot.driveFieldCentric(fx, fy, turn)` | Same but in field coordinates |
| `robot.setMotorPowers(fl, fr, bl, br)` | Drive the four motors directly |
| `robot.stop()` / `waitSeconds(s)` / `sleep(ms)` | — |

State: `getPose()` · `getX()` · `getY()` · `getHeadingDeg()` · `getSpeed()` ·
`getTurnRateDegPerSec()` · `getRuntime()` · `log("...")`

---

## Coordinates

- 144 × 144 inches, origin at the **bottom-left**, x to the right, y up, 24 in tiles
- `heading = 0°` faces right (+x); **counter-clockwise is positive**
- The robot stops at the wall, using its real body size

---

## Files

```
src/robot/
├── MyPaths.java        ★ your poses and paths
├── MyProgram.java      ★ your action code
├── Constants.java        robot dimensions and the current alliance
├── Pose.java             pose, mirrorIfBlue(), calculateAimHeading()
├── Alliance.java         RED / BLUE
├── Path.java             a path: curve plus heading interpolation
├── BezierCurve.java      Bezier curve maths
├── Robot.java            four motors, path following, all commands
├── Motor.java            one motor: power and encoder
├── Simulation.java       100 Hz physics loop
├── Platform.java         window, toolbar, runs your program
├── FieldPanel.java       field, path preview, robot
├── TelemetryPanel.java   side data panel
└── Main.java             entry point
```

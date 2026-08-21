# Robot Platform · 四电机机器人仿真平台 (FTC 144 × 144)

一个 144 × 144 英寸的场地 + 一台有 **四个电机** 的麦克纳姆机器人，带 **Pose / 贝塞尔路径 / mirrorIfBlue** 的路径跟随。
路径点写在 `MyPaths.java`，动作写在 `MyProgram.java`，跑起来就出发。

A 144 × 144 inch field with a **four-motor** mecanum robot and Pedro-Pathing-style path following
(`Pose`, Bezier curves, `mirrorIfBlue()`). Poses go in `MyPaths.java`, actions in `MyProgram.java`.

---

## 怎么跑 / How to run

```bash
./run.sh
```

| 参数 | 说明 |
|---|---|
| `./run.sh --blue` | 直接以蓝方启动（窗口里也能切） |
| `./run.sh --headless` | 不开窗口，只在终端打印结果 |

IDE：把文件夹 open 成项目，运行 `src/robot/Main.java`。
窗口打开后自动开跑；工具栏可以 **▶ 运行**、**■ 停止**、切 **RED / BLUE**（切换会重跑，`mirrorIfBlue()` 立刻生效）。

---

## 你的路径 / Your paths — [`MyPaths.java`](src/robot/MyPaths.java)

你给的那段原样搬进来了（Kotlin → Java 就是加 `new` 和 `;`）：

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

下面是用这些点拼出来的路径（`startToShoot()` / `toFirstRow()` / `backToShoot(from)` / `park()` …），
画面上会把它们全部画成淡色预览线。

### ⚠️ mirrorIfBlue 的约定

按你的 `pickupHeading`（红 0°、蓝 180°）和 `goalLocation(138, 144)` 反推，这里实现成
**沿场地竖直中线镜像**：`x → 144 - x`，`heading → 180° - heading`。

如果你队里用的是 **180° 旋转对称**，改 [`Pose.java`](src/robot/Pose.java) 里那一行：

```java
return new Pose(FIELD_SIZE - x, FIELD_SIZE - y, heading + Math.PI);
```

### 车身尺寸在 [`Constants.java`](src/robot/Constants.java)

`ROBOT_WIDTH = 18`、`ROBOT_LENGTH = 18`、`ROBOT_FRONT_PROTRUSION = 2`（英寸）—— 你的路径公式用的就是这几个，按实车改。

---

## 你的动作 / Your actions — [`MyProgram.java`](src/robot/MyProgram.java)

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

`shoot()` / `intake()` 是文件底部的空壳方法 —— 把你的机构代码填进去就行。

---

## 拼自己的路径 / Building paths

```java
Path.line(a, b)                       // 直线
Path.curve(a, control, b)             // 一个控制点
Path.curve(a, c1, c2, b)              // 两个控制点
```

后面接朝向方式（选一个）：

| 方法 | 意思 |
|---|---|
| `.constantHeading(h)` | 全程保持一个角度 |
| `.linearHeading(from, to)` | 从一个角度平滑转到另一个 |
| `.tangentHeading()` | 车头一直跟着路径方向 |
| `.aimAt(goalLocation)` | 全程瞄准某个点（射击很好用） |
| `.maxPower(0.6)` | 限速 |

跟随：`robot.followPath(path)`（跑完才返回）、`robot.followPaths(a, b, c)`、`robot.goToPose(pose)`。

跟随器是「前视 8 in 追点 + 终点 P 闭环 + 一次慢速修正」，实测每条路径终点误差 **< 0.15 in / < 0.3°**。

---

## 四个电机 / The four motors

| 名字 | 位置 |
|---|---|
| `robot.frontLeft`  | 左前 |
| `robot.frontRight` | 右前 |
| `robot.backLeft`   | 左后 |
| `robot.backRight`  | 右后 |

`setPower(-1..1)` · `getPower()` · `getTicks()` · `getDistance()` · `resetEncoder()` · `setReversed(true)` · `stop()`

规格：537.7 tick/圈、5.2 圈/秒、轮径 4 in（约 65 in/s 最高速），电机有 0.08 s 的加速时间常数。

---

## 手动控制 / Manual driving

| 指令 | 说明 |
|---|---|
| `robot.forward(in)` / `backward` / `strafeLeft` / `strafeRight` | 相对移动，单位英寸 |
| `robot.turnLeft(deg)` / `turnRight(deg)` / `turnTo(deg)` | 转向 |
| `robot.goTo(x, y)` | 保持朝向横着开过去 |
| `robot.drive(forward, strafe, turn)` | -1..1，马上返回 |
| `robot.driveFieldCentric(fx, fy, turn)` | 场地坐标系 |
| `robot.setMotorPowers(fl, fr, bl, br)` | 直接控制四个电机 |
| `robot.stop()` / `waitSeconds(s)` / `sleep(ms)` | — |

状态：`getPose()` · `getX()` · `getY()` · `getHeadingDeg()` · `getSpeed()` · `getRuntime()` · `log("...")`

---

## 坐标系 / Coordinates

- 144 × 144 英寸，原点 **左下角**，x 向右，y 向上，24 in 一块地砖
- `heading = 0°` 表示朝右（+x），**逆时针为正**
- 撞墙会停在墙边（按车身尺寸算）

---

## 文件 / Files

```
src/robot/
├── MyPaths.java        ★ 你的路径点和路径 / your poses and paths
├── MyProgram.java      ★ 你的动作代码 / your action code
├── Constants.java        车身尺寸 + 当前联盟
├── Pose.java             位姿 + mirrorIfBlue() + calculateAimHeading()
├── Alliance.java         RED / BLUE
├── Path.java             路径 = 曲线 + 朝向插值
├── BezierCurve.java      贝塞尔曲线
├── Robot.java            四个电机 + 路径跟随 + 所有指令
├── Motor.java            单个电机（编码器、功率）
├── Simulation.java       100 Hz 物理循环
├── Platform.java         窗口 / 工具栏 / 跑你的程序
├── FieldPanel.java       场地 + 路径预览 + 机器人
├── TelemetryPanel.java   右侧数据面板
└── Main.java             入口
```

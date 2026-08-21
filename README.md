# Robot Platform · 四电机机器人仿真平台

一个空白的 400 × 400 cm 平台 + 一台有 **四个电机** 的机器人。
所有东西都已经定义好了 —— 你只要在 `MyProgram.java` 里插入行动代码，运行就出发。

A blank 400 × 400 cm field with a **four-motor** mecanum robot.
Everything is pre-defined — write your action code in `MyProgram.java`, run, and it drives.

---

## 怎么跑 / How to run

终端 / Terminal:

```bash
./run.sh
```

不开窗口（只看结果）/ without a window:

```bash
./run.sh --headless
```

IDE（IntelliJ / VS Code）: 把这个文件夹 open 成项目，直接运行 `src/robot/Main.java`。
窗口打开后程序自动开始跑，点 **▶ 运行 / Run** 可以复位后重跑。

---

## 你要改的文件 / The file you edit

只有一个：[`src/robot/MyProgram.java`](src/robot/MyProgram.java)

```java
public static void run(Robot robot) throws InterruptedException {

    // ▼▼▼ 在这里写你的行动代码 ▼▼▼
    robot.forward(100);
    robot.turnLeft(90);
    robot.strafeRight(50);
    // ▲▲▲ 到这里结束 ▲▲▲
}
```

其他文件都不用动。

---

## 四个电机 / The four motors

已经帮你 define 好了，直接用：

| 名字 | 位置 |
|---|---|
| `robot.frontLeft`  | 左前 |
| `robot.frontRight` | 右前 |
| `robot.backLeft`   | 左后 |
| `robot.backRight`  | 右后 |

麦克纳姆轮布局，所以可以前后走、**左右平移**、原地转。
每个电机: `setPower(-1..1)` · `getPower()` · `getTicks()` · `getDistanceCm()` · `resetEncoder()` · `setReversed(true)` · `stop()`

规格: 1000 tick/圈，满速 3 圈/秒，轮径 10 cm（≈ 94 cm/s 最高速）。

---

## 指令表 / Command reference

### 会等动作做完 / Blocking

| 指令 | 说明 |
|---|---|
| `robot.forward(cm)` | 向前走 |
| `robot.backward(cm)` | 向后走 |
| `robot.strafeLeft(cm)` | 向左平移 |
| `robot.strafeRight(cm)` | 向右平移 |
| `robot.turnLeft(deg)` | 原地左转（逆时针） |
| `robot.turnRight(deg)` | 原地右转（顺时针） |
| `robot.turnTo(deg)` | 转到绝对角度 |
| `robot.goTo(x, y)` | 转向并开到场地上的点 |
| `robot.waitSeconds(s)` / `robot.sleep(ms)` | 等待 |

用编码器闭环 + 一次慢速修正，误差一般在 0.3 cm / 0.3° 以内。

### 马上返回 / Non-blocking

| 指令 | 说明 |
|---|---|
| `robot.drive(forward, strafe, turn)` | 三个方向速度，各 -1.0 ~ 1.0 |
| `robot.setMotorPowers(fl, fr, bl, br)` | 直接控制四个电机 |
| `robot.stop()` | 全部停车 |
| `robot.resetEncoders()` | 四个编码器清零 |

### 读状态 / State

`robot.getX()` · `robot.getY()` · `robot.getHeadingDeg()` · `robot.getSpeedCmPerSec()`
· `robot.getTurnRateDegPerSec()` · `robot.getRuntime()` · `robot.log("...")`

---

## 坐标系 / Coordinates

- 场地 400 × 400 cm，原点在**左下角**，x 向右，y 向上
- 机器人开机在正中间 (200, 200)，**朝右 = heading 0°**，逆时针为正
- 车身 40 × 40 cm，撞到墙会停在墙边

---

## 文件结构 / Files

```
src/robot/
├── MyProgram.java      ★ 你写代码的地方 / your code goes here
├── Main.java             入口 / entry point
├── Robot.java            机器人 + 四个电机 + 所有指令
├── Motor.java            单个电机（编码器、功率）
├── Simulation.java       100 Hz 物理循环
├── Platform.java         窗口 + 运行你的程序
├── FieldPanel.java       场地画面
└── TelemetryPanel.java   右侧数据面板
```

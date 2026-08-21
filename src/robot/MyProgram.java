package robot;

/**
 * ★★★ 你只需要改这一个文件 ★★★
 * ★★★ This is the ONLY file you need to edit ★★★
 *
 * 把你的行动代码写在下面 run() 里面，保存 -> 运行 Main -> 机器人马上出发。
 * Put your action code inside run(), save, run Main, and the robot starts.
 *
 * ----------------------------------------------------------------------
 * 可以用的指令 / commands you can use
 * ----------------------------------------------------------------------
 *  会等动作做完 / blocking:
 *      robot.forward(cm);              向前走
 *      robot.backward(cm);             向后走
 *      robot.strafeLeft(cm);           向左平移
 *      robot.strafeRight(cm);          向右平移
 *      robot.turnLeft(degrees);        原地左转（逆时针）
 *      robot.turnRight(degrees);       原地右转（顺时针）
 *      robot.turnTo(degrees);          转到绝对角度（0 度 = 朝右 / +x）
 *      robot.goTo(x, y);               开到场地上的某个点（0..400 cm）
 *      robot.waitSeconds(1.5);         等 1.5 秒
 *      robot.sleep(500);               等 500 毫秒
 *
 *  马上返回，电机保持速度 / non-blocking:
 *      robot.drive(forward, strafe, turn);        每项 -1.0 .. 1.0
 *      robot.setMotorPowers(fl, fr, bl, br);      直接控制四个电机
 *      robot.stop();                              全部停车
 *
 *  单独控制某个电机 / one motor at a time:
 *      robot.frontLeft.setPower(0.8);
 *      robot.frontRight.setPower(-0.5);
 *      robot.backLeft.getTicks();          编码器
 *      robot.backRight.getDistanceCm();    这个轮子走了多远
 *      robot.resetEncoders();              四个编码器清零
 *
 *  读状态 / read state:
 *      robot.getX(), robot.getY()          位置 cm
 *      robot.getHeadingDeg()               朝向，度
 *      robot.getSpeedCmPerSec()            速度
 *      robot.getRuntime()                  已经跑了几秒
 *      robot.log("hello");                 在右边面板打印一行
 * ----------------------------------------------------------------------
 */
public class MyProgram {

    public static void run(Robot robot) throws InterruptedException {

        // ==================================================================
        // ▼▼▼ 在这里写你的行动代码 / WRITE YOUR ACTION CODE HERE ▼▼▼
        // ==================================================================

        robot.log("开始 / start");

        // 示例：走一个正方形（不需要的话整段删掉）
        // Example: drive a square (delete this block and write your own)
        for (int i = 0; i < 4; i++) {
            robot.forward(100);
            robot.turnLeft(90);
            robot.log("完成第 " + (i + 1) + " 条边 / side " + (i + 1) + " done");
        }

        robot.log("结束 / done");

        // ==================================================================
        // ▲▲▲ 你的代码到这里结束 / END OF YOUR CODE ▲▲▲
        // ==================================================================
    }
}

package org.firstinspires.ftc.teamcode;

import android.graphics.Color;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.ReadWriteFile;

import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.util.Locale;

public abstract class AutonomousBaseMercury extends OpMode {
    public final double HEADING_TOLERANCE = 7; //tolerance for heading calculations
    public final double DISTANCE_TOLERANCE = 1.0 / 10; //tolerance for heading calculations
    public final double DEGREES_TO_FEET = 3.96 * Math.PI / 1120 / 12;

    //EXPLANATION:
    // (wheel diameter) * pi / (encoder ticks per rotation) /(inches in a foot)
    // This converts encoder ticks into feet.
    //**WARNING** Always calculate distance CHANGED, since encoders have no
    // concept of direction, and we are moving across a 2D plane.

    public static class MoveState   {
        public static final int STOP = 0;
        public static final int FORWARD = 1;
        public static final int BACKWARD = 2;
        public static final int LEFT = 3;
        public static final int RIGHT = 4;
        public static final int TURN_TOWARDS_GOAL = 5;
        public static final int BACKWARD_SLOW = 9;
        public static final int FULL_STOP = 12;
        public static final int STRAFE_TOWARDS_GOAL = 15;
        public static final int TURN_TOWARDS_ANGLE = 16;
        public static final int LEFT_SLOW = 17;
        public static final int RIGHT_SLOW = 18;
        public static final int TURN_TOWARDS_ANGLE_SLOW = 19;
        public static final int DEFINE_COLOR = 901937;  }

    int i;
    private OpenGLMatrix lastLocation;
    private DcMotor motorFrontRight;
    private DcMotor motorBackLeft;
    private DcMotor motorFrontLeft;
    private DcMotor motorBackRight;
    private DcMotor top;
    private DcMotor front;
    private Servo franny = null; //left servo
    private Servo mobert = null; //right servo
    private Servo servo;
    private VuforiaLocalizer vuforia;
    private VuforiaTrackable relicTemplate;
    private RelicRecoveryVuMark vuMark = RelicRecoveryVuMark.from(relicTemplate);
    private int startDeg;
    public boolean started;
    private ColorSensor sensorColor;
    private double waitTime;
    int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
    VuforiaLocalizer.Parameters vulocal = new VuforiaLocalizer.Parameters(cameraMonitorViewId);
    BNO055IMU imu;
    private boolean blue;//true if blue detected

    //We stateful now
    int gameState;
    int moveState;

    double power;
    public Orientation angles;
    public Acceleration gravity;
    double heading;
    double desiredAngle;
    boolean turnRight;
    int cDistF, lDistF, dDistF; //Forward distance variables
    int cDistS, lDistS, dDistS; //Sideways distance variables
    int cDistW, lDistW, dDistW; //Sideways distance variables
    double sTime; //Shooting timer
    double pTime; //Button presser timer
    double tDiff;
    ElapsedTime runtime = new ElapsedTime();

    int startPos = 6;
    Map map = new Map(startPos); //this map object will allow for easy manipulations.

    final double SCALE_FACTOR = 255;
    float hsvValues[] = {0F, 0F, 0F};

    public void init()  {
        motorFrontRight = hardwareMap.dcMotor.get("frontRight");
        motorFrontLeft = hardwareMap.dcMotor.get("frontLeft");
        motorBackRight = hardwareMap.dcMotor.get("backRight");
        motorBackLeft = hardwareMap.dcMotor.get("backLeft");
        top = hardwareMap.dcMotor.get("top");
        front = hardwareMap.dcMotor.get("front");
        franny = hardwareMap.servo.get("franny");
        mobert = hardwareMap.servo.get("mobert");
        servo = hardwareMap.servo.get("servo");
        sensorColor = hardwareMap.get(ColorSensor.class, "sensorColor");
        waitTime = 0;

//        motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        motorFrontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorFrontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        motorFrontRight.setDirection(DcMotor.Direction.REVERSE);
        motorBackRight.setDirection(DcMotor.Direction.REVERSE);

        I2cAddr colorAddrLeft = I2cAddr.create8bit(0x3C);
        I2cAddr colorAddrRight = I2cAddr.create8bit(0x4C);
        // sensorColor = hardwareMap.get(ColorSensor.class, "sensor_color_distance");
        // sensorDistance = hardwareMap.get(DistanceSensor.class, "sensor_color_distance");

        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);
    }

    public void moveState() {
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        imu.initialize(parameters);

        BNO055IMU.CalibrationData calibrationData = imu.readCalibrationData();
        String filename = "BNO055IMUCalibration.json";
        File file = AppUtil.getInstance().getSettingsFile(filename);
        ReadWriteFile.writeFile(file, calibrationData.serialize());
        telemetry.log().add("saved to '%s'", filename);

        switch (moveState)  {
            case MoveState.STOP:
                // Halts all drivetrain movement of the robot
                motorFrontRight.setPower(0);
                motorFrontLeft.setPower(0);
                motorBackLeft.setPower(0);
                motorBackRight.setPower(0);
                break;
            case MoveState.FORWARD:
                // Moves the bot forward at half speed
                power = .50; //power coefficient
                if (map.distanceToGoal() > DISTANCE_TOLERANCE)  {
                    motorFrontRight.setPower(power);
                    motorFrontLeft.setPower(power);
                    motorBackLeft.setPower(power);
                    motorBackRight.setPower(power); }
                break;
            case MoveState.BACKWARD:
                // Moves the bot backwards at half speed
                power = .50; //power coefficient
                if (map.distanceToGoal() > DISTANCE_TOLERANCE)  {
                    motorFrontRight.setPower(-power);
                    motorFrontLeft.setPower(-power);
                    motorBackLeft.setPower(-power);
                    motorBackRight.setPower(-power); }
                break;
            case MoveState.BACKWARD_SLOW:
                // Moves the bot backwards at minimum speed
                power = .25; //power coefficient
                if (map.distanceToGoal() > DISTANCE_TOLERANCE)  {
                    motorFrontRight.setPower(-power);
                    motorFrontLeft.setPower(-power);
                    motorBackLeft.setPower(-power);
                    motorBackRight.setPower(-power); }
                //servoLeftButton.setPosition(.5); // HACK
                break;
            case MoveState.LEFT:
                // Moves the bot left at half speed
                power = .50; //power coefficient
                if (map.distanceToGoal() > DISTANCE_TOLERANCE)  {
                    motorFrontRight.setPower(power);
                    motorFrontLeft.setPower(-power);
                    motorBackLeft.setPower(-power);
                    motorBackRight.setPower(power);    }
                break;
            case MoveState.LEFT_SLOW:
                // Moves the bot left at half speed
                power = .25; //power coefficient
                if (map.distanceToGoal() > DISTANCE_TOLERANCE)  {
                    motorFrontRight.setPower(power);
                    motorFrontLeft.setPower(-power);
                    motorBackLeft.setPower(-power);
                    motorBackRight.setPower(power);    }
                break;
            case MoveState.RIGHT:
                // Moves the bot right at half speed
                power = .50; //power coefficient
                if (map.distanceToGoal() > DISTANCE_TOLERANCE)  {
                    motorFrontRight.setPower(-power);
                    motorFrontLeft.setPower(power);
                    motorBackLeft.setPower(power);
                    motorBackRight.setPower(-power); }
                break;
            case MoveState.RIGHT_SLOW:
                // Moves the bot right at half speed
                power = .25; //power coefficient
                if (map.distanceToGoal() > DISTANCE_TOLERANCE)  {
                    motorFrontRight.setPower(-power);
                    motorFrontLeft.setPower(power);
                    motorBackLeft.setPower(power);
                    motorBackRight.setPower(-power); }
                break;
            case MoveState.STRAFE_TOWARDS_GOAL:
                // Moves the bot towards the goal, while always pointing at desiredAngle
                double P = .50;
                double H = Math.toRadians(heading);
                double Ht = Math.toRadians(map.angleToGoal());

                motorFrontRight.setPower(-P * Math.sin(H - Ht));
                motorFrontLeft.setPower(-P * Math.sin(H - Ht));
                motorBackLeft.setPower(P * Math.cos(H - Ht));
                motorBackRight.setPower(P * Math.cos(H - Ht));
                break;
            case MoveState.TURN_TOWARDS_GOAL:
                // Orients the bot to face the goal
                power = .25;
                if (heading <= 180) {
                    turnRight = heading <= map.angleToGoal() && heading + 180 >= map.angleToGoal(); }
                else    {
                    turnRight = !(heading >= map.angleToGoal() && heading - 180 <= map.angleToGoal());  }

                if (turnRight)  {
                    motorFrontRight.setPower(power);
                    motorFrontLeft.setPower(-power);
                    motorBackLeft.setPower(-power);
                    motorBackRight.setPower(power); }
                else    {
                    motorFrontRight.setPower(-power);
                    motorFrontLeft.setPower(power);
                    motorBackLeft.setPower(power);
                    motorBackRight.setPower(-power);    }

                break;
            case MoveState.TURN_TOWARDS_ANGLE:
                // Orients the bot to face at desiredAngle.
                power = .3;
                if (heading <= 180) {
                    turnRight = heading <= desiredAngle && heading + 180 >= desiredAngle;   }
                else    {
                    turnRight = !(heading >= desiredAngle && heading - 180 <= desiredAngle);    }

                if (turnRight)  {
                    motorFrontRight.setPower(power);
                    motorFrontLeft.setPower(-power);
                    motorBackLeft.setPower(power);
                    motorBackRight.setPower(-power);    }
                else    {
                    motorFrontRight.setPower(-power);
                    motorFrontLeft.setPower(power);
                    motorBackLeft.setPower(power);
                    motorBackRight.setPower(-power);    }
                break;
            case MoveState.DEFINE_COLOR:
                Color.RGBToHSV((int) (sensorColor.red() * SCALE_FACTOR),
                        (int) (sensorColor.green() * SCALE_FACTOR),
                        (int) (sensorColor.blue() * SCALE_FACTOR),
                        hsvValues); }

        //break;
            /*case MoveState.TURN_TOWARDS_ANGLE_SLOW:
                // Orients the bot to face at desiredAngle.
                power = .2;
                if(heading<=180){
                    turnRight = heading <= desiredAngle && heading + 180 >= desiredAngle;
                }else{
                    turnRight = !(heading >= desiredAngle && heading - 180 <= desiredAngle);
                }

                if(turnRight){
                    motorFrontRight.setPower(power);
                    motorFrontLeft.setPower(-power);
                    motorBackLeft.setPower(-power);
                    motorBackRight.setPower(power);
                }else{
                    motorFrontRight.setPower(-power);
                    motorFrontLeft.setPower(power);
                    motorBackLeft.setPower(power);
                    motorBackRight.setPower(-power);
                }
                break;*/

       // case MoveState.FULL_STOP:
        // Stop ALL robot movement, and resets servo to default pos

        // servo.setPosition(.5);
        motorFrontRight.setPower(0);
        motorFrontLeft.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0); }
        // motorConveyor.setPower(0);  }

        // break;

    //  map.moveRobot(dDistS * DEGREES_TO_FEET, (heading+90%360));
    // map.moveRobot(dDistF * DEGREES_TO_FEET, heading);
//}

    public void gameState() {
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        imu.initialize(parameters);

        BNO055IMU.CalibrationData calibrationData = imu.readCalibrationData();
        String filename = "BNO055IMUCalibration.json";
        File file = AppUtil.getInstance().getSettingsFile(filename);
        ReadWriteFile.writeFile(file, calibrationData.serialize());
        telemetry.log().add("saved to '%s'", filename);

        lDistF = cDistF;
        cDistF = ( motorBackLeft.getCurrentPosition()
                 + motorBackRight.getCurrentPosition()) / 2;
        dDistF = cDistF - lDistF;

        lDistS = cDistS;
        cDistS = ( motorFrontRight.getCurrentPosition()
                 + motorFrontLeft.getCurrentPosition()) / 2;
        dDistW = cDistW - lDistW;

        if(tDiff == 0){
            tDiff = getRuntime();   }   }

    public void telemetry() {
        telemetry.addData("angle to goal ",map.angleToGoal());
        telemetry.addData("Runtime ",getRuntime());

        telemetry.addData("dist from goal ",map.distanceToGoal());
        telemetry.addData("goal (x,y) ","(" +
          map.getGoalX() + "," + 
          map.getGoalY() + ")");
        telemetry.addData("Robot(x,y) ","(" +
          map.getRobotX() + "," + 
          map.getRobotY() + ")");
        telemetry.addData("robot theta",heading);
        telemetry.addData("Am I lined up?", linedUpAngle(5));
        telemetry.addData("Desired Angle", desiredAngle);
        telemetry.addData("moveState", moveState);
        telemetry.addData("gameState", gameState);
    }

    @Override
    public void loop()  {
        gameState();
        moveState();
        telemetry();    }
    public boolean linedUp()    {
        return Math.abs(heading - map.angleToGoal()) < HEADING_TOLERANCE || (heading > 360 - HEADING_TOLERANCE && map.angleToGoal() < HEADING_TOLERANCE || (heading < HEADING_TOLERANCE && map.angleToGoal() > 360 - HEADING_TOLERANCE));
    }
    public boolean linedUpAngle()   {
        return Math.abs(heading - desiredAngle) < HEADING_TOLERANCE || (heading > 360 - HEADING_TOLERANCE && desiredAngle < HEADING_TOLERANCE || (heading < HEADING_TOLERANCE && desiredAngle > 360 - HEADING_TOLERANCE));
    }

    public boolean linedUpAngle(int HEADING_TOLERANCE) {
        return Math.abs(heading - desiredAngle) < HEADING_TOLERANCE || (heading > 360 - HEADING_TOLERANCE && desiredAngle < HEADING_TOLERANCE || (heading < HEADING_TOLERANCE && desiredAngle > 360 - HEADING_TOLERANCE));
    }

    public boolean linedUpRev() {
        return Math.abs(heading - map.angleToGoalRev()) < HEADING_TOLERANCE || (heading > 360 - HEADING_TOLERANCE && map.angleToGoalRev() < HEADING_TOLERANCE || (heading < HEADING_TOLERANCE && map.angleToGoalRev() > 360 - HEADING_TOLERANCE));
    }

    public double actualRuntime() {
        return getRuntime() - tDiff;
    }

    public int Vuforia(int cameraMonitorViewId, String side, VuforiaLocalizer vuforia) {

        int choosen = 0;

        try {

            VuforiaTrackables relicTrackables = vuforia.loadTrackablesFromAsset("RelicVuMark");
            VuforiaTrackable relicTemplate = relicTrackables.get(0);
            relicTemplate.setName("relicVuMarkTemplate"); // can help in debugging; otherwise not necessary

            relicTrackables.activate();
            runtime.reset();
            while (runtime.seconds() < 3) {
                RelicRecoveryVuMark vuMark = RelicRecoveryVuMark.from(relicTemplate);
                if (vuMark != RelicRecoveryVuMark.UNKNOWN) {
                    if(side == "red") {
                        switch (vuMark) {
                            case LEFT:
                                choosen = 3;
                                break;
                            case CENTER:
                                choosen = 2;
                                break;
                            case RIGHT:
                                choosen = 1;
                                break;
                        }
                    }
                    else {
                        switch (vuMark) {
                            case LEFT:
                                choosen = 1;
                                break;
                            case CENTER:
                                choosen = 2;
                                break;
                            case RIGHT:
                                choosen = 3;
                                break;
                        }
                    }
                }
            }
        }catch (Exception e){
            choosen = 0;
        }

        return choosen;
    }

    private String composeTelemetry() {
        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() {
            @Override
            public void run() {
                // Acquiring the angles is relatively expensive; we don't want
                // to do that in each of the three items that need that info, as that's
                // three times the necessary expense.
                angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
                gravity = imu.getGravity();
            }
        });

        telemetry.addLine()
                .addData("status", new Func<String>() {
                    @Override
                    public String value() {
                        return imu.getSystemStatus().toShortString();
                    }
                })
                .addData("calib", new Func<String>() {
                    @Override
                    public String value() {
                        return imu.getCalibrationStatus().toString();
                    }
                });

        telemetry.addLine()
                .addData("heading", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.firstAngle);
                    }
                })
                .addData("roll", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.secondAngle);
                    }
                })
                .addData("pitch", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.thirdAngle);
                    }
                });

        telemetry.addLine()
                .addData("grvty", new Func<String>() {
                    @Override
                    public String value() {
                        return gravity.toString();
                    }
                })
                .addData("mag", new Func<String>() {
                    @Override
                    public String value() {
                        return String.format(Locale.getDefault(), "%.3f",
                                Math.sqrt(gravity.xAccel * gravity.xAccel
                                        + gravity.yAccel * gravity.yAccel
                                        + gravity.zAccel * gravity.zAccel));
                    }
                });
        return formatAngle(angles.angleUnit, angles.firstAngle);
    }

    String formatAngle(AngleUnit angleUnit, double angle) {
        return formatDegrees(AngleUnit.DEGREES.fromUnit(angleUnit, angle));
    }

    String formatDegrees(double degrees) {
        return String.format(Locale.getDefault(), "%.1f", AngleUnit.DEGREES.normalize(degrees));
    }
}
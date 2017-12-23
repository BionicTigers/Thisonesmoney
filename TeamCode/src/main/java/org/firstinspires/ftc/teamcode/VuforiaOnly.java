package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;

@Autonomous(name = "VuforiaOnly", group="Vuforia" )
//public class VuforiaOnly extends AutonomousBaseMercury {

public class VuforiaOnly extends AutonomousBaseMercury {

    public DcMotor motorFrontRight;
    public DcMotor motorFrontLeft;
    public DcMotor motorBackLeft;
    public DcMotor motorBackRight;
    public DcMotor top;
    public DcMotor front;
    public int gameState;
    public ColorSensor sensorColor;
    public double waitTime;
    public Servo servo;
    public Servo mobert;
    public Servo franny;
    public VuforiaTrackable relicTemplate;
    public RelicRecoveryVuMark vuMark = RelicRecoveryVuMark.from(relicTemplate);
    public int moveState;

    VuforiaLocalizer vuforia;



    public void init() {
        motorFrontRight = hardwareMap.dcMotor.get("frontRight");
        motorFrontLeft = hardwareMap.dcMotor.get("frontLeft");
        motorBackRight = hardwareMap.dcMotor.get("backRight");
        motorBackLeft = hardwareMap.dcMotor.get("backLeft");

        front = hardwareMap.dcMotor.get("front");
        top = hardwareMap.dcMotor.get("top");
        servo = hardwareMap.servo.get("servo");

        gameState = 0;
        moveState = 0;
        waitTime = 0;

        sensorColor = hardwareMap.get(ColorSensor.class, "sensorColor");

        motorFrontRight.setDirection(DcMotor.Direction.REVERSE);
        motorBackRight.setDirection(DcMotor.Direction.REVERSE);

        franny = hardwareMap.servo.get("franny");
        mobert = hardwareMap.servo.get("mobert");

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);
        parameters.vuforiaLicenseKey = "AfBkGLH/////AAAAGUUS7r9Ue00upoglw/0yqTBLwhqYHpjwUK9zxmWMMFGuNGPjo/RjNOTsS8POmdQLHwe3/75saYsyb+mxz6p4O8xFwDT7FEYMmKW2NKaLKCA2078PZgJjnyw+34GV8IBUvi2SOre0m/g0X5eajpAhJ8ZFYNIMbUfavjQX3O7P0UHyXsC3MKxfjMzIqG1AgfRevcR/ONOJlONZw7YIZU3STjODyuPWupm2p7DtSY4TRX5opqFjGQVKWa2IlNoszsN0szgW/xJ1Oz5VZp4oDRS8efG0jOq1QlGw7IJOs4XXZMcsk0RW/70fVeBiT+LMzM8Ih/BUxtVVK4pcLMpb2wlzdKVLkSD8LOpaFWmgOhxtNz2M";

        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.FRONT;
        this.vuforia = ClassFactory.createVuforiaLocalizer(parameters);


    }

    public void loop() {


        switch (gameState) {
            case 0:

                if (getRuntime() > waitTime + 2) {
                    gameState = 1;
                    telemetry.addData("vumark", vuMark);
                }

                break;


            case 1: // moving robot to correct position in safe zone
                //This case needs to be after we knock over the correct jewel
                angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS);
                heading = angles.firstAngle;

                if(vuMark == RelicRecoveryVuMark.LEFT) {

                    Map.setGoal(11, 5.4);
                    moveState = AutonomousBaseMercury.MoveState.RIGHT;
                }
                else if(vuMark == RelicRecoveryVuMark.CENTER) {
                    Map.setGoal(11, 5);
                    moveState = AutonomousBaseMercury.MoveState.RIGHT;
                }
                else if(vuMark == RelicRecoveryVuMark.RIGHT) {
                    Map.setGoal(11, 4.6);
                    moveState = AutonomousBaseMercury.MoveState.RIGHT;
                }
                else {
                    Map.setGoal(11, 5);
                    moveState = AutonomousBaseMercury.MoveState.RIGHT;
                }
                break;
        }
    }
}
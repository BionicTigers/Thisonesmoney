package org.firstinspires.ftc.teamcode;


import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ReadWriteFile;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.util.Locale;


@TeleOp(name="Venus Mechanum", group="Venus")


public class VenusMechanum extends OpMode {

    // DRIVETRAIN \\
    public DcMotor motorFrontRight;
    public DcMotor motorFrontLeft;
    public DcMotor motorBackLeft;
    public DcMotor motorBackRight;
    // CAR WASHER \\
    public DcMotor billiam;
    // GLYPH FLIPPER \\
    public Servo hamilton = null;
    // LIFT \\
    public DcMotor evangelino; // Left
    public DcMotor wilbert; // Right
    public Servo donneet; // Gate
    // HAMMER \\
    public Servo eddie = null; // Flicker
    public Servo clark = null; // Dropper
    // RELIC \\
    //public DcMotor georgery; // Extender
    //public Servo brandy = null; // Elbow
    //public Servo franny = null; // Left Finger
    //public Servo mobert = null; // Right Finger
// VARIABLES \\
    public double elbowPos;
    private int targetPos;
    private double topPos;


    public void init() {

// DRIVETRAIN \\
        motorFrontRight = hardwareMap.dcMotor.get("frontRight");
        motorFrontLeft = hardwareMap.dcMotor.get("frontLeft");
        motorBackRight = hardwareMap.dcMotor.get("backRight");
        motorBackLeft = hardwareMap.dcMotor.get("backLeft");

        motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
// CAR WASHER \\
        billiam = hardwareMap.dcMotor.get("billiam");
// GLYPH FLIPPER \\
        hamilton = hardwareMap.servo.get("hamilton");
// LIFT \\
        evangelino = hardwareMap.dcMotor.get("evangelino");
        wilbert = hardwareMap.dcMotor.get("wilbert");
        donneet = hardwareMap.servo.get("donneet");
// HAMMER \\
        eddie = hardwareMap.servo.get("eddie");
        clark = hardwareMap.servo.get("clark");
// RELIC \\
        //georgery = hardwareMap.dcMotor.get("georgery");
        //brandy = hardwareMap.servo.get("brandy");
        //franny = hardwareMap.servo.get("franny");
        //mobert = hardwareMap.servo.get("mobert");
// VARIABLES \\
        motorBackLeft.setDirection(DcMotor.Direction.REVERSE);
        motorFrontLeft.setDirection(DcMotor.Direction.REVERSE);
        elbowPos = 0.00;
        targetPos = 0;
        topPos = 0;
    }


    public void loop() {
        telemetry.addData("evangelino", evangelino.getCurrentPosition());
        telemetry.addData("TopPos", topPos);
        telemetry.addData("targetPos", targetPos);
        telemetry.update();
        topPos = evangelino.getCurrentPosition();


        ///////////////
        // GAMEPAD 1 //
        ///////////////

        double P = Math.hypot(-gamepad1.left_stick_x, -gamepad1.left_stick_y);
        double robotAngle = Math.atan2(-gamepad1.left_stick_y, -gamepad1.left_stick_x);
        double rightX = -gamepad1.right_stick_x;
        double sinRAngle = Math.sin(robotAngle);
        double cosRAngle = Math.cos(robotAngle);

        final double v1 = (P * sinRAngle) + (P * cosRAngle) + rightX;
        final double v2 = (P * sinRAngle) - (P * cosRAngle) - rightX;
        final double v3 = (P * sinRAngle) - (P * cosRAngle) + rightX;
        final double v4 = (P * sinRAngle) + (P * cosRAngle) - rightX;

        motorFrontRight.setPower(v1);
        motorFrontLeft.setPower(v2);
        motorBackRight.setPower(v3);
        motorBackLeft.setPower(v4);

// RELIC //
//    if (gamepad1.dpad_up) { // Extension
//        georgery.setPower(0.75); }
//    else if (gamepad1.dpad_down) {
//        georgery.setPower(-0.75); }
//    else {
//        georgery.setPower(0.0); }
//
//    if (gamepad1.right_bumper) { // Grabbing
//        elbowPos += .01;
//        brandy.setPosition(elbowPos); }
//
//    else if (gamepad1.left_bumper) {
//        elbowPos -= .01;
//        brandy.setPosition(elbowPos); }
//
//    if (gamepad1.right_trigger > .7) { // Grabbing
//        franny.setPosition(0.00);
//        mobert.setPosition(0.00); }
//    else if (gamepad1.left_trigger > .7) {
//        franny.setPosition(1.00);
//        mobert.setPosition(1.00); }
//
//    if (gamepad2.dpad_up) {
//            brandy.setPosition(0.3); } // Elbow
//
//    if (gamepad2.dpad_down) {
//            brandy.setPosition(0.9); }
//
//    if (gamepad2.dpad_left) {
//            franny.setPosition(0.5); }
//
//    if (gamepad2.dpad_right) {
//            franny.setPosition(1); }


        ///////////////
        // GAMEPAD 2 //
        ///////////////

// FOUR BAR WITH ENCODERS //
        if (gamepad2.a) {
            evangelino.setPower(-.90);
            wilbert.setPower(.90);

        } else if (gamepad2.y) {
            evangelino.setPower(.90);
            wilbert.setPower(-.90);

        } else {
            evangelino.setPower(0);
            wilbert.setPower(0);
        }

// GATE //
        if (gamepad2.x) {
            donneet.setPosition(1);
        } else if (gamepad2.b) {
            donneet.setPosition(.4);
        }

// CAR WASHER //
        if (gamepad2.right_bumper) {
            billiam.setPower(1);
        } else if (gamepad2.right_trigger > .7) {
            billiam.setPower(-1);
        } else {
            billiam.setPower(0.00);
        }

// GLYPH FLIPPER //
        if (gamepad2.left_trigger > .7) {
            hamilton.setPosition(0.3);
        } else if (gamepad2.left_bumper) {
            hamilton.setPosition(1);
        } else if (gamepad2.dpad_up) {
            hamilton.setPosition(0.4);
        }
    }
}
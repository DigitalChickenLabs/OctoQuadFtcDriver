/*
 * Copyright (c) 2022 DigitalChickenLabs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.Telemetry;

@TeleOp(name = "OctoQuad Sample", group="OctoQuad")
public class OctoQuadSample extends LinearOpMode {

  private OctoQuadBlocks octoquad;
  private DcMotor left_drive;
  private DcMotor right_drive;

  /**
   * This function is executed when this Op Mode is selected from the Driver Station.
   */
  @Override
  public void runOpMode() {
    octoquad = hardwareMap.get(OctoQuadBlocks.class, "octoquad");
    left_drive = hardwareMap.get(DcMotor.class, "left_drive");
    right_drive = hardwareMap.get(DcMotor.class, "right_drive");

    // Read the Firmware Revision number from an OctoQuad and returns it as text.
    telemetry.addData("OctoQuad Version ", octoquad.version());
    telemetry.update();
    
    // Reverse one of the drive motors.
    // You will have to determine which motor to reverse for your robot.
    left_drive.setDirection(DcMotorSimple.Direction.REVERSE);
    
    // Reverse the count-direction of an encoder being read by an OctoQuad interface module.
    // You will REALLY need to test which one need to be flipped because the OctoQuad input
    // will not be automatically Inverted for a motor that runs against the FTC default direction
    octoquad.reverseEncoderDirection(0, true);
    octoquad.reverseEncoderDirection(1, false);

    waitForStart();

    telemetry.setDisplayFormat(Telemetry.DisplayFormat.MONOSPACE);
    while (opModeIsActive()) {
      telemetry.addData(">", "Press X to Reset Encoders");
    
      // Use left stick to drive and right stick to turn (POV mode)
      // The Y axis of a joystick ranges from -1 in its top-most position
      // to +1 in its bottom-most position. We negate this value so that
      // the topmost position corresponds to maximum forward power.
      left_drive.setPower(-gamepad1.left_stick_y + gamepad1.right_stick_x);
      right_drive.setPower(-gamepad1.left_stick_y - gamepad1.right_stick_x);
    
      // Check for X button to reset encoders
      if (gamepad1.x) {
        // Reset the position of all encoders to zero.
        octoquad.resetAllEncoders();
      }
    
      // Read the position of an encoder connected to an OctoQuad.
      telemetry.addData("Left  Pwr / Pos", "%5.2f / %d",left_drive.getPower(), octoquad.getPosition(0));
      telemetry.addData("Right Pwr / Pos", "%5.2f / %d", right_drive.getPower(), octoquad.getPosition(1));
      telemetry.update();
    }
  }
}

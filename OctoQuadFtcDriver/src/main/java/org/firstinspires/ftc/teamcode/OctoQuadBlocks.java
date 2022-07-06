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

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.ExportClassToBlocks;
import org.firstinspires.ftc.robotcore.external.ExportToBlocks;

@ExportClassToBlocks
public class OctoQuadBlocks
{
    static OctoQuad octoQuad = null;
    static int[] cAge = new int[OctoQuad.ENCODER_LAST + 1];
    static int[] vAge = new int[OctoQuad.ENCODER_LAST + 1];
    static int[] counts = null;
    static short[] velocities = null;

    // ============== Methods that appear as Blocks ================
    @ExportToBlocks(
            parameterLabels = {"hardwareMap", "name"},
            color=0, heading="OctoQuad",
            comment = "Use this Block to access an OctoQuad Encoder Interface board.  " +
                      "Add the OctoQuad to an I2C port in the Robot Configuration, and enter the " +
                      "same device name here.",
            tooltip = "Attach to an OctoQuad module connected to an I2C port"
    )
    /***
     * Attach to OctoQuad hardware based on Name in Robot Configuration.
     * Reset all the encoders and get fresh values (should all be zero)
     */
    public static void attach(HardwareMap map, String name)
    {
        octoQuad = map.get(OctoQuad.class, name);
        octoQuad.resetEverything();
        getFreshCounts();
        getFreshVelocities();
    }

    @ExportToBlocks(
            color=0, heading="OctoQuad",
            comment = "Read the Firmware Revision number from an OctoQuad and returns it as text.",
            tooltip = "Get OctoQuad Revision as text"
    )
    /***
     * Return OctoQuad firmware version
     */
    public static String getVersion()
    {
        return octoQuad.getFirmwareVersion().toString();
    }

    @ExportToBlocks(
            parameterLabels = {"encoder (0-7)"},
            color=0, heading="OctoQuad",
            comment = "Read the position of an encoder connected to an OctoQuad.  " +
                    "Pass the desired channel number to the block.  " +
                    "Valid values are 0 to 7.",
            tooltip = "Get position of selected encoder (0-7)"
    )
    /***
     * Return a fresh copy of the requested encoder count.
     * @param idx  The index number of the desired encoder (0-7)
     * Perform a single bulk read of all Encoders the second time an encoder is read after after a refresh.
     */
    public static int getPosition(int idx) {
        if (++cAge[idx] > 1) {
            getFreshCounts();
        }
        return counts[idx];
    }

    /***
     * Return a fresh copy of the requested encoder velocity.
     * @param idx  The index number of the desired encoder (0-7)
     *
     * Velocity is measured as Counts per VelocityInterval (default = 50 mSec)
     * Change velocityInterval using setVelocityInterval()
     * Perform a single bulk read of all Velocities the second time a velocity is read after a refresh.
     */
    @ExportToBlocks(
            parameterLabels = {"encoder (0-7)"},
            color=0, heading="OctoQuad",
            comment = "Read the velocity of an encoder connected to an OctoQuad.  " +
                    "Pass the desired channel number to the block.  " +
                    "Valid values are 0 to 7.  " +
                    "The units for Velocity are 'Counts/Sample Interval' (Interval defaults to 50 mSec)",
            tooltip = "Get velocity of selected encoder (0-7)"
    )
    public static int getVelocity(int idx) {
        if (++vAge[idx] > 1) {
            getFreshVelocities();
        }
        return velocities[idx];
    }

    /***
     * Set an encoder to reverse it's direction
     * invert = true means invert
     */
    @ExportToBlocks(
            parameterLabels = {"encoder (0-7)", "reverse"},
            color=0, heading="OctoQuad",
            comment = "Reverse the count-direction of an encoder being read by an OctoQuad interface module." +
                    "Pass the desired channel number to the block.  " +
                    "Valid values are 0 to 7." +
                    " The direction is inverted if the 'reverse' input is set to 'true'.",
            tooltip = "Reverse the count-direction of selected encoder (0-7)"

    )
    public static void reverseEncoderDirection(int idx, boolean invert)
    {
        octoQuad.setSingleEncoderDirection(idx, invert);
    }


    /***
     * set the velocity count interval for ALL encoders
     * Interval is set in mSec  Valid values 1-255. Default is 50mS
     */
    @ExportToBlocks(parameterLabels = {"interval (mS)"},
            color=0, heading="OctoQuad",
            comment = "Sets the Velocity Sample Interval for all encoders connected to an OctoQuad. (Advanced)  " +
                    "This interval is when when measuring encoder velocity, and the default is 50 mSec.  " +
                    "Encoder steps are counted every 'Velocity Interval' and reported as 'Velocity'. " +
                    "Since the max velocity is clipped at +/-32767 you can reduce the Sample Interval to prevent overflow.  " +
                    "This is only needed if you have an extremely high pulse rate.  The range of values is 1 - 255.",
            tooltip = "Set the interval over which pulses are counted to determine velocity. (1-255 mSec)"
    )
    public static void setVelocityInterval(int ms)
    {
        octoQuad.setAllVelocitySampleIntervals(ms);
    }

    /***
     * Reset a single encoder.
     */
    @ExportToBlocks(
            parameterLabels = {"encoder (0-7)"},
            color=0, heading="OctoQuad",
            comment = "Reset the position of the selected encoder to zero." +
                    "Pass the desired channel number to the block.  " +
                    "Valid values are 0 to 7.",
            tooltip = "Reset the selected encoder position to zero. (0-7)"
    )
    public static void resetEncoder(int idx)
    {
        octoQuad.resetSinglePosition(idx);
        cAge[idx] = 1;
        vAge[idx] = 1;
    }

    /***
     * Reset all encoders.
     */
    @ExportToBlocks(
            color=0, heading="OctoQuad",
            comment = "Reset the position of all encoders to zero.",
            tooltip = "Reset all encoders to zero. (0-7)"
    )
    public static void resetAllEncoders()
    {
        octoQuad.resetAllPositions();
        java.util.Arrays.fill(cAge, 1);
        java.util.Arrays.fill(vAge, 1);
    }

    // ============== Methods that Do Not appear as Blocks ================

    /***
     * Get a fresh copy of ALL Encoder Counts
     */
    private static void getFreshCounts() {
        counts = octoQuad.readPositionRange(OctoQuad.ENCODER_FIRST, OctoQuad.ENCODER_LAST);
        java.util.Arrays.fill(cAge, 0);
    }

    /***
     * Get a fresh copy of ALL Encoder Velocities
     */
    private static void getFreshVelocities() {
        velocities = octoQuad.readVelocityRange(OctoQuad.ENCODER_FIRST, OctoQuad.ENCODER_LAST);
        java.util.Arrays.fill(vAge, 0);
    }
}


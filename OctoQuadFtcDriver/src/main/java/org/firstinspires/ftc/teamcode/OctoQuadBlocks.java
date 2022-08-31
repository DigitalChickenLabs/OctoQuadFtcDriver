package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;

import org.firstinspires.ftc.robotcore.external.ExportToBlocks;

@I2cDeviceType
@DeviceProperties(xmlTag = "OctoQuad", name = "OctoQuad")
public class OctoQuadBlocks extends OctoQuad
{
    public OctoQuadBlocks(I2cDeviceSynch deviceClient)
    {
        super(deviceClient);
    }

    // --------------------------------------------------------------------------------------------------------------------------------
    // Interface to Blocks Users
    //---------------------------------------------------------------------------------------------------------------------------------
    private int[]   pAge = new int[OctoQuad.ENCODER_LAST + 1];
    private int[]   vAge = new int[OctoQuad.ENCODER_LAST + 1];
    private int[]   positions = null;
    private short[] velocities = null;

    /***
     * Return OctoQuad firmware version
     */
    @ExportToBlocks(
            color=60, heading="OctoQuad",
            comment = "Get the Firmware Revision number from an OctoQuad and returns it as text.",
            tooltip = "Get OctoQuad Firmware Version as text"
    )
    public String version()
    {
        return getFirmwareVersion().toString();
    }

    /***
     * Return a fresh copy of the requested encoder count.
     * @param idx  The index number of the desired encoder (0-7)
     * Perform a single bulk read of all Encoders the second time an encoder is read after after a refresh.
     */
    @ExportToBlocks(
            parameterLabels = {"encoder (0-7)"},
            color=60, heading="OctoQuad",
            comment = "Read the position of an encoder connected to an OctoQuad.  " +
                    "Pass the desired channel number to the block.  " +
                    "Valid values are 0 to 7.",
            tooltip = "Get position of selected encoder (0-7)"
    )
    public int getPosition(int idx) {
        if (--pAge[idx] < 0) {
            refreshPositions();
        }
        return positions[idx];
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
            color=60, heading="OctoQuad",
            comment = "Read the velocity of an encoder connected to an OctoQuad.  " +
                    "Pass the desired channel number to the block.  " +
                    "Valid values are 0 to 7.  " +
                    "The units for Velocity are 'Counts/Sample Interval' (Interval defaults to 50 mSec)",
            tooltip = "Get velocity of selected encoder (0-7)"
    )
    public int getVelocity(int idx) {
        if (--vAge[idx] < 0) {
            refreshVelocities();
        }
        return velocities[idx];
    }

    /***
     * Reset a single encoder.
     */
    @ExportToBlocks(
            parameterLabels = {"encoder (0-7)"},
            color=60, heading="OctoQuad",
            comment = "Reset the position of the selected encoder to zero." +
                    "Pass the desired channel number to the block.  " +
                    "Valid values are 0 to 7.",
            tooltip = "Reset the selected encoder position to zero. (0-7)" 
    )
    public void resetEncoder(int idx)
    {
        resetSinglePosition(idx);
        pAge[idx] = 0;
        vAge[idx] = 0;
    }

    /***
     * Reset all encoders.
     */
    @ExportToBlocks(
            color=60, heading="OctoQuad",
            comment = "Reset the position of all encoders to zero.",
            tooltip = "Reset all encoders to zero."
    )
    public void resetAllEncoders()
    {
        resetAllPositions();
        java.util.Arrays.fill(pAge, 0);
        java.util.Arrays.fill(vAge, 0);
    }

    /***
     * Get a fresh copy of ALL Encoder Counts
     */
    @ExportToBlocks(
            color=60, heading="OctoQuad",
            comment = "Refresh the Position buffer with the latest values. " +
                    "Note: This is done automatically if you are looping reading the same encoders."
    )
    public void refreshPositions() {
        positions = readPositionRange(ENCODER_FIRST, ENCODER_LAST);
        java.util.Arrays.fill(pAge, 1);
    }

    /***
     * Get a fresh copy of ALL Encoder Velocities
     */
    @ExportToBlocks(
            color=60, heading="OctoQuad",
            comment = "Refresh the Velocity buffer with the latest values. " +
                    "Note: This is done automatically if you are looping reading the same encoders."
    )
    public void refreshVelocities() {
        velocities = readVelocityRange(ENCODER_FIRST, ENCODER_LAST);
        java.util.Arrays.fill(vAge, 1);
    }

    // ===========================================================================================================
    // The following blocks set OctoQuad parameters that can be made the "defaults" by saving them in FLASH storage
    // ===========================================================================================================

    /***
     * Save all current OctoQuad settings to FLASH
     */
    @ExportToBlocks(
            color=60, heading="OctoQuad",
            comment = "Save all current Octoquad settings to FLASH memory, for automatic recall on next startup.",
            tooltip = "Save Params to FLASH"
    )
    public void saveSettingsToFlash()
    {
        saveParametersToFlash();
    }


    /***
     * Configure OctoQuad encoder banks.
     * The OctoQuad has two banks of 4 encoders (bank 1 and bank2)
     * Each bank can be either 4 Quadrature encoders, or 4 Absolute Pulse Width encoders
     * Bank assignments cab be :
     * 0 = Bank 1 & 2 Both Quadrature Encoders
     * 1 = Bank 1 & 2 Both Pulse Width Encoders
     * 2 = Bank 1 Quad,  Bank 2 Pulse Width
     */
    @ExportToBlocks(
            parameterLabels = {"Bank Mode (0,1 or 2)"},
            color=60, heading="OctoQuad",
            comment = "Configure Encoder banks.  " +
                    "The OctoQuad has two banks of 4 encoders (bank 1 and bank2).  " +
                    "Each bank can be either 4 Quadrature encoders, or 4 Absolute Pulse Width encoders.  " +
                    "Bank Mode values can be: " +
                    "0 = Bank 1 & 2 Both Quadrature Encoders.  " +
                    "1 = Bank 1 & 2 Both Pulse Width Encoders.  " +
                    "2 = Bank 1 Quad,  Bank 2 Pulse Width.  ",
            tooltip = "Assign what type of encoders are connected"
    )
    public void setBankMode(int mode)
    {
        ChannelBankConfig commands[] = {ChannelBankConfig.ALL_QUADRATURE,
                ChannelBankConfig.ALL_PULSE_WIDTH,
                ChannelBankConfig.BANK1_QUADRATURE_BANK2_PULSE_WIDTH};
        setChannelBankConfig(commands[mode]) ;
    }

    /***
     * Set an encoder's direction to reverse (or not)
     * invert = true means invert
     */
    @ExportToBlocks(
            parameterLabels = {"encoder (0-7)", "reverse"},
            color=60, heading="OctoQuad",
            comment = "Reverse the count-direction of an encoder being read by an OctoQuad interface module." +
                    "Pass the desired channel number to the block.  " +
                    "Valid values are 0 to 7." +
                    " The direction is inverted if the 'reverse' input is set to 'true'.",
            tooltip = "Reverse the count-direction of selected encoder (0-7)"

    )
    public void reverseEncoderDirection(int idx, boolean invert)
    {
        setSingleEncoderDirection(idx, invert);
    }

    /***
     * set the velocity count interval for ALL encoders
     * Interval is set in mSec  Valid values 1-255. Default is 50mS
     */
    @ExportToBlocks(parameterLabels = {"interval (mSec)"},
            color=60, heading="OctoQuad",
            comment = "Sets the Velocity Sample Interval for all encoders connected to an OctoQuad. (Advanced)  " +
                    "This interval is when when measuring encoder velocity, and the default is 50 mSec.  " +
                    "Encoder steps are counted every 'Velocity Interval' and reported as 'Velocity'. " +
                    "Since the max velocity is clipped at +/-32767 you can reduce the Sample Interval to prevent overflow.  " +
                    "This is only needed if you have an extremely high pulse rate.  The range of values is 1 - 255.",
            tooltip = "Set the interval over which pulses are counted to determine velocity. (1-255 mSec)"

    )
    public void setVelocityInterval(int ms)
    {
        setAllVelocitySampleIntervals(ms);
    }


    /***
     * Set Absolute Encoder Pulse Width Range.
     * The Min and Max pulse Width corresponds to 0-360 deg rotation
     * This only applies to Absolute Position Pulse Width Encoders, and is
     * only required if you will be using the encoder Velocity.
     */
    @ExportToBlocks(
            parameterLabels = {"Encoder (0-7)", "Min Pulse (uSec)", "Max Pulse (uSec)"},
            color=60, heading="OctoQuad",
            comment = "Define the pulse width range for an Abs Encoder (default 1,1024)",
            tooltip = "Set Abs Encoder Pulse Width in uSec."
    )
    public void setPulseWidths(int encoder, int minPulse, int maxPulse)
    {
        ChannelPulseWidthParams param = new ChannelPulseWidthParams(minPulse, maxPulse);
        setSingleChannelPulseWidthParams(encoder, param);
    }


}

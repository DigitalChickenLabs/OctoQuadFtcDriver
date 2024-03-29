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

import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.util.RobotLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OctoQuad extends I2cDeviceSynchDevice<I2cDeviceSynch>
{
    public static final int I2C_ADDRESS = 0x30;
    public static final byte OCTOQUAD_CHIP_ID = 0x51;
    public static final int SUPPORTED_FW_VERSION_MAJ = 2;
    public static final ByteOrder OCTOQUAD_ENDIAN = ByteOrder.LITTLE_ENDIAN;
    public static final int ENCODER_FIRST = 0;
    public static final int ENCODER_LAST = 7;
    public static final int NUM_ENCODERS = 8;
    public static final int VELOCITY_MEASUREMENT_INTERVAL_MIN = 1;
    public static final int VELOCITY_MEASUREMENT_INTERVAL_MAX = 255;
    public static final int PULSE_WIDTH_LENGTH_MIN = 1; // us
    public static final int PULSE_WIDTH_LENGTH_MAX = 0xFFFF;

    private static final byte CMD_SET_PARAM = 1;
    private static final byte CMD_READ_PARAM = 2;
    private static final byte CMD_WRITE_PARAMS_TO_FLASH = 3;

    private static final byte CMD_RESET_EVERYTHING = 20;
    private static final byte CMD_RESET_ENCODERS = 21;

    private static final byte PARAM_ENCODER_DIRECTIONS = 0;
    private static final byte PARAM_I2C_RECOVERY_MODE = 1;
    private static final byte PARAM_CHANNEL_BANK_CONFIG = 2;
    private static final byte PARAM_CHANNEL_VEL_INTVL = 3;
    private static final byte PARAM_CHANNEL_PULSE_WIDTH_MIN_MAX = 4;

    private byte directionRegisterData = 0;

    private boolean isInitialized = false;

    public class OctoQuadException extends RuntimeException
    {
        public OctoQuadException(String msg)
        {
            super(msg);
        }
    }

    public OctoQuad(I2cDeviceSynch deviceClient)
    {
        super(deviceClient, true);

        this.deviceClient.setI2cAddress(I2cAddr.create7bit(I2C_ADDRESS));
        super.registerArmingStateCallback(false);
        this.deviceClient.engage();
    }

    public OctoQuad(I2cDeviceSynch deviceClient, boolean deviceClientIsOwned)
    {
        super(deviceClient, deviceClientIsOwned);
    }

    @Override
    protected boolean doInitialize()
    {
        return true;
    }

    @Override
    public Manufacturer getManufacturer()
    {
        return Manufacturer.Other;
    }

    @Override
    public String getDeviceName()
    {
        return "OctoQuad";
    }

    enum RegisterType
    {
        uint8_t(1),
        int32_t(4),
        int16_t(2);

        public final int length;

        RegisterType(int length)
        {
            this.length = length;
        }
    }

    enum Register
    {
        CHIP_ID                           (0x00, RegisterType.uint8_t),
        FIRMWARE_VERSION_MAJOR            (0x01, RegisterType.uint8_t),
        FIRMWARE_VERSION_MINOR            (0x02, RegisterType.uint8_t),
        FIRMWARE_VERSION_ENGINEERING      (0x03, RegisterType.uint8_t),
        COMMAND                           (0x04, RegisterType.uint8_t),
        COMMAND_DAT_0                     (0x05, RegisterType.uint8_t),
        COMMAND_DAT_1                     (0x06, RegisterType.uint8_t),
        COMMAND_DAT_2                     (0x07, RegisterType.uint8_t),
        COMMAND_DAT_3                     (0x08, RegisterType.uint8_t),
        COMMAND_DAT_4                     (0x09, RegisterType.uint8_t),
        COMMAND_DAT_5                     (0x0A, RegisterType.uint8_t),
        COMMAND_DAT_6                     (0x0B, RegisterType.uint8_t),

        ENCODER_0_POSITION                (0x0C, RegisterType.int32_t),
        ENCODER_1_POSITION                (0x10, RegisterType.int32_t),
        ENCODER_2_POSITION                (0x14, RegisterType.int32_t),
        ENCODER_3_POSITION                (0x18, RegisterType.int32_t),
        ENCODER_4_POSITION                (0x1C, RegisterType.int32_t),
        ENCODER_5_POSITION                (0x20, RegisterType.int32_t),
        ENCODER_6_POSITION                (0x24, RegisterType.int32_t),
        ENCODER_7_POSITION                (0x28, RegisterType.int32_t),

        ENCODER_0_VELOCITY                (0x2C, RegisterType.int16_t),
        ENCODER_1_VELOCITY                (0x2E, RegisterType.int16_t),
        ENCODER_2_VELOCITY                (0x30, RegisterType.int16_t),
        ENCODER_3_VELOCITY                (0x32, RegisterType.int16_t),
        ENCODER_4_VELOCITY                (0x34, RegisterType.int16_t),
        ENCODER_5_VELOCITY                (0x36, RegisterType.int16_t),
        ENCODER_6_VELOCITY                (0x38, RegisterType.int16_t),
        ENCODER_7_VELOCITY                (0x3A, RegisterType.int16_t);

        public final byte addr;
        public final int length;

        Register(int addr, RegisterType type)
        {
            this.addr = (byte) addr;
            this.length = type.length;
        }

        public static final Register[] all = Register.values();
    }

    // --------------------------------------------------------------------------------------------------------------------------------
    // PUBLIC API
    //---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Reads the CHIP_ID register of the OctoQuad
     * @return the value in the CHIP_ID register of the OctoQuad
     */
    public byte getChipId()
    {
        return readRegister(Register.CHIP_ID)[0];
    }

    /**
     * Class to represent an OctoQuad firmware version
     */
    public static class FirmwareVersion
    {
        public final int maj;
        public final int min;
        public final int eng;

        public FirmwareVersion(int maj, int min, int eng)
        {
            this.maj = maj;
            this.min = min;
            this.eng = eng;
        }

        @Override
        public String toString()
        {
            return String.format("%d.%d.%d", maj, min, eng);
        }
    }

    /**
     * Get the firmware version running on the OctoQuad
     * @return the firmware version running on the OctoQuad
     */
    public FirmwareVersion getFirmwareVersion()
    {
        byte[] fw = readContiguousRegisters(Register.FIRMWARE_VERSION_MAJOR, Register.FIRMWARE_VERSION_ENGINEERING);

        int maj = fw[0] & 0xFF;
        int min = fw[1] & 0xFF;
        int eng = fw[2] & 0xFF;

        return new FirmwareVersion(maj, min, eng);
    }

    /**
     * Read a single position from the OctoQuad
     * Depending on the channel bank configuration, this may
     * either be quadrature step count, or pulse width.
     * @param idx the index of the encoder to read
     * @return the position for the specified encoder
     */
    public int readSinglePosition(int idx)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        Register register = Register.all[Register.ENCODER_0_POSITION.ordinal()+idx];
        return intFromBytes(readRegister(register));
    }

    /**
     * Reads all positions from the OctoQuad, writing the data into
     * an existing int[] object. The previous values are destroyed.
     * Depending on the channel bank configuration, this may
     * either be quadrature step count, or pulse width.
     * @param out the int[] object to fill with new data
     */
    public void readAllPositions(int[] out)
    {
        verifyInitialization();

        if(out.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("out.length != 8");
        }

        byte[] bytes = readContiguousRegisters(Register.ENCODER_0_POSITION, Register.ENCODER_7_POSITION);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(OCTOQUAD_ENDIAN);

        for(int i = 0; i < NUM_ENCODERS; i++)
        {
            out[i] = buffer.getInt();
        }
    }

    /**
     * Reads all positions from the OctoQuad
     * Depending on the channel bank configuration, this may
     * either be quadrature step count, or pulse width.
     * @return an int[] object with the new data
     */
    public int[] readAllPositions()
    {
        verifyInitialization();

        int[] block = new int[NUM_ENCODERS];
        readAllPositions(block);
        return block;
    }

    /**
     * Read a selected range of encoders
     * Depending on the channel bank configuration, this may
     * either be quadrature step count, or pulse width.
     * @param idxFirst the first encoder (inclusive)
     * @param idxLast the last encoder (inclusive)
     * @return an array containing the requested encoder positions
     */
    public int[] readPositionRange(int idxFirst, int idxLast)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idxFirst, ENCODER_FIRST, ENCODER_LAST);
        Range.throwIfRangeIsInvalid(idxLast, ENCODER_FIRST, ENCODER_LAST);

        Register registerFirst = Register.all[Register.ENCODER_0_POSITION.ordinal()+idxFirst];
        Register registerLast = Register.all[Register.ENCODER_0_POSITION.ordinal()+idxLast];

        byte[] data = readContiguousRegisters(registerFirst, registerLast);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int numEncodersRead = idxLast-idxFirst+1;
        int[] encoderCounts = new int[numEncodersRead];

        for(int i = 0; i < numEncodersRead; i++)
        {
            encoderCounts[i] = buffer.getInt();
        }

        return encoderCounts;
    }

    /**
     * Reset a single encoder in the OctoQuad firmware
     * @param idx the index of the encoder to reset
     */
    public void resetSinglePosition(int idx)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        byte dat = (byte) (1 << idx);
        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_0, new byte[]{CMD_RESET_ENCODERS, dat});
    }

    /**
     * Reset all encoder counts in the OctoQuad firmware
     */
    public void resetAllPositions()
    {
        verifyInitialization();
        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_0, new byte[] {CMD_RESET_ENCODERS, (byte)0xFF});
    }

    /**
     * Reset multiple encoders in the OctoQuad firmware in one command
     * @param resets the encoders to be reset
     */
    public void resetMultiplePositions(boolean[] resets)
    {
        verifyInitialization();

        if(resets.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("resets.length != 8");
        }

        byte dat = 0;

        for(int i = ENCODER_FIRST; i <= ENCODER_LAST; i++)
        {
            dat |= resets[i] ? (byte)(1 << i) : 0;
        }

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_0, new byte[] {CMD_RESET_ENCODERS, dat});
    }

    /**
     * Reset multiple encoders in the OctoQuad firmware in one command
     * @param indices the indices of the encoders to reset
     */
    public void resetMultiplePositions(int... indices)
    {
        verifyInitialization();

        for(int idx : indices)
        {
            Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);
        }

        byte dat = 0;

        for(int idx : indices)
        {
            dat |= 1 << idx;
        }

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_0, new byte[] {CMD_RESET_ENCODERS, dat});
    }

    /**
     * Set the direction for a single encoder
     * This parameter will NOT be retained across power cycles, unless
     * you call {@link #saveParametersToFlash()} ()}
     * @param idx the index of the encoder
     * @param reverse direction
     */
    public void setSingleEncoderDirection(int idx, boolean reverse)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        if(reverse)
        {
            directionRegisterData |= (byte) (1 << idx);
        }
        else
        {
            directionRegisterData &= (byte) ~(1 << idx);
        }

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_SET_PARAM, PARAM_ENCODER_DIRECTIONS, directionRegisterData});
    }

    /**
     * Get the direction for a single encoder
     * @param idx the index of the encoder
     * @return whether the encoder is reversed
     */
    public boolean getSingleEncoderDirection(int idx)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_0, new byte[]{CMD_READ_PARAM, PARAM_ENCODER_DIRECTIONS});
        byte directions = readRegister(Register.COMMAND_DAT_0)[0];

        return (directions & (1 << idx)) != 0;
    }

    /**
     * Set the direction for all encoders
     * This parameter will NOT be retained across power cycles, unless
     * you call {@link #saveParametersToFlash()} ()}
     * @param reverse 8-length direction array
     */
    public void setAllEncoderDirections(boolean[] reverse)
    {
        verifyInitialization();

        if(reverse.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("reverse.length != 8");
        }

        directionRegisterData = 0;

        for(int i = ENCODER_FIRST; i <= ENCODER_LAST; i++)
        {
            if(reverse[i])
            {
                directionRegisterData |= (byte) (1 << i);
            }
        }

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_SET_PARAM, PARAM_ENCODER_DIRECTIONS, directionRegisterData});
    }

    /**
     * Read a single velocity from the OctoQuad
     * NOTE: if using an absolute pulse width encoder, in order to get sane
     * velocity data, you must set the channel min/max pulse width parameter.
     * @param idx the index of the encoder to read
     * @return the velocity for the specified encoder
     */
    public short readSingleVelocity(int idx)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        Register register = Register.all[Register.ENCODER_0_VELOCITY.ordinal()+idx];
        return shortFromBytes(readRegister(register));
    }

    /**
     * Reads all velocities from the OctoQuad, writing the data into
     * an existing short[] object. The previous values are destroyed.
     * NOTE: if using an absolute pulse width encoder, in order to get sane
     * velocity data, you must set the channel min/max pulse width parameter.
     * @param out the short[] object to fill with new data
     */
    public void readAllVelocities(short[] out)
    {
        verifyInitialization();

        if(out.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("out.length != 8");
        }

        byte[] bytes = readContiguousRegisters(Register.ENCODER_0_VELOCITY, Register.ENCODER_7_VELOCITY);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(OCTOQUAD_ENDIAN);

        for(int i = 0; i < NUM_ENCODERS; i++)
        {
            out[i] = buffer.getShort();
        }
    }

    /**
     * Reads all velocities from the OctoQuad
     * NOTE: if using an absolute pulse width encoder, in order to get sane
     * velocity data, you must set the channel min/max pulse width parameter.
     * @return a short[] object with the new data
     */
    public short[] readAllVelocities()
    {
        verifyInitialization();

        short[] block = new short[NUM_ENCODERS];
        readAllVelocities(block);
        return block;
    }

    /**
     * Read a selected range of encoder velocities
     * NOTE: if using an absolute pulse width encoder, in order to get sane
     * velocity data, you must set the channel min/max pulse width parameter.
     * @param idxFirst the first encoder (inclusive)
     * @param idxLast the last encoder (inclusive)
     * @return an array containing the requested velocities
     */
    public short[] readVelocityRange(int idxFirst, int idxLast)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idxFirst, ENCODER_FIRST, ENCODER_LAST);
        Range.throwIfRangeIsInvalid(idxLast, ENCODER_FIRST, ENCODER_LAST);

        Register registerFirst = Register.all[Register.ENCODER_0_VELOCITY.ordinal()+idxFirst];
        Register registerLast = Register.all[Register.ENCODER_0_VELOCITY.ordinal()+idxLast];

        byte[] data = readContiguousRegisters(registerFirst, registerLast);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int numVelocitiesRead = idxLast-idxFirst+1;
        short[] velocities = new short[numVelocitiesRead];

        for(int i = 0; i < numVelocitiesRead; i++)
        {
            velocities[i] = buffer.getShort();
        }

        return velocities;
    }

    public static class EncoderDataBlock
    {
        public int[] positions = new int[NUM_ENCODERS];
        public short[] velocities = new short[NUM_ENCODERS];
    }

    /**
     * Reads all encoder data from the OctoQuad, writing the data into
     * an existing {@link EncoderDataBlock} object. The previous values are destroyed.
     * @param out the {@link EncoderDataBlock} object to fill with new data
     */
    public void readAllEncoderData(EncoderDataBlock out)
    {
        verifyInitialization();

        if(out.positions.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("out.counts.length != 8");
        }

        if(out.velocities.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("out.velocities.length != 8");
        }

        byte[] bytes = readContiguousRegisters(Register.ENCODER_0_POSITION, Register.ENCODER_7_VELOCITY);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(OCTOQUAD_ENDIAN);

        for(int i = 0; i < NUM_ENCODERS; i++)
        {
            out.positions[i] = buffer.getInt();
        }

        for(int i = 0; i < NUM_ENCODERS; i++)
        {
            out.velocities[i] = buffer.getShort();
        }
    }

    /**
     * Reads all encoder data from the OctoQuad
     * @return a {@link EncoderDataBlock} object with the new data
     */
    public EncoderDataBlock readAllEncoderData()
    {
        verifyInitialization();

        EncoderDataBlock block = new EncoderDataBlock();
        readAllEncoderData(block);

        return block;
    }

    /**
     * Set the velocity sample interval for a single encoder
     * This parameter will NOT be retained across power cycles, unless
     * you call {@link #saveParametersToFlash()} ()}
     * @param idx the index of the encoder in question
     * @param intvlms the sample interval in milliseconds
     */
    public void setSingleVelocitySampleInterval(int idx, int intvlms)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);
        Range.throwIfRangeIsInvalid(intvlms, VELOCITY_MEASUREMENT_INTERVAL_MIN, VELOCITY_MEASUREMENT_INTERVAL_MAX);

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_2, new byte[]{CMD_SET_PARAM, PARAM_CHANNEL_VEL_INTVL, (byte)idx, (byte)intvlms});
    }

    /**
     * Set the velocity sample interval for all encoders
     * This parameter will NOT be retained across power cycles, unless
     * you call {@link #saveParametersToFlash()} ()}
     * @param intvlms the sample interval in milliseconds
     */
    public void setAllVelocitySampleIntervals(int intvlms)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(intvlms, VELOCITY_MEASUREMENT_INTERVAL_MIN, VELOCITY_MEASUREMENT_INTERVAL_MAX);

        for(int i = ENCODER_FIRST; i <= ENCODER_LAST; i++)
        {
            writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_2, new byte[]{CMD_SET_PARAM, PARAM_CHANNEL_VEL_INTVL, (byte)i, (byte)intvlms});
        }
    }

    /**
     * Set the velocity sample intervals for all encoders
     * This parameter will NOT be retained across power cycles, unless
     * you call {@link #saveParametersToFlash()} ()}
     * @param intvlms the sample intervals in milliseconds
     */
    public void setAllVelocitySampleIntervals(int[] intvlms)
    {
        verifyInitialization();

        if(intvlms.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("intvls.length != 8");
        }

        for(int i : intvlms)
        {
            Range.throwIfRangeIsInvalid(i, VELOCITY_MEASUREMENT_INTERVAL_MIN, VELOCITY_MEASUREMENT_INTERVAL_MAX);
        }

        for(int i = ENCODER_FIRST; i <= ENCODER_LAST; i++)
        {
            writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_2, new byte[]{CMD_SET_PARAM, PARAM_CHANNEL_VEL_INTVL, (byte)i, (byte)intvlms[i]});
        }
    }

    /**
     * Read a single velocity sample interval
     * @param idx the index of the encoder in question
     * @return the velocity sample interval
     */
    public int getSingleVelocitySampleInterval(int idx)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_READ_PARAM, PARAM_CHANNEL_VEL_INTVL, (byte)idx});
        byte ms = readRegister(Register.COMMAND_DAT_0)[0];
        return ms & 0xFF;
    }

    /**
     * Reads all velocity sample intervals from the OctoQuad
     * @return all velocity sample intervals from the OctoQuad
     */
    public int[] getAllVelocitySampleIntervals()
    {
        verifyInitialization();

        int[] ret = new int[NUM_ENCODERS];

        for(int i = ENCODER_FIRST; i <= ENCODER_FIRST; i++)
        {
            writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_READ_PARAM, PARAM_CHANNEL_VEL_INTVL, (byte)i});
            byte ms = readRegister(Register.COMMAND_DAT_0)[0];
            ret[i] = ms & 0xFF;
        }

        return ret;
    }

    public static class ChannelPulseWidthParams
    {
        public int min_length_us;
        public int max_length_us;

        public ChannelPulseWidthParams() {};

        public ChannelPulseWidthParams(int min_length_us, int max_length_us) {
            this.min_length_us = min_length_us;
            this.max_length_us = max_length_us;
        }
    }

    /**
     * Configure the minimum/maximum pulse width reported by an absolute encoder
     * which is connected to a given channel, to allow the ability to provide
     * accurate velocity data.
     * These parameters will NOT be retained across power cycles, unless
     * you call {@link #saveParametersToFlash()} ()}
     * @param idx the channel in question
     * @param params minimum/maximum pulse width
     */
    public void setSingleChannelPulseWidthParams(int idx, ChannelPulseWidthParams params)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);
        Range.throwIfRangeIsInvalid(params.min_length_us, PULSE_WIDTH_LENGTH_MIN, PULSE_WIDTH_LENGTH_MAX);
        Range.throwIfRangeIsInvalid(params.max_length_us, PULSE_WIDTH_LENGTH_MIN, PULSE_WIDTH_LENGTH_MAX);

        if(params.max_length_us <= params.min_length_us)
        {
            throw new RuntimeException("params.max_length_us <= params.min_length_us");
        }

        ByteBuffer outgoing = ByteBuffer.allocate(7);
        outgoing.order(OCTOQUAD_ENDIAN);
        outgoing.put(CMD_SET_PARAM);
        outgoing.put(PARAM_CHANNEL_PULSE_WIDTH_MIN_MAX);
        outgoing.put((byte)idx);
        outgoing.putShort((short)params.min_length_us);
        outgoing.putShort((short)params.max_length_us);

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_5, outgoing.array());
    }

    /**
     * Queries the OctoQuad to determine the currently set minimum/maxiumum pulse
     * width for an encoder channel, to allow sane velocity data.
     * @param idx the channel in question
     * @return minimum/maximum pulse width
     */
    public ChannelPulseWidthParams getSingleChannelPulseWidthParams(int idx)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_READ_PARAM, PARAM_CHANNEL_PULSE_WIDTH_MIN_MAX, (byte)idx});
        byte[] result = readContiguousRegisters(Register.COMMAND_DAT_0, Register.COMMAND_DAT_3);

        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.order(OCTOQUAD_ENDIAN);

        ChannelPulseWidthParams params = new ChannelPulseWidthParams();
        params.min_length_us = buffer.getShort() & 0xFFFF;
        params.max_length_us = buffer.getShort() & 0xFFFF;

        return params;
    }

    /**
     * Run the firmware's internal reset routine
     */
    public void resetEverything()
    {
        verifyInitialization();

        writeRegister(Register.COMMAND, new byte[]{CMD_RESET_EVERYTHING});
    }

    public enum ChannelBankConfig
    {
        /**
         * Both channel banks are configured for Quadrature input
         */
        ALL_QUADRATURE(0),

        /**
         * Both channel banks are configured for pulse width input
         */
        ALL_PULSE_WIDTH(1),

        /**
         * Bank 1 (channels 0-3) is configured for Quadrature input;
         * Bank 2 (channels 4-7) is configured for pulse width input.
         */
        BANK1_QUADRATURE_BANK2_PULSE_WIDTH(2);

        public byte bVal;

        ChannelBankConfig(int bVal)
        {
            this.bVal = (byte) bVal;
        }
    }

    /**
     * Configures the OctoQuad's channel banks
     * This parameter will NOT be retained across power cycles, unless
     * you call {@link #saveParametersToFlash()} ()}
     * @param config the channel bank configuration to use
     */
    public void setChannelBankConfig(ChannelBankConfig config)
    {
        verifyInitialization();

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_SET_PARAM, PARAM_CHANNEL_BANK_CONFIG, config.bVal});
    }

    /**
     * Queries the OctoQuad to determine the current channel bank configuration
     * @return the current channel bank configuration
     */
    public ChannelBankConfig getChannelBankConfig()
    {
        verifyInitialization();

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_0, new byte[]{CMD_READ_PARAM, PARAM_CHANNEL_BANK_CONFIG});
        byte result = readRegister(Register.COMMAND_DAT_0)[0];

        for(ChannelBankConfig c : ChannelBankConfig.values())
        {
            if(c.bVal == result)
            {
                return c;
            }
        }

        return ChannelBankConfig.ALL_QUADRATURE;
    }

    public enum I2cRecoveryMode
    {
        /**
         * Does not perform any active attempts to recover a wedged I2C bus
         */
        NONE(0),

        /**
         * The OctoQuad will reset its I2C peripheral if 50ms elapses between
         * byte transmissions or between bytes and start/stop conditions
         */
        MODE_1_PERIPH_RST_ON_FRAME_ERR(1),

        /**
         * Mode 1 actions + the OctoQuad will toggle the clock line briefly,
         * once, after 1500ms of no communications.
         */
        MODE_2_M1_PLUS_SCL_IDLE_ONESHOT_TGL(2);

        public byte bVal;

        I2cRecoveryMode(int bVal)
        {
            this.bVal = (byte) bVal;
        }
    }

    /**
     * Configures the OctoQuad to use the specified I2C recovery mode.
     * This parameter will NOT be retained across power cycles, unless
     * you call {@link #saveParametersToFlash()} ()}
     * @param mode the recovery mode to use
     */
    public void setI2cRecoveryMode(I2cRecoveryMode mode)
    {
        verifyInitialization();

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_SET_PARAM, PARAM_I2C_RECOVERY_MODE, mode.bVal});
    }

    /**
     * Queries the OctoQuad to determine the currently configured I2C recovery mode
     * @return the currently configured I2C recovery mode
     */
    public I2cRecoveryMode getI2cRecoveryMode()
    {
        verifyInitialization();

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_0, new byte[]{CMD_READ_PARAM, PARAM_I2C_RECOVERY_MODE});
        byte result = readRegister(Register.COMMAND_DAT_0)[0];

        for(I2cRecoveryMode m : I2cRecoveryMode.values())
        {
            if(m.bVal == result)
            {
                return m;
            }
        }

        return I2cRecoveryMode.NONE;
    }

    /**
     * Stores the current state of parameters to flash, to be applied at next boot
     */
    public void saveParametersToFlash()
    {
        verifyInitialization();

        writeRegister(Register.COMMAND, new byte[] {CMD_WRITE_PARAMS_TO_FLASH});
        try
        {
            Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------------
    // INTERNAL
    //---------------------------------------------------------------------------------------------------------------------------------

    private void verifyInitialization()
    {
        if(!isInitialized)
        {
            byte chipId = getChipId();
            if(chipId != OCTOQUAD_CHIP_ID)
            {
                RobotLog.addGlobalWarningMessage("OctoQuad does not report correct CHIP_ID value; got 0x%X, expect 0x%X", chipId, OCTOQUAD_CHIP_ID);
            }

            FirmwareVersion fw = getFirmwareVersion();

            if(fw.maj != SUPPORTED_FW_VERSION_MAJ)
            {
                RobotLog.addGlobalWarningMessage("OctoQuad is running a different major firmware version than this driver was built for (current=%d; expect=%d)", fw.maj, SUPPORTED_FW_VERSION_MAJ);
            }

            isInitialized = true;
        }
    }

    private static int intFromBytes(byte[] bytes)
    {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(OCTOQUAD_ENDIAN);
        return byteBuffer.getInt();
    }

    private static short shortFromBytes(byte[] bytes)
    {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(OCTOQUAD_ENDIAN);
        return byteBuffer.getShort();
    }

    private byte[] readRegister(Register reg)
    {
        return deviceClient.read(reg.addr, reg.length);
    }

    private byte[] readContiguousRegisters(Register first, Register last)
    {
        int addrStart = first.addr;
        int addrEnd = last.addr + last.length;
        int bytesToRead = addrEnd-addrStart;

        return deviceClient.read(addrStart, bytesToRead);
    }

    private void writeRegister(Register reg, byte[] bytes)
    {
        if(reg.length != bytes.length)
        {
            throw new IllegalArgumentException("reg.length != bytes.length");
        }

        deviceClient.write(reg.addr, bytes);
    }

    private void writeContiguousRegisters(Register first, Register last, byte[] dat)
    {
        int addrStart = first.addr;
        int addrEnd = last.addr + last.length;
        int bytesToWrite = addrEnd-addrStart;

        if(bytesToWrite != dat.length)
        {
            throw new IllegalArgumentException("bytesToWrite != dat.length");
        }

        deviceClient.write(addrStart, dat);
    }
}


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
import com.qualcomm.robotcore.hardware.configuration.annotations.DeviceProperties;
import com.qualcomm.robotcore.hardware.configuration.annotations.I2cDeviceType;
import com.qualcomm.robotcore.util.Range;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@I2cDeviceType
@DeviceProperties(xmlTag = "OctoQuad", name = "OctoQuad")
public class OctoQuad extends I2cDeviceSynchDevice<I2cDeviceSynch>
{
    public static final int I2C_ADDRESS = 0x30;
    public static final byte OCTOQUAD_CHIP_ID = 0x51;
    public static final int SUPPORTED_FW_VERSION_MAJ = 1;
    public static final ByteOrder OCTOQUAD_ENDIAN = ByteOrder.LITTLE_ENDIAN;
    public static final int ENCODER_FIRST = 0;
    public static final int ENCODER_LAST = 7;
    public static final int NUM_ENCODERS = 8;

    private static final byte CMD_RESET_EVERYTHING = 1;
    private static final byte CMD_RESET_ENCODERS = 2;
    private static final byte CMD_SET_DIRECTIONS = 3;
    private static final byte CMD_SET_I2C_RECOVERY_MODE = 4;

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
        COMMAND_DAT_1                     (0x05, RegisterType.uint8_t),
        COMMAND_DAT_2                     (0x06, RegisterType.uint8_t),
        ENCODER_0_COUNT                   (0x08, RegisterType.int32_t),
        ENCODER_1_COUNT                   (0x0C, RegisterType.int32_t),
        ENCODER_2_COUNT                   (0x10, RegisterType.int32_t),
        ENCODER_3_COUNT                   (0x14, RegisterType.int32_t),
        ENCODER_4_COUNT                   (0x18, RegisterType.int32_t),
        ENCODER_5_COUNT                   (0x1C, RegisterType.int32_t),
        ENCODER_6_COUNT                   (0x20, RegisterType.int32_t),
        ENCODER_7_COUNT                   (0x24, RegisterType.int32_t),
        ENCODER_0_VELOCITY                (0x28, RegisterType.int16_t),
        ENCODER_1_VELOCITY                (0x2A, RegisterType.int16_t),
        ENCODER_2_VELOCITY                (0x2C, RegisterType.int16_t),
        ENCODER_3_VELOCITY                (0x2E, RegisterType.int16_t),
        ENCODER_4_VELOCITY                (0x30, RegisterType.int16_t),
        ENCODER_5_VELOCITY                (0x32, RegisterType.int16_t),
        ENCODER_6_VELOCITY                (0x34, RegisterType.int16_t),
        ENCODER_7_VELOCITY                (0x36, RegisterType.int16_t),
        ENCODER_0_VELOCITY_SAMPLE_INTERVAL(0x38, RegisterType.uint8_t),
        ENCODER_1_VELOCITY_SAMPLE_INTERVAL(0x39, RegisterType.uint8_t),
        ENCODER_2_VELOCITY_SAMPLE_INTERVAL(0x3A, RegisterType.uint8_t),
        ENCODER_3_VELOCITY_SAMPLE_INTERVAL(0x3B, RegisterType.uint8_t),
        ENCODER_4_VELOCITY_SAMPLE_INTERVAL(0x3C, RegisterType.uint8_t),
        ENCODER_5_VELOCITY_SAMPLE_INTERVAL(0x3D, RegisterType.uint8_t),
        ENCODER_6_VELOCITY_SAMPLE_INTERVAL(0x3E, RegisterType.uint8_t),
        ENCODER_7_VELOCITY_SAMPLE_INTERVAL(0x3F, RegisterType.uint8_t);

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
    public byte readChipId()
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
     * @param idx the index of the encoder to read
     * @return the count for the specified encoder
     */
    public int readSinglePosition(int idx)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        Register register = Register.all[Register.ENCODER_0_COUNT.ordinal()+idx];
        return intFromBytes(readRegister(register));
    }

    /**
     * Reads all positions from the OctoQuad, writing the data into
     * an existing int[] object. The previous values are destroyed.
     * @param out the int[] object to fill with new data
     */
    public void readAllPositions(int[] out)
    {
        verifyInitialization();

        if(out.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("out.length != 8");
        }

        byte[] bytes = readContiguousRegisters(Register.ENCODER_0_COUNT, Register.ENCODER_7_COUNT);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(OCTOQUAD_ENDIAN);

        for(int i = 0; i < NUM_ENCODERS; i++)
        {
            out[i] = buffer.getInt();
        }
    }

    /**
     * Reads all positions from the OctoQuad
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
     * @param idxFirst the first encoder (inclusive)
     * @param idxLast the last encoder (inclusive)
     * @return an array containing the requested encoder counts
     */
    public int[] readPositionRange(int idxFirst, int idxLast)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idxFirst, ENCODER_FIRST, ENCODER_LAST);
        Range.throwIfRangeIsInvalid(idxLast, ENCODER_FIRST, ENCODER_LAST);

        Register registerFirst = Register.all[Register.ENCODER_0_COUNT.ordinal()+idxFirst];
        Register registerLast = Register.all[Register.ENCODER_0_COUNT.ordinal()+idxLast];

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
        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_RESET_ENCODERS, dat});
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

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[] {CMD_RESET_ENCODERS, dat});
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

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[] {CMD_RESET_ENCODERS, dat});
    }

    /**
     * Reset all encoder counts in the OctoQuad firmware
     */
    public void resetAllPositions()
    {
        verifyInitialization();
        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[] {CMD_RESET_ENCODERS, (byte)0xFF});
    }

    /**
     * Set the direction for a single encoder
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

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_SET_DIRECTIONS, directionRegisterData});
    }

    /**
     * Set the direction for all encoders
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

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_SET_DIRECTIONS, directionRegisterData});
    }

    /**
     * Read a single velocity from the OctoQuad
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
        public int[] counts = new int[NUM_ENCODERS];
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

        if(out.counts.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("out.counts.length != 8");
        }

        if(out.velocities.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("out.velocities.length != 8");
        }

        byte[] bytes = readContiguousRegisters(Register.ENCODER_0_COUNT, Register.ENCODER_7_VELOCITY);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(OCTOQUAD_ENDIAN);

        for(int i = 0; i < NUM_ENCODERS; i++)
        {
            out.counts[i] = buffer.getInt();
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
     * @param idx the index of the encoder in question
     * @param ms the sample interval in milliseconds
     */
    public void setSingleVelocitySampleInterval(int idx, int ms)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);
        Range.throwIfRangeIsInvalid(ms, 0, 255);

        Register register = Register.all[Register.ENCODER_0_VELOCITY_SAMPLE_INTERVAL.ordinal()+idx];
        writeRegister(register, new byte[] {(byte)ms});
    }

    /**
     * Set the velocity sample interval for all encoders
     * @param ms the sample interval in milliseconds
     */
    public void setAllVelocitySampleIntervals(int ms)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(ms, 0, 255);

        byte intvl = (byte) ms;
        byte[] dat = new byte[] {intvl, intvl, intvl, intvl, intvl, intvl, intvl, intvl};

        writeContiguousRegisters(Register.ENCODER_0_VELOCITY_SAMPLE_INTERVAL, Register.ENCODER_7_VELOCITY_SAMPLE_INTERVAL, dat);
    }

    /**
     * Set the velocity sample intervals for all encoders
     * @param intvls the sample intervals in milliseconds
     */
    public void setAllVelocitySampleIntervals(int[] intvls)
    {
        verifyInitialization();

        if(intvls.length != NUM_ENCODERS)
        {
            throw new IllegalArgumentException("intvls.length != 8");
        }

        for(int i : intvls)
        {
            Range.throwIfRangeIsInvalid(i, 0, 255);
        }

        byte[] dat = new byte[intvls.length];

        for(int i = 0; i < intvls.length; i++)
        {
            dat[i] = (byte) intvls[i];
        }

        writeContiguousRegisters(Register.ENCODER_0_VELOCITY_SAMPLE_INTERVAL, Register.ENCODER_7_VELOCITY_SAMPLE_INTERVAL, dat);
    }

    /**
     * Read a single velocity sample interval
     * @param idx the index of the encoder in question
     * @return the velocity sample interval
     */
    public int readSingleVelocitySampleInterval(int idx)
    {
        verifyInitialization();

        Range.throwIfRangeIsInvalid(idx, ENCODER_FIRST, ENCODER_LAST);

        Register register = Register.all[Register.ENCODER_0_VELOCITY_SAMPLE_INTERVAL.ordinal()+idx];

        byte ms = readRegister(register)[0];
        return ms & 0xFF;
    }

    /**
     * Reads all velocity sample intervals from the OctoQuad
     * @return all velocity sample intervals from the OctoQuad
     */
    public int[] readAllVelocitySampleIntervals()
    {
        verifyInitialization();

        byte[] bytes = readContiguousRegisters(Register.ENCODER_0_VELOCITY_SAMPLE_INTERVAL, Register.ENCODER_7_VELOCITY_SAMPLE_INTERVAL);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(OCTOQUAD_ENDIAN);

        int[] ret = new int[NUM_ENCODERS];

        for(int i = 0; i < NUM_ENCODERS; i++)
        {
            ret[i] = buffer.get() & 0xFF;
        }

        return ret;
    }

    /**
     * Run the firmware's internal reset routine
     */
    public void resetEverything()
    {
        verifyInitialization();

        writeRegister(Register.COMMAND, new byte[]{CMD_RESET_EVERYTHING});
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
     * This configuration is retained across power cycles.
     * @param mode the recovery mode to use
     */
    public void setI2cRecoveryModeMode(I2cRecoveryMode mode)
    {
        verifyInitialization();

        writeContiguousRegisters(Register.COMMAND, Register.COMMAND_DAT_1, new byte[]{CMD_SET_I2C_RECOVERY_MODE, mode.bVal});
    }


    // --------------------------------------------------------------------------------------------------------------------------------
    // INTERNAL
    //---------------------------------------------------------------------------------------------------------------------------------

    private void verifyInitialization()
    {
        if(!isInitialized)
        {
            byte chipId = readChipId();
            if(chipId != OCTOQUAD_CHIP_ID)
            {
                throw new OctoQuadException(String.format("OctoQuad does not report correct CHIP_ID value; got 0x%X, expect 0x%X", chipId, OCTOQUAD_CHIP_ID));
            }

            FirmwareVersion fw = getFirmwareVersion();

            if(fw.maj != SUPPORTED_FW_VERSION_MAJ)
            {
                throw new OctoQuadException(String.format("OctoQuad is running a different major firmware version than this driver was built for (expect %d)", SUPPORTED_FW_VERSION_MAJ));
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


# **OctoQuad: Encoder & Pulse Width Interface**

<img title="" src="tutorials/Media/octoquad_ftc.jpg">

*An OctoQuad connected to a REV Control Hub with a quadrature encoder and absolute encoder attached*

## **Overview**

The OctoQuad is an 8-channel quadrature & pulse width decode module. For quadrature signals, position and velocity can be tracked for all 8 channels simultaneously at up to a 250KHz pulse rate (up to 1 million counts/s). For pulse width signals, pulses up to 65535us are supported. The channel inputs and I2C lines are protected from ESD to +/- 15kV (air).

The OctoQuad's I2C connector is pin-compatible with the REV Robotics Control Hub / Expansion Hub.

This project creates a driver which can be loaded as an external library to allow easy integration of the OctoQuad into all three of the FTC programming environments (Blocks, OnBot Java and Android Studio).

In addition to the library project itself, this repository includes [tutorials](tutorials) showing how to install the library, and also provides several sample opmodes.



Latest binaary .arr is [here](https://repo1.maven.org/maven2/io/github/digitalchickenlabs/octoquad-ftc/2.0-A/octoquad-ftc-2.0-A.aar).


*Updated: 8/30/2022*

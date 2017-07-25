Yonah Temperature Logger System 
===============================

**What Is This For**
--------------------
A Temperature Logging System for cold chain logistics. This project will serve as the temperature logging system for Yonah's UAV vaccine delivery service in Papua New Guinea.

**Hardware and System diagram**
-------------------------------
Hardware comprises: 
- smartphone
- microprocessor (arduino)
- temperature sensor (DS18B20)
- lux sensor (TEMT6000)
- votage converter 
- SD card

*System Overview*
![System Diagram](https://github.com/LiTangqing/YonahTemperatureLoggerAndroid/blob/master/images/system_diagram.png)

*Board Schematics*
![Board Schematics](https://github.com/LiTangqing/YonahTemperatureLoggerAndroid/blob/master/images/schematics.png)

**Software**
------------
The software system comprises: 
1. An android phone App that has the following 
Main functionalities:
- communicates with microprocessor (serial communication)
- fetchs temeperature data from microprocessor
- tags incoming data point with current time stamp and send back to microprocessor
- determine geolacation upon opening/closing vacccine box 
- vibrate when phone is not facing up 
- uploads data entries to remote server 

*App Program Work flow*
![Android Program Work Flow](https://github.com/LiTangqing/YonahTemperatureLoggerAndroid/blob/master/images/app_programflow.png)

2. Script for microprocessor board(Arduino)
- get temeperature data 
- constantly check for light signal and determine if box is opened
- write to SD card 

**More**
--------
This project is initiated as part of [Yonah](www.yonah.sg)'s UAV delivery Service.

Yonah CoolerMan 
===============

**What Is This For**
--------------------
A Temperature Logging System for cold chain logistics. This project will serve as the temperature logging system for Yonah's UAV vaccine delivery in Papua New Guinea.

**Hardware and System diagram**
-------------------------------
Hardware comprises: 
- smartphone
- microprocessor (arduino)
- wifi module (esp8266)
- temperature sensor (tmp102)
- SD card

![System Diagram](https://github.com/LiTangqing/YonahCoolerMan/blob/master/System_Diagram.png)

**Software**
------------
The software system comprises: 
1. An android phone App that has the following 
Main functionalities:
- communicates with microprocessor (with wifi module) via UDP 
- fetchs temeperature data from microprocessor
- tags incoming data point with current time stamp and send back to microprocessor
- checks geolacation and auto sends out SMS to receiver when system approaching destiantion 
- generates basic statistics summary(for temperature data) upon arrival(send through sms)
- uploads data entries to remote server(currently working in progress)

2. Script for microprocessor board(Arduino)
- get temeperature data 
- write to SD card 

**More**
--------
This project is created as part of [Yonah](www.yonah.sg)'s UAV delivery Service.

#include <TimerOne.h>
#include <SD.h>
#include <SPI.h>
#include <SoftwareSerial.h>
#include <OneWire.h>
#include <DallasTemperature.h>          //DS18B20 Temperature Sensor


#define period 5000000                  //  time span between two temperature measures, in microsecond(us)
#define LED_GREEN 2
#define LED_YELLOW 3
#define SD_CardDetect 7
#define ONE_WIRE_BUS 4                  //DS18B20 Temperature Sensor
#define LUX_LOWER_LIMIT 10              //boundary of lux intensity between box opened and closed, range:0~1024
#define DATA_LENGTH 16                  //*Temp: xx.xx Box Closed"           


OneWire oneWire(ONE_WIRE_BUS);            //Initialize DS18B20 Temperature sensor
DallasTemperature TempSensor(&oneWire);

File myFile;
File newFile;

//Declare global varaibles
float temperature = 0;
String data;
String logdown;
String check = "*";                     //acknowledge esp8266 the data is sending to it from Arduino
String Temp = "Temp: ";
bool BOX_CLOSED = false;                //indication for box closed
bool BOX_OPENED = false;                //indication for box opened
bool hasLight = true;                   //current light status


void setup() {
  // put your setup code here, to run once:
  pinMode(A0, INPUT);
  analogReference(DEFAULT);                       //Set up TEMT6000 Lux Sensor
  if (analogRead(A0) > LUX_LOWER_LIMIT) {         //Initialize the lux status
    hasLight = true;
  } else {
    hasLight = false;
  }

  Timer1.initialize(period);                      //initialize Timer1
  Timer1.attachInterrupt(sendTemp, period);      //Set sendTemp() as an interrupt. Trigger the function once a period.

  //Setup the Serial Connection between Arduino and phone
  Serial.begin(115200);

  //Set up Temp Sensor
  TempSensor.begin();

  //Setup SD card
  pinMode(10, OUTPUT);
  digitalWrite(10, HIGH);
  SD.begin();
  newFile = SD.open("templogs.txt", FILE_WRITE);    //Create a file on the SD card: "templogs.txt"
  newFile.close();
}


void sendTemp() {
  if (BOX_OPENED) {
    data = "???" + Temp + String(temperature) + "\n";       //"???" stands for box opened
    BOX_OPENED = false;                                        //clear the indicator after send out the message
  } else if (BOX_CLOSED) {
    data = "!!!" + Temp + String(temperature) + "\n";       //"!!!" stands for box closed
    BOX_CLOSED = false;                                      //clear the indicator after send out the message
  } else {
    data = "..." + Temp + String(temperature) + "\n";       //"..." means no change in ambient light
  }
  char sendthis[DATA_LENGTH];
  for (int i = 0; i < DATA_LENGTH - 1; i++) {
    sendthis[i] = data.charAt(i);
  }
  sendthis[(DATA_LENGTH - 1)] = '\0';
  Serial.write(sendthis);                                   
}


void loop() {
   //Temperature sensor read value
  TempSensor.requestTemperatures();
  temperature = TempSensor.getTempCByIndex(0);

  if (hasLight) {
    if (analogRead(A0) < LUX_LOWER_LIMIT) {                  //ambient light change from bright to dark, box closed
      BOX_CLOSED = true;
      hasLight = false;
    }
  } else {
    if (analogRead(A0) > LUX_LOWER_LIMIT) {                  //ambient light change from dark to bright, box opened
      hasLight = true;
      BOX_OPENED = true;
      BOX_CLOSED = false;
    }
  }

  //  //Listen to phone
  while (Serial.available() > 0) {
    logdown = Serial.readString();
    //Serial.println(logdown);
    SD.begin();
    myFile = SD.open("templogs.txt", FILE_WRITE);                     //start to log temperature value and time on SD card
    if (myFile) {                                                   // so you have to close this one before opening another.
        myFile.println(logdown);
        myFile.close();
      } 

  }

}


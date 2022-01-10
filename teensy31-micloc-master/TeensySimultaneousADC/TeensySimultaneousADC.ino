



//FOR 2 ADC
#include <ADC.h>
#include "DHT.h"
#include "Teensy31FastADC.h"

//FOR TEMPERATURE/HUMIDITY SENSOR

#define DHTPIN 12


#define DHTTYPE DHT11 // DHT 11

// Teensy 3.1 has the LED on pin 13
#define LEDPIN 13


DHT dht(DHTPIN, DHTTYPE);

void setup() {

  pinMode(LEDPIN, OUTPUT);
  pinMode(A2, INPUT); 
  pinMode(A3, INPUT); 
  pinMode(A10, INPUT); 
  pinMode(A11, INPUT);
  highSpeed8bitADCSetup();

  Serial.begin(115200);

  //Serial.println("DHTxx test!");
  dht.begin();
  
  //BLINK LED, WE ARE ALIVE
  digitalWrite(LEDPIN,1);
  delay(2000);
  digitalWrite(LEDPIN,0);
}


#define SAMPLES 2048
#define BUFFERSIZE 2048

const int channelA2 = ADC::channel2sc1aADC0[2];
const int channelA3 = ADC::channel2sc1aADC1[3];
const int channelA11 = ADC::channel2sc1aADC0[11];
const int channelA10 = ADC::channel2sc1aADC1[10];

byte THRESHOLD = 180;
byte value1;
byte value2;
byte value3;
byte value4;

byte buffer1[BUFFERSIZE];
byte buffer2[BUFFERSIZE];
byte buffer3[BUFFERSIZE];
byte buffer4[BUFFERSIZE];

int samples;
long startTime;
long stopTime;
long totalTime;
int event;

int i;
int k;


void loop() {
  startTime = micros();
     //START SAMPLING
     //Strange init in this for, but the compiler seems to optimize this code better, so we get faster sampling
  for(i=0,k=0,samples=SAMPLES,event=0;i<samples;i++) {
    //TAKE THE READINGS
    highSpeed8bitAnalogReadMacro(channelA2,channelA3,value1,value2);
    //SHOULD ADJUST THIS 2nd READING
    highSpeed8bitAnalogReadMacro(channelA11, channelA10,value3,value4);
    
    buffer1[k] = value1;
    buffer2[k] = value2;
    buffer3[k] = value3;
    buffer4[k] = value4;
    
    //CHECK FOR EVENTS
    if (value1 > THRESHOLD && !event) {
      event = k;
      //THERE IS AN EVENT, ARE WE REACHING THE END? IF SO TAKE MORE SAMPLES
      if (i > SAMPLES-1024) samples = SAMPLES+1024;
      //SHOULD AJUST TIME LOST IN THIS LOGIC TOO
    }
    
    if (++k == BUFFERSIZE) k = 0; 
  }
  stopTime = micros();
  
  //WAS AN EVENT BEEN DETECTED?
  if (event != 0) {
    printInfo();
    printSamples(); 
  }
 
  //DID WE RECEIVE COMMANDS?
  if (Serial.available()) parseSerial();

}


void parseSerial() {
  char c = Serial.read();

  switch (c) {
  case 'p': 
    printInfo();
    break;
  case 's': 
    printSamples();
    break;
  case '+': 
    THRESHOLD += 5;
    break;             
  case '-': 
    THRESHOLD -= 5;
    break;             
  default:  
    break;
  }
}


void printSamples() {
  
  Serial.print("BUFFSIZE,");
  Serial.print(BUFFERSIZE);
  Serial.print(",Event,");
  Serial.print(event);
  Serial.print(":");
  serialWrite(buffer1,BUFFERSIZE);
  Serial.print(":");
  serialWrite(buffer2,BUFFERSIZE);
  Serial.print(":");
  serialWrite(buffer3,BUFFERSIZE);
  Serial.print(":");
  serialWrite(buffer4,BUFFERSIZE);
  Serial.flush();
  
}


//This should be optimized. Writing raw binary data seems to fail a lot of times
//and I ended up loosing bytes. Maybe some form of flow-control should be used.
void serialWrite(byte *buffer,int siz) {
  int kk;
  for (kk=0;kk<siz;kk++) {
    Serial.print(buffer[kk],HEX);    
    Serial.print(" ");
  }
  Serial.println();
}

void printInfo() {
  totalTime = stopTime-startTime;
  double samplesPerSec = i*1000.0/totalTime;
  
  //Take a temperature/humidity reading
  //The DHT11 should be connected with a resistor for less errors in readings,
  // but works without it if you take some readings untils you got an ok one.
 
  //while(DHT11.read(DHT11PIN) != DHTLIB_OK);

// Connect pin 1 (on the left) of the sensor to +5V
// NOTE: If using a board with 3.3V logic like an Arduino Due connect pin 1
// to 3.3V instead of 5V!
// Connect pin 2 of the sensor to whatever your DHTPIN is
// Connect pin 4 (on the right) of the sensor to GROUND
// Connect a 10K resistor from pin 2 (data) to pin 1 (power) of the sensor
  // Reading temperature or humidity takes about 250 milliseconds!
  // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
  float h = dht.readHumidity();
  // Read temperature as Celsius (the default)
  float t = dht.readTemperature();
  // Read temperature as Fahrenheit (isFahrenheit = true)
  float f = dht.readTemperature(true);
  
  // Check if any reads failed and exit early (to try again).
  if (isnan(h) || isnan(t) || isnan(f)) {
  Serial.println("Failed to read from DHT sensor!");
  return;
  }
  
//  // Compute heat index in Fahrenheit (the default)
//  float hif = dht.computeHeatIndex(f, h);
//  // Compute heat index in Celsius (isFahreheit = false)
//  float hic = dht.computeHeatIndex(t, h, false);



 Serial.print("T,");
  Serial.print(totalTime);
  Serial.print(",Samples,");
  Serial.print(i);
  Serial.print(",Samples/uSec,");
  Serial.print(samplesPerSec,7);
  Serial.print(",Temp,");
  Serial.print(t);
  Serial.print(",Hum,");
  Serial.print(h);
  Serial.print(",Threshold,");
  Serial.print(THRESHOLD);
  Serial.print(":");
  //this serial flush causes a parsing problem
  //Serial.flush();
}

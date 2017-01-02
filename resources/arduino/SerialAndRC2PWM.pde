// ---------------------------------------------------------------------
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// any later version. see <http://www.gnu.org/licenses/>
//
// Use this software in combination with the smartphone app as a 
// Bluetooth to PWM "converter" and with a rc receiver to "overwrite"
// the smartphone app like a instructor/student combination.
// You can do whatever you want with this software, but at your
// own risk. 
// It's tested on a Arduino Pro Mini 5V/16MHz.
// 
// Please take into consideration that a lot of BT modules have a
// limited range. Some not more than 10m! 
// ---------------------------------------------------------------------


// Some stick values
#define STICK_LOW 128
#define STICK_MIDDLE 192
#define STICK_HIGH 255
#define EMERGENCY_VALUE_THRUST 192 // car/boat=192

// Time for failsave activation
#define TWO_SEC 2000

// Output pin definition
#define ROLL_PIN 9
#define NICK_PIN 3
#define YAW_PIN 10
#define THRUST_PIN 11
#define AUX1_PIN 5
#define AUX2_PIN 6
#define LED_PIN 13

// Index of commands within controlBlock
#define ROLL 0
#define NICK 1
#define YAW 2
#define THRUST 3
#define AUX 4
#define MOVE_ROLL 6
#define MOVE_NICK 5
#define MOVE_YAW 7

#define RX_BUFFER_SIZE 64
#define TX_BUFFER_SIZE 128
#define INBUF_SIZE 64

#define MSP_SET_RAW_RC 200 // in message

static volatile uint8_t serialHeadRX[0],serialTailRX[0];
static uint8_t serialBufferRX[RX_BUFFER_SIZE][0];
static volatile uint8_t headTX,tailTX;
static uint8_t bufTX[TX_BUFFER_SIZE];
static uint8_t inBuf[INBUF_SIZE];

static uint8_t checksum;
static uint8_t indRX;
static uint8_t cmdMSP;

static int16_t rcData[8];          // interval [1000;2000] - serial/bluetooth data as well as rc-receiver
static int16_t masterRcData[8];
static int16_t controlBlock[7];    // interval [128;255]

static uint8_t rcControlActive;

uint32_t timestampLastBlockAvailable;

// ******************
// rc functions
// ******************
#define MINCHECK 1168
#define MAXCHECK 1900
#define MAXCHANNELS 8

//RX PIN assignment
#define THROTTLEPIN 2
#define ROLLPIN 4
#define PITCHPIN 5
#define YAWPIN 6
#define AUX1PIN 7
#define AUX2PIN                    7   //unused just for compatibility with MEGA
#define CAM1PIN                    7   //unused just for compatibility with MEGA
#define CAM2PIN                    7   //unused just for compatibility with MEGA

/*********** RC alias *****************/
#define RC_ROLL 0
#define RC_PITCH 3
#define RC_YAW 1
#define RC_THROTTLE 2
#define RC_AUX1 4

static uint8_t pinRcChannel[MAXCHANNELS] = {
  ROLLPIN, PITCHPIN, YAWPIN, THROTTLEPIN, AUX1PIN,AUX2PIN,CAM1PIN,CAM2PIN};
volatile int16_t rcPinValue[8] = {1500,1500,1500,1500,1500,1500,1500,1500}; // interval [1000;2000]
volatile uint8_t signalAvailable;

/**************************************************************************************/
/***************                   RX Pin Setup                    ********************/
/**************************************************************************************/
void configureReceiver() {
  uint8_t chan,a;
  PORTD   = (1<<2) | (1<<4) | (1<<5) | (1<<6) | (1<<7); //enable internal pull ups on the PINs of PORTD (no high impedence PINs)
  PCMSK2 |= (1<<2) | (1<<4) | (1<<5) | (1<<6) | (1<<7); 
  PCICR   = 1<<2; // PCINT activated only for [D0-D7] port
}

ISR(PCINT2_vect) {
  uint8_t mask;
  uint8_t pin;
  uint16_t cTime,dTime;
  static uint16_t edgeTime[8];
  static uint8_t PCintLast;

  pin = PIND;              // PIND indicates the state of each PIN for the arduino port dealing with [D0-D7] digital pins (8 bits variable)
  mask = pin ^ PCintLast;  // doing a ^ between the current interruption and the last one indicates wich pin changed
  sei();                    // re enable other interrupts at this point, the rest of this interrupt is not so time critical and can be interrted safely
  PCintLast = pin;         // we memorize the current state of all PINs [D0-D7]
  cTime = micros();         // micros() return a uint32_t, but it is not usefull to keep the whole bits => we keep only 16 bits
  // mask is pins [D0-D7] that have changed
  // chan = pin sequence of the port. chan begins at D2 and ends at D7
  if (mask & 1<<2)           //indicates the bit 2 of the arduino port [D0-D7], that is to say digital pin 2, if 1 => this pin has just changed
    if (!(pin & 1<<2)) {     //indicates if the bit 2 of the arduino port [D0-D7] is not at a high state (so that we match here only descending PPM pulse)
      dTime = cTime-edgeTime[2]; if (900<dTime && dTime<2200) rcPinValue[2] = dTime; // just a verification: the value must be in the range [1000;2000] + some margin
    } else edgeTime[2] = cTime;    // if the bit 2 of the arduino port [D0-D7] is at a high state (ascending PPM pulse), we memorize the time
  if (mask & 1<<4)   //same principle for other channels   // avoiding a for() is more than twice faster, and it's important to minimize execution time in ISR
    if (!(pin & 1<<4)) {
      dTime = cTime-edgeTime[4]; if (900<dTime && dTime<2200) rcPinValue[4] = dTime;
    } else edgeTime[4] = cTime;
  if (mask & 1<<5)
    if (!(pin & 1<<5)) {
      dTime = cTime-edgeTime[5]; if (900<dTime && dTime<2200) rcPinValue[5] = dTime;
    } else edgeTime[5] = cTime;
  if (mask & 1<<6)
    if (!(pin & 1<<6)) {
      dTime = cTime-edgeTime[6]; if (900<dTime && dTime<2200) rcPinValue[6] = dTime;
    } else edgeTime[6] = cTime;
  if (mask & 1<<7)
    if (!(pin & 1<<7)) {
      dTime = cTime-edgeTime[7]; if (900<dTime && dTime<2200) rcPinValue[7] = dTime;
    } else edgeTime[7] = cTime;
  signalAvailable = (mask & 1<<THROTTLEPIN); // If pulse present on THROTTLE pin signal is available
}
    
/**************************************************************************************/
/***************          combine and sort the RX Datas            ********************/
/**************************************************************************************/
uint16_t readRawRC(uint8_t chan) {
  int16_t data;
  uint8_t oldSREG;
  oldSREG = SREG;
  cli(); // Let's disable interrupts
  data = rcPinValue[pinRcChannel[chan]]; // Let's copy the data Atomically
  SREG = oldSREG;
  sei();// Let's enable the interrupts
  return data; // We return the value correctly copied when the IRQ's where disabled
}

/**************************************************************************************/
/***************          compute and Filter the RX data           ********************/
/**************************************************************************************/
void computeRC() {
  static int16_t rcData4Values[8][4], rcDataMean[8];
  static uint8_t rc4ValuesIndex = 0;
  uint8_t chan,a;
    for (chan = 0; chan < 8; chan++) {
      masterRcData[chan] = readRawRC(chan);
    }  
}

void setup() {
  Serial.begin(115200); // Communication with BT module
  rcControlActive = 1;
  configureReceiver();
  initialSetting();
  setPWMOutputPins();
  pinMode(LED_PIN, OUTPUT);   
}

void loop() {
  serialCom();
  computeRC();
  // if AUX1 is on, overwrite bluetooth rc commands
  if (masterRcData[AUX] > 1600) {
    rcControlActive = 1;
  }
//  rcData[ROLL]   = map(masterRcData[ROLL], 1070, 1890, 1000, 2000); // rc control at every time
//  rcData[NICK] = map(masterRcData[THRUST], 1070, 1870, 1000, 2000); // rc control at every time
  if (masterRcData[AUX] < 1400) {
    rcControlActive = 0;
  }
  if (rcControlActive) {
    mapRcChannels2OutputChannels();
  }

  setPWMOutputPins();
  delay(10);
 
  uint32_t timeSinceLastSignal = millis() - timestampLastBlockAvailable;
  if (rcControlActive < 1 && timeSinceLastSignal > TWO_SEC) {
    failsafeIsActive();
  } 
  else {
    digitalWrite(LED_PIN, LOW);
    for(uint8_t i=0;i<8;i++) {
      controlBlock[i] = map(rcData[i], 1000, 2000, 128, 255);
      if (controlBlock[i] > 255)
        controlBlock[i] = 255;
      if (controlBlock[i] < 128)
        controlBlock[i] = 128;
    }
  }  
}

void mapRcChannels2OutputChannels() {
    rcData[ROLL]   = map(masterRcData[ROLL], 1070, 1890, 1000, 2000); // comment if rc control is at every time
    rcData[NICK]   = map(masterRcData[NICK], 1050, 1880, 1000, 2000); // changed thrust<->nick
    rcData[YAW]    = map(masterRcData[YAW],  1050, 1880, 1000, 2000);
    rcData[THRUST] = map(masterRcData[THRUST], 1070, 1870, 1000, 2000); // changed thrust<->nick | comment if rc control is at every time
    rcData[AUX] = masterRcData[AUX];
    rcData[MOVE_ROLL] = masterRcData[MOVE_ROLL];
    rcData[MOVE_NICK] = masterRcData[MOVE_NICK];
    rcData[MOVE_YAW] = masterRcData[MOVE_YAW];
    /*
    Serial.print(masterRcData[ROLL], DEC);
    Serial.print("\t");
    Serial.print(masterRcData[NICK], DEC);
    Serial.print("\t");
    Serial.print(masterRcData[YAW], DEC);
    Serial.print("\t");
    Serial.print(masterRcData[THRUST], DEC);
    Serial.println();
    */
}

void failsafeIsActive() {
  digitalWrite(LED_PIN, HIGH);
  failsafeSetting();
}

uint16_t read16() {
  uint16_t t = read8();
  t+= (uint16_t)read8()<<8;
  return t;
}
uint8_t read8()  {
  return inBuf[indRX++]&0xff;
}

void serialCom() {
  uint8_t c;  
  static uint8_t offset;
  static uint8_t dataSize;
  static enum _serial_state {
    IDLE,
    HEADER_START,
    HEADER_M,
    HEADER_ARROW,
    HEADER_SIZE,
    HEADER_CMD,
  } 
  c_state = IDLE;

  while (Serial.available()) {
    uint8_t bytesTXBuff = ((uint8_t)(headTX-tailTX))%TX_BUFFER_SIZE; // indicates the number of occupied bytes in TX buffer
    if (bytesTXBuff > TX_BUFFER_SIZE - 40 ) return; // ensure there is enough free TX buffer to go further (40 bytes margin)
    c = Serial.read();

    if (c_state == IDLE) {
      c_state = (c=='$') ? HEADER_START : IDLE;
    } 
    else if (c_state == HEADER_START) {
      c_state = (c=='M') ? HEADER_M : IDLE;
    } 
    else if (c_state == HEADER_M) {
      c_state = (c=='<') ? HEADER_ARROW : IDLE;
    } 
    else if (c_state == HEADER_ARROW) {
      if (c > INBUF_SIZE) {  // now we are expecting the payload size
        c_state = IDLE;
        continue;
      }
      dataSize = c;
      offset = 0;
      checksum = 0;
      indRX = 0;
      checksum ^= c;
      c_state = HEADER_SIZE;  // the command is to follow
    } 
    else if (c_state == HEADER_SIZE) {
      cmdMSP = c;
      checksum ^= c;
      c_state = HEADER_CMD;
    } 
    else if (c_state == HEADER_CMD && offset < dataSize) {
      checksum ^= c;
      inBuf[offset++] = c;
    } 
    else if (c_state == HEADER_CMD && offset >= dataSize) {
      if (checksum == c) {  // compare calculated and transferred checksum
        evaluateCommand();  // we got a valid packet, evaluate it
      }
      c_state = IDLE;
    }
  }
}

void evaluateCommand() {
  switch(cmdMSP) {
  case MSP_SET_RAW_RC:
    for(uint8_t i=0;i<8;i++) {
      rcData[i] = read16();
    }
    timestampLastBlockAvailable = millis();
    break;
  }
}

void failsafeSetting() {
  controlBlock[ROLL]       = STICK_MIDDLE;
  controlBlock[NICK]       = STICK_MIDDLE;
  controlBlock[YAW]       = STICK_MIDDLE;
  controlBlock[THRUST]       = EMERGENCY_VALUE_THRUST;
  controlBlock[MOVE_ROLL]  = STICK_MIDDLE;
  controlBlock[MOVE_NICK]  = STICK_MIDDLE;
  controlBlock[MOVE_YAW]  = STICK_MIDDLE;
}

void initialSetting() {
  rcControlActive = 0;
  controlBlock[ROLL]       = STICK_MIDDLE;
  controlBlock[NICK]       = STICK_MIDDLE;
  controlBlock[YAW]       = STICK_MIDDLE;
  controlBlock[THRUST]       = STICK_MIDDLE; // middle for car/boat, low for others
  controlBlock[MOVE_ROLL]  = STICK_MIDDLE;
  controlBlock[MOVE_NICK]  = STICK_MIDDLE;
  controlBlock[MOVE_YAW]  = STICK_MIDDLE;
}

void setPWMOutputPins() {
/*
  Serial.print(controlBlock[ROLL]);
  Serial.print("\t");
  Serial.print(controlBlock[NICK]);
  Serial.print("\t");
  Serial.print(controlBlock[YAW]);
  Serial.print("\t");
  Serial.print(controlBlock[THRUST]);
  Serial.println();
*/
  analogWrite(ROLL_PIN, controlBlock[ROLL]); 
  analogWrite(NICK_PIN, controlBlock[NICK]);  // changed thrust<->nick back
  analogWrite(THRUST_PIN, controlBlock[THRUST]);  
  analogWrite(YAW_PIN, controlBlock[YAW]);
}


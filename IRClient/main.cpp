/* Version Christian Lefebvre
 *
 * receive commands from serial port to record or send IR sequences
 * wiring = ISEN students work :
 * each pin from 9 to 11 are connected to PNP transistors base,
 *  emitter to IR led anode and collectors to +5.
 *  => set pin to GND opens transistor and thus connects led to +5
 * pin 3 connected to a NPN transistor, emitter to LED cathode, collector to GND
 *  => set pin to +5 opens transistor and thus connects diode to GND
 * pin 3 = PWM 2 => carrier signal is sent to pin 3, with adapted top counter to
 *  obtain the asked frequency
 *
 */

/**
 * TODO :
 * - tester sur les 4 sorties et plusieurs LED enchainées pour valider la puissance
 *   OK avec 2
 *   revoir pour faire une guirlande avec que des LED et une résistance en entrée
 *   => calcul de valeur pour 1 LED, 2, 3 ...
 * - tester toutes les touches sur une vraie STB
 *   sur netgem, touches back, play; stop marchent pas. le reste � l'air ok
 *   l'ensemble est lent � reagir => voir comment enchainer des touches plus rapidement => timer cote java pour comprendre ou ca coince
 * - tester d'autres équipements
 *   dvdk7 de la maison => pas de r�action => tester au labo
 * - simplifier l'enregistrement : commande RECORD up,down,.....
     => split sur , (on laisse tomber les period
     => sortir tout le paragraphe (avec des " � la place des ' !)
     => permet de saisir "en brut" tout une telecommande

     necessite surement d'agrandir le buffer de commande, donc de le partager avec
     l'autre (celui de receive)
     Non : oscillo ecrit dans le buffer, mais on lit dans la commande le nom des touches
   - cot� java :
     - implementer un /RELOAD pour relire les json � chaud
     - implementer /RECORD/protocol => envoie un RECORD de toutes les touches
       associ�es dans le protocole.json => utilisateur peut ensuite copier/coller
       puis faire un RELOAD.
 */
#include "Arduino.h"
#include "avr/io.h"
#include "avr/interrupt.h"
#include "HardwareSerial.h"
#include "pwm/pwm.h"

#define DEBUG 0

#define RECORD_INPUT 2
#define RECORD_INTR 0

// 4 outputs from 9 to 12
#define IR_FIRST_OUTPUT 9
#define IR_NB_OUTPUT 4

#define stringify(value) #value

// output for IR common return = 3 = PWN 2
#define IR_COMMON PWM_2_B

void setup() {
	pinMode(IR_COMMON, OUTPUT);
	digitalWrite(IR_COMMON, LOW);
	pinMode(RECORD_INPUT, INPUT);

	for (int i = IR_FIRST_OUTPUT; i < IR_FIRST_OUTPUT + IR_NB_OUTPUT; i++) {
		pinMode(i, OUTPUT);
		digitalWrite(i, HIGH);
	}

	Serial.begin(115200);
#if DEBUG
	Serial.println("Setup.");
#endif
}

void sendReady() {
	Serial.println("READY");
}

void sendResult(char *result) {
	Serial.print("RESULT:");
	Serial.println(result);
}

/*
 * set timer 2 parameters to obtain given frequency and "on" ratio
 */
unsigned int IRsetParams(unsigned long carrierFreq, unsigned int cycle) {
	// in clock cycles
	unsigned long carrierPeriod = FREQUENCY_TO_PERIOD(carrierFreq);
	// Freqs are 36KHz or 38KHz => periods = 421 or 444 which is > 256
	// => use phase PWM which divide frequency by 2 => ocra = 210 or 222 < 256
	// => carrierPeriod must be /2
	carrierPeriod >>= 1;

#if DEBUG
	Serial.print(" => carrierPeriod/2 =");
	Serial.println(carrierPeriod);
	Serial.print(" => cycle =");
	Serial.println(cycle * carrierPeriod / 256);
#endif

	// counter goes from 0 to OCRA then OCRA to 0,
	// B is high between ocrb mounting then descending
	// => signal with 2*ocra period, with "ON" time of 2*ocrb
	setPWM(2, 0,
		COMPARE_OUTPUT_MODE_NONE, carrierPeriod,
		COMPARE_OUTPUT_MODE_NORMAL, cycle * carrierPeriod / 256,
		WGM_2_PHASE_OCRA, PWM2_PRESCALER_1);

	byte oldSREG = SREG;
	cli();
	TIMSK2 |= 1 << OCIE2A;
	SREG = oldSREG;

	return 1;
}

// one buffer to point to current frame to send
volatile unsigned int *buffer;
volatile unsigned int buflen;

// and variables used from timer interrupt => volatiles
volatile bool state = LOW;
volatile unsigned int index = -1;
volatile byte outputPin = 0;

/*
 * send signal to "out" pin, according to "dataLen" specified timings in "data"
 */
void IRsendData(byte out, unsigned long carrierPeriod,
		unsigned int sigPeriod, unsigned int *data, unsigned int dataLen) {
	buffer = data;
	buflen = dataLen;
	outputPin = out;
	state = LOW; // -> output to GND => light on
	index = 0;

	// translate time value in number of carrier periods, propagating
	// remainder to avoid to shift global timing
	unsigned int remainder = 0;
	for (unsigned int i = 0; i < dataLen; i++) {
		unsigned int duration = buffer[i] * sigPeriod + remainder;
		remainder = duration % carrierPeriod;
		buffer[i] = duration / carrierPeriod;
		if (remainder > carrierPeriod / 2) {
			buffer[i]++;
			remainder = remainder - carrierPeriod;
		}
	}

#if DEBUG
	Serial.println("periods conversion :");
	for (unsigned int i = 0; i < dataLen; i++) {
		Serial.print(" ");
		Serial.print(buffer[i]);
	}
	Serial.println("");
#endif

	digitalWrite(outputPin, state);
}

void counterNext() {
	// while duration is not terminated, do nothing
	if(buffer[index]--) {
		return;
	}
	// prepare next duration value
	index++;
	// if it's last, stop signal :
	if (index == buflen) {
		// remove interrupt handler
		TIMSK2 &= (byte)(~(1 << OCIE2A));
		// and carrier pwm
		setPWMmode(2, COMPARE_OUTPUT_MODE_NONE,
				COMPARE_OUTPUT_MODE_NONE);
		// + light off output
		digitalWrite(outputPin, HIGH);
		return;
	}
	state = !state;
	digitalWrite(outputPin, state);
}

// will be called each time PWM 2 counter loops
ISR(TIMER2_COMPA_vect) {
	counterNext();
}

char *commandRecord();
char *commandSend();

/* read a command from serial port
 * FORMAT :
 *   'S' out(byte, 1->4) frequency(byte, KHz) cycle(byte, ratio/256) period(int, µs)
 *       frameLen(int) frameBytes[frameLen*2](int, nb of periods)
 * or
 *   'R' period(int) messageLen(byte) message[messageLen](char)
 */
char *readCommand() {
	int firstByte = Serial.read();
	switch(firstByte) {
	case -1:
		return (char *)"can't read data";
	case 'R':
		return commandRecord();
	case 'S':
		return commandSend();
	default:
		return (char *)"unknown command";
	}
}

// max number of timing in receive buffer/send command
#define MAX_FRAME_LEN 200
// max len of send command
#define SEND_COMMAND_LEN (8 + MAX_FRAME_LEN * 2 + 1)
// max len of receive buffer
#define REC_BUFFER_LEN (sizeof(unsigned long) * MAX_FRAME_LEN)

// buffer for received commands from serial port
char commandBytes[8 + MAX_FRAME_LEN * 2 + 1];
// the same viewed as a int array
unsigned int *command = (unsigned int *)commandBytes;

// buffer for timings read
unsigned long recBuffer[MAX_FRAME_LEN];
int recBufferSize=0;

volatile unsigned long recordingTimeout;

void recordInterruptHandler() {
	unsigned long now = micros();
// we trust the interrupt and the fact that input should be HIGH at start
//	boolean signalState = digitalRead(RECORD_INPUT);

	if (recBufferSize < MAX_FRAME_LEN) {
		recordingTimeout = now + 100000;
		recBuffer[recBufferSize] = now;
		recBufferSize++;
	}
	// else, buffer full => ignore and let timeout terminate the job
}

void recordKey(char *key) {
	Serial.print("\"");
	Serial.print(key);
	Serial.println("\":");

	// stop wait first input after 5 seconds, then, when receiving info, will
	// reduce this timeout to "last change + 100ms"
	recBufferSize = 0;
	recordingTimeout = micros() + 5000000;
	attachInterrupt(RECORD_INTR, recordInterruptHandler, CHANGE);
	while (micros() < recordingTimeout) {
		// loop until timeout expires
		// will be updated by interruptions
		delayMicroseconds(1000);
	}

	// here, recBuffer contains timestamp of input changes
	// convert them in duration between changes
	for (int i = 0; i < recBufferSize - 1; i++) {
		recBuffer[i] =  recBuffer[i + 1] - recBuffer[i];
	}
	recBufferSize--;

//	Serial.print("// dumping ");
//	Serial.print(recBufferSize);
//	Serial.println(" values :");
//	Serial.print(message);
	Serial.print(" [");

	for(int i = 0; i < recBufferSize; i++) {
		Serial.print((i==0) ? " " : ", ");
		Serial.print(recBuffer[i]);
	}
	Serial.println(" ],");
}

char *commandRecord() {
	if (Serial.readBytes(commandBytes + 1, 2) != 2) {
		return (char *)"can't read data";
	}
	unsigned int messageLen = *((unsigned int *)(commandBytes+1));

	if (Serial.readBytes(commandBytes + 3, messageLen + 1) != (unsigned)(messageLen + 1)) {
		return (char *)"command truncated";
	}

	if(commandBytes[3 + messageLen ] != 'F') {
		return (char *)"bad end marker";
	}

	char *key = commandBytes + 3;
	char *ptr = key;
	key[messageLen] = '\0';

	for(;;) {
		ptr++;
		if (*ptr == '\0') {
			// last key => record it and end
			recordKey(key);
			break;
		} else if (*ptr == ',') {
			// end of key name => record it and loop to next
			*ptr = '\0';
			recordKey(key);
			key = ptr + 1;
		}
	}
	return (char *)"OK";
}

char *commandSend() {
	if(Serial.readBytes(commandBytes+1, 7) != 7) {
		return (char *)"can't read data";
	}
	byte out = commandBytes[1];

	unsigned long frequency = 1000UL * commandBytes[2];
	byte cycle = commandBytes[3];
	unsigned int period = command[2];
	unsigned int frameLen = command[3];
	if(frameLen > MAX_FRAME_LEN) {
		return (char *)"command too long";
	}

	if(Serial.readBytes(commandBytes+8, frameLen * 2 + 1) != frameLen * 2 + 1) {
		return (char *)"command truncated";
	}

	if(commandBytes[8 + frameLen * 2] != 'F') {
		Serial.print("bad end marker ");
		Serial.println(commandBytes[8 + frameLen * 2]);
		return (char *)"bad end marker";
	}

	if (out < 1 || out > IR_NB_OUTPUT) {
		return (char *)"bad output number (must be between 1 and " stringify(IR_NB_OUTPUT) ")";
	}
	out += IR_FIRST_OUTPUT - 1;

#if DEBUG
	Serial.print("commandSend freq=");
	Serial.print(frequency);
	Serial.print(", period=");
	Serial.print(period);
	Serial.print(", cycle=");
	Serial.print(cycle);
	Serial.print(", frame=");
	Serial.print(frameLen);
	Serial.print(":");

	unsigned int i;
	for (i = 0; i < frameLen; i++) {
		Serial.print(" ");
		Serial.print(command[4+i]);
	}
	Serial.println("");
#endif

	unsigned long carrierPeriod = 1000000L / frequency; // in µs
	IRsendData(out, carrierPeriod, period, command+4, frameLen);
	IRsetParams(frequency, cycle);

	return 0;
}

int main(void) {
	init();
	setup();

	delay(1000);

	sendReady();

	for(;;) {
		if (Serial.available() > 0) {
			char *result = readCommand();
			if (result == 0) {
				sendResult((char *)"OK");
			} else {
				sendResult(result);
			}
			sendReady();
		}
	}
}

/* from http://embeddedfreak.wordpress.com/2008/08/08/how-to-open-serial-port-using-rxtx/
 */

package net.atos.aw.tum;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TooManyListenersException;

public class SerialConn implements SerialConnIntf {
	private SerialPort serialPort = null;
	private OutputStream outStream = null;
	private InputStream inStream = null;

	private StringBuffer inBuffer = new StringBuffer();

	public SerialConn(String portName, int baudRate) throws IOException {
		try {
			// ugly hack found here :
			// https://bugs.launchpad.net/ubuntu/+source/rxtx/+bug/367833/comments/6
			System.setProperty("gnu.io.rxtx.SerialPorts", portName);

			// Obtain a CommPortIdentifier object for the port you want to open
			CommPortIdentifier portId = CommPortIdentifier
					.getPortIdentifier(portName);

			// Get the port's ownership
			serialPort = (SerialPort) portId.open("Java SerialConn", 5000);

			// Set the parameters of the connection.
			setSerialPortParameters(baudRate);

			// Open the input and output streams for the connection. If they
			// won't
			// open, close the port before throwing an exception.
			outStream = serialPort.getOutputStream();
			inStream = serialPort.getInputStream();

			try {
				serialPort.addEventListener(new SerialReader());
			} catch (TooManyListenersException e) {
				// should never happen
				throw new RuntimeException(e);
			}
			serialPort.notifyOnDataAvailable(true);
			
		} catch (NoSuchPortException e) {
			throw new IOException(e);
		} catch (PortInUseException e) {
			throw new IOException(e);
		} catch (IOException e) {
			serialPort.close();
			throw e;
		}
	}

	public void write(String data) throws IOException {
		outStream.write(data.getBytes());
	}

	public void write(byte[] data) throws IOException {
		outStream.write(data);
	}

	public String read() {
		synchronized (inBuffer) {
			if (inBuffer.length() == 0) {
				try {
					inBuffer.wait();
				} catch (InterruptedException e) {
					return null;
				}
			}
			int end = inBuffer.indexOf("\n");
			if (end == -1) {
				end = inBuffer.length();
			}

			String result;
			// result without \n nor \r before if any
			if (end > 1 && inBuffer.charAt(end-1) == '\r') {
				result = inBuffer.substring(0, end-1);
			} else {
				result = inBuffer.substring(0, end);
			}
			inBuffer.delete(0, end+1); // delete this line "\n" included

			return result;
		}			
	}

//	/**
//	 * Get the serial port input stream
//	 * 
//	 * @return The serial port input stream
//	 */
//	public InputStream getSerialInputStream() {
//		return inStream;
//	}
//
//	/**
//	 * Get the serial port output stream
//	 * 
//	 * @return The serial port output stream
//	 */
//	public OutputStream getSerialOutputStream() {
//		return outStream;
//	}

	public void close() {
		System.err.println("closing serial");
		serialPort.close();
		serialPort.removeEventListener();
		serialPort = null;
		outStream = null;
		inStream = null;
	}

	/**
	 * Sets the serial port parameters
	 */
	private void setSerialPortParameters(int baudRate) throws IOException {
		try {
			serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		} catch (UnsupportedCommOperationException ex) {
			throw new IOException("Unsupported serial port parameter");
		}
	}

	static public List<String> getAvailablePorts() {
		List<String> list = new ArrayList<String>();
		@SuppressWarnings("rawtypes")
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList
					.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				list.add(portId.getName());
			}
		}
		return list;
	}

	class SerialReader implements SerialPortEventListener  {
		public void serialEvent(SerialPortEvent arg0) {
			int data;
			char[] buffer = new char[1000];
			int index = 0;

			try {
				while ( index < 1000 && (data = inStream.read()) > -1 ) {
					buffer[index] = (char)data;
					index++;
					if ( data == '\n' ) {
						break;
					}
				}
			} catch ( IOException e ) {
				e.printStackTrace();
				System.exit(-1);
			}

			synchronized (inBuffer) {
				inBuffer.append(buffer, 0, index);
				inBuffer.notifyAll();
			}
		}

	}
		
}

/* from http://embeddedfreak.wordpress.com/2008/08/08/how-to-open-serial-port-using-rxtx/
 */

package net.atos.aw.tum;

import net.atos.aw.tum.utils.HexDump;

public class FakeSerialConn implements SerialConnIntf {

	public FakeSerialConn() {
		System.out.println("Opening fake serial\n.");
	}

	public void write(String data) {
		System.out.println("Sending\n" + data + "\n.");
	}

	public void write(byte[] data) {
		System.out.println("Sending\n" + HexDump.toHexDump(data) + "\n.");
	}

	String result[] = {
			"RESULT:OK",
			"READY"
	};
	int index = 0;

	public String read() {
		String res = result[index];
		index++;
		if (index >= result.length) {
			index = 0;
		}
		return res;
	}

	public void close() {
		System.out.println("Closing fake serial\n.");
	}
}

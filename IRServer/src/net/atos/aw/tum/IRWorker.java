package net.atos.aw.tum;

import java.io.IOException;
import java.util.Iterator;

import net.atos.aw.tum.utils.HexDump;

public class IRWorker {
	private SerialConnIntf cnx;

	public static final String READY_STRING = "READY";
	public static final String RESULT_STRING = "RESULT:";

	public IRWorker(SerialConnIntf cnx) {
		this.cnx = cnx;
		waitResult();
		System.err.println("worker Ready");
	}

	public String waitResult() {
		String line;
		String result = null;
		for(;;) {
			line = cnx.read();
			System.err.println(" <== '" + line + "'");
			if (line.startsWith(RESULT_STRING)) {
				result = line.substring(RESULT_STRING.length());
			} else if (line.equals(READY_STRING)) {
				return result;
			}
		}
	}

	public String sendKey(String remoteName, String key) {
		IRRemote remote = IRRemote.getRemote(remoteName);
		if (remote == null) {
			return "Unknown remote";
		}
		byte[] command = remote.getCommand(key);
		if (command == null) {
			return "Unknown key";
		}

		System.err.println(" ==> \n" + HexDump.toHexDump(command));

		// send to serial
		try {
			synchronized (cnx) {
				cnx.write(command);
				return waitResult();
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return "Exception " + e;
		}
	}

	public String recordKey(String key) {
		int len = key.length();
		byte[] command = new byte[3 + len + 1];

		command[0] = 'R';
		command[1] = (byte)(len & 0xff);
		command[2] = (byte)(len >> 8);
		System.arraycopy(key.getBytes(), 0, command, 3, len);
		command[3 + len] = 'F';

		System.err.println(" ==> \n" + HexDump.toHexDump(command));

		try {
			synchronized (cnx) {
				cnx.write(command);
				return waitResult();
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return "Exception " + e;
		}
	}

	public String recordKeys(String protocolName) {
		// get list of keys for this protocol
		IRProtocol protocol = IRProtocol.getProtocol(protocolName);
		if (protocol == null) {
			return "Unknown protocol";
		}

		Iterator<String> keys = protocol.keys.keySet().iterator();
		StringBuffer keyList = new StringBuffer(keys.next());
		while (keys.hasNext()) {
			keyList.append(',');
			keyList.append(keys.next());
		}
		int keyListLen = keyList.length();

		// send to serial
		byte[] command = new byte[3 + keyListLen + 1];
		command[0] = 'R';
		command[1] = (byte)(keyListLen & 0xff);
		command[2] = (byte)(keyListLen >> 8);
		System.arraycopy(keyList.toString().getBytes(), 0, command, 3, keyListLen);
		command[3 + keyListLen] = 'F';

		System.err.println(" ==> \n" + HexDump.toHexDump(command));

		try {
			synchronized (cnx) {
				cnx.write(command);
				return waitResult();
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return "Exception " + e;
		}
	}

	public void shutdown() {
		cnx.close();
	}
}

package net.atos.aw.tum.utils;

public class HexDump {
	// xxxx: xx .. xx  xx .. xx nnnnnnnn nnnnnnnn\n
	private static final String HexDumpModel =
			"....: " +
			".. .. .. .. .. .. .. ..  .. .. .. .. .. .. .. .. " +
			"                 \n";
	// => 72 char / 16 byte
	private static final int HexDumpModelLen = HexDumpModel.length();

	private static final String hexDigits = "0123456789abcdef";

	static public char hexDigit(byte value) {
		return hexDigits.charAt(value);
	}

	static public String toHex(int value, int size) {
		StringBuffer result = new StringBuffer(size);
		while(size != 0) {
			result.insert(0, hexDigit((byte)(value & 0xF)));
			value >>= 4;
			size--;
		}
		return result.toString();
	}

	static public String toHexDump(byte[] data) {
		int lines = (data.length + 0xF) >> 4;
		StringBuffer result = new StringBuffer(HexDumpModelLen * lines);

		int iData = 0, iHex = 6, iChar = 55, iLine = 0;
		while(iData < data.length) {
			if ((iData & 0xF) == 0) {
				result.append(HexDumpModel);
				if (iData != 0) {
					iLine += HexDumpModelLen;
					iHex += 23;
					iChar += 57;
				}
				result.replace(iLine, iLine + 4, toHex(iData, 4));
			}
			result.setCharAt(iHex, hexDigit((byte)((data[iData] >> 4) & 0xF)));
			result.setCharAt(iHex+1, hexDigit((byte)(data[iData] & 0xF)));
			iHex+=3;
			if ((iData & 0x7) == 7) {
				iHex++;
			}

			result.setCharAt(iChar, (data[iData] > 32) ? (char)data[iData] : '.');
			iChar++;

			iData++;
		}
		return result.toString();
	}
//	testing example
//	public static void main(String[] args) {
//		byte[] example = ("abcdefghIJKLMNOP" + "ABCDEFGHijklmonp" + "qrs\n\tt").getBytes();
//		System.out.println(toHexDump(example));
//	}
}

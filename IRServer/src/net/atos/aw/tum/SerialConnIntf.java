/* from http://embeddedfreak.wordpress.com/2008/08/08/how-to-open-serial-port-using-rxtx/
 */

package net.atos.aw.tum;

import java.io.IOException;

interface SerialConnIntf {
	public void write(String data) throws IOException;

	public void write(byte[] data) throws IOException;

	public String read();

	public void close();
}

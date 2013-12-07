package net.atos.aw.tum;

import java.io.Console;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class IRConsole extends Thread {
	IRServer parent;
	IRWorker worker;
	Console console;

	public IRConsole(IRServer parent) {
		this.parent = parent;
		worker = parent.worker;

		// TODO : read input, interpret commands :
		// list protocols
		// list remotes
		// list keys proto
		// send remote key
		// record proto
		// reload
		// shutdown / exit / ^D
		// interact remote => read key aliases from keyboard + ESC to exit
		//  => how to read keys from input without buffering
		
		console = System.console();
	}

	private static Map<String, String> keymap;

	/**
	 * load protocol list from json config
	 * @param config
	 * @throws JSONException
	 */
	public static void loadRemotes(JSONObject keymapJson)
			throws JSONException, ConfigException {
		keymap = new HashMap<String, String>();

		@SuppressWarnings("unchecked")
		Iterator<String> iter = keymapJson.keys();
		while(iter.hasNext()) {
			String key = iter.next();
			String value = keymapJson.getString(key);
			keymap.put(key, value);
		}
	}

	public void run() {
		for(;;) {
			System.out.print("> ");
			String command = console.readLine();
			if (command == null) {
				break;
			}
			String words[] = command.split(" ");

			if (words[0].equals("list")) {
				if (words.length < 2) {
					System.err.println("must be : list remotes, protocols or keys protocolName");
					continue;
				}

				if (words[1].equals("remotes")) {
					System.out.println("Defined remotes :");

					Iterator<String> keys = IRRemote.getRemoteNames();
					while (keys.hasNext()) {
						System.out.println("  " + keys.next());
					}

				} else if (words[1].equals("protocols")) {
					System.out.println("Defined protocols :");

					Iterator<String> keys = IRProtocol.getProtocolNames();
					while (keys.hasNext()) {
						System.out.println("  " + keys.next());
					}

				} else if (words[1].equals("keys")) {
					if (words.length != 3) {
						System.err.println("must be : list keys protocolName");
						continue;
					}
					String protocolName = words[2];
					IRProtocol protocol = IRProtocol.getProtocol(protocolName);
					if (protocol == null) {
						System.err.println("Unknown protocol " + protocolName);
						continue;
					}
					Iterator<String> keys = protocol.keys.keySet().iterator();
					System.out.println("Defined keys for protocol " + protocolName + ":");
					while (keys.hasNext()) {
						System.out.println("  " + keys.next());
					}

				} else {
					System.err.println("must be : list remotes, protocols or keys protocolName");
				}

			} else if (words[0].equals("send")) {
				if (words.length != 3) {
					System.err.println("must be : send remoteName keyName");
					continue;
				}
				String remote = words[1];
				String key = words[2];
				System.out.println(worker.sendKey(remote, key));

			} else if (words[0].equals("record")) {
				if (words.length > 2) {
					System.err.println("must be : record [modelName]");
					continue;
				}
				if (words.length == 2) {
					String model = words[1];
					System.out.println(worker.recordKeys(model));
				} else {
					System.out.println(worker.recordKey("one key"));
				}

			} else if (words[0].equals("reload")) {
				parent.load();

			} else if (words[0].equals("interact")) {
				if (words.length != 2) {
					System.err.println("must be : interact remoteName");
					continue;
				}
				String remote = words[1];
				interact(remote);

			} else if (words[0].equals("shutdown")) {
				break;

			} else if (words[0].equals("?") || words[0].equals("help")) {
				System.out.println("list protocols");
				System.out.println("list remotes");
				System.out.println("list keys protocolName");
				System.out.println("send remoteName keyName");
				System.out.println("record [protocolName]");
				System.out.println("reload");
				System.out.println("interact remoteName");
				System.out.println("shutdown / exit / ^D");

			} else if (words[0].equals("")) {
				continue;

			} else {
				System.err.println("Unknown command " + words[0]);
				System.err.println(" type 'help' or '?' for help");
			}
		}
		parent.shutdown();
	}
	
	public void interact(String remote) {
		System.out.println("interactive mode, press ESC ESC to quit");

		String key;
		// TODO : verify this remote exists
		try {
			Stty.setRow();

            while (true) {
            	int ch = System.in.read();
            	if (ch == 27) {
            		int subkey = System.in.read();
            		if (subkey == 27) {
            			break;
            		} else if (subkey == '[') {
            			// special key -> read third part
            			ch = System.in.read();
            			key = "[" + String.valueOf((char)ch);
            		} else {
            			// one ESC followed by something else
            			// ignore ESC
            			key = String.valueOf((char)ch);
            		}
            	} else {
            		key = String.valueOf((char)ch);
            	}
            	if (keymap.containsKey(key)) {
            		System.out.println("key " + key + " = " + keymap.get(key));
            		worker.sendKey(remote, key);
            	} else {
            		System.out.println("key " + key + " ???");
            	}
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				Stty.unsetRow();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

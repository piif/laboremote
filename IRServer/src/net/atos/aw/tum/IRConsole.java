package net.atos.aw.tum;

import java.io.Console;
import java.io.IOException;
import java.util.Iterator;

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
					System.err.println("must be : record [protocolName]");
					continue;
				}
				if (words.length == 2) {
					String protocol = words[1];
					System.out.println(worker.recordKeys(protocol));
				} else {
					System.out.println(worker.recordKey("dummy"));
				}

			} else if (words[0].equals("reload")) {
				parent.load();

			} else if (words[0].equals("interact")) {
				if (words.length != 2) {
					System.err.println("must be : interact remoteName");
					continue;
				}
				String remote = words[1];

				System.out.println("interactive mode, press ESC ESC to quit");

				try {
					Stty.setRow();

                    while (true) {
                    	int key = System.in.read();
                    	if (key == 27) {
                    		int subkey = System.in.read();
                    		if (subkey == 27) {
                    			break;
                    		} else if (subkey == '[') {
                    			// special key -> read third part
                    			key = System.in.read();
                    		} else {
                    			// one ESC followed by something else
                    			// ignore ESC
                    			key = subkey;
                    		}
                    	}
                    	System.out.println("key " + key);
                    	// TODO : monter une liste d'alias dans les protocoles
                    	// + appeler la touche associée
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
}

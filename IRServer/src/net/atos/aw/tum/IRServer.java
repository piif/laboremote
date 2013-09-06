package net.atos.aw.tum;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class IRServer {
	IRWebServer server;
	// @see http://docs.oracle.com/javase/6/docs/jre/api/net/httpserver/spec/index.html?com/sun/net/httpserver/HttpServer.html
	IRConsole console;
	IRWorker worker;

	static boolean fakeSerial = false;
	SerialConnIntf cnx;

	static String configFile = "config.json";
	JSONObject config;
	JSONObject protocols, models, remotes;

	JSONObject loadJson(String name) throws IOException, JSONException {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
//		InputStream input = cl.getResourceAsStream(name);
		URL resURL = cl.getResource(name);
		URLConnection resConn = resURL.openConnection();
		resConn.setUseCaches(false);
		InputStream input = resConn.getInputStream();
		JSONTokener tokener = new JSONTokener(input);
		JSONObject result = new JSONObject(tokener);
		input.close();
		return result;
	}

	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-fake")) {
				fakeSerial = true;
			} else if (args[i].equals("-config")) {
				if (i >  args.length - 2) {
					usage("missing '-config' argument");
					return;
				}
				i++;
				configFile = args[i];
			} else if (args[i].equals("-help")) {
				usage(null);
				return;
			} else {
				usage("unknown option");
				return;
			}
		}
		IRServer instance = new IRServer();
		instance.load();
		instance.start();
	}

	public static void usage(String msg) {
		if (msg != null) {
			System.err.println(msg);
		}
		System.err.println("Usage : IRServer [-fake] [-config configFileName]");
	}

	protected void load() {

//	    List<String> ports = SerialConn.getAvailablePorts();
//	    Iterator<String> iter = ports.iterator();
//	    System.out.println("Ports : ");
//	    while (iter.hasNext()) {
//	    	System.out.println(" - " + iter.next());
//	    }

		// read 4 configuration files
		JSONObject resources;
		try {
			config = loadJson(configFile);
			resources = config.getJSONObject("resources");
		} catch(IOException e) {
			throw new RuntimeException("Can't load global config : " + e);
		} catch(JSONException e) {
			throw new RuntimeException("Error in global config : " + e);
		}

		try {
			protocols = loadJson(resources.getString("protocols"));
			IRProtocol.loadProtocols(protocols);
		} catch(IOException e) {
			throw new RuntimeException("Can't load protocols config : " + e);
		} catch(JSONException e) {
			throw new RuntimeException("Error in protocols config : " + e);
		}
		try {
			models = loadJson(resources.getString("models"));
			IRModel.loadModels(models);
		} catch(IOException e) {
			throw new RuntimeException("Can't load models config : " + e);
		} catch(JSONException e) {
			throw new RuntimeException("Error in models config : " + e);
		} catch(ConfigException e) {
			throw new RuntimeException("Error in models config : " + e);
		}
		try {
			remotes = loadJson(resources.getString("remotes"));
			IRRemote.loadRemotes(remotes);
		} catch(IOException e) {
			throw new RuntimeException("Can't load remotes config : " + e);
		} catch(JSONException e) {
			throw new RuntimeException("Error in remotes config : " + e);
		} catch(ConfigException e) {
			throw new RuntimeException("Error in remotes config : " + e);
		}
	}

	protected void start() {
		if (fakeSerial) {
			cnx = new FakeSerialConn();
		} else {
//			loadNativeLib();
			// connect to serial port
			try {
				JSONObject serialConfig = config.getJSONObject("serial");
				String device = serialConfig.getString("device");
				int baudRate = serialConfig.getInt("baudRate");
				cnx = new SerialConn(device, baudRate);
			} catch(JSONException e) {
				throw new RuntimeException("Error in serial config", e);
			} catch(Exception e) {
				throw new RuntimeException("can't connect to serial", e);
			}
		}

		worker = new IRWorker(cnx);

		console = new IRConsole(this);
		console.start();

		// launch web server
		try {
			JSONObject webConfig = config.getJSONObject("web");
			int port = webConfig.getInt("port");
			server = new IRWebServer(this, port, worker);
		} catch(Exception e) {
			throw new RuntimeException("can't launch web server", e);			
		}
	}

	protected void shutdown() {
		System.out.println("Server shutdown ...");
		server.stop();
		worker.shutdown();
		System.out.println("Server stopped.");
	}

	private static void loadNativeLib() throws RuntimeException {
		String os = System.getProperty("os.name").toLowerCase();
		String arch = System.getProperty("os.arch").toLowerCase();

		String libraryPath = null;
		if(os.indexOf("win") >= 0){
			libraryPath = "rxtxSerial.dll";
		} else {
			if (arch.indexOf("64") >= 0) {
				libraryPath = "librxtxSerial.so";
			} else {
				libraryPath = "librxtxSerial.so";
			}
		}
		InputStream is = null;
		BufferedOutputStream bos = null;
		try {
			File f = null;
			is = net.atos.aw.tum.utils.FileTools.loadResource(libraryPath);
			if(os.indexOf("win") >= 0){
				f = File.createTempFile("rxtxSerial", ".dll");
//			} else {
//				// unix
//				f = File.createTempFile("librxtxSerial", ".so");
			}
			f.deleteOnExit();
			bos = new BufferedOutputStream(new FileOutputStream(f));
			int c;
			while ((c = is.read()) != -1) {
				bos.write(c);
			}
			bos.flush();
			bos.close();
			System.load(f.getAbsolutePath());
		} catch (Exception e) {
			System.out.println(e);
			System.loadLibrary("rxtxSerial");
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
				}
			}
		}
	}

	class MyExecutor extends ScheduledThreadPoolExecutor {
		public MyExecutor(int corePoolSize) {
			super(corePoolSize);
		}
		protected void terminated() {
			cnx.close();
			super.terminated();
		}
	}
}

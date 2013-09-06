package net.atos.aw.tum;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class IRWebServer {
	private IRServer parent;
	private HttpServer server;
	private IRWorker worker;

	class WebHandler implements HttpHandler {
		public void handle(HttpExchange exchange) throws IOException {
//		    java.io.InputStream is = exchange.getRequestBody();
//		    byte[] b = new byte[500];
//		    int l;
//		    do {
//		    	l = is.read(b);
//		    } while(l > 0);

			String path = exchange.getRequestURI().getPath();

			System.out.println("received request " + path);

			if (path.equals("/SHUTDOWN")) {
				sendOk(exchange, "shutdown");

				parent.shutdown();
				return;
			}

			if (path.startsWith("/INTERFACE/")) {
				path = path.substring("/INTERFACE/".length());
				try {
					InputStream is = net.atos.aw.tum.utils.FileTools.loadResource(path);
					String ext = path.substring(path.lastIndexOf('.'));
					String contentType = "text/plain"; // default
					if (ext.equals(".html")) {
						contentType = "text/html";
					} else if (ext.equals(".css")) {
						contentType = "text/css";
					} else if (ext.equals(".js")) {
						contentType = "application/javascript";
					} else if (ext.equals(".json")) {
						contentType = "	application/json";
					} else if (ext.equals(".png")) {
						contentType = "image/png";
					}
					exchange.getResponseHeaders().set("Content-Type", contentType);
					exchange.sendResponseHeaders(200, 0);
					OutputStream responseStream = exchange.getResponseBody();
					byte data[] = new byte[1000];
					int len;
					while((len = is.read(data)) > 0) {
						responseStream.write(data, 0, len);
					}
					exchange.close();
				} catch(java.util.MissingResourceException e) {
					sendError(exchange, "not found");
				}
				return;
			}

			if (path.startsWith("/RECEIVE/")) {
				String key = path.substring("/RECEIVE/".length());
				String result = worker.recordKey(key);
				sendOk(exchange, result);
				return;
			}

			// else, it's a send => split in 2 parts remote + key
			int slash = path.indexOf('/', 1);
			if (slash < 0) {
				sendError(exchange, "bad url");
				return;
			}

			String remote = path.substring(1, slash);
			String key = path.substring(slash + 1);
			if(key.indexOf('/') >= 0) {
				sendError(exchange, "bad url");
				return;
			}

			String result = worker.sendKey(remote, key);
			if(result.equals("OK")) {
				sendOk(exchange, key);
			} else {
				sendError(exchange, result + " for key " + key);
			}
		}

		void sendOk(HttpExchange exchange, String msg) throws IOException {
			String response = "OK for " + msg + ".\r\n";
			int len = response.getBytes().length;

			exchange.sendResponseHeaders(200, len);
			OutputStream responseStream = exchange.getResponseBody();
			responseStream.write(response.getBytes());

			exchange.close();
		}

		void sendError(HttpExchange exchange, String msg) throws IOException {
			String response = "ERROR " + msg + ".\r\n";
			int len = response.getBytes().length;

			exchange.sendResponseHeaders(500, len);
			OutputStream responseStream = exchange.getResponseBody();
			responseStream.write(response.getBytes());

			exchange.close();
		}
	}

	public IRWebServer(IRServer parent, int port, IRWorker worker) throws IOException {
		this.parent = parent;
		this.worker = worker;
		InetSocketAddress addr = new InetSocketAddress(port);
		server = HttpServer.create(addr, 5);

		WebHandler webHandler = new WebHandler();
		server.createContext("/", webHandler);

		server.start();
	}

	protected void stop() {
		server.stop(2);
	}
}

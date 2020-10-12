package urlShortener;

import cache.Cache;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

public class URLShortener {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("USAGE: java URLShortener PORT");
			System.exit(1);
		}

		Properties prop = new Properties();
		prop.load(new FileInputStream("config.properties"));

		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("Port must be an integer");
			System.exit(1);
		}

		String dbProxyHost = prop.getProperty("proxy.db.host");
		int dbProxyHostPort = Integer.parseInt(prop.getProperty("proxy.db.port"));

		Cache cache = new Cache();

		try {
			ServerSocket serverConnect = new ServerSocket(port);
			System.out.println("Server started.\nListening for connections on port : " + port + " ...\n");

			// we listen until user halts server execution
			while (true) {
				new URLShortenerThread(serverConnect.accept(), dbProxyHost, dbProxyHostPort, cache).start();
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}
}


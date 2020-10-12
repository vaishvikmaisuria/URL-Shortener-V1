package proxy.url;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ProxyThread extends Thread {
    private Socket client;
    private ProxyHostHandler hostHandler;

    public ProxyThread(Socket socket, ProxyHostHandler hostHandler) {
        this.client = socket;
        this.hostHandler = hostHandler;
    }

    public void run() {
        Socket server = null;
        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];

        try {
            // Get client streams.
            final InputStream streamFromClient = client.getInputStream();
            final OutputStream streamToClient = client.getOutputStream();

            String originalHost = this.hostHandler.getHost();
            int currentHostPort = this.hostHandler.getPort();
            this.hostHandler.incrementHost();

            String currentHost = originalHost;

            do {
                System.out.println("Using " + currentHost + ":" + currentHostPort);
                try {
                    server = new Socket(currentHost, currentHostPort);
                } catch (IOException e) {
                    PrintWriter out = new PrintWriter(streamToClient);
                    out.print("Proxy server cannot connect to " + currentHost + ":"
                            + currentHostPort + ":\n" + e + "\n");
                    out.flush();

                    if (this.hostHandler.peekNextHost() != originalHost) {
                        currentHost = this.hostHandler.getHost();
                        currentHostPort = this.hostHandler.getPort();
                        this.hostHandler.incrementHost();
                    } else {
                        break;
                    }
                }
            } while (server == null);

            // If all the URLShorteners are down, then close connection with client
            if (server == null) {
                client.close();
                return;
            }

            // Get server streams.
            final InputStream streamFromServer = server.getInputStream();
            final OutputStream streamToServer = server.getOutputStream();

            // A thread to read the client's requests and pass them to the server
            Thread t = new Thread() {
                public void run() {
                int bytesRead;
                try {
                    while ((bytesRead = streamFromClient.read(request)) != -1) {
                        streamToServer.write(request, 0, bytesRead);
                        streamToServer.flush();
                    }
                } catch (IOException e) {

                }

                // Close server connection
                try {
                    streamToServer.close();
                } catch (IOException e) {

                }
                }
            };

            t.start();

            // Read the server's response and pass them back to the client
            int bytesRead;
            try {
                while ((bytesRead = streamFromServer.read(reply)) != -1) {
                    streamToClient.write(reply, 0, bytesRead);
                    streamToClient.flush();
                }
            } catch (IOException e) {

            }

            // Close client connection
            streamToClient.close();

        } catch (IOException e) {
            System.err.println(e);
        } finally {
            try {
                if (server != null)
                    server.close();
                if (client != null)
                    client.close();
            } catch (IOException e) {

            }
        }
    }
}
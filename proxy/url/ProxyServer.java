package proxy.url;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static proxy.ProxyConstants.*;

public class ProxyServer {

    public static void main(String[] args) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(CONFIG_FILE));

        int port = 0;
        try {
            port = Integer.parseInt(prop.getProperty("proxy.url.port"));
        } catch (Exception e) {
            System.err.println("Port must be an integer");
            System.exit(1);
        }

        ProxyHostHandler hostHandler = new ProxyHostHandler();

        Timer t = new Timer();
        // Check if hosts/ports were added or removed in config file every UPDATE_CHECK milliseconds
        t.scheduleAtFixedRate(
            new TimerTask() {
                @Override
                public void run() {
                    try {
                        prop.load(new FileInputStream(CONFIG_FILE));
                    } catch (IOException e) {
                        System.err.println("Could not load config file");
                    }

                    String newHostsAndPorts = prop.getProperty("url.hostsAndPorts");
                    ArrayList<String> hosts = new ArrayList<>();
                    ArrayList<Integer> hostPorts = new ArrayList<>();

                    // Loop through URLShortener host:ports
                    for (String hostAndPort : newHostsAndPorts.split(",")) {
                        Pattern hostPattern = Pattern.compile("^(\\S+):(\\d+)$");
                        Matcher hostMatcher = hostPattern.matcher(hostAndPort);

                        if(hostMatcher.matches()) {
                            String host = hostMatcher.group(1);
                            int hostPort = 0;

                            try {
                                hostPort = Integer.parseInt(hostMatcher.group(2));
                            } catch (Exception e) {
                                System.err.println(hostAndPort + " is not valid. URLShortener port must be an integer");
                            }

                            hosts.add(host);
                            hostPorts.add(hostPort);
                        } else {
                            System.err.println(hostAndPort + " is not valid. It has to follow the form HOST:PORT");
                        }
                    }

                    // None of the hosts had the correct pattern
                    if (hosts.size() == 0) {
                        System.err.println("None of the URLShortener args had the correct format of HOST:PORT");
                        System.exit(1);
                    }

                    hostHandler.setHostsAndPorts(hosts, hostPorts);
                }
            }, 0, UPDATE_CHECK);

        try {
            System.out.println("Starting proxy server on port " + port);
            ServerSocket proxySocket = new ServerSocket(port);

            while (true) {
                new ProxyThread(proxySocket.accept(), hostHandler).start();
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}

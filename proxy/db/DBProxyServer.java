package proxy.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static proxy.ProxyConstants.*;

public class DBProxyServer {
    public static void main(String[] args) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(CONFIG_FILE));

        int port = 0;
        try {
            port = Integer.parseInt(prop.getProperty("proxy.db.port"));
        } catch (Exception e) {
            System.err.println("Port must be an integer");
            System.exit(1);
        }

        DBHostHandler hostHandler = new DBHostHandler();
        DBRepartitionHandler repartitionHandler = new DBRepartitionHandler();

        Timer t = new Timer();
        // Check if hosts/ports were added or removed in config file every UPDATE_CHECK milliseconds
        t.scheduleAtFixedRate(
            new TimerTask() {
                @Override
                public void run() {
                    // If we're currently repartitioning, do not re-update the hosts/backup hosts
                    if (repartitionHandler.getRepartitionFlag()) {
                        return;
                    }

                    try {
                        prop.load(new FileInputStream(CONFIG_FILE));
                    } catch (IOException e) {
                        System.err.println("Could not load config file");
                    }

                    String newHostsAndPorts = prop.getProperty("db.hostsAndPorts");
                    String[] hostsAndPortsLst = newHostsAndPorts.split(",");

                    String newBackupHostsAndPorts = prop.getProperty("db.backupHostsAndPorts");
                    String[] backupHostsAndPortsLst = newBackupHostsAndPorts.split(",");

                    if (hostsAndPortsLst.length != backupHostsAndPortsLst.length) {
                        System.err.println("Need to have the same number of DB servers and backup DB servers");
                        System.exit(1);
                    }

                    ArrayList<String> hosts = new ArrayList<>();
                    ArrayList<Integer> hostPorts = new ArrayList<>();

                    ArrayList<String> backupHosts = new ArrayList<>();
                    ArrayList<Integer> backupHostPorts = new ArrayList<>();

                    // Loop through DB host:ports
                    for (int i = 0; i < hostsAndPortsLst.length; i++) {
                        String hostAndPort = hostsAndPortsLst[i];
                        String backupHostAndPort = backupHostsAndPortsLst[i];

                        Pattern hostPattern = Pattern.compile("^(\\S+):(\\d+)$");
                        Matcher hostMatcher = hostPattern.matcher(hostAndPort);
                        Matcher backupHostMatcher = hostPattern.matcher(backupHostAndPort);

                        if(hostMatcher.matches() && backupHostMatcher.matches()) {
                            String host = hostMatcher.group(1);
                            String backupHost = backupHostMatcher.group(1);
                            int hostPort = 0;
                            int backupHostPort = 0;

                            try {
                                hostPort = Integer.parseInt(hostMatcher.group(2));
                                backupHostPort = Integer.parseInt(backupHostMatcher.group(2));

                            } catch (Exception e) {
                                System.err.println("DB and backup DBs ports must be an integer");
                                System.exit(1);
                            }

                            hosts.add(host);
                            hostPorts.add(hostPort);

                            backupHosts.add(backupHost);
                            backupHostPorts.add(backupHostPort);
                        } else {
                            System.err.println(hostAndPort + " is not valid. It has to follow the form HOST:PORT");
                        }
                    }

                    // None of the hosts had the correct pattern
                    if (hosts.size() == 0) {
                        System.err.println("None of the DB args had the correct format of HOST:PORT");
                        System.exit(1);
                    }

                    hostHandler.setHostsAndPorts(hosts, hostPorts, backupHosts, backupHostPorts);
                    repartitionHandler.setHostsAndPorts(hosts, hostPorts, backupHosts, backupHostPorts);
                }
            }, 0, UPDATE_CHECK);

        try {
            System.out.println("Starting DB proxy server on port " + port);
            ServerSocket proxySocket = new ServerSocket(port);

            while (true) {
                new DBThread(proxySocket.accept(), hostHandler, repartitionHandler).start();
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}






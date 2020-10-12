package proxy.db;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class DBRepartitionHandler {
    private ArrayList<String> hosts;
    private ArrayList<Integer> hostPorts;
    private ArrayList<String> backupHosts;
    private ArrayList<Integer> backupHostPorts;

    private boolean hostsChanged;
    private boolean initialCall;
    private int repartitionsDone;

    public DBRepartitionHandler() {
        this(new ArrayList<String>(), new ArrayList<Integer>(), new ArrayList<String>(), new ArrayList<Integer>());
        this.hostsChanged = false;
        this.initialCall = true;
    }

    public DBRepartitionHandler(ArrayList<String> hosts, ArrayList<Integer> hostPorts, ArrayList<String> backupHosts, ArrayList<Integer> backupHostPorts) {
        this.hosts = hosts;
        this.hostPorts = hostPorts;
        this.backupHosts = backupHosts;
        this.backupHostPorts = backupHostPorts;
    }

    public synchronized boolean getRepartitionFlag() {
        return this.hostsChanged;
    }

    public synchronized void setHostsAndPorts(ArrayList<String> newHosts, ArrayList<Integer>  newHostPorts,
                                              ArrayList<String> newBackupHosts, ArrayList<Integer>  newBackupHostPorts) {
        // If we're currently repartitioning, do not re-update the hosts/backup hosts
        if (this.hostsChanged) {
            return;
        }

        // If there were changes detected, set repartition flag to true
        if (!this.initialCall && (!this.hosts.equals(newHosts) || !this.hostPorts.equals(newHostPorts) ||
                !this.backupHosts.equals(newBackupHosts) || !this.backupHostPorts.equals(newBackupHostPorts))) {
            this.hostsChanged = true;
        }

        this.hosts = newHosts;
        this.hostPorts = newHostPorts;
        this.backupHosts = newBackupHosts;
        this.backupHostPorts = newBackupHostPorts;

        // If we just set the flag to true, send out repartition calls to the DBs
        if (this.hostsChanged && !this.initialCall) {
            this.repartitionsDone = 0;
            this.sendRepartitionRequests();
        }

        if (this.initialCall) {
            this.initialCall = false;
        }
    }

    public synchronized boolean isRepartitionsDone() {
        boolean isDone = this.repartitionsDone >= this.hosts.size();
        if (isDone) {
            this.repartitionsDone = 0;
            this.hostsChanged = false;
        }
        return isDone;
    }

    public synchronized void incrementRepartitionsDone() {
        this.repartitionsDone += 1;
        System.out.println("Repartitions done: " + this.repartitionsDone);
    }

    private synchronized void sendRepartitionRequests() {
        for (int i = 0; i < this.hosts.size(); i++) {
            String host = this.hosts.get(i);
            int port = this.hostPorts.get(i);
            String backupHost = this.backupHosts.get(i);
            int backupPort = this.backupHostPorts.get(i);

            this.sendRepartitionRequest(host, port, backupHost, backupPort);
        }
    }

    private void sendRepartitionRequest(String host, int port, String backupHost, int backupPort) {
        Socket dbServer = null;
        boolean usingBackup = false;

        try {
            // Send request to the DB server
            dbServer = new Socket(host, port);
        } catch (Exception e) {
            // Try connecting to the backup DB
            try {
                System.out.println("Could not send repartition request to " + host + ":" + port);
                System.out.println("Sending repartition request to backup " + backupHost + ":" + backupPort);
                dbServer = new Socket(backupHost, backupPort);
                usingBackup = true;
            } catch (IOException err) {
                System.out.println("DB proxy server cannot connect to backup " + backupPort + ":" + backupPort
                        + ". Giving up on this data.");
                return;
            }
        }

        try {
            DataOutputStream streamToServer = new DataOutputStream(dbServer.getOutputStream());
            streamToServer.writeUTF("POST /repartition");
            streamToServer.close();
            dbServer.close();
        } catch (IOException e) {
            System.out.println("Error sending repartition request. Giving up on this data." + ":\n" + e + "\n");
        }

        // If we're retrieving values from the backup, do not send a DELETE request to it
        if (usingBackup) {
            return;
        }

        // Send DELETE request to backup
        try {
            dbServer = new Socket(backupHost, backupPort);
        } catch (Exception e) {
            System.out.println("Could not send delete request to " + host + ":" + port);
            return;
        }

        try {
            DataOutputStream streamToServer = new DataOutputStream(dbServer.getOutputStream());
            streamToServer.writeUTF("DELETE /repartition");
            streamToServer.close();
            dbServer.close();
        } catch (IOException e) {
            System.out.println("Error sending delete request." + ":\n" + e + "\n");
        }
    }
}

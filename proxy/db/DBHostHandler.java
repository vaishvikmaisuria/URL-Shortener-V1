package proxy.db;

import java.util.ArrayList;

public class DBHostHandler {
    private ArrayList<String> hosts;
    private ArrayList<Integer> hostPorts;
    private ArrayList<String> backupHosts;
    private ArrayList<Integer> backupHostPorts;

    public DBHostHandler() {
        this(new ArrayList<String>(), new ArrayList<Integer>(), new ArrayList<String>(), new ArrayList<Integer>());
    }

    public DBHostHandler(ArrayList<String> hosts, ArrayList<Integer> hostPorts, ArrayList<String> backupHosts, ArrayList<Integer> backupHostPorts) {
        this.hosts = hosts;
        this.hostPorts = hostPorts;
        this.backupHosts = backupHosts;
        this.backupHostPorts = backupHostPorts;
    }

    public synchronized String getHost(String word) {
        return this.hosts.get(getIndex(word));
    }

    public synchronized Integer getPort(String word) {
        return this.hostPorts.get(getIndex(word));
    }

    public synchronized String getBackupHost(String word) {
        return this.backupHosts.get(getIndex(word));
    }

    public synchronized Integer getBackupPort(String word) {
        return this.backupHostPorts.get(getIndex(word));
    }

    private int getIndex(String word) {
        char[] charArray = word.toCharArray();
        int sum = 0;

        for (char ch : charArray) {
            sum += (int)ch;
        }

        return sum % this.hosts.size();
    }

    public synchronized void setHostsAndPorts(ArrayList<String> newHosts, ArrayList<Integer>  newHostPorts,
                                              ArrayList<String> newBackupHosts, ArrayList<Integer>  newBackupHostPorts) {
        this.hosts = newHosts;
        this.hostPorts = newHostPorts;
        this.backupHosts = newBackupHosts;
        this.backupHostPorts = newBackupHostPorts;
    }
}
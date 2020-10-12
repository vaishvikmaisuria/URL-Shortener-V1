package proxy.url;

import java.util.ArrayList;

public class ProxyHostHandler {
    private ArrayList<String> hosts;
    private ArrayList<Integer> hostPorts;
    private int i = 0;

    public ProxyHostHandler() {
        this(new ArrayList<String>(), new ArrayList<Integer>());
    }

    public ProxyHostHandler(ArrayList<String> hosts, ArrayList<Integer> hostPorts) {
        this.hosts = hosts;
        this.hostPorts = hostPorts;
    }

    public synchronized String getHost() {
        return this.hosts.get(i);
    }

    public synchronized Integer getPort() {
        return this.hostPorts.get(i);
    }

    public synchronized void incrementHost() {
        i = (i + 1) % this.hosts.size();
    }

    public synchronized String peekNextHost() {
        return this.hosts.get(i);
    }

    public synchronized void setHostsAndPorts(ArrayList<String> newHosts, ArrayList<Integer>  newHostPorts) {
        this.hosts = newHosts;
        this.hostPorts = newHostPorts;
        i = 0;
    }
}

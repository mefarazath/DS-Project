package org.uom.cse;

public class RoutingTableEntry {

    private String userName;
    private String ipAddress;
    private int port;

    public RoutingTableEntry(String userName, String ipAddress, int port) {
        this.userName = userName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        RoutingTableEntry otherEntry = (RoutingTableEntry)obj;
        // check the equality of all three attributes

        return true;
    }
}

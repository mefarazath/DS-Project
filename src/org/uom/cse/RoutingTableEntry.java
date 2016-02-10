package org.uom.cse;

public class RoutingTableEntry {

    private String userName;
    private String ipAddress;
    private String port;
    private boolean active;

    public RoutingTableEntry(String userName, String ipAddress, String port) {
        this.userName = userName;
        this.ipAddress = ipAddress;
        this.port = port;
        this.active = false; // gets active only when the JOINOK is received
    }

    public String getUserName() {
        return userName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPort() {
        return port;
    }

    public boolean isActive(){ return active; }

    public void setActive(boolean active){
        this.active = active;
    }

    @Override
    public boolean equals(Object obj) {
        RoutingTableEntry otherEntry = (RoutingTableEntry)obj;
        // check the equality of all three attributes

        return true;
    }
}

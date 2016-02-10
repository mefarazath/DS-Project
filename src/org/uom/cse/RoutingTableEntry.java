package org.uom.cse;

public class RoutingTableEntry {

    private String userName;
    private String ipAddress;
    private String port;
    private boolean isActive;

    public RoutingTableEntry(String userName, String ipAddress, String port) {
        this.userName = userName;
        this.ipAddress = ipAddress;
        this.port = port;
        this.isActive = true; // gets Active only when the JOINOK is received
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean equals(String ipAddress, String port) {
        if (ipAddress.equals(this.ipAddress) && port.equals(this.port)) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return ipAddress + ":" + port + "\t" + userName + "\t" + isActive;
    }
}

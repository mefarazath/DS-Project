package org.uom.cse.communication.client;

public interface CommunicationClient {

    public static final String ERROR = "ERROR";

    public String receive();

    public void send(String message);
}

package org.uom.cse.communication.client;


import java.net.*;

public class UDPClient {

    // own ip-address and port
    private InetAddress ipAddress;
    private int port = 0;

    // Constructor to use when using as server
    public UDPClient(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Method to send a message to another node via UDP
     *
     * @param message   String : message to be sent
     * @param ipAddress
     * @param port
     */
    private void sendMessage(InetAddress ipAddress, int port, String message) {

        DatagramSocket clientSocket = null;
        try {
            clientSocket = new DatagramSocket(null);
            clientSocket.setReuseAddress(true);
            clientSocket.bind(new InetSocketAddress(this.ipAddress, this.port));

            System.out.println(clientSocket.getLocalAddress().getHostAddress()+":"+clientSocket.getLocalPort());

            byte[] messageToSend = message.getBytes();
            int messageLength = messageToSend.length;

            // send data
            DatagramPacket sendPacket = new DatagramPacket(messageToSend, messageLength, ipAddress, port);
            clientSocket.send(sendPacket);
            System.out.println("\n" + message + "\t --> " + ipAddress + ":" + port);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
    }


    public void send(String ipAddress, int port, String message) throws UnknownHostException {
        // send the message here

        sendMessage(InetAddress.getByName(ipAddress), port, message);
    }
}

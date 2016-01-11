package org.uom.cse.udp;

import java.net.*;

public class UDPClient {

    private static final int size = 65553;
    private static final String ERROR = "ERROR";


    private InetAddress ipAddress;
    private int port = 0;

    public UDPClient() {
    }

    // Constructor to use when using as server
    public UDPClient(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Method to send a message to another node via UDP
     * @param message   String : message to be sent
     * @param ipAddress
     * @param port
     */
    public static void sendMessage(String message, InetAddress ipAddress, int port){

        DatagramSocket clientSocket = null;
        try {
            clientSocket = new DatagramSocket();
            byte[] messageToSend = message.getBytes();
            int messageLength = messageToSend.length;

            // send data
            DatagramPacket sendPacket = new DatagramPacket(messageToSend, messageLength, ipAddress, port);
            clientSocket.send(sendPacket);
            System.out.println(message+"\t --> "+ipAddress+":"+port);

        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if ( clientSocket != null) {
                clientSocket.close();
            }
        }
    }


    /**
     * Method to receive messages via UDP from another node or client
     */
    public String recieveMessage(){

        DatagramSocket clientSocket = null;
        String outputMessage = ERROR;
        try {
            if (this.port == 0){
                clientSocket = new DatagramSocket();
            } else {
                clientSocket = new DatagramSocket(this.port, ipAddress);
            }

            byte[] receiveData = new byte[size];

            // receive data
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            // output the received bytes as a string
            outputMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
       //     System.out.println(outputMessage+"\t<-- "+receivePacket.getAddress().getHostAddress()+":"+receivePacket.getPort());

        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }

            return outputMessage;
        }
    }


    public static void main(String[] args) throws UnknownHostException {

    }


}

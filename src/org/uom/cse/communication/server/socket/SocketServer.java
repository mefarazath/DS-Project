package org.uom.cse.communication.server.socket;

import org.uom.cse.Commands;
import org.uom.cse.Node;
import org.uom.cse.communication.client.UDPClient;
import org.uom.cse.message.MessageBuilder;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SocketServer extends Thread {
    private static final int size = 65553;
   // private static final String ERROR = "ERROR";

    private UDPClient udpClient;
    private InetAddress ipAddress;
    private int port;
    private Node node;

    public SocketServer(Node node, InetAddress ipAddress, int port) {
        this.node = node;
        this.ipAddress = ipAddress;
        this.port = port;
        this.udpClient = new UDPClient(ipAddress, port);
    }

    @Override
    public void run() {

        System.out.println("SERVER listening at " + ipAddress.getHostAddress() + ":" + port);
        // receive data

        while (true) {

            String message = receive();
            System.out.println("Received : " + message);

            handleMessage(message);

        }

        // call the handleMessage() from here
        // handle according to the method --> ie. send appropriate responses.

    }

    public String receive() {
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
            System.out.println(receivePacket.getAddress()+" "+receivePacket.getPort());

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

    public void handleMessage(String message) {

        String[] msgParts = message.split(" ");

        String messageCode = msgParts[1];

        switch (messageCode){
            case Commands.JOIN:
                //length JOIN IP_address port_no
                if (!this.handleJOIN(msgParts)) {
                    System.err.println("Message error : " + message);
                }
                break;

            case Commands.JOINOK:
                //length JOINOK value
                if (!this.handleJOINOK(msgParts)) {
                    System.err.println("Message error : " + message);
                }
                break;

            case Commands.SER:

                break;

            case Commands.SEROK:
                break;

            case Commands.LEAVE:
                //length LEAVE IP_address port_no
                if(!this.handleLEAVE(msgParts)){
                    System.err.println("Message error : " + message);
                }
                break;

            case Commands.LEAVEOK:
                //length LEAVEOK value
                if(!this.handleLEAVEOK(msgParts)){
                    System.err.println("Message error : " + message);
                }
                break;

            case Commands.ERROR:
                //length ERROR
                if(!this.handleERROR(msgParts)){
                    System.err.println("Message error : " + message);
                }
                break;

            default:
                System.err.println("Message error : invalid command");
        }

        // select which handle method to call here.
        // TODO based on the message write seperate handle message
        // eg : for a JOIN message send a JOINOK etc.


    }

    protected String updateRoutingTable(String ipAddress, String port, boolean active) {
        List<RoutingTableEntry> routingTableEntryList = this.node.getRoutingTable();

        for(int i=0; i<routingTableEntryList.size;i++){
            
        }
        //add to the routing table if it is not already in the table
        //return SUCCESS_CODE or ERROR_CODE

        return Commands.SUCCESS_CODE;
    }

    protected boolean handleERROR(String[] msgPatrs){
        //length ERROR
        System.out.println("Error message.");

        if(msgPatrs.length != 2){
            return false;
        }

        return true;
    }

    protected boolean handleJOIN(String[] msgParts) {
        //length JOIN IP_address port_no
        System.out.println("Joining :" + msgParts[2] + " " + msgParts[3]);

        if (msgParts.length != 4) {
            return false;
        }

        String successCode = updateRoutingTable(msgParts[2], msgParts[3], true);

        if(successCode.equals(Commands.ERROR_CODE)){
            //inactivate the node that returns errors in updating the routing table
            this.updateRoutingTable(msgParts[2], msgParts[3], false);
        }

        try {

            //create JOINOK message
            String joinOkMessage = new MessageBuilder().append(Commands.JOINOK)
                    .append(successCode)
                    .buildMessage();

            InetAddress ipAddress = InetAddress.getByName(msgParts[2]);
            int port = Integer.parseInt(msgParts[3]);

            udpClient.send(ipAddress, port, joinOkMessage);

        } catch (UnknownHostException e) {
            return false;
        }

        return true;
    }

    protected boolean handleJOINOK(String[] msgParts) {
        //length JOINOK value
        if (msgParts.length != 3) {
            return false;
        }

        if (msgParts[2].equals(Commands.SUCCESS_CODE)) {
            System.out.println("Joined Successfully");
        } else if (msgParts[2].equals(Commands.ERROR_CODE)) {
            System.out.println("Join Failed");
        } else {
            return false;
        }

        return true;
    }

    protected boolean handleLEAVE(String[] msgParts){
        //length LEAVE IP_address port_no
        if(msgParts.length != 4){
            return false;
        }

        String successCode = this.updateRoutingTable(msgParts[2], msgParts[3], false);

        if (successCode.equals(Commands.SUCCESS_CODE)){
            System.out.println("Updated Successfully : " + Commands.LEAVE + " " + msgParts[2] + " " + msgParts[3]);
        }else if(successCode.equals(Commands.ERROR_CODE)){
            System.out.println("Update Failed : " + Commands.LEAVE + " " + msgParts[2] + " " + msgParts[3]);
        }else{
            return false;
        }

        try {
            InetAddress ipAddress = InetAddress.getByName(msgParts[2]);
            int port = Integer.parseInt(msgParts[3]);

            //create LEAVEOK message
            String leaveOkMessage = new MessageBuilder().append(Commands.LEAVEOK)
                    .append(successCode)
                    .buildMessage();

            udpClient.send(ipAddress, port, leaveOkMessage);

        } catch (UnknownHostException e) {
            return false;
        }

        return true;
    }

    protected boolean handleLEAVEOK(String[] msgParts){
        //length LEAVEOK value
        if(msgParts.length != 3){
            return false;
        }

        if (msgParts[2].equals(Commands.SUCCESS_CODE)) {
            System.out.println("Left Successfully");
        } else if (msgParts[2].equals(Commands.ERROR_CODE)) {
            System.out.println("Leaving Failed");
        } else {
            return false;
        }

        return true;
    }

    private boolean handleSER(String message){

        if(message.split(" ").length < 6){
            return false;
        }

        String searchOkMessage = this.node.search(message);

        if(searchOkMessage != null){

            try {
                InetAddress ipAddressSM = InetAddress.getByName(message.split(" ")[2]);
                int portSM = Integer.parseInt(message.split(" ")[3]);


                udpClient.send(ipAddressSM, portSM, searchOkMessage);

            } catch (UnknownHostException e) {
                return false;
            }
        }

        return true;
    }

}
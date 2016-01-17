package org.uom.cse;

import org.uom.cse.communication.UDPClient;
import org.uom.cse.message.MessageBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread {

    private List<RoutingTableEntry> routingTable;
    private UDPClient udpClient;
    private InetAddress ipAddress;
    private int port;

    public Server(InetAddress ipAddress, int port, List<RoutingTableEntry> routingTable) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.routingTable = routingTable;
        this.udpClient = new UDPClient(ipAddress, port);
    }

    @Override
    public void run() {

        System.out.println("SERVER listening at " + ipAddress.getHostAddress() + ":" + port);
        // receive data

        while (true) {

            String message = udpClient.receive();
            System.out.println("Received : " + message);

            handleMessage(message);

        }

        // call the handleMessage() from here
        // handle according to the method --> ie. send appropriate responses.

    }

    public void handleMessage(String message) {

        String[] msgParts = message.split(" ");

        if (Commands.JOIN.equals(msgParts[1])) {
            //length JOIN IP_address port_no
            if(!this.handleJOIN(msgParts)){
                System.err.println("Message error : " + message);
            }

        } else if (Commands.JOINOK.equals(msgParts[1])) {
            //length JOINOK value
            if (!this.handleJOINOK(msgParts)) {
                System.err.println("Message error : " + message);
            }
        }



        // select which handle method to call here.
        // TODO based on the message write seperate handle message
        // eg : for a JOIN message send a JOINOK etc.


    }

    private String updateRoutingTable(String ipAddress, String port){
        //add to the routing table if it is not already in the table
        //return SUCCESS_CODE or ERROR_CODE

        return Commands.SUCCESS_CODE;
    }

    private boolean handleJOIN(String[] msgParts){
        //length JOIN IP_address port_no
        System.out.println("Joining :" + msgParts[2] + " " + msgParts[3]);

        if(msgParts.length != 4)
            return false;

        String successCode = updateRoutingTable(msgParts[2], msgParts[3]);

        try {
            udpClient.setDestinationAddress(InetAddress.getByName(msgParts[2]));
            udpClient.setDestinationPort(Integer.parseInt(msgParts[3]));

            //create JOINOK message
            String joinOkMessage = new MessageBuilder().append(Commands.JOINOK)
                    .append(successCode)
                    .buildMessage();

            udpClient.send(joinOkMessage);

        } catch (UnknownHostException e) {
            return false;
        }

        return true;
    }

    private boolean handleJOINOK(String[] msgParts){
        //length JOINOK value
        if (msgParts.length != 3)
            return false;

        if(Commands.SUCCESS_CODE.equals(msgParts[2])){
            System.out.println("Joined Successfully");
        }else if(Commands.ERROR_CODE.equals(msgParts[2])){
            System.out.println("Join Failed");
        }else {
            return false;
        }

        return true;
    }
    // JOIN FUNCTIONALITY
    // { add to neighbour table}
    // reply
}


package org.uom.cse;

import org.uom.cse.Commands;
import org.uom.cse.communication.client.UDPClient;
import org.uom.cse.message.MessageBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Server extends Thread {

    private UDPClient udpClient;
    private InetAddress ipAddress;
    private int port;

    public Server(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
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
                //length LEAVE IP_address port_no
                if(!this.handleLEAVEOK(msgParts)){
                    System.err.println("Message error : " + message);
                }
                break;

            case Commands.ERROR:
                //length LEAVE IP_address port_no
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
        //add to the routing table if it is not already in the table
        //return SUCCESS_CODE or ERROR_CODE

        return Commands.SUCCESS_CODE;
    }

    protected boolean handleERROR(String[] msgPatrs){
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
            udpClient.setDestinationAddress(InetAddress.getByName(msgParts[2]));
            udpClient.setDestinationPort(Integer.parseInt(msgParts[3]));

            //create LEAVEOK message
            String leaveOkMessage = new MessageBuilder().append(Commands.LEAVEOK)
                    .append(successCode)
                    .buildMessage();

            udpClient.send(leaveOkMessage);

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

}

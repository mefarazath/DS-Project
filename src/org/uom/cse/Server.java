package org.uom.cse;

import org.uom.cse.communication.UDPClient;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread{

    private List<RoutingTableEntry> routingTable;
    private UDPClient udpClient;
    private InetAddress ipAddress;
    private int port;

    public Server(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.routingTable = new ArrayList<>();
        this.udpClient = new UDPClient(ipAddress,port);
    }

    @Override
    public void run() {

        System.out.println("SERVER listening at "+ipAddress.getHostAddress()+":"+port);
        // receive data

        while (true) {

            String message = udpClient.receive();
            System.out.println("Recieved : "+message);

        }

        // call the handleMessage() from here
        // handle according to the method --> ie. send appropriate responses.

    }

    public  void handleMessage(String message){

        // select which handle method to call here.


    }
    // JOIN FUNCTIONALITY
    // { add to neighbour table}
    // reply
}

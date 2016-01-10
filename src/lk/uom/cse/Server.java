package lk.uom.cse;

import java.util.ArrayList;
import java.util.List;

public class Server extends Thread{

    private List<RoutingTableEntry> routingTable;
    private int port;

    public Server(int port) {
        this.port = port;
        this.routingTable = new ArrayList<>();
    }

    @Override
    public void run() {

        //  recieve data

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

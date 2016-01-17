package org.uom.cse;

import org.uom.cse.communication.CommunicationClient;
import org.uom.cse.message.MessageBuilder;
import org.uom.cse.communication.UDPClient;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Node {

    private static String bootstrapServerIp = "127.0.0.1";
    private static int bootStrapServerPort = 9999;

    private static final String LOCALHOST = "localhost";

    List<RoutingTableEntry> routingTable;
    List<String> files;

    private Server server;
    private CommunicationClient udpClient;

    // IP address and port of the node
    private InetAddress ipAddress;
    private int port;

    private Node() {
        routingTable = new ArrayList<RoutingTableEntry>();
        files = new ArrayList<String>();
        udpClient = new UDPClient();
    }

    public Node(InetAddress ipAddress, int port) {
        this();
        this.ipAddress = ipAddress;
        this.port = port;
        this.server = new Server(ipAddress, port, routingTable);
    }


    public boolean registerToBootstrapServer(String ipAddress, int port) throws IOException {
        // send REG message to the bootstrap server
        System.out.print("Enter username : ");
        String userName = new Scanner(System.in).nextLine();

        // build REG message
        String msg = createRegMessage(ipAddress, port, userName);
        System.out.println(msg);

        // TCP send and receive
        Socket clientSocket = new Socket(bootstrapServerIp, bootStrapServerPort);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        writer.write(msg);
        writer.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String serverResponse = reader.readLine();
        System.out.println(serverResponse);

        reader.close();
        writer.close();
        clientSocket.close();

        // handle the Registration response
        boolean success = handleRegistrationMessage(serverResponse);
        return success;
    }

    private String createRegMessage(String ipAddress, int port, String userName) {

        String regMessage = new MessageBuilder().append(Commands.REG)
                .append(ipAddress)
                .append(port + "")
                .append(userName)
                .buildMessage();

        return regMessage;
    }

    private boolean handleRegistrationMessage(String msg) {
        boolean result = false;
        String msgParts[] = msg.split("\\s+");

        if (msgParts.length == 2) {
            System.out.println("Error Message:" + msgParts[1]);
        } else {
            if (Commands.REGOK.equals(msgParts[1])) {

                int noOfNodes = Integer.parseInt(msgParts[2]);
                String ip, port, username;

                if (noOfNodes == 9999) {
                    System.out.println("failed, there is some error in the command");
                } else if (noOfNodes == 9998) {
                    System.out.println("failed, you are already registered, unregister first");
                } else if (noOfNodes == 9997) {
                    System.out.println("failed, registered to another user, try a different IP and port");
                } else if (noOfNodes == 9996) {
                    System.out.println("failed, can’t register. BS full");
                } else {
                    RoutingTableEntry entry;
                    for (int i = 1; i <= noOfNodes; i++) {
                        int index = 3 * i;
                        ip = msgParts[index];
                        port = msgParts[++index];
                        username = msgParts[++index];

                        // add neighbours to the routing table
                        entry = new RoutingTableEntry(username, ip, port);
                        routingTable.add(entry);
                    }

                    result = true;
                }
            }
        }

        return result;

    }

    private void joinWithNeighbours() {

        // build the UDP JOIN message
        String joinMessage = new MessageBuilder()
                .append(Commands.JOIN)
                .append(ipAddress.getHostAddress())
                .append(port + "")
                .buildMessage();

        // iterate through the routing list and send UDP JOIN message
        for (RoutingTableEntry entry : routingTable) {
            try {
                InetAddress ipAddress = InetAddress.getByName(entry.getIpAddress());
                int port = Integer.parseInt(entry.getPort());

                // send JOIN via UDP
                ((UDPClient) udpClient).setDestinationAddress(ipAddress);
                ((UDPClient) udpClient).setDestinationPort(port);
                udpClient.send(joinMessage);

            } catch (UnknownHostException e) {
                System.err.println("Error sending JOIN message to " + entry.getIpAddress() + ":" + entry.getPort());
            }

        }

    }

    private static boolean validityOfPortNumber(String input) {
        //check validity of the port number
        int port;
        try {
            port = Integer.parseInt(input);
            return !(port < 0 || port > 65535);
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public static void main(String[] args) throws IOException {

        boolean isInitialized = false;
        Scanner scanner = new Scanner(System.in);

//        System.out.println("Enter bootstrap ipAddress:port  --> ");
//        String bsInput = scanner.nextLine();
//
//        String ip[] = bsInput.split(":");
//        bootstrapServerIp = ip[0].trim();
//        bootStrapServerPort = Integer.parseInt(ip[1]);

        String input;
        Node node = null;
        while (!isInitialized) {
            System.out.print("Enter the port number to start the node on : ");
            input = scanner.nextLine();
                try {
                    InetAddress ipAddress = InetAddress.getLocalHost();
                    // TODO check whether the port number is legal

                    if (!validityOfPortNumber(input))
                        throw new Exception("Illegal port number");

                    int nodePortNumber = Integer.parseInt(input.trim());

                    // create a node
                    node = new Node(ipAddress, nodePortNumber);

                    // register with the bootstrap server
                    isInitialized = node.registerToBootstrapServer(ipAddress.getHostAddress(), nodePortNumber);

                    if (isInitialized) {
                        System.out.println("Node " + ipAddress.getHostAddress() + ":" + nodePortNumber + " registered successfully");
                        // start the internal server of the node to listen to messages
                        node.server.start();
                    }

                } catch (Exception ex) {
                    System.err.println("Unable to initialize the node");
                    if (isInitialized) {
                        // need to deregister from the BS
                    }

                    isInitialized = false;
                }

        }

        node.joinWithNeighbours();

    }
}

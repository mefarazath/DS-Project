package org.uom.cse;

import org.uom.cse.communication.client.UDPClient;
import org.uom.cse.communication.client.WebServiceClient;
import org.uom.cse.communication.server.socket.SocketServer;
import org.uom.cse.communication.server.webservice.SearchServiceImpl;
import org.uom.cse.communication.server.webservice.SearchServicePublisher;
import org.uom.cse.message.MessageBuilder;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;


public class Node {

    private static final String LOCALHOST = "localhost";
    private static final String QUIT = "q";
    private static final int JOINING_NODES_COUNT = 2;
    // commands
    private static final String REGOK = "REGOK";
    private static final String REG = "REG";
    private static final String SERVER_IP = "bootstrapServerIp";
    private static final String SERVER_PORT = "bootstrapServerPort";
    private static final String PROPERTIES_FILE = "config.properties";
    private static String bootstrapServerIp;
    private static int bootstrapServerPort;
    List<RoutingTableEntry> routingTable;
    List<String> files;

    private SocketServer server;
    private UDPClient udpClient;
    private WebServiceClient webServiceClient;

    // IP address and port of the node
    private InetAddress ipAddress;
    private int port;
    private Properties properties;

    private Node() {
        routingTable = new ArrayList<>();
        files = new ArrayList<>();
        properties = loadProperties();
    }

    public Node(InetAddress ipAddress, int port) {
        this();
        this.ipAddress = ipAddress;
        this.port = port;
        properties = loadProperties();
        this.udpClient = new UDPClient(ipAddress,port);
        this.server = new SocketServer(this,ipAddress, port);
    }

    public static void main(String[] args) throws IOException {

        boolean isInitialized = false;
        Scanner scanner = new Scanner(System.in);

        InetAddress ipAddress = null;
        int nodePortNumber = 0;
        String userName = "";

        String input;
        Node node = null;
        while (!isInitialized) {
            System.out.print("Enter the port number to start the node on : ");
            input = scanner.nextLine();

            System.out.print("Enter username : ");
            userName = new Scanner(System.in).nextLine();

            try {
                ipAddress = InetAddress.getLocalHost();
                nodePortNumber = Integer.parseInt(input.trim());

                // TODO check port number

                // create a node
                node = new Node(ipAddress, nodePortNumber);

                // register with the bootstrap server
                isInitialized = node.registerToBootstrapServer(ipAddress.getHostAddress(), nodePortNumber,userName);

                if (isInitialized) {
                    System.out.println("Node " + ipAddress.getHostAddress() + ":" + nodePortNumber + " registered successfully");
                    // start the internal server of the node to listen to messages
                    node.server.start();
                    // start the search Web Service
                    SearchServicePublisher.publish(ipAddress.getHostAddress(), nodePortNumber, new SearchServiceImpl(node));
                }

            } catch (Exception ex) {
                System.err.println("Unable to initialize the node "+ex.getMessage());

                if (isInitialized) {
                    // need to deregister from the BS
                    node.deregisterFromBS(ipAddress,nodePortNumber,userName);
                }

                isInitialized = false;
            }

        }

        node.joinWithNeighbours();


        System.out.println("\n\n");
        String choice = "";

        while (!choice.equalsIgnoreCase(QUIT)){
            System.out.println("1. Print File List");
            System.out.println("2. Print Routing Table");
            System.out.println("3. Unregister Node");
            System.out.println("4. Search");
            System.out.println("Please Enter your choice (Enter q to quit) :");

            choice = scanner.nextLine();

            switch (choice.toLowerCase()){
                case "1" :
                    System.out.println("Printing File List");
                    break;

                case "2" :
                    node.printRoutingTable();
                    break;

                case "3" :
                    node.deregisterFromBS(ipAddress,nodePortNumber,userName);
                    break;

                case "4" :
                    System.out.println("Enter File name to search for : ");
                    String fileName = scanner.nextLine();
                    node.search(fileName);
                    break;

                case QUIT :
                    System.out.println("Deregistering from the BS");
                    node.deregisterFromBS(ipAddress,nodePortNumber,userName);
                    break;

                default:
                    System.out.println("\nEnter valid choice");
            }
        }
        System.exit(0);
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(PROPERTIES_FILE));

            bootstrapServerIp = properties.getProperty(SERVER_IP);
            bootstrapServerPort = Integer.parseInt(properties
                    .getProperty(SERVER_PORT));


        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return properties;
    }


    public String sendMessageToBS(String msg) throws IOException {
        // TCP send and receive
        Socket clientSocket = new Socket(bootstrapServerIp, bootstrapServerPort);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        writer.write(msg);
        writer.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String serverResponse = reader.readLine();
        System.out.println(serverResponse);

        reader.close();
        writer.close();
        clientSocket.close();

        return serverResponse;
    }

    public boolean registerToBootstrapServer(String ipAddress, int port,String userName) throws IOException {
        // send REG message to the bootstrap server


        // build REG message
        String msg = createRegMessage(ipAddress, port, userName);
        System.out.println(msg);

        String serverResponse = sendMessageToBS(msg);
        // handle the Registration response
        boolean success = handleRegistrationMessage(serverResponse);
        return success;
    }

    public void deregisterFromBS(InetAddress bsAddress, int port, String username) throws IOException {
        String deregMessage = new MessageBuilder().append(Commands.UNREG)
                .append(bsAddress.getHostAddress())
                .append(port + "")
                .append(username)
                .buildMessage();

        sendMessageToBS(deregMessage);
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

                    if (noOfNodes < JOINING_NODES_COUNT) {
                        for (int i = 1; i <= noOfNodes; i++) {
                            int index = 3 * i;
                            ip = msgParts[index];
                            port = msgParts[++index];
                            username = msgParts[++index];

                            // add neighbours to the routing table
                            entry = new RoutingTableEntry(username, ip, port);
                            routingTable.add(entry);
                        }
                    } else {

                        int randIndex[] = randomNodeIndices(noOfNodes);

                        for (int i = 0; i < JOINING_NODES_COUNT; i++) {
                            int index = 3 * randIndex[i];
                            ip = msgParts[index];
                            port = msgParts[++index];
                            username = msgParts[++index];

                            // add neighbours to the routing table
                            entry = new RoutingTableEntry(username, ip, port);
                            routingTable.add(entry);
                        }
                    }

                    result = true;
                }
            }
        }

        return result;

    }

    private int[] randomNodeIndices(int noOfNodes) {
        int randIndex[] = new int[JOINING_NODES_COUNT];
        int randNumber;
        HashSet<Integer> numbers = new HashSet<Integer>();

        for (int i = 0; i < JOINING_NODES_COUNT; i++) {
            do {
                randNumber = (int) Math.floor(Math.random() * noOfNodes) + 1;
            } while (numbers.contains(randNumber));
            randIndex[i] = randNumber;
            numbers.add(randNumber);
        }
        return randIndex;
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

                udpClient.send(ipAddress, port, joinMessage);

            } catch (UnknownHostException e) {
                System.err.println("Error sending JOIN message to " + entry.getIpAddress() + ":" + entry.getPort());
            }

        }

    }

    public void printRoutingTable() {
        System.out.println("\nRouting Table");

        if (routingTable.size() == 0) {
            System.out.println("No entries present");
            return;
        }

        int count = 0;
        for (RoutingTableEntry entry : routingTable) {
            System.out.println( (++count) + "\t" +entry);
        }
        System.out.println();
    }

    public String search(String searchMessage) {

        //search in the the own file list
        //search message format - length SER IP port file_name hops
        Set<String> filesFound = new HashSet<>();

        String[] searchMessageComponents = searchMessage.split(" ");

        String ipAddressSM = searchMessageComponents[2];
        String portSM = searchMessageComponents[3];
        String fileNameSM = searchMessageComponents[4];

        if (searchMessageComponents.length > 6) {
            for (int i = 5; i < searchMessageComponents.length - 1; i++) {
                fileNameSM += " " + searchMessageComponents[i];
            }
        }

        int hopsSM = Integer.parseInt(searchMessage.split(" ")[searchMessageComponents.length - 1]);

        String[] tokens = fileNameSM.split(" ");

        String outputMessage = null;


        for (String fileName : files) {
            List<String> fileNameTokens = Arrays.asList(fileName.split(" "));
            for(String token: tokens){
                if(fileNameTokens.contains(token)){
                    filesFound.add(fileName);
                    break;
                }
            }
        }

        if (filesFound.isEmpty()) {
            hopsSM++;

            searchMessage = this.createSearchMessage(hopsSM, fileNameSM, ipAddressSM, portSM);
            // iterate through the routing list and send UDP SER message
            for (RoutingTableEntry entry : routingTable) {
                try {
                    InetAddress ipAddress = InetAddress.getByName(entry.getIpAddress());
                    int port = Integer.parseInt(entry.getPort());

                    udpClient.send(ipAddress, port, searchMessage);

                    System.out.println("Search message sent to " + entry.getIpAddress() + ":" + entry.getPort());

                } catch (UnknownHostException e) {
                    System.err.println("Error sending SER message to " + entry.getIpAddress() + ":" + entry.getPort());
                }
            }

            outputMessage = this.createSearchOkMessage(this.ipAddress.getHostAddress(), Integer.toString(this.port), hopsSM, filesFound);
        } else if (hopsSM != 0) {
            outputMessage = this.createSearchOkMessage(this.ipAddress.getHostAddress(), Integer.toString(this.port), hopsSM, filesFound);
        } else {
            System.out.println("File found");
        }

        return outputMessage;
    }

    private String createSearchMessage(int hops, String fileName, String ipAddress, String port) {

        //SER message format - length SER IP port file_name hops
        String searchMessage = new MessageBuilder().append(Commands.SER)
                .append(ipAddress)
                .append(port)
                .append(fileName)
                .append(hops + "")
                .buildMessage();

        return searchMessage;
    }

    private String createSearchOkMessage(String ipAddress, String port, int hops, Set<String> filesFound) {

        String fileNames = "";
        for (String file : filesFound) {
            fileNames += file + " ";
        }
        //SEROK message format - length SEROK no_files IP port hops filename1 filename2 ... ...
        String searchOkMessage = new MessageBuilder().append(Commands.SEROK)
                .append(filesFound.size() + "")
                .append(ipAddress)
                .append(port)
                .append(hops + "")
                .append(fileNames)
                .buildMessage();

        return searchOkMessage;
    }

    public void initializeSearch(String fileName) {

        String searchMessage = this.createSearchMessage(0, fileName, this.ipAddress.getHostAddress(), Integer.toString(this.port));
        this.search(searchMessage);

    }
}

package org.uom.cse;

import org.uom.cse.communication.client.CommunicationClient;
import org.uom.cse.communication.client.UDPClient;
import org.uom.cse.message.MessageBuilder;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;


public class Node {

    private static final String LOCALHOST = "localhost";
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

    private Server server;
    private CommunicationClient udpClient;

    // IP address and port of the node
    private InetAddress ipAddress;
    private int port;
    private Properties properties;

    private Node() {
        routingTable = new ArrayList<>();
        files = new ArrayList<>();
        udpClient = new UDPClient();
        properties = loadProperties();
    }

    public Node(InetAddress ipAddress, int port) {
        this();
        this.ipAddress = ipAddress;
        this.port = port;
        properties = loadProperties();
        this.server = new Server(this,ipAddress, port);
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

        node.printRoutingTable();

        node.initializeSearch("file");
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


    public boolean registerToBootstrapServer(String ipAddress, int port) throws IOException {
        // send REG message to the bootstrap server
        System.out.print("Enter username : ");
        String userName = new Scanner(System.in).nextLine();

        // build REG message
        String msg = createRegMessage(ipAddress, port, userName);
        System.out.println(msg);

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

                // send JOIN via UDP
                ((UDPClient) udpClient).setDestinationAddress(ipAddress);
                ((UDPClient) udpClient).setDestinationPort(port);
                udpClient.send(joinMessage);

            } catch (UnknownHostException e) {
                System.err.println("Error sending JOIN message to " + entry.getIpAddress() + ":" + entry.getPort());
            }

        }

    }

    public void printRoutingTable() {
        System.out.println("Routing table entries");

        if (routingTable.size() == 0) {
            System.out.println("No entries present");
            return;
        }
        for (RoutingTableEntry entry : routingTable) {
            System.out.println(entry);
        }
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

                    // send SER via UDP
                    ((UDPClient) udpClient).setDestinationAddress(ipAddress);
                    ((UDPClient) udpClient).setDestinationPort(port);
                    udpClient.send(searchMessage);

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

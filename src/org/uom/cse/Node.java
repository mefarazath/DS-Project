package org.uom.cse;

import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.uom.cse.communication.client.UDPClient;
import org.uom.cse.communication.client.WebServiceClient;
import org.uom.cse.communication.server.socket.SocketServer;
import org.uom.cse.communication.server.webservice.SearchServiceImpl;
import org.uom.cse.communication.server.webservice.SearchServicePublisher;
import org.uom.cse.message.MessageBuilder;
import org.uom.cse.message.SearchQuery;


public class Node {

    private static final String LOCALHOST = "localhost";
    private static final String QUIT = "q";
    private static final int JOINING_NODES_COUNT = 2;

    // commands
    private static final String REGOK = "REGOK";
    private static final String REG = "REG";
    private static final String SERVER_IP = "bootstrapServerIp";
    private static final String SERVER_PORT = "bootstrapServerPort";
    private static final String LOCAL_IP = "localIp";
    private static final String FILE_NAMES = "fileNames";
    private static final String QUERY_FILE = "queryFile";
    private static final String UDP = "udp";
    private static final String PROPERTIES_FILE = "config.properties";
    private static String bootstrapServerIp;
    private static int bootstrapServerPort;
    private static boolean udp;
    private static String localIp;
    private static String fileName;
    private static String queryFile;
    private List<RoutingTableEntry> routingTable;
    private List<String> messageIds = new ArrayList<>();
    private Map<String, SearchQuery> sentMessages = new HashMap<>();
    private boolean isActiveNode = false;
    private List<String> fileList;
    private List<String> queryList;
    private SocketServer server;
    private UDPClient udpClient;
    // IP address and port of the node
    private InetAddress ipAddress;
    private int port;
    private Properties properties;

    private int noOfRecievedMsgs = 0;
    private int noOfForwardedMsgs = 0;
    private int noOfAnsweredMsgs = 0;

    private String outputFileName;

    private Node() {
        routingTable = new ArrayList<>();
        fileList = new ArrayList<>();
        queryList = new ArrayList<>();
    }

    public Node(InetAddress ipAddress, int port) {
        this();
        this.outputFileName = "OutputFiles/file_"+ipAddress.getHostAddress()+"_"+port+".txt";
        createFileForNode();
        this.ipAddress = ipAddress;
        this.port = port;
        this.udpClient = new UDPClient(ipAddress, port);
        this.server = new SocketServer(this, ipAddress, port);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        boolean isInitialized = false;
        Scanner scanner = new Scanner(System.in);

        InetAddress ipAddress = null;
        int nodePortNumber = 0;
        String userName = "";

        loadProperties();

        String input;
        Node node = null;
        while (!isInitialized) {
            System.out.print("Enter the port number to start the node on : ");
            input = scanner.nextLine();

            System.out.print("Enter username : ");
            userName = new Scanner(System.in).nextLine();

            try {
                ipAddress = InetAddress.getByName(localIp);
                nodePortNumber = Integer.parseInt(input.trim());

                if ((nodePortNumber < 1000 || nodePortNumber > 65535)) {
                    throw new Exception("Invalid port number");
                }

                // create a node
                node = new Node(ipAddress, nodePortNumber);
                node.loadFiles();
                node.loadQueries();

                // register with the bootstrap server
                isInitialized = node.registerToBootstrapServer(ipAddress.getHostAddress(), nodePortNumber, userName);

                if (isInitialized) {
                    System.out.println("Node " + ipAddress.getHostAddress() + ":" + nodePortNumber + " registered successfully");
                    node.isActiveNode = true;
                    // start the internal server of the node to listen to messages
                    node.server.start();
                    // start the search Web Service
                    SearchServicePublisher.publish(ipAddress.getHostAddress(), nodePortNumber, new SearchServiceImpl(node));
                }

            } catch (Exception ex) {
                System.err.println("Unable to initialize the node " + ex.getMessage());

                if (isInitialized) {
                    // need to deregister from the BS
                    node.unregisterFromBS(ipAddress, nodePortNumber, userName);
                }

                isInitialized = false;
                node.isActiveNode = false;
            }

        }

        node.joinWithNeighbours();


        System.out.println("\n\n");
        String choice = "";

        while (!choice.equalsIgnoreCase(QUIT)) {
            System.out.println();
            System.out.println("***1. Print File List");
            System.out.println("***2. Print Routing Table");
            System.out.println("***3. Unregister Node");
            System.out.println("***4. Search Manually");
            System.out.println("***5. Random Query Search");
            System.out.println("***6. Search all from file");
            System.out.println("***7. Show Evaluation Results");
            System.out.println("***Please Enter your choice (Enter q to quit) : ");

            choice = scanner.nextLine();

            switch (choice.toLowerCase()) {
                case "1":
                    node.printFiles();
                    break;

                case "2":
                    node.printRoutingTable();
                    break;

                case "3":
                    node.unregisterFromBS(ipAddress, nodePortNumber, userName);
                    break;

                case "4":
                    System.out.println("***Enter File name to search for : ");
                    String fileName = scanner.nextLine();
                    node.initializeSearch(fileName);
                    break;

                case "5":
                    boolean error = true;
                    while(error){
                        System.out.println("***Enter number of random queries : ");
                        try {
                            int numberOfQueries = scanner.nextInt();
                            node.randomQuerySearch(numberOfQueries);
                            error = false;
                        } catch(Exception ex){
                            error = true;
                        }
                    }
                    break;

                case QUIT:
                    System.out.println("Unregistering from the BS");
                    node.unregisterFromBS(ipAddress, nodePortNumber, userName);
                    break;
                    
                case "6":
                    System.out.println("***Performing search based on the queries.txt file***");

                    node.resetEvaluationParameters();
                    node.writeToFile("\n\n------------------------------------------------------------------------\n\n");
                    
                    BufferedReader br = new BufferedReader(new FileReader(queryFile));
                    String fileName2;
                    while ( (fileName2 = br.readLine()) != null ) {
                    	System.out.println("Searching for : " + fileName2);
                    	node.initializeSearch(fileName2);
                    }
                    
                    break;

                case "7":
                    node.printEvaluationResults();
                    break;

                default:
                    System.out.println("\nEnter valid choice");
            }
        }

        System.exit(0);
    }

    public void resetEvaluationParameters(){
        this.noOfAnsweredMsgs = 0;
        this.noOfRecievedMsgs = 0;
        this.noOfForwardedMsgs = 0;
    }

    public static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(PROPERTIES_FILE));

            bootstrapServerIp = properties.getProperty(SERVER_IP);
            bootstrapServerPort = Integer.parseInt(properties
                    .getProperty(SERVER_PORT));
            udp = Boolean.parseBoolean(properties.getProperty(UDP,"false"));
            localIp = properties.getProperty(LOCAL_IP);
            fileName = properties.getProperty(FILE_NAMES,"files.txt");
            queryFile = properties.getProperty(QUERY_FILE,"queries.txt");

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return properties;
    }

    public void setActiveNode(boolean activeNode) {
        isActiveNode = activeNode;
    }

    public List<RoutingTableEntry> getRoutingTable() {
        return routingTable;
    }

    public List<String> getFileList() {
        return fileList;
    }

    public Map<String, SearchQuery> getSentMessages() {
        return sentMessages;
    }

    public String sendMessageToBS(String msg) throws IOException {

        String serverResponse = Commands.ERROR;
        try {
            // TCP send and receive
            Socket clientSocket = new Socket(bootstrapServerIp, bootstrapServerPort);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            writer.write(msg);
            writer.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            serverResponse = reader.readLine();
            System.out.println(serverResponse);

            reader.close();
            writer.close();
            clientSocket.close();
        }catch (ConnectException ex){
            System.err.println("Cannot connect to the BS : "+ex.getMessage());
        }

        return serverResponse;
    }

    public boolean registerToBootstrapServer(String ipAddress, int port, String userName) throws IOException {
        // build REG message
        String msg = createRegMessage(ipAddress, port, userName);
        System.out.println(msg);

        String serverResponse = sendMessageToBS(msg);
        if (serverResponse.equals(Commands.ERROR)){
            return false;
        }
        // handle the Registration response
        boolean success = handleRegistrationMessage(serverResponse);
        return success;
    }

    public void unregisterFromBS(InetAddress bsAddress, int bsPort, String username) throws IOException {

        if (this.isActiveNode) {
            String deregMessage = new MessageBuilder().append(Commands.UNREG)
                    .append(bsAddress.getHostAddress())
                    .append(bsPort + "")
                    .append(username)
                    .buildMessage();

            String leaveMessage = new MessageBuilder().append(Commands.LEAVE)
                    .append(ipAddress.getHostAddress())
                    .append(this.port + "")
                    .buildMessage();


            //send leave message to each neighbour
            for (RoutingTableEntry routingTableEntry : routingTable) {

                if (routingTableEntry.isActive()) {
                    String neighborIP = routingTableEntry.getIpAddress();
                    int neighborPort = Integer.parseInt(routingTableEntry.getPort());

                    udpClient.send(neighborIP, neighborPort, leaveMessage);

                }
            }

            // send the deregistration message to BS
            String serverResponse = sendMessageToBS(deregMessage);

            if (serverResponse.equals(Commands.ERROR)){
                return;
            }

            String[] parts = serverResponse.split("\\s+");
            if ("0".equals(parts[parts.length - 1])) {
                this.setActiveNode(false);
            } else {
                System.err.println("IP and port may not be in the registry or command is incorrect");
            }

        } else {
            System.out.println("Node is not connected or already deregisterd");
        }


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

                    if (noOfNodes <= JOINING_NODES_COUNT) {
                        for (int i = 1; i <= noOfNodes; i++) {
                            int index = 3 * i;
                            ip = msgParts[index];
                            port = msgParts[++index];
                            username = msgParts[++index];

                            // add neighbours to the routing table
                            entry = new RoutingTableEntry(username, ip, port);
                            entry.setActive(true);
                            routingTable.add(entry);
                        }
                    } else {

                        int randIndex[] = generateRandomIndices(noOfNodes, JOINING_NODES_COUNT);

                        for (int i = 0; i < JOINING_NODES_COUNT; i++) {
                            int index = 3 * randIndex[i];
                            ip = msgParts[index];
                            port = msgParts[++index];
                            username = msgParts[++index];

                            // add neighbours to the routing table
                            entry = new RoutingTableEntry(username, ip, port);
                            entry.setActive(true);
                            routingTable.add(entry);
                        }
                    }

                    result = true;
                }
            }
        }

        return result;

    }

    private int[] generateRandomIndices(int noOfNodes, int numberOfIndices) {
        int randIndex[] = new int[numberOfIndices];
        int randNumber;
        HashSet<Integer> numbers = new HashSet<Integer>();

        for (int i = 0; i < numberOfIndices; i++) {
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
                String ipAddress = entry.getIpAddress();
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
            if (entry.isActive()) {
                System.out.println((++count) + "\t" + entry);
            }
        }
        System.out.println();
    }

    public void loadFiles() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        List<String> temp = new ArrayList<>();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            temp.add(line.trim());
        }

        int fileToSelect = (((int) Math.floor(Math.random() * 3))) + 3;

        int[] randomIndices = generateRandomIndices(temp.size() - 1, fileToSelect);

        for (int i : randomIndices) {
            fileList.add(temp.get(i));
        }

        System.out.println(fileToSelect + " files indexed in the node");
    }

    public void loadQueries() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            queryList.add(line.trim());
        }

    }

    public void randomQuerySearch(int numberOfRandomQueries) throws InterruptedException {
        int randomIndices[] = generateRandomIndices(queryList.size() - 1, numberOfRandomQueries);

        String query;
        int count = 0;
        for (int index : randomIndices) {
            query = queryList.get(index);
            System.out.println("\nRandom Query #" + (++count) + " : " + query);
            this.initializeSearch(query);
            Thread.sleep(1000);
        }

    }

    public void printFiles() {

        System.out.println("\nFile List");
        int count = 0;
        for (String fileName : fileList) {
            System.out.println((++count) + ".\t" + fileName);
        }
        System.out.println();
    }


    public void search(String searchMessage) {

        //search in the the own file list
        //search message format - length SER IP port file_name hops
        Set<String> filesFound = new HashSet<>();

        String[] searchMessageComponents = searchMessage.split(" ");

        String id = searchMessageComponents[searchMessageComponents.length - 1];

        if (!messageIds.contains(id)) {


            messageIds.add(id);

            String ipAddressSM = searchMessageComponents[2];
            String portSM = searchMessageComponents[3];
            String fileNameSM = searchMessageComponents[4];


            if (searchMessageComponents.length > 6) {
                for (int i = 5; i < searchMessageComponents.length - 2; i++) {
                    fileNameSM += " " + searchMessageComponents[i];
                }
            }

            int hopsSM = Integer.parseInt(searchMessage.split(" ")[searchMessageComponents.length - 2]);

            fileNameSM = fileNameSM.toLowerCase();
            String[] tokens = fileNameSM.split(" ");

            String outputMessage = null;


            for (String fileName : fileList) {
                String temp = fileName.toLowerCase();
                List<String> fileNameTokens = Arrays.asList(temp.split(" "));
                for (String token : tokens) {
                    if (fileNameTokens.contains(token)) {
                        filesFound.add(fileName);
                        break;
                    }
                }
            }

            if(hopsSM != 0){
                this.noOfRecievedMsgs +=1;
            }

            if (filesFound.isEmpty() || hopsSM == 0) {

                if (hopsSM == 0 && !filesFound.isEmpty()) {
                    String fileNames = "";
                    for (String file : filesFound) {
                        fileNames += file + " ";
                    }
                    System.out.println("File \"" + fileNames + "\" found locally");
                }

                hopsSM++;

                searchMessage = this.createSearchMessage(hopsSM, fileNameSM, ipAddressSM, portSM, id);
                // iterate through the routing list and send UDP SER message
                for (RoutingTableEntry entry : routingTable) {
                    if (!(entry.getIpAddress().equals(ipAddressSM) && entry.getPort().equals(portSM))) {
                        try {
                            String ipAddress = entry.getIpAddress();
                            int port = Integer.parseInt(entry.getPort());

                            if (udp) {
                                this.noOfForwardedMsgs += 1;
                                udpClient.send(ipAddress, port, searchMessage);
                            } else {
                                this.noOfForwardedMsgs += 1;
                                WebServiceClient.sendSearchQuery(ipAddress, port, searchMessage);
                            }

                            System.out.println("Passing on the search query to neighbour " + entry.getIpAddress() + ":" + entry.getPort());

                        } catch (Exception e) {
                            System.err.println("Error sending SER message to " + entry.getIpAddress() + ":" + entry.getPort());
                        }
                    }
                }

            } else if (hopsSM != 0) {
                outputMessage = this.createSearchOkMessage(this.ipAddress.getHostAddress(), Integer.toString(this.port), hopsSM, filesFound, id);
                System.out.println("Search Results found for query : "+fileNameSM +"\n"+outputMessage+"\n");

                try {
                    if (udp) {
                        this.noOfAnsweredMsgs += 1;
                        udpClient.send(ipAddressSM, Integer.parseInt(portSM), outputMessage);
                    } else {
                        this.noOfAnsweredMsgs += 1;
                        WebServiceClient.sendSearchReply(ipAddressSM, Integer.parseInt(portSM), outputMessage);
                    }
                } catch (Exception e) {
                    System.err.println("Error sending SEROK message to " + ipAddressSM + ":" + portSM);
                }

            }
        }
    }

    private String createSearchMessage(int hops, String fileName, String ipAddress, String port, String id) {

        String hashCode = id;
        if (hops == 0) {
            String idString = System.currentTimeMillis() + ipAddress + port + fileName;
            hashCode = Integer.toString(idString.hashCode());
        }

        //SER message format - length SER IP port file_name hops
        String searchMessage = new MessageBuilder().append(Commands.SER)
                .append(ipAddress)
                .append(port)
                .append(fileName)
                .append(hops + "")
                .append(hashCode)
                .buildMessage();

        SearchQuery searchQuery = new SearchQuery(fileName, System.currentTimeMillis());
        this.sentMessages.put(hashCode, searchQuery);

        return searchMessage;
    }

    private String createSearchOkMessage(String ipAddress, String port, int hops, Set<String> filesFound, String id) {

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
                .append(id)
                .buildMessage();

        return searchOkMessage;
    }

    public void initializeSearch(String fileName) {

        String searchMessage = this.createSearchMessage(0, fileName, this.ipAddress.getHostAddress(), Integer.toString(this.port), "");
        this.search(searchMessage);

    }

    public void printEvaluationResults(){
        System.out.println("\nEvaluation Results");
        System.out.println("No of Search Queries Received : "+this.noOfRecievedMsgs);
        System.out.println("No of Search Queries Forwarded : "+this.noOfForwardedMsgs);
        System.out.println("No of Search Queries Answered : "+this.noOfAnsweredMsgs);
        System.out.println("Routing Table : "+ this.routingTable.size());
        System.out.println();
    }

    public void createFileForNode(){
        try {
            File file = new File(this.outputFileName);
            file.createNewFile();

            BufferedWriter output = new BufferedWriter(new FileWriter(file, true));
            output.write(this.outputFileName);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void writeToFile(String outputString){
        try {
            File fileName = new File(this.outputFileName);
            BufferedWriter output = new BufferedWriter(new FileWriter(fileName, true));

            output.write(outputString+"\n");
            output.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

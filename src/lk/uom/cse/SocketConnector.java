package lk.uom.cse;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SocketConnector {
    private int size = 65553;
    private int port = 55555;
    private InetAddress ipAddress;


    public SocketConnector(InetAddress ipAddress, int port){
        this.ipAddress = ipAddress;
        this.port = port;
    }


    public void sendRequest(String packet, InetAddress ipAddress, int port){
        try {
            DatagramSocket clientSocket = new DatagramSocket();

            byte[] sendData;
            byte[] receiveData = new byte[size];

            sendData = packet.getBytes();

            // send data
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
            clientSocket.send(sendPacket);

            // recieve data
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        //    clientSocket.receive(receivePacket);


            String modifiedSentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("FROM SERVER:" + modifiedSentence);
            clientSocket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws UnknownHostException {

        InetAddress ipAddress = InetAddress.getByName("localhost");

        SocketConnector socketCom = new SocketConnector(ipAddress,9877);
        String command = "0036 REG 129.82.123.45 5001 1234abcd";

        socketCom.sendRequest(command, socketCom.ipAddress, 55555);
    }


}

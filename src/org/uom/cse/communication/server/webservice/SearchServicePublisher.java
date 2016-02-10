package org.uom.cse.communication.server.webservice;


import javax.xml.ws.Endpoint;

public class SearchServicePublisher {
    public static final String SERVICE_NAME = "searchService";

    private SearchServicePublisher(){}

    public static void publish(String ipAddress, int port, SearchService service){
        String urlString = "http://" + ipAddress + ":" + port + "/" + SERVICE_NAME;
        Endpoint.publish(urlString,service);
        System.out.println("Search service published at "+urlString);
    }

}

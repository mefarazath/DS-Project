package org.uom.cse.communication.server.webservice;


import javax.xml.ws.Endpoint;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchServicePublisher {
    public static final String SERVICE_NAME = "searchService";

    private SearchServicePublisher() {
    }

    public static void publish(String ipAddress, int port, SearchService service) {
        String urlString = "http://" + ipAddress + ":" + port + "/" + SERVICE_NAME;

        ExecutorService es = Executors.newFixedThreadPool(5);
        Endpoint ep = Endpoint.create(service);
        ep.setExecutor(es);
        ep.publish(urlString);
      //  Endpoint.publish(urlString, service);
        System.out.println("Search service published at " + urlString);
    }

}

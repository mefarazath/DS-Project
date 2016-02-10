package org.uom.cse.communication.server.webservice;


import javax.xml.ws.Endpoint;

public class SearchServicePublisher {

    private SearchServicePublisher(){}

    public static void publishService(String URL, SearchService service){
        Endpoint.publish(URL,service);
        System.out.println(service + "published at "+URL);
    }

}

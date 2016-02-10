package org.uom.cse.communication.server.webservice;


import org.uom.cse.Node;

import javax.jws.WebService;

@WebService(endpointInterface = "org.uom.cse.communication.server.webservice.SearchService")
public class SearchServiceImpl implements SearchService{
    private Node node;

    public SearchServiceImpl(){

    }

    public SearchServiceImpl(Node node) {
        this.node = node;
    }

    @Override
    public void search(String searchMessage) {
        node.search(searchMessage);
        System.out.println("Search Query received : "+searchMessage);
    }

    @Override
    public void searchReply(String queryReply) {
        System.out.println("Search Reply received : "+queryReply);
    }

}

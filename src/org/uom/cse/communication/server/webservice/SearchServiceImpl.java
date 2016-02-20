package org.uom.cse.communication.server.webservice;


import org.uom.cse.Node;
import org.uom.cse.message.SearchQuery;

import javax.jws.WebService;

@WebService(endpointInterface = "org.uom.cse.communication.server.webservice.SearchService")
public class SearchServiceImpl implements SearchService {
    private Node node;

    public SearchServiceImpl() {

    }

    public SearchServiceImpl(Node node) {
        this.node = node;
    }

    @Override
    public void search(String searchMessage) {
        System.out.println();
        System.out.println("Search Query received : " + searchMessage);
        System.out.println();
        node.search(searchMessage);
    }

    @Override
    public void searchReply(String queryReply) {

        String[] messageComponents = queryReply.split(" ");

        SearchQuery searchQuery = node.getSentMessages().get(messageComponents[messageComponents.length - 1]);

        Long latency = System.currentTimeMillis() - searchQuery.getSearchTime();
        String hops = messageComponents[5];

        System.out.println(queryReply);
        System.out.println("Search successful for query : \"" + searchQuery.getSearchQuery() + "\" in " + latency + " ms and in " + hops + " hops");
        System.out.println();
        node.writeToFile(latency+" ms\t" + hops + " hops\t"+searchQuery.getSearchQuery());
    }

}

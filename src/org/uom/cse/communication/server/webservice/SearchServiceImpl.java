package org.uom.cse.communication.server.webservice;


import javax.jws.WebService;

@WebService(endpointInterface = "org.uom.cse.communication.server.webservice.SearchService")
public class SearchServiceImpl implements SearchService{

    @Override
    public void search(String query) {
        System.out.println("Search Query received : "+query);
    }

    @Override
    public void searchReply(String queryReply) {
        System.out.println("Search Reply received : "+queryReply);
    }

}

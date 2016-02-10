package org.uom.cse.communication.server.webservice;


import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface SearchService {

    @WebMethod
    void search(String query);

    @WebMethod
    void searchReply(String queryReply);

}

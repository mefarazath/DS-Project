package org.uom.cse.communication.client;

import org.uom.cse.communication.server.webservice.SearchService;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

public class WebServiceClient {

    public static final String SERVICE_NAME = "searchService";
    public static final String QNAME = "http://webservice.server.communication.cse.uom.org/";
    public static final String SERVICE_IMPL = "SearchServiceImplService";


    public static void sendSearchQuery(String ipAddress, int port, String query) throws MalformedURLException {
        SearchService service = init(ipAddress, port);
        service.search(query);
    }

    public static void sendSearchReply(String ipAddress, int port, String message) throws MalformedURLException {
        SearchService service = init(ipAddress, port);
        service.searchReply(message);
    }


    private static SearchService init(String ipAddress, int port) throws MalformedURLException {

        String urlString = "http://" + ipAddress + ":" + port + "/" + SERVICE_NAME + "?wsdl";

        URL url = new URL(urlString);
        QName qname = new QName(QNAME, SERVICE_IMPL);
        Service service = Service.create(url, qname);
        return service.getPort(SearchService.class);

    }

}

package org.uom.cse.communication.client;

import org.uom.cse.communication.server.webservice.SearchService;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

public class WebServiceClient implements CommunicationClient {


    @Override
    public String receive() {
        return null;
    }

    @Override
    public void send(String message) {
        URL url = null;
        try {
            url = new URL("http://localhost:9999/service?wsdl");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        //1st argument service URI, refer to wsdl document above
        //2nd argument is service name, refer to wsdl document above
        QName qname = new QName("http://webservice.communication.cse.uom.org/", "SearchServiceImplService");
        Service service = Service.create(url, qname);

        SearchService searchService = service.getPort(SearchService.class);
        searchService.search(message);

    }

    public static void main(String[] args) {
        new WebServiceClient().send("JACK JILL JUCK");
    }
}

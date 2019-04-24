package com.a1s.subscribegeneratorapp.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Service, that is used to unsubscribe one currently processing tele2_abonent from all subscriptions.
 */
@Service
public class SOAPClientService {
    private static final Log logger = LogFactory.getLog(SOAPClientService.class);

    @Autowired
    private RequestQueueService requestQueueService;

    private final String wsdlUrl = "http://portal-subscribe.a1s/ws/";

    private final String namespaceUri =  "urn:http://service.a1s/PortalSubscribe";
    private final String operationQNamePrefix = "ns1";

    private final String msisdnQNameLocalPart = "msisdn";
    private final String operatorQNameLocalPart = "operatorId";
    private final String tele2OperatorId = "107";

    void unsubscribeAllForMsisdn(String currentMsisdn) {
        try {
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            String operationQNameLocalPart = "UnsubscribeAllRequestEl";

            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage();
            SOAPHeader header = message.getSOAPHeader();
            SOAPBody body = message.getSOAPBody();
            header.detachNode();

            QName bodyName = new QName(namespaceUri,
                    operationQNameLocalPart, operationQNamePrefix);
            SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

            QName msisdn = new QName(msisdnQNameLocalPart);
            SOAPElement symbol1 = bodyElement.addChildElement(msisdn);
            symbol1.addTextNode(currentMsisdn);

            QName operatorId = new QName(operatorQNameLocalPart);
            SOAPElement symbol2 = bodyElement.addChildElement(operatorId);
            symbol2.addTextNode(tele2OperatorId);

            message.saveChanges();

            URL endpoint = new URL(wsdlUrl);

            ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();
            message.writeTo(outputStream1);
            logger.info("Sending SOAP 'UnsubscribeAll' request: \n" + outputStream1.toString());
            SOAPMessage response = connection.call(message, endpoint);

            ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
            response.writeTo(outputStream2);
            logger.info("Getting SOAP 'UnsubscribeAll' response: \n" + outputStream2.toString());

            outputStream1.close();
            outputStream2.close();
            connection.close();

        } catch (SOAPException e) {
            logger.error("Got SOAP Exception while making SOAP request, going to continue processing " +
                    "next request", e);
            requestQueueService.makeMsisdnNotBusy(currentMsisdn);

        } catch (IOException e1) {
            logger.error("Got IO Exception while making SOAP request, going to continue processing next request", e1);
            requestQueueService.makeMsisdnNotBusy(currentMsisdn);

        }
    }

    String checkSubscriptionsForMsisdn(String currentMsisdn) {
        try {
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            String operationQNameLocalPart = "GetAbonentActiveSubscriptionsRequestEl";

            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage();
            SOAPHeader header = message.getSOAPHeader();
            SOAPBody body = message.getSOAPBody();
            header.detachNode();

            QName bodyName = new QName(namespaceUri,
                    operationQNameLocalPart, operationQNamePrefix);
            SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

            QName msisdn = new QName(msisdnQNameLocalPart);
            SOAPElement symbol1 = bodyElement.addChildElement(msisdn);
            symbol1.addTextNode(currentMsisdn);

            message.saveChanges();

            URL endpoint = new URL(wsdlUrl);

            ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();
            message.writeTo(outputStream1);
            logger.info("Sending SOAP 'UnsubscribeAll' request: \n" + outputStream1.toString());
            SOAPMessage response = connection.call(message, endpoint);

            ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
            response.writeTo(outputStream2);
            logger.info("Getting SOAP 'UnsubscribeAll' response: \n" + outputStream2.toString());

            outputStream1.close();
            outputStream2.close();
            connection.close();

            return outputStream2.toString();

        } catch (SOAPException e) {
            logger.error("Got SOAP Exception while making SOAP request, going to continue processing " +
                    "next request", e);
            requestQueueService.makeMsisdnNotBusy(currentMsisdn);

        } catch (IOException e1) {
            logger.error("Got IO Exception while making SOAP request, going to continue processing next request", e1);
            requestQueueService.makeMsisdnNotBusy(currentMsisdn);

        }

        return null;
    }

}

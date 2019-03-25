package com.a1s.subscribegeneratorapp.service;

import static com.a1s.ConfigurationConstantsAndMethods.*;
import com.a1s.subscribegeneratorapp.smsc.CustomSmppServer;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

/**
 * Service, that starts and stops CustomSmppServer, makes deliver_sm requests
 * out of subscribeRequestData.
 */
@Service
public class SmscProcessorService {
    private static final Log logger = LogFactory.getLog(SmscProcessorService.class);

    @Autowired
    private RequestQueueService requestQueueService;
    @Autowired
    private TransactionReportService transactionReportService;
    @Autowired
    private CustomSmppServer customSmppServer;

    private DeliverSm currentReadyDeliverSm = new DeliverSm();

    /**
     * Starts SMSC and sets the smppSession to the one with SYSTEM_ID.
     * @param bindCompleted will be counted down after smppSession is started
     */
    void startSmsc(CountDownLatch bindCompleted) { //todo remove after using smartLifeCycle
        customSmppServer.startServerMain(bindCompleted);
        requestQueueService.setSmppSession();
    }

    /**
     * Stops SMSC.
     */
    void stopSmsc() {   //todo remove after using smartLifeCycle
        customSmppServer.stop();
    }

    /**
     * Makes deliver_sm request without source_address out of subscribeRequestData
     * @param dataForRequest contains short_num and short_message for request
     */
    void makeRequestFromDataAndSend(final SubscribeRequestData dataForRequest) {
        Address destinationAddress = new Address((byte) 1, (byte) 1, dataForRequest.getShortNum());
        logger.info("Creating " + dataForRequest.getId() + "th deliver_sm, yet without msisdn...");
        try {
            currentReadyDeliverSm.setDestAddress(destinationAddress);
            currentReadyDeliverSm.setShortMessage(dataForRequest.getRequestText().getBytes(Charset.forName("UTF-16")));
            currentReadyDeliverSm.setDataCoding(SmppConstants.DATA_CODING_UCS2);
        } catch (SmppInvalidArgumentException e) {
            logger.error("Smth wrong while setting short message for deliver_sm", e);
            transactionReportService.processOneFailureReport(dataForRequest.getId(), GOT_SMPP_INVALID_ARG_EXCEPTION);
        }

        sendRequest(currentReadyDeliverSm, dataForRequest.getId());
    }

    /**
     * Puts outgoingDeliverSm to SMSC queue.
     * @param outgoingDeliverSm currently processing deliver_sm request
     * @param transactionId id of current operation
     */
    private void sendRequest(final DeliverSm outgoingDeliverSm, final int transactionId) {
        requestQueueService.putDeliverSmToQueue(outgoingDeliverSm, transactionId);
    }
}

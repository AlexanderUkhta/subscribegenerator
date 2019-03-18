package com.a1s.subscribegeneratorapp.service;

import static com.a1s.ConfigurationConstantsAndMethods.*;
import com.a1s.subscribegeneratorapp.smsc.CustomSmppServer;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

@Service
public class SmscProcessorService {
    private static final Log logger = LogFactory.getLog(SmscProcessorService.class);

    @Autowired
    private RequestQueueService requestQueueService;
    @Autowired
    private TransactionReportService transactionReportService;

    private DeliverSm currentReadyDeliverSm = new DeliverSm();

    //todo make autowired and put this to lifecycle.start()
    private CustomSmppServer customSmppServer;
    {
        customSmppServer = new CustomSmppServer(
                CustomSmppServer.getBaseServerConfiguration(SMPP_SERVER_PORT, SYSTEM_ID), new NioEventLoopGroup(),
                new NioEventLoopGroup());
    }

    //todo remove?
    void startSmsc(CountDownLatch bindCompleted) {
        customSmppServer.startServerMain(bindCompleted);
        requestQueueService.setSmppSession(SYSTEM_ID);
    }

    void makeRequestFromDataAndSend(final SubscribeRequestData dataForRequest) {
        Address destinationAddress = new Address((byte) 1, (byte) 1, dataForRequest.getShortNum());

        try {
            currentReadyDeliverSm.setDestAddress(destinationAddress);
            currentReadyDeliverSm.setShortMessage(dataForRequest.getRequestText().getBytes(Charset.forName("UTF-16")));
            currentReadyDeliverSm.setDataCoding(SmppConstants.DATA_CODING_UCS2);
        } catch (SmppInvalidArgumentException e) {
            logger.error("Smth wrong while setting short message for deliver_sm", e);
            transactionReportService.processOneFailureReport(dataForRequest.getId(), GOT_SMPP_INVALID_ARG_EXCEPTION);
            //how to continue with another request?
        }

        sendRequest(currentReadyDeliverSm, dataForRequest.getId());
    }

    private void sendRequest(final DeliverSm outgoingDeliverSm, final int transactionId) {
        requestQueueService.putDeliverSmToQueue(outgoingDeliverSm, transactionId);
    }
}

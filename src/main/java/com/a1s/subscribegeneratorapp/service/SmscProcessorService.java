package com.a1s.subscribegeneratorapp.service;

import static com.a1s.ConfigurationConstantsAndMethods.*;
import com.a1s.subscribegeneratorapp.smsc.CustomSmppServer;
import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
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

    private DeliverSm currentReadyDeliverSm;
    private CustomSmppServer customSmppServer;
    {
        currentReadyDeliverSm = new DeliverSm();
        customSmppServer = new CustomSmppServer(
                CustomSmppServer.getBaseServerConfiguration(SMPP_SERVER_PORT, SYSTEM_ID), new NioEventLoopGroup(),
                new NioEventLoopGroup());
    }

    void startSmsc(CountDownLatch bindCompleted) {
        customSmppServer.startServerMain(bindCompleted);
        requestQueueService.setSmppSession(SYSTEM_ID);
    }

    void makeRequestFromDataAndSend(final SubscribeRequest dataForRequest) {
        Address destinationAddress = new Address((byte) 1, (byte) 1, dataForRequest.getShortNum());

        try {
            currentReadyDeliverSm.setDestAddress(destinationAddress);
            currentReadyDeliverSm.setShortMessage(dataForRequest.getRequestText().getBytes(Charset.forName("UTF-16")));
            currentReadyDeliverSm.setDataCoding(SmppConstants.DATA_CODING_UCS2);
        } catch (SmppInvalidArgumentException e) {
            logger.error("Smth wrong while setting short message for deliver_sm", e);
        }

        sendRequest(currentReadyDeliverSm, dataForRequest.getId());
    }

    private void sendRequest(final DeliverSm outgoingDeliverSm, final int transactionId) {
        requestQueueService.putDeliverSmToQueue(outgoingDeliverSm, transactionId);
    }
}

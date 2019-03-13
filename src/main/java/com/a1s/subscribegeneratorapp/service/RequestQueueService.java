package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.smsc.CustomSmppServer;
import com.a1s.subscribegeneratorapp.dao.MsisdnDao;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.type.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.a1s.ConfigurationConstantsAndMethods.*;

@Service
public class RequestQueueService {
    private static final Log logger = LogFactory.getLog(RequestQueueService.class);

    @Autowired
    private MsisdnDao msisdnDao;

    private Map<String, Integer> msisdnProcessMap = msisdnDao.findAll();
    private DefaultSmppSession smppSession;


    void putDeliverSmToQueue(DeliverSm deliverSm) {

        try {
            ultimateWhile(this::hasNoFreeMsisdn, 60);
        } catch (TimeoutException e) {
            logger.error("All msisdns are busy for too long, next request will be processed", e);
        }

        String currentFreeMsisdn = getNextFreeMsisdn();
        deliverSm.setSourceAddress(new Address((byte) 1, (byte) 1, currentFreeMsisdn));

        try {

            smppSession.sendRequestPdu(deliverSm, TimeUnit.SECONDS.toMillis(60), false);
            logger.info("*** " + counterOfSentMessages.incrementAndGet() +
                    "th DeliverSm is sent on " + currentFreeMsisdn + " ***");
            msisdnProcessMap.put(currentFreeMsisdn, MSISDN_BUSY);

        } catch (RecoverablePduException e1) {
            logger.error("Got recoverable pdu exception while sending pdu", e1);
            msisdnProcessMap.put(currentFreeMsisdn, MSISDN_NOT_BUSY);
        } catch (UnrecoverablePduException e2) {
            logger.error("Got unrecoverable pdu exception while sending pdu", e2);
            msisdnProcessMap.put(currentFreeMsisdn, MSISDN_NOT_BUSY);
        } catch (SmppTimeoutException e3) {
            logger.error("Got smpp timeout exception", e3);
            msisdnProcessMap.put(currentFreeMsisdn, MSISDN_NOT_BUSY);
        } catch (SmppChannelException e4) {
            logger.error("Got smpp channel exception", e4);
            msisdnProcessMap.put(currentFreeMsisdn, MSISDN_NOT_BUSY);
        } catch (InterruptedException e5) {
            logger.error("Got interrupted exception", e5);
            msisdnProcessMap.put(currentFreeMsisdn, MSISDN_NOT_BUSY);
        }
    }

    private String getNextFreeMsisdn() {

        return msisdnProcessMap.entrySet().stream()
                .filter(entry -> ((int) entry.getValue() == MSISDN_NOT_BUSY))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

    }

    private Boolean hasNoFreeMsisdn() {
        return msisdnProcessMap.values().stream().noneMatch(value -> ((int) value == MSISDN_NOT_BUSY));
    }

    void setSmppSession(final String systemId) {
        smppSession = (DefaultSmppSession) CustomSmppServer.getServerSession(systemId);
    }

    public void makeMsisdnNotBusy(final String msisdnNowNotBusy) { //Используем в UdhConcatenationTask, или в OnPduReceived, если нет UDH
        msisdnProcessMap.put(msisdnNowNotBusy, MSISDN_NOT_BUSY);
    }


}

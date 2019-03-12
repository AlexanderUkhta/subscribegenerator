package com.a1s.subscribegeneratorapp.service;

import com.a1s.smsc.CustomSmppServer;
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
import java.util.function.Supplier;

import static com.a1s.ConfigurationConstants.*;

@Service
public class RequestQueueService {
    private static final Log logger = LogFactory.getLog(RequestQueueService.class);

    @Autowired
    private MsisdnDao msisdnDao;

    private Map<String, Integer> msisdnProcessMap = msisdnDao.findAll();
    private DefaultSmppSession smppSession;


    public void putDeliverSmToQueue(DeliverSm deliverSm) {
        DeliverSm outgoingDeliverSm = deliverSm;

        try {
            ultimateWhile(this::hasNextFreeMsisdn, 30);
        } catch (TimeoutException e) {
            logger.error("All msisdns are busy for too long, next request will be processed", e);
        }

        String currentFreeMsisdn = getNextFreeMsisdn();
        outgoingDeliverSm.setSourceAddress(new Address((byte) 1, (byte) 1, currentFreeMsisdn));

        try {
            smppSession.sendRequestPdu(outgoingDeliverSm, TimeUnit.SECONDS.toMillis(60), false);
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

        String currentFreeMsisndn = msisdnProcessMap.entrySet().stream()
                .filter(entry -> ((int) entry.getValue() == MSISDN_NOT_BUSY))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        return currentFreeMsisndn;
    }

    private Boolean hasNextFreeMsisdn() {
        return msisdnProcessMap.values().stream().anyMatch(value -> ((int) value == MSISDN_NOT_BUSY));
    }

    public void setSmppSession(String systemId) {
        smppSession = (DefaultSmppSession) CustomSmppServer.getServerSession(systemId);
    }

    private void ultimateWhile(Supplier<Boolean> condition, Integer timeoutSeconds) throws TimeoutException {
        Long start = System.currentTimeMillis();
        Long end = 0L;

        while (condition.get() && ((end - start) / 1000) < timeoutSeconds) {
            end = System.currentTimeMillis();
        }

        if  (((end - start) / 1000) >= timeoutSeconds) {
            throw new TimeoutException();
        }
    }


}

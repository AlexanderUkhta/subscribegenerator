package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.MsisdnState;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.a1s.ConfigurationConstantsAndMethods.*;

@Service
public class RequestQueueService {
    private static final Log logger = LogFactory.getLog(RequestQueueService.class);

    @Autowired
    TransactionReportService transactionReportService;

    private Map<String, MsisdnState> msisdnProcessMap = new ConcurrentHashMap<>(); //todo: msisdnDao.findAll(); по возможности
    private DefaultSmppSession smppSession;


    void putDeliverSmToQueue(DeliverSm deliverSm, final int transactionId) {

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
            msisdnProcessMap.put(currentFreeMsisdn, new MsisdnState(transactionId, 1000, MSISDN_BUSY));

        } catch (RecoverablePduException e1) {
            logger.error("Got recoverable pdu exception while sending pdu", e1);
            msisdnProcessMap.put(currentFreeMsisdn, new MsisdnState(-1, 1000, MSISDN_NOT_BUSY));
        } catch (UnrecoverablePduException e2) {
            logger.error("Got unrecoverable pdu exception while sending pdu", e2);
            msisdnProcessMap.put(currentFreeMsisdn, new MsisdnState(-1, 1000, MSISDN_NOT_BUSY));
        } catch (SmppTimeoutException e3) {
            logger.error("Got smpp timeout exception", e3);
            msisdnProcessMap.put(currentFreeMsisdn, new MsisdnState(-1, 1000, MSISDN_NOT_BUSY));
        } catch (SmppChannelException e4) {
            logger.error("Got smpp channel exception", e4);
            msisdnProcessMap.put(currentFreeMsisdn, new MsisdnState(-1, 1000, MSISDN_NOT_BUSY));
        } catch (InterruptedException e5) {
            logger.error("Got interrupted exception", e5);
            msisdnProcessMap.put(currentFreeMsisdn, new MsisdnState(-1, 1000, MSISDN_NOT_BUSY));
        }
    }

    private String getNextFreeMsisdn() {

        return msisdnProcessMap.entrySet().stream()
                .filter(entry -> (entry.getValue().getBusyState() == MSISDN_NOT_BUSY))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

    }

    private Boolean hasNoFreeMsisdn() {
        return msisdnProcessMap.values().stream().noneMatch(value -> (value.getBusyState() == MSISDN_NOT_BUSY));
    }

    private int getTransactionIdByMsisdn(final String msisdn) {
        return msisdnProcessMap.get(msisdn).getCurrentTransactionId();
    }

    void setSmppSession(final String systemId) {
        smppSession = (DefaultSmppSession) CustomSmppServer.getServerSession(systemId);
    }

    public void makeTransactionReport(final String msisdn, final byte[] shortMessage) {

        transactionReportService.processReportInfo(shortMessage, getTransactionIdByMsisdn(msisdn));
        msisdnProcessMap.put(msisdn, new MsisdnState(-1, 1000, MSISDN_NOT_BUSY));
        //todo: после этого процессинг транзакции по ID внутри msisdn_state и сравнение short_message c ожидаемым
        //мб создать мапу с данными файла, которые уже в обработке, так не будет двойного обращения к requests map
    }


}

package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.config.ReadMsisdnProperties;
import com.a1s.subscribegeneratorapp.model.MsisdnStateData;
import com.a1s.subscribegeneratorapp.smsc.CustomSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.type.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.a1s.ConfigurationConstantsAndMethods.*;

@Service
public class RequestQueueService {
    private static final Log logger = LogFactory.getLog(RequestQueueService.class);

    @Autowired
    private TransactionReportService transactionReportService;
    @Autowired
    private ReadMsisdnProperties readMsisdnProperties;
    @Autowired
    private SOAPClientService soapClientService;

    private Map<String, MsisdnStateData> msisdnProcessMap = new ConcurrentHashMap<>();
    private DefaultSmppSession smppSession;


    void putDeliverSmToQueue(DeliverSm deliverSm, final int transactionId) {

        try {
            logger.info("Deliver_sm with transaction_id = " + transactionId + " is waiting for msisdn...");
            ultimateWhile(this::hasNoFreeMsisdn, 60);
        } catch (TimeoutException e) {
            logger.error("All msisdns are busy for too long, got current request failed. The next request " +
                    "will be processed", e);
            transactionReportService.processOneFailureReport(transactionId, ALL_MSISDN_BUSY_FOR_TOO_LONG);

        }

        String currentFreeMsisdn = getNextFreeMsisdn();
        deliverSm.setSourceAddress(new Address((byte) 1, (byte) 1, currentFreeMsisdn));

        soapClientService.unsubscribeAllForMsisdn(currentFreeMsisdn);

        try {
            smppSession.sendRequestPdu(deliverSm, TimeUnit.SECONDS.toMillis(60), false);
            logger.info("*** " + transactionId +
                    "th DeliverSm request is sent on " + currentFreeMsisdn + " ***");
            msisdnProcessMap.put(currentFreeMsisdn, new MsisdnStateData(transactionId,
                    System.currentTimeMillis(), MSISDN_BUSY));

        } catch (RecoverablePduException e1) {
            logger.error("Got recoverable pdu exception while sending request", e1);
            transactionReportService.processOneFailureReport(transactionId, GOT_RCVRBL_PDU_EXCEPTION);
            makeMsisdnNotBusy(currentFreeMsisdn);

        } catch (UnrecoverablePduException e2) {
            logger.error("Got unrecoverable pdu exception while sending request", e2);
            transactionReportService.processOneFailureReport(transactionId, GOT_UNRCVRBL_PDU_EXCEPTION);
            makeMsisdnNotBusy(currentFreeMsisdn);

        } catch (SmppTimeoutException e3) {
            logger.error("Got smpp timeout exception while sending request", e3);
            transactionReportService.processOneFailureReport(transactionId, GOT_SMPP_TIMEOUT_EXCEPTION);
            makeMsisdnNotBusy(currentFreeMsisdn);

        } catch (SmppChannelException e4) {
            logger.error("Got smpp channel exception while sending request", e4);
            transactionReportService.processOneFailureReport(transactionId, GOT_SMPP_CHANNEL_EXCEPTION);
            makeMsisdnNotBusy(currentFreeMsisdn);

        } catch (InterruptedException e5) {
            logger.error("Got interrupted exception while sending request", e5);
            transactionReportService.processOneFailureReport(transactionId, GOT_INTERRUPTED_EXCEPTION);
            makeMsisdnNotBusy(currentFreeMsisdn);

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
        return msisdnProcessMap.values()
                .stream()
                .noneMatch(value -> (value.getBusyState() == MSISDN_NOT_BUSY));

    }

    Boolean hasAllMsisdnFree() {
        return msisdnProcessMap.values()
                .stream()
                .allMatch(value -> (value.getBusyState() == MSISDN_NOT_BUSY));

    }

    void makeMsisdnNotBusy(final String msisdn) {
        msisdnProcessMap.put(msisdn, new MsisdnStateData(-1, -1, MSISDN_NOT_BUSY));
        logger.info("Msisdn " + msisdn + " is now free");

    }

    public void processMsisdnTimeoutCase(final String msisdn) {
        transactionReportService.processOneFailureReport(getTransactionIdByMsisdn(msisdn),
                GOT_MSISDN_TIMEOUT_EXCEPTION + msisdn);
        makeMsisdnNotBusy(msisdn);

    }

    void fillMsisdnMap() {
        List<String> msisdnList = readMsisdnProperties.getMsisdnList();
        msisdnList.forEach(msisdn -> msisdnProcessMap
                .put(msisdn, new MsisdnStateData(-1, -1, MSISDN_NOT_BUSY)));
        logger.info("Msisdn map is filled with " + msisdnProcessMap.size() + " pairs," +
                " needed " + msisdnList.size() + "pairs");

    }

    public Map<String, MsisdnStateData> getMsisdnMap() {
        return msisdnProcessMap;
    }

    int getTransactionIdByMsisdn(final String msisdn) {
        return msisdnProcessMap.get(msisdn).getCurrentTransactionId();

    }

    void setSmppSession(final String systemId) {
        smppSession = (DefaultSmppSession) CustomSmppServer.getServerSession(systemId);

    }
}

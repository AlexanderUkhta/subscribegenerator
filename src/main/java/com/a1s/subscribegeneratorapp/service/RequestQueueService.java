package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.config.MsisdnAndExcelProperties;
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

/**
 * Service, that gradually appropriates msisdn value to each formed deliver_sm request.
 * Then deliver_sm request is put in CustomSmppServer queue and sent to client.
 * If all msisdns in a pool are busy for more than 20 seconds, transaction is finished by force
 * and next transaction starts.
 */
@Service
public class RequestQueueService {
    private static final Log logger = LogFactory.getLog(RequestQueueService.class);

    @Autowired
    private TransactionReportService transactionReportService;
    @Autowired
    private MsisdnAndExcelProperties msisdnAndExcelProperties;
    @Autowired
    private SOAPClientService soapClientService;

    private Map<String, MsisdnStateData> msisdnProcessMap = new ConcurrentHashMap<>();
    private DefaultSmppSession smppSession;

    /**
     * Sets deliver_sm source_address by msisdn value. Puts deliver_sm to CustomSmpServer queue.
     * When deliver_sm is sent with a source_address, a proper msisdn becomes BUSY.
     * If all msisdns in a pool are busy for more than 20 seconds, transaction is finished by force
     * and next transaction starts.
     * If there are problems with sending pdu, PduException appears. In this case, error report is generated,
     * msisdn becomes NOT_BUSY.
     * @param deliverSm currently processing deliver_sm
     * @param transactionId id of current transaction
     */
    void putDeliverSmToQueue(DeliverSm deliverSm, final int transactionId) {

        try {
            logger.info("Deliver_sm with transaction_id = " + transactionId + " is waiting for msisdn...");
            ultimateWhile(this::hasNoFreeMsisdn, 40);

        } catch (TimeoutException e) {
            logger.error("All msisdns are busy for too long, got current request failed. The next request " +
                    "will be processed", e);
            transactionReportService.processOneFailureReport(transactionId, ALL_MSISDN_BUSY_FOR_TOO_LONG);
            return;
        }

        String currentFreeMsisdn = getNextFreeMsisdn();
        deliverSm.setSourceAddress(new Address((byte) 1, (byte) 1, currentFreeMsisdn));

        soapClientService.unsubscribeAllForMsisdn(currentFreeMsisdn);
        logger.info("Current deliver_sm text: " + new String(deliverSm.getShortMessage()));
        try {
            smppSession.sendRequestPdu(deliverSm, TimeUnit.SECONDS.toMillis(60), false);
            logger.info(transactionId + "th deliver_sm request is sent from " + currentFreeMsisdn);

            msisdnProcessMap.put(currentFreeMsisdn, new MsisdnStateData(transactionId,
                    System.currentTimeMillis(), MSISDN_BUSY));

        } catch (RecoverablePduException e1) {
            logger.error("Got recoverable pdu exception while sending request", e1);
            transactionReportService.processOneFailureReport(transactionId, GOT_RCVRBL_PDU_EXCEPTION);

        } catch (UnrecoverablePduException e2) {
            logger.error("Got unrecoverable pdu exception while sending request", e2);
            transactionReportService.processOneFailureReport(transactionId, GOT_UNRCVRBL_PDU_EXCEPTION);

        } catch (SmppTimeoutException e3) {
            logger.error("Got smpp timeout exception while sending request", e3);
            transactionReportService.processOneFailureReport(transactionId, GOT_SMPP_TIMEOUT_EXCEPTION);

        } catch (SmppChannelException e4) {
            logger.error("Got smpp channel exception while sending request", e4);
            transactionReportService.processOneFailureReport(transactionId, GOT_SMPP_CHANNEL_EXCEPTION);

        } catch (InterruptedException e5) {
            logger.error("Got interrupted exception while sending request", e5);
            transactionReportService.processOneFailureReport(transactionId, GOT_INTERRUPTED_EXCEPTION);
        }

    }

    /**
     * Gets the first free msisdn from msisdnProcessMap, if such exists.
     * @return msisdn which is NOT_BUSY, and 'null' if there is no free msisdn
     */
    private String getNextFreeMsisdn() {
        return msisdnProcessMap.entrySet().stream()
                .filter(entry -> (entry.getValue().getBusyState() == MSISDN_NOT_BUSY))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

    }

    /**
     * Returns the state of msisdnProcessMap, either it is full or not.
     * @return 'true' if map is full, otherwise 'false'
     */
    private Boolean hasNoFreeMsisdn() {
        return msisdnProcessMap.values()
                .stream()
                .noneMatch(value -> (value.getBusyState() == MSISDN_NOT_BUSY));

    }

    /**
     * Returns the state of msisdnProcessMap, either it is fully NOT_BUSY.
     * @return 'true' if map has only NOT_BUSY msisdns, otherwise 'false'
     */
    Boolean stillHasBusyMsisdns() {
        return !(msisdnProcessMap.values()
                .stream()
                .allMatch(value -> (value.getBusyState() == MSISDN_NOT_BUSY)));

    }

    /**
     * Makes given msisdn NOT_BUSY.
     * @param msisdn current msisdn
     */
    void makeMsisdnNotBusy(final String msisdn) {
        msisdnProcessMap.put(msisdn, new MsisdnStateData(-1, -1, MSISDN_NOT_BUSY));
        logger.info("Msisdn " + msisdn + " is now not_busy");

    }

    /**
     * Method is invoked, when MsisdnTimeoutTask reports timeout on given msisdn.
     * In this case, error report is processed and msisdn become NOT_BUSY.
     * @param msisdn msisdn that caught timeout
     */
    public void processMsisdnTimeoutCase(final String msisdn) {
        transactionReportService.processMsisdnTimeoutReport(msisdn);

    }

    /**
     * Fills msisdnProcessMap with test msisdns from application.properties.
     */
    void fillMsisdnMap() {
        List<String> msisdnList = msisdnAndExcelProperties.getMsisdnList();
        msisdnList.forEach(msisdn -> msisdnProcessMap
                .put(msisdn, new MsisdnStateData(-1, -1, MSISDN_NOT_BUSY)));
        logger.info("Msisdn map is filled with " + msisdnProcessMap.size() + " pairs," +
                " needed " + msisdnList.size() + " pairs");

    }

    /**
     * Returns msisdnProcessMap.
     * @return
     */
    public Map<String, MsisdnStateData> getMsisdnMap() {
        return msisdnProcessMap;

    }

    /**
     * Returns transactionId by given msisdn.
     * @param msisdn current msisdn
     * @return transactionId
     */
    int getTransactionIdByMsisdn(final String msisdn) {
        return msisdnProcessMap.get(msisdn).getCurrentTransactionId();

    }

    /**
     * Sets smppSession to a non-null value after CustomSmppServer is started.
     */
    void setSmppSession() {
        smppSession = (DefaultSmppSession) CustomSmppServer.getServerSession(SYSTEM_ID);

    }
}

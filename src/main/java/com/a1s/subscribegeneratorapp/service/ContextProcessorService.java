package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.MsisdnStateData;
import com.a1s.subscribegeneratorapp.model.MsisdnTimeoutTask;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import com.a1s.subscribegeneratorapp.smsc.CustomSmppServer;
import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.type.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.a1s.ConfigurationConstantsAndMethods.*;
import static com.a1s.ConfigurationConstantsAndMethods.GOT_INTERRUPTED_EXCEPTION;
import static com.a1s.ConfigurationConstantsAndMethods.GOT_SMPP_CHANNEL_EXCEPTION;

/**
 * Service, that starts the main components of application, implements full process of generating requests from
 * excel-data and then finishes by generating full transaction report.
 *
 */
@Service
public class ContextProcessorService {
    private static final Log logger = LogFactory.getLog(ContextProcessorService.class);

    private Map<Integer, SubscribeRequestData> requests;

    @Autowired
    private SmscProcessorService smscProcessorService;
    @Autowired
    private RequestQueueService requestQueueService;
    @Autowired
    private TransactionReportService transactionReportService;
    @Autowired
    private MsisdnTimeoutTask msisdnTimeoutTask;
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    /**
     * Starts SMSC, sets msisnMap in requestQueue service, starts subscribe requests processing.
     * At the end makes full report out of transaction results.
     */
    public long[] process() {
        logger.info("Got context map full, going to start SMSC...");
        CountDownLatch bindCompleted = new CountDownLatch(1);
        smscProcessorService.startSmsc(bindCompleted);

        logger.info("Filling msisdn map...");
        requestQueueService.fillMsisdnMap();
        threadPoolTaskScheduler.schedule(msisdnTimeoutTask, new Date(System.currentTimeMillis() + 1000));

        logger.info("Start making requests from userdata...");
        requests.forEach((id, requestInfo) ->
                smscProcessorService.makeRequestFromDataAndSend(requestInfo));

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("All requests have been formed from requestData Map, going to make full report, " +
                "when all responses would be received.");
        try {
            ultimateWhile(requestQueueService::stillHasBusyMsisdns, 90);
        } catch (TimeoutException e) {
            logger.error("Waiting for too long, can't start making report.", e);
            logger.warn("Can't get all transactions' results, going to make report on existing data.");
        }

        //todo wait for all resps are sent or received
        stopMsisdnTimeoutService.set(1);

        logger.info("Start creating data report...");
        transactionReportService.startMakingFullDataReport();

        long[] infoAboutResults = new long[2];
        infoAboutResults[0] = transactionReportService.getSuccessfulTransactionsQuantity();
        infoAboutResults[1] = transactionReportService.getFailedTransactionsQuantity();

        smscProcessorService.stopSmsc();

        return infoAboutResults;

    }

    public void setSubscribeRequestMap(final Map<Integer, SubscribeRequestData> requests) {
        this.requests = requests;

    }

    SubscribeRequestData findRequestDataById(final int transactionId) {
        return requests.get(transactionId);

    }

    public void startUssdTest() {
        logger.info("Got context map full, going to start SMSC...");
        CountDownLatch bindCompleted = new CountDownLatch(1);
        smscProcessorService.startSmsc(bindCompleted);

        DefaultSmppSession smppSession = (DefaultSmppSession) CustomSmppServer.getServerSession(SYSTEM_ID);
        sendRequest(smppSession);

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        smscProcessorService.stopSmsc();
    }

    private void sendRequest(DefaultSmppSession smppSession) {
        String msisdn = "79532467581";
        String shortNum = "101030";
        int transactionId = 11112222;
        String shortMessage = shortNum + "&";

        DeliverSm deliverSm = new DeliverSm();
        deliverSm.setDestAddress(new Address((byte) 1, (byte) 1, shortNum));
        deliverSm.setSourceAddress(new Address((byte) 1, (byte) 1, msisdn));
        deliverSm.setSequenceNumber(transactionId);
        try {
            deliverSm.setShortMessage(CharsetUtil.encode(shortMessage, CharsetUtil.CHARSET_UTF_8));
        } catch (SmppInvalidArgumentException e) {
            e.printStackTrace();
        }

        try {
            smppSession.sendRequestPdu(deliverSm, TimeUnit.SECONDS.toMillis(60), false);
            logger.info(transactionId + "th deliver_sm request is sent from " + msisdn);

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
}


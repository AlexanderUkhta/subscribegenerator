package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.MsisdnTimeoutTask;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static com.a1s.ConfigurationConstantsAndMethods.stopMsisdnTimeoutService;
import static com.a1s.ConfigurationConstantsAndMethods.ultimateWhile;

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

//        try {
//            ultimateWhile(() -> (transactionReportService.getReportDataMapSize() ==
//                    ConfigurationConstantsAndMethods.rowQuantityInExcel.get()), 90);
//        } catch (TimeoutException e) {
//            logger.error("Not all notifications returned after 90 secs waiting");
//        }

        logger.info("All requests have been formed from requestData Map, going to make full report, " +
                "when all responses would be received");
        try {
            ultimateWhile(requestQueueService::stillHasBusyMsisdns, 90);
        } catch (TimeoutException e) {
            logger.error("Waiting for too long, can't start making report.", e);
            logger.warn("Can't get all transactions' results, going to make report on existing data.");
        }

        //todo wait for all resps are sent or received
        stopMsisdnTimeoutService.set(1);

        logger.info("Start creating data report...");
        transactionReportService.makeFullDataReport();

        long[] infoAboutResults = new long[2];
        infoAboutResults[0] = transactionReportService.getSuccessfulTransactionsQuantity();
        infoAboutResults[1] = transactionReportService.getFailedTransactionsQuantity();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        smscProcessorService.stopSmsc();

        return infoAboutResults;

    }

    public void setSubscribeRequestMap(final Map<Integer, SubscribeRequestData> requests) {
        this.requests = requests;

    }

    SubscribeRequestData findRequestDataById(final int transactionId) {
        return requests.get(transactionId);

    }
}


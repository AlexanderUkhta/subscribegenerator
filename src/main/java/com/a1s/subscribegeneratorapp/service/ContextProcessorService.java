package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.a1s.ConfigurationConstantsAndMethods.ultimateWhile;

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

    public void process() {
        logger.info("Got context map full, going to start SMSC...");
        //todo all this away?
        CountDownLatch bindCompleted = new CountDownLatch(1);
        smscProcessorService.startSmsc(bindCompleted);
        try {
            bindCompleted.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Smpp server did not start at 20 seconds", e);
        }

        logger.info("*** Filling msisdn map... ***");
        requestQueueService.fillMsisdnMap();
        //todo if smppSession != null?
        logger.info("*** Start making requests from userdata... ***");
        requests.forEach((id, requestInfo) ->
                smscProcessorService.makeRequestFromDataAndSend(requestInfo));

        try {
            ultimateWhile(requestQueueService::hasAllMsisdnFree, 200);
        } catch (TimeoutException e) {
            logger.error("Waiting for too long, can't start making report", e);
            logger.warn("Can't get all transactions' results, going to make report on existing data");
        }

        logger.info("*** Start creating data report...");
        transactionReportService.makeFullDataReport();
    }

    public void setSubscribeRequestMap(final Map<Integer, SubscribeRequestData> requests) {
        this.requests = requests;
    }

    SubscribeRequestData findRequestDataById(final int transactionId) {
        return requests.get(transactionId);
    }
}


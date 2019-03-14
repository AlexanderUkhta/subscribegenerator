package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class ContextProcessorService {
    private static final Log logger = LogFactory.getLog(ContextProcessorService.class);
    private SmscProcessorService smscProcessorService;

    @Autowired
    public void setSmscProcessorService(SmscProcessorService smscProcessorService) {
        this.smscProcessorService = smscProcessorService;
    }

    public void process(final Map<Integer, SubscribeRequest> requests) {

        logger.info("Got context map full, going to start SMSC...");

        CountDownLatch bindCompleted = new CountDownLatch(1);
        smscProcessorService.startSmsc(bindCompleted);
        try {
            bindCompleted.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Smpp server did not start at 15 seconds", e);
        }

        logger.info("Start making requests from RequestData");
        requests.forEach((id, requestInfo) ->
                smscProcessorService.makeRequestFromDataAndSend(requestInfo));
    }
}


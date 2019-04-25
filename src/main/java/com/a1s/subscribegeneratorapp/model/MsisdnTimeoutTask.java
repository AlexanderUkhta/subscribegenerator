package com.a1s.subscribegeneratorapp.model;

import static com.a1s.ConfigurationConstantsAndMethods.*;

import com.a1s.subscribegeneratorapp.service.ContextProcessorService;
import com.a1s.subscribegeneratorapp.service.RequestQueueService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Thread, that is running permanently after msisdn pool is created.
 * Generates TimeoutException, when msisdn does not receive response for more than 30 seconds.
 * When exception is thrown, msisdn becomes NOT_BUSY.
 */
@Component
public class MsisdnTimeoutTask implements Runnable{
    private static final Log logger = LogFactory.getLog(MsisdnTimeoutTask.class);

    @Autowired
    private RequestQueueService requestQueueService;

    private int timeoutSecs = 60;

    @Override
    public void run() {
        Map<String, MsisdnStateData> map = requestQueueService.getMsisdnMap();

        while(stopMsisdnTimeoutService.get() == 0) {
            map.forEach((msisdn, msisdnStateData) -> {
                try {
                    if (msisdnStateData.getBusyState() == MSISDN_BUSY) {
                        if ((System.currentTimeMillis() - msisdnStateData.getStartTransactionTime()) / 1000 > timeoutSecs) {
                            throw new TimeoutException();
                        }
                    }
                } catch (TimeoutException e) {
                    logger.error("Got timeout while waiting a subscribe_response for msisdn: " + msisdn, e);
                    requestQueueService.processMsisdnTimeoutCase(msisdn);
                }
            });
        }

    }
}

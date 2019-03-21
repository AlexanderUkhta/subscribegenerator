package com.a1s.subscribegeneratorapp.service;

import static com.a1s.ConfigurationConstantsAndMethods.*;

import com.a1s.subscribegeneratorapp.model.MsisdnStateData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
public class MsisdnTimeoutService {
    private static final Log logger = LogFactory.getLog(MsisdnTimeoutService.class);

    @Autowired
    private RequestQueueService requestQueueService;

    private int timeoutSecs = 30;

    @Async
    public void run() {
        Map<String, MsisdnStateData> map = requestQueueService.getMsisdnMap();

        while(stopMsisdnTimeoutService.get() == 0) {
            map.forEach((msisdn, msisdnStateData) -> {
                try {
                    if (msisdnStateData.getBusyState() == MSISDN_BUSY) {
                        if ((System.currentTimeMillis() - msisdnStateData.getProcessingTime()) / 1000 > timeoutSecs) {
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

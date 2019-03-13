package com.a1s.subscribegeneratorapp.smsc;

import com.cloudhopper.smpp.pdu.SubmitSm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static com.a1s.ConfigurationConstantsAndMethods.ultimateWhile;

public class SarConcatenationTask implements Runnable{
    private String msisdn;
    private Integer partsQuantity;
    private ConcurrentHashMap<Integer, SubmitSm> receivedMessageparts;

    public SarConcatenationTask(final String msisdn, final Integer partsQuantity,
                                final Integer currentPartNumber, final SubmitSm firstArrivedPart) {
        this.msisdn = msisdn;
        this.partsQuantity = partsQuantity;

        receivedMessageparts.put(currentPartNumber, firstArrivedPart);
    }

    @Override
    public void run() {
        try {
            ultimateWhile(() -> (receivedMessageparts.size() != partsQuantity), 60);
        } catch (TimeoutException e) {
            // refer to ResultDocument with the cause of timeout, include in final document as failed transaction
        }


    }
}

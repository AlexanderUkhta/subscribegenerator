package com.a1s;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ConfigurationConstantsAndMethods {
    public static final Integer SMPP_SERVER_PORT = 8055;
    public static final String SYSTEM_ID = "smppLogin";

    public static final Integer MSISDN_NOT_BUSY = 0;
    public static final Integer MSISDN_BUSY = 1;

    public static AtomicInteger counterOfSentMessages = new AtomicInteger(0);
    public static AtomicInteger stopMsisdnTimeoutService = new AtomicInteger(0);

    public static final String ALL_MSISDN_BUSY_FOR_TOO_LONG = "All msisdns were busy for too long, transaction failed";
    public static final String GOT_RCVRBL_PDU_EXCEPTION = "Got recoverable pdu exception while sending request";
    public static final String GOT_UNRCVRBL_PDU_EXCEPTION = "Got recoverable pdu exception while sending request";
    public static final String GOT_SMPP_TIMEOUT_EXCEPTION = "Got smpp timeout exception while sending request";
    public static final String GOT_SMPP_CHANNEL_EXCEPTION = "Got smpp channel exception while sending request";
    public static final String GOT_INTERRUPTED_EXCEPTION = "Got interrupted exception while sending request";
    public static final String GOT_SMPP_INVALID_ARG_EXCEPTION = "Smth wrong while setting short message for deliver_sm";
    public static final String GOT_MSISDN_TIMEOUT_EXCEPTION = "Subscribe response has not been received at 30 secs for msisdn: ";

    public static void ultimateWhile(Supplier<Boolean> condition, Integer timeoutSeconds) throws TimeoutException {
        Long start = System.currentTimeMillis();
        Long end = 0L;

        while (condition.get() && ((end - start) / 1000) < timeoutSeconds) {
            end = System.currentTimeMillis();
        }

        if  (((end - start) / 1000) >= timeoutSeconds) {
            throw new TimeoutException();
        }
    }

}

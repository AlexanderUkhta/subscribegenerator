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

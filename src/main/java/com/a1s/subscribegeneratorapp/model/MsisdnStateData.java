package com.a1s.subscribegeneratorapp.model;

/**
 * Represents data for msisdns, that are used as source_address for output subscribe requests.
 * When msisdn is waiting for response, its state is BUSY.
 */
public class MsisdnStateData {
    private int currentTransactionId;
    private long startTransactionTime;
    private int busyState;


    public MsisdnStateData(int currentTransactionId, long startTransactionTime, int busyState) {
        this.currentTransactionId = currentTransactionId;
        this.startTransactionTime = startTransactionTime;
        this.busyState = busyState;
    }

    public int getCurrentTransactionId() {
        return currentTransactionId;
    }

    public long getStartTransactionTime() {
        return startTransactionTime;
    }

    public int getBusyState() {
        return busyState;
    }
}

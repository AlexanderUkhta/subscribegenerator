package com.a1s.subscribegeneratorapp.model;


public class MsisdnState {
    private int currentTransactionId;
    private long processingTime;
    private int busyState;


    public MsisdnState(int currentTransactionId, long processingTime, int busyState) {
        this.currentTransactionId = currentTransactionId;
        this.processingTime = processingTime;
        this.busyState = busyState;
    }

    public int getCurrentTransactionId() {
        return currentTransactionId;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public int getBusyState() {
        return busyState;
    }
}

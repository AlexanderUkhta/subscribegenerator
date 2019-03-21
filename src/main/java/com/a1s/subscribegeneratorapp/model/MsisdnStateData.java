package com.a1s.subscribegeneratorapp.model;


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

package com.a1s.subscribegeneratorapp.model;

public class SubmitSmData {
    private int partId;
    private byte[] shortMessage;

    public SubmitSmData (int partId, byte[] shortMessage) {
        this.partId = partId;
        this.shortMessage = shortMessage;
    }

    public int getPartId() {
        return partId;
    }

    public byte[] getShortMessage() {
        return shortMessage;
    }

}

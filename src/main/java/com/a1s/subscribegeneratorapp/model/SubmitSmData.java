package com.a1s.subscribegeneratorapp.model;

/**
 * Represents information, kept in a concatenation map, if input request needs to be concatenated.
 * Includes current message parts and the required quantity of these parts.
 */
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

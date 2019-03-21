package com.a1s.subscribegeneratorapp.model;

public class ReportData {
    private int id;
    private String actualResponse, errorMessage;
    private SubscribeRequestData subscribeRequestData;

    public ReportData(int id, String actualResponse, SubscribeRequestData subscribeRequestData) {
        this.id = id;
        this.errorMessage = null;
        this.actualResponse = actualResponse;
        this.subscribeRequestData = subscribeRequestData;

    }

    public ReportData(int id, String errorMessage) {
        this.id = id;
        this.errorMessage = errorMessage;
        this.actualResponse = null;
        this.subscribeRequestData = null;

    }

    public int getId() {
        return id;
    }

    public String getActualResponse() {
        return actualResponse;
    }

    public SubscribeRequestData getSubscribeRequestData() {
        return subscribeRequestData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}

package com.a1s.subscribegeneratorapp.model;

public class SubscribeRequest {
    private int id;
    private String shortNum, requestText, responseText;

    public SubscribeRequest(int id, String shortNum, String requestText, String responseText) {
        super();
        this.id = id;
        this.shortNum = shortNum;
        this.requestText = requestText;
        this.responseText = responseText;
    }

    public int getId() {
        return id;
    }

    public String getShortNum() {
        return shortNum;
    }

    public String getRequestText() {
        return requestText;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setShortNum(String shortNum) {
        this.shortNum = shortNum;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setRequestText(String requestText) {
        this.requestText = requestText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }
}

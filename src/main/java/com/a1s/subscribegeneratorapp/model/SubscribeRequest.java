package com.a1s.subscribegeneratorapp.model;

public class SubscribeRequest {
    private int id, psId;
    private String psIdName, shortNum, requestText, responseText;

    public SubscribeRequest(int id, int psId, String psIdName,
                            String shortNum, String requestText, String responseText) {
        this.id = id;
        this.psId = psId;
        this.shortNum = shortNum;
        this.requestText = requestText;
        this.responseText = responseText;
    }

    public int getId() {
        return id;
    }

    public int getPsId() {
        return psId;
    }

    public String getPsIdName() {
        return psIdName;
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

    public void setId(int id) {
        this.id = id;
    }

    public void setPsId(int psId) {
        this.psId = psId;
    }

    public void setPsIdName(String psIdName) {
        this.psIdName = psIdName;
    }

    public void setShortNum(String shortNum) {
        this.shortNum = shortNum;
    }

    public void setRequestText(String requestText) {
        this.requestText = requestText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }


}

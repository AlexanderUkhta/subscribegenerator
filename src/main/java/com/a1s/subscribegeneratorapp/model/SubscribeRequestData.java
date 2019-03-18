package com.a1s.subscribegeneratorapp.model;

public class SubscribeRequestData {
    private int id, psId;
    private String psIdName, shortNum, requestText, responseText;

    public SubscribeRequestData(int id, int psId, String psIdName,
                                String shortNum, String requestText, String responseText) {
        this.id = id;
        this.psId = psId;
        this.psIdName = psIdName;
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

}

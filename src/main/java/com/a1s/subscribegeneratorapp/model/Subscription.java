package com.a1s.subscribegeneratorapp.model;

public class Subscription {
    private int psid;
    private String shortNum, textRequest, welcomeNotification;

    public Subscription(int psid, String shortNum, String textRequest, String welcomeNotification) {
        this.psid = psid;
        this.shortNum = shortNum;
        this.textRequest = textRequest;
        this.welcomeNotification = welcomeNotification;
    }

    public int getPsid() {
        return psid;
    }

    public String getShortNum() {
        return shortNum;
    }

    public String getTextRequest() {
        return textRequest;
    }

    public String getWelcomeNotification() {
        return welcomeNotification;
    }
}

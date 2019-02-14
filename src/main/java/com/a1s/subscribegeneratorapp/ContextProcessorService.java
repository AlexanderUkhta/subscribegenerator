package com.a1s.subscribegeneratorapp;

import com.a1s.subscribegeneratorapp.model.SubscribeRequest;

import java.util.Map;

public class ContextProcessorService extends Thread {
    private Map<Integer, SubscribeRequest> contextMap;

    public ContextProcessorService(Map<Integer, SubscribeRequest> contextMap) {
        this.contextMap = contextMap;
    }

    @Override
    public void run() {
        while (!contextMap.isEmpty()) {
            for (SubscribeRequest dataForRequest : contextMap.values()) {
                SmppRequestService.makeRequestFromData(dataForRequest);
            }
        }
    }


}

package com.a1s.subscribegeneratorapp;

import com.a1s.subscribegeneratorapp.model.SubscribeRequest;

public class SmppRequestService {

    public static void makeRequestFromData(SubscribeRequest dataForRequest) {
        //while(queue.isBusy) --> DeliverSm.sendPdu() --> to Query
        //mapWithMsisdn.put(msisdn, dataForRequest.getResponse)
        //DeliverSm (getRequest, msisdn)
    }
}

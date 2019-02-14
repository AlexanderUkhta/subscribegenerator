package com.a1s.subscribegeneratorapp.controller;

import com.a1s.subscribegeneratorapp.dao.SubscribeRequestDao;
import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class MainController {

    @Autowired
    private SubscribeRequestDao subscribeRequestDao;

    @RequestMapping("/showRequests")
    String showRequests() {
        List<SubscribeRequest> list = new ArrayList<>(subscribeRequestDao.getAllAsTreeMap().values());
        StringBuffer response = new StringBuffer();
        for (SubscribeRequest request : list)
            response.append(request.getId() + "  " + request.getShortNum() + "  " +
                    request.getRequestText() + "  " + request.getResponseText() + "\n");

        return response.toString();
    }
}

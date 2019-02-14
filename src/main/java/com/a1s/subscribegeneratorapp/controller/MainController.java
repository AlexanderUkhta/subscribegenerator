package com.a1s.subscribegeneratorapp.controller;

import com.a1s.subscribegeneratorapp.ContextProcessorService;
import com.a1s.subscribegeneratorapp.dao.SubscribeRequestDao;
import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
public class MainController {

    @Autowired
    private SubscribeRequestDao subscribeRequestDao;

    @RequestMapping("/showRequests")
    String showRequests() {
        //Запуск отправки реквестов в очередь на SMSC
        Long timerStart = System.currentTimeMillis();
        Map<Integer, SubscribeRequest> map = subscribeRequestDao.getAllAsTreeMap();
        new ContextProcessorService(map).run();
        int timerTotal = (int) (System.currentTimeMillis() - timerStart) / 1000;

        //Черновой вывод ответа на GET-запрос
        String response = "Map with requestContext created in" + timerTotal + "seconds";

        return response;
    }
}

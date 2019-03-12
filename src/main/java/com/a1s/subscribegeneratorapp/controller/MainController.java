package com.a1s.subscribegeneratorapp.controller;

import com.a1s.subscribegeneratorapp.service.ContextProcessorService;
import com.a1s.subscribegeneratorapp.dao.SubscribeRequestDao;
import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;


@Controller
public class MainController {

    @Autowired
    private SubscribeRequestDao subscribeRequestDao;
    @Autowired
    private ContextProcessorService contextProcessorService;

    @RequestMapping(value = "/processData", method = RequestMethod.GET)
    public String showRequests(ModelMap model) {
        Long timerStart = System.currentTimeMillis();
        Map<Integer, SubscribeRequest> readyForSmppProcessMap = subscribeRequestDao.findAll();
        contextProcessorService.process(readyForSmppProcessMap);

        model.addAttribute("message", "Got SMPP templates processing");
        return "process";
    }
}

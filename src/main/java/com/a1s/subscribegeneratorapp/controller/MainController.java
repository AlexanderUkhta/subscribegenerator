package com.a1s.subscribegeneratorapp.controller;

import com.a1s.file.ExcelReadService;
import com.a1s.subscribegeneratorapp.service.ContextProcessorService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class MainController {

    @Autowired
    private ExcelReadService excelReadService;
    @Autowired
    private ContextProcessorService contextProcessorService;

    @RequestMapping(value = "/processData", method = RequestMethod.GET)
    public String showRequests(ModelMap model) {

        contextProcessorService.setSubscribeRequestMap(excelReadService.findAll());
        contextProcessorService.process();

        model.addAttribute("message", "Got SMPP templates processing");
        return "process";

    }
}

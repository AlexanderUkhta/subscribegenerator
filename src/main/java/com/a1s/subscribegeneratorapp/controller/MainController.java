package com.a1s.subscribegeneratorapp.controller;

import com.a1s.subscribegeneratorapp.excel.ReadFromExcel;
import com.a1s.subscribegeneratorapp.model.ReportData;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import com.a1s.subscribegeneratorapp.service.ExcelCreateService;
import com.a1s.subscribegeneratorapp.service.ExcelReadService;
import com.a1s.subscribegeneratorapp.service.ContextProcessorService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class MainController {

    @Autowired
    private ExcelReadService excelReadService;
    @Autowired
    private ExcelCreateService excelCreateService;
    @Autowired
    private ContextProcessorService contextProcessorService;
    @Autowired
    private ReadFromExcel readFromExcel;

    @RequestMapping(value = "/processData", method = RequestMethod.GET)
    public String processRequests(ModelMap model) {

        contextProcessorService.setSubscribeRequestMap(excelReadService.findAll());
        contextProcessorService.process();

        model.addAttribute("message", "Got SMPP templates processing");
        return "process";
    }

    @RequestMapping(value = "/testExcelReadService", method = RequestMethod.GET)
    public String showContext(ModelMap model) {

        Map<Integer, SubscribeRequestData> startMap = excelReadService.findAll();
        Map<Integer, ReportData> finishMap = new ConcurrentHashMap<>();
        startMap.forEach((id, subscribeRequestData) -> {
            if (id % 2 == 0) {
                finishMap.put(id, new ReportData(id, subscribeRequestData.getResponseText(), subscribeRequestData));
            } else finishMap.put(id, new ReportData(id, "oIIIibo4ka"));
        });

        excelCreateService.makeFullDataReport(new TreeMap<>(finishMap));

        return "test";
    }
}

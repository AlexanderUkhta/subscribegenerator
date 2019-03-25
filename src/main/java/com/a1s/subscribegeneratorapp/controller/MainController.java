package com.a1s.subscribegeneratorapp.controller;

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

/**
 * Represents the main controller of the app.
 * Here http-requests are mapped to /processData or /testExcel methods, then are processed.
 */
@Controller
public class MainController {

    @Autowired
    private ExcelReadService excelReadService;
    @Autowired
    private ExcelCreateService excelCreateService;
    @Autowired
    private ContextProcessorService contextProcessorService;

    /**
     * Processes the /processData request, then runs subscribe generator process.
     * Final result is mapped to 'process.jsp' view.
     * @param model
     * @return
     */
    @RequestMapping(value = "/processData", method = RequestMethod.GET)
    public String processRequests(ModelMap model) {

        contextProcessorService.setSubscribeRequestMap(excelReadService.findAll());
        contextProcessorService.process();

        model.addAttribute("message", "Got SMPP templates processing");
        return "process";
    }

    /**
     * Used for testing. Processes the /testExcel request, then gets request_map by ExcelReadService,
     * creates report by ExcelCreateService.
     * Final result is mapped to 'test.jsp' view.
     * @param model
     * @return
     */
    @RequestMapping(value = "/testExcel", method = RequestMethod.GET)
    public String showContext(ModelMap model) {
        Map<Integer, SubscribeRequestData> startMap = excelReadService.findAll();
        Map<Integer, ReportData> finishMap = new ConcurrentHashMap<>();

        startMap.forEach((id, subscribeRequestData) -> {
            if (id % 3 == 0) {
                finishMap.put(id, new ReportData(id, "test_error"));
            } else if (id % 4 == 0) {
                finishMap.put(id, new ReportData(id, subscribeRequestData.getResponseText() + "***", subscribeRequestData));
            } else {
                finishMap.put(id, new ReportData(id, subscribeRequestData.getResponseText(), subscribeRequestData));
            }
        });

        int processedAtAll = excelCreateService.makeFullDataReport(new TreeMap<>(finishMap));

        model.addAttribute("message", processedAtAll);
        return "test";
    }
}

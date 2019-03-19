package com.a1s.subscribegeneratorapp.controller;

import com.a1s.subscribegeneratorapp.file.File;
import com.a1s.subscribegeneratorapp.service.ExcelReadService;
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
    @Autowired
    private File file;

    @RequestMapping(value = "/processData", method = RequestMethod.GET)
    public String processRequests(ModelMap model) {

        contextProcessorService.setSubscribeRequestMap(excelReadService.findAll());
        contextProcessorService.process();

        model.addAttribute("message", "Got SMPP templates processing");
        return "process";
    }

    @RequestMapping(value = "/testExcelReadService", method = RequestMethod.GET)
    public String showContext(ModelMap model) {

        int lastRowNum = 0;
        try {
           lastRowNum = file.getSheet("Подключение").getLastRowNum();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        model.addAttribute("message", lastRowNum);
        return "test";
    }
}

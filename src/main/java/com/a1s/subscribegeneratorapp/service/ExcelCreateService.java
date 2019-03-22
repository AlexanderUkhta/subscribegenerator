package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.excel.WriteToExcel;
import com.a1s.subscribegeneratorapp.model.ReportData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExcelCreateService {
    private static final Log logger = LogFactory.getLog(ExcelCreateService.class);

    @Autowired
    private WriteToExcel writeToExcel;

    public int makeFullDataReport(final Map<Integer, ReportData> reportDataTreeMap) {
        writeToExcel.createFirstRow();
        int counter = writeToExcel.writeMap(reportDataTreeMap);
        logger.info("Processing report data");

        return counter;
    }

    /*private void makeErrorReport(Object... objects) {

    }

    private void makeUnexpectedResponseReport(Object... objects) {

    }

    private void makeSuccessReport(Object... objects) {

    }*/
}

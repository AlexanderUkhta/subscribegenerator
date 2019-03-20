package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.excel.WriteToExcel;
import com.a1s.subscribegeneratorapp.model.ReportData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExcelCreateService {
    @Autowired
    private WriteToExcel writeToExcel;

    public void makeFullDataReport(final Map<Integer, ReportData> reportDataTreeMap) {
        reportDataTreeMap.forEach((transactionId, reportData) ->
            writeToExcel.createRow(transactionId, reportData));
    }

    /*private void makeErrorReport(Object... objects) {

    }

    private void makeUnexpectedResponseReport(Object... objects) {

    }

    private void makeSuccessReport(Object... objects) {

    }*/
}

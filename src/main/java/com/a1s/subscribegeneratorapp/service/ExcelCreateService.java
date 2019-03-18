package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.ReportData;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExcelCreateService {

    void makeFullDataReport(final Map<Integer, ReportData> reportDataTreeMap) {
        reportDataTreeMap.forEach((transactionId, reportData) -> {
            if (reportData.getErrorMessage() != null)
                makeErrorReport();
            else if (!reportData.getActualResponse().equals(reportData.getSubscribeRequestData().getResponseText()))
                makeUnexpectedResponseReport();
            else
                makeSuccessReport();
        });
    }

    private void makeErrorReport(Object... objects) {

    }

    private void makeUnexpectedResponseReport(Object... objects) {

    }

    private void makeSuccessReport(Object... objects) {

    }
}

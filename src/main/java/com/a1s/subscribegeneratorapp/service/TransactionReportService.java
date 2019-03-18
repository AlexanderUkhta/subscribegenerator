package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.ReportData;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransactionReportService {

    @Autowired
    private RequestQueueService requestQueueService;
    @Autowired
    private ContextProcessorService contextProcessorService;
    @Autowired
    private ExcelCreateService excelCreateService;

    private Map<Integer, ReportData> reportDataMap = new ConcurrentHashMap<>();

    void processOneInfoReport(final byte[] receivedShortMessage, final String msisdn) {

        int transactionId = requestQueueService.getTransactionIdByMsisdn(msisdn);
        String actualResponse = new String(receivedShortMessage);
        SubscribeRequestData requestThatMatchesCurrentResponse = contextProcessorService.findRequestDataById(transactionId);

        reportDataMap.put(transactionId, new ReportData(transactionId, actualResponse, requestThatMatchesCurrentResponse));

        requestQueueService.makeMsisdnNotBusy(msisdn);

    }

    void processOneFailureReport(final int transactionId, final String errorMessage) {
        reportDataMap.put(transactionId, new ReportData(transactionId, errorMessage));
    }

    void makeFullDataReport() {
        TreeMap<Integer, ReportData> reportDataTreeMap = new TreeMap<>(reportDataMap);
        excelCreateService.makeFullDataReport(reportDataTreeMap);

    }
}

package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.ReportData;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransactionReportService {
    private static final Log logger = LogFactory.getLog(TransactionReportService.class);

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

        logger.info("Putting in Map one normal report for operation with id = " + transactionId);
        reportDataMap.put(transactionId, new ReportData(transactionId, actualResponse, requestThatMatchesCurrentResponse));

        requestQueueService.makeMsisdnNotBusy(msisdn);

    }

    void processOneFailureReport(final int transactionId, final String errorMessage) {
        logger.info("Putting in Map one error report for operation with id = " + transactionId);
        reportDataMap.put(transactionId, new ReportData(transactionId, errorMessage));

    }

    void makeFullDataReport() {
        TreeMap<Integer, ReportData> reportDataTreeMap = new TreeMap<>(reportDataMap);
        logger.info("ExelCreateService is pushed to make full data report...");
        excelCreateService.makeFullDataReport(reportDataTreeMap);

    }
}

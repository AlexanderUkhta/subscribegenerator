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

/**
 * Service, that collects success and error reports of app.
 * Reports are put into reportDataMap.
 */
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

    /**
     * Puts one normal report data in a map, makes the proper msisdn NOT_BUSY.
     * @param receivedShortMessage currently received message, that needs to be reported
     * @param msisdn current msisdn, which got subscribe response and now needs to be NOT_BUSY
     */
    void processOneInfoReport(final byte[] receivedShortMessage, final String msisdn) {

        int transactionId = requestQueueService.getTransactionIdByMsisdn(msisdn);
        String actualResponse = new String(receivedShortMessage);
        SubscribeRequestData requestThatMatchesCurrentResponse = contextProcessorService.findRequestDataById(transactionId);

        logger.info("Putting in Map one normal report for operation with id = " + transactionId + ", msisdn = " + msisdn);
        reportDataMap.put(transactionId, new ReportData(transactionId, actualResponse, requestThatMatchesCurrentResponse));

        requestQueueService.makeMsisdnNotBusy(msisdn);

    }

    /**
     * Puts one error report data in a map, makes the proper msisdn NOT_BUSY.
     * @param transactionId id of current failed operation
     * @param errorMessage contains error description
     */
    public void processOneFailureReport(final int transactionId, final String errorMessage) {
        logger.info("Putting in Map one error report for operation with id = " + transactionId);
        reportDataMap.put(transactionId, new ReportData(transactionId, errorMessage));

    }

    /**
     * Passes reportDataMap to excelCreateService, where full report will be processed.
     */
    void makeFullDataReport() {
        TreeMap<Integer, ReportData> reportDataTreeMap = new TreeMap<>(reportDataMap);
        logger.info("ExelCreateService is pushed to make full data report...");
        excelCreateService.makeFullDataReport(reportDataTreeMap);

    }

    long getSuccessfulTransactionsQuantity() {
        return reportDataMap.entrySet().stream()
                .filter(entry -> (entry.getValue().getErrorMessage() == null))
                .map(Map.Entry::getValue)
                .count();
    }

    long getFailedTransactionsQuantity() {
        return reportDataMap.entrySet().stream()
                .filter(entry -> (entry.getValue().getErrorMessage() != null))
                .map(Map.Entry::getValue)
                .count();
    }
}

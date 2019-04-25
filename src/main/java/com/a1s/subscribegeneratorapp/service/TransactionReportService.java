package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.model.Charsets;
import com.a1s.subscribegeneratorapp.model.ReportData;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import com.cloudhopper.commons.charset.CharsetUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.a1s.ConfigurationConstantsAndMethods.GOT_MSISDN_TIMEOUT_EXCEPTION;

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
    @Autowired
    private SOAPClientService soapClientService;

    private Map<Integer, ReportData> reportDataMap = new ConcurrentHashMap<>();

    /**
     * Puts one normal report data in a map, makes the proper msisdn NOT_BUSY.
     * @param receivedShortMessage currently received message, that needs to be reported
     * @param msisdn current msisdn, which got subscribe response and now needs to be NOT_BUSY
     */
    void processOneInfoReport(final byte[] receivedShortMessage, final String msisdn, final byte dcs) {

        int transactionId = requestQueueService.getTransactionIdByMsisdn(msisdn);
        String actualResponse = CharsetUtil.decode(receivedShortMessage, Charsets.getCharsetName(dcs));
        SubscribeRequestData requestThatMatchesCurrentResponse = contextProcessorService.findRequestDataById(transactionId);

        logger.info("Putting in Map one NORMAL report for operation with id = " + transactionId + ", msisdn = " + msisdn);
        reportDataMap.put(transactionId, new ReportData(transactionId, actualResponse, requestThatMatchesCurrentResponse));
        try {
            logger.info("Waiting 3 secs to make SOAP 'GetAbonentSubscriptions' request for msisdn:" + msisdn);
            Thread.sleep(500);
        } catch (InterruptedException e) {
            logger.error("An error occurred while making a SOAP request for msisdn:" + msisdn, e);
        }

        String soapResponse = soapClientService.checkSubscriptionsForMsisdn(msisdn);
        String expectedPsIdName = ((Integer) requestThatMatchesCurrentResponse.getPsId()).toString();

        if (soapResponse.contains(expectedPsIdName))
            logger.info("Abonent " + msisdn + " has been subscribed on the required " + expectedPsIdName + "!");

        requestQueueService.makeMsisdnNotBusy(msisdn);

    }

    /**
     * Puts one msisdn timeout report data in a map.
     * Is invoked when there is no welcome notification from SDP for more, than msisdn_timeout value.
     * @param msisdn current msisdn, which got subscribe response and now needs to be NOT_BUSY
     */
    void processMsisdnTimeoutReport(final String msisdn) {

        int transactionId = requestQueueService.getTransactionIdByMsisdn(msisdn);
        SubscribeRequestData requestThatMatchesCurrentResponse = contextProcessorService.findRequestDataById(transactionId);

        logger.info("Putting in Map one MSISDN_TIMEOUT report for operation with id = "
                + transactionId + ", msisdn = " + msisdn);
        reportDataMap.put(transactionId, new ReportData(transactionId, requestThatMatchesCurrentResponse,
                GOT_MSISDN_TIMEOUT_EXCEPTION + msisdn));

        requestQueueService.makeMsisdnNotBusy(msisdn);
    }

    /**
     * Puts one error report data in a map, makes the proper msisdn NOT_BUSY.
     * @param transactionId id of current failed operation
     * @param errorMessage contains error description
     */
    void processOneFailureReport(final int transactionId, final String errorMessage) {
        logger.info("Putting in Map one ERROR report for operation with id = " + transactionId);
        reportDataMap.put(transactionId, new ReportData(transactionId, errorMessage));

    }

    /**
     * Passes reportDataMap to excelCreateService, where full report will be processed.
     */
    void startMakingFullDataReport() {
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

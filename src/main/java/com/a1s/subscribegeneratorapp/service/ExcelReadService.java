package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.excel.ReadFromExcel;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExcelReadService {
    private static final Log logger = LogFactory.getLog(ExcelReadService.class);

    @Autowired
    private ReadFromExcel readFromExcel;
    @Autowired
    private TransactionReportService transactionReportService;

    /**
     * Fills map with objects consisting of id (row number), ps id, short number, request text, response text (welcome notification).
     * @return
     */
    public Map<Integer, SubscribeRequestData> findAll() {
        logger.info("Started reading context from excel document.");

        Map<Integer, SubscribeRequestData> requestDataMap = new ConcurrentHashMap<>();
        for(int i = 1; i < readFromExcel.getLastRowId(readFromExcel.getSheet("Подключение")); i++) { //todo maybe <= ?

            int psid = Integer.parseInt(readFromExcel.getCellValue(i, readFromExcel.getCellId("ps id",
                    readFromExcel.getSheet("Подключение")), readFromExcel.getSheet("Подключение")));

            if(psid == 0) {
                logger.warn("in row " + (i + 1) + ", check the required parameter ps id");
                transactionReportService.processOneFailureReport(i, "Check the required parameters in row " + (i + 1));

            } else {
                String shortNum = readFromExcel.getCellValue(i, readFromExcel.getCellId("short num",
                        readFromExcel.getSheet("Подключение")), readFromExcel.getSheet("Подключение"));

                String textRequest = readFromExcel.getCellValue(i, readFromExcel.getCellId("text request",
                        readFromExcel.getSheet("Подключение")), readFromExcel.getSheet("Подключение"));

                String welcomeNotification = readFromExcel.getCellValue(findRow(psid), readFromExcel.getCellId("welcome notification",
                        readFromExcel.getSheet("Рассылки")), readFromExcel.getSheet("Рассылки"));

                String subscriptionName = readFromExcel.getCellValue(findRow(psid), readFromExcel.getCellId("subscribe name",
                        readFromExcel.getSheet("Рассылки")), readFromExcel.getSheet("Рассылки"));

                if(isInvalid(shortNum, textRequest, welcomeNotification)) {
                    logger.warn("in row " + (i + 1) + ", check the required parameters: ps id, Короткий номер, Текст сообщения, Уведомление при подключении");
                    transactionReportService.processOneFailureReport(i, "Check the required parameters in row " + (i + 1));

                } else {
                    requestDataMap.put(i, new SubscribeRequestData(i, psid, subscriptionName, shortNum, textRequest, welcomeNotification));
                }

            }
        }

        logger.info("Finished reading context from excel document.");
        return requestDataMap;
    }

    /**
     * Get row number from subscription page
     * @param psId
     * @return
     */
    private int findRow(final int psId) {
        int row = 0;

        for (int i = 1; i < readFromExcel.getLastRowId(readFromExcel.getSheet("Рассылки")); i++) {
            int id = Integer.valueOf(readFromExcel.getCellValue(i, readFromExcel.getCellId("ps id",
                    readFromExcel.getSheet("Рассылки")), readFromExcel.getSheet("Рассылки")));

            if(psId == id) {
                row = i;
                break;
            }
        }
        return row;
    }

    private boolean isInvalid(String shortNum, String textRequest, String welcomeNotification) {
//        if(shortNum.equals("0") && textRequest.equals("0") && welcomeNotification.equals("0")) {
//            return true;
//        } else {
//            return false;
//        }
        return shortNum.equals("0") && textRequest.equals("0") && welcomeNotification.equals("0");
    }
}
//    public int makeFullDataReport(final Map<Integer, ReportData> reportDataTreeMap) {
//        int counter = 0;
//        writeToExcel.createFirstRow();
//        reportDataTreeMap.forEach((transactionId, reportData) -> {
//            writeToExcel.createRow(transactionId, reportData);
//            logger.info("Processing report data: " + counter);
//        });
//
//        return counter;
//    }

package com.a1s.subscribegeneratorapp.service;

import com.a1s.ConfigurationConstantsAndMethods;
import com.a1s.subscribegeneratorapp.excel.ReadFromExcel;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service, that reads required columns from initial excel document.
 * Makes subscribeRequestData from read data. This subscribeRequestData is then used to make proper subscribe requests.
 */
@Service
public class ExcelReadService {
    private static final Log logger = LogFactory.getLog(ExcelReadService.class);

    @Autowired
    private ReadFromExcel readFromExcel;
    @Autowired
    private TransactionReportService transactionReportService;

    /**
     * Gets required text from excel document and fills requestDataMap with objects consisting of
     * id(row_number), ps_id, ps_id_name, short_num, request_text, response_text(welcome_notification).
     * @return map with subscribeRequestData
     */
    public Map<Integer, SubscribeRequestData> findAll() {
        logger.info("Started reading context from excel document.");

        Map<Integer, SubscribeRequestData> requestDataMap = new ConcurrentHashMap<>();
        ConfigurationConstantsAndMethods.rowQuantityInExcel.set(readFromExcel.
                getLastRowId(readFromExcel.getSheet("Подключение")));

        for(int i = 1; i <= ConfigurationConstantsAndMethods.rowQuantityInExcel.get(); i++) {
            int psid = Integer.parseInt(readFromExcel.getCellValue(i, readFromExcel.getCellId("ps id",
                    readFromExcel.getSheet("Подключение")), readFromExcel.getSheet("Подключение")));

            if(psid == 0) {
                logger.warn("In row " + (i + 1) + ", check the required parameter ps_id");
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

                if(areInvalid(shortNum, textRequest, welcomeNotification)) {
                    logger.warn("In row " + (i + 1) + ", check the required parameters: ps_id, short_num, request_text, welcome_notification");
                    transactionReportService.processOneFailureReport(i, "Check the required parameters in row " + (i + 1));

                } else {
                    requestDataMap.put(i, new SubscribeRequestData(i, psid, subscriptionName, shortNum, textRequest, welcomeNotification));
                    logger.info("Successfully processed row " + (i + 1) + " (request number " + i + ")");
                }
            }

        }

        logger.info("Finished reading context from excel document.");
        return requestDataMap;
    }

    /**
     * Get row number from subscription page by psId.
     * @param psId
     * @return the number of row with given psId
     */
    private int findRow(final int psId) {
        int row = 0;

        for (int i = 1; i <= readFromExcel.getLastRowId(readFromExcel.getSheet("Рассылки")); i++) {
            int id = Integer.valueOf(readFromExcel.getCellValue(i, readFromExcel.getCellId("ps id",
                    readFromExcel.getSheet("Рассылки")), readFromExcel.getSheet("Рассылки")));

            if(psId == id) {
                row = i;
                break;
            }
        }
        return row;
    }

    /**
     * Returns true if one of three input parameters is invalid(set to '0').
     * @param shortNum short_num of current row
     * @param textRequest text_request of current row
     * @param welcomeNotification welcome_notification from current row
     * @return true or false
     */
    private boolean areInvalid(String shortNum, String textRequest, String welcomeNotification) {
        return shortNum.equals("0") || textRequest.equals("0") || welcomeNotification.equals("0");
    }
}

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

    /**
     * Fills map with objects consisting of id (row number), ps id, short number, request text, response text (welcome notification).
     * @return
     */
    public Map<Integer, SubscribeRequestData> findAll() {
        logger.info("Started reading context from excel document.");

        Map<Integer, SubscribeRequestData> requestDataMap = new ConcurrentHashMap<>();
        for(int i = 1; i < readFromExcel.getLastRowId(readFromExcel.getSheet("Подключение")); i++) {

            int psid = Integer.parseInt(readFromExcel.getCellValue(i, readFromExcel.getCellId("ps id",
                    readFromExcel.getSheet("Подключение")), readFromExcel.getSheet("Подключение")));

            if (psid == 0) {
                logger.warn("Empty ps id in row " + i + 1);

            } else {
                String shortNum = readFromExcel.getCellValue(i, readFromExcel.getCellId("Короткий номер",
                        readFromExcel.getSheet("Подключение")), readFromExcel.getSheet("Подключение"));

                String textRequest = readFromExcel.getCellValue(i, readFromExcel.getCellId("Текст сообщения",
                        readFromExcel.getSheet("Подключение")), readFromExcel.getSheet("Подключение"));

                String welcomeNotification = readFromExcel.getCellValue(findRow(psid), readFromExcel.getCellId("Уведомление при подключении",
                                readFromExcel.getSheet("Рассылки")), readFromExcel.getSheet("Рассылки"));

                String subscriptionName = readFromExcel.getCellValue(findRow(psid), readFromExcel.getCellId("Название рассылки",
                        readFromExcel.getSheet("Рассылки")), readFromExcel.getSheet("Рассылки"));

                requestDataMap.put(i, new SubscribeRequestData(i, psid, subscriptionName, shortNum, textRequest, welcomeNotification));
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
}

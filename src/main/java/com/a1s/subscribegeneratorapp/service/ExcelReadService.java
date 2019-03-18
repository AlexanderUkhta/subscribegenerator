package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.file.Read;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Service
public class ExcelReadService {
    private static final Log logger = LogFactory.getLog(ExcelReadService.class);

    @Autowired
    private Read read;

    /**
     * Fills map with objects consisting of id (row number), ps id, short number, request text, response text (welcome notification).
     * @return
     */
    public Map<Integer, SubscribeRequestData> findAll() {
        Map<Integer, SubscribeRequestData> treeMap = new TreeMap<>();
        for(int i = 1; i < read.getLastRowId(read.getSheet("Подключение")); i++) {
            int psid = Integer.parseInt(read.getCellValue(i, read.getCellId("ps id", read.getSheet("Подключение")),
                    read.getSheet("Подключение")));
            if (psid == 0) {
                logger.warn("Empty ps id in row " + i + 1);
            } else {
                String shortNum = read.getCellValue(i, read.getCellId("Короткий номер", read.getSheet("Подключение")),
                        read.getSheet("Подключение"));
                String textRequest = read.getCellValue(i, read.getCellId("Текст сообщения", read.getSheet("Подключение")),
                        read.getSheet("Подключение"));
                String welcomeNotification = read.getCellValue(findRow(psid), read.getCellId("Уведомление при подключении", read.getSheet("Рассылки")),
                        read.getSheet("Рассылки"));
                String subscriptionName = read.getCellValue(findRow(psid), read.getCellId("Название рассылки", read.getSheet("Рассылки")),
                        read.getSheet("Рассылки"));
                treeMap.put(i, new SubscribeRequestData(i, psid, subscriptionName, shortNum, textRequest, welcomeNotification));
            }
        }
        return treeMap;
    }

    /**
     * Get row number from subscription page
     * @param psId
     * @return
     */
    private int findRow(final int psId) {
        int row = 0;
        for (int i = 1; i < read.getLastRowId(read.getSheet("Рассылки")); i++) {
            int id = Integer.valueOf(read.getCellValue(i, read.getCellId("ps id", read.getSheet("Рассылки")),
                    read.getSheet("Рассылки")));
            if(psId == id) {
                row = i;
                break;
            }
        }
        return row;
    }
}

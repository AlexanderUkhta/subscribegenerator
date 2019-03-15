package com.a1s.file;

import com.a1s.subscribegeneratorapp.file.File;
import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
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
    private File file = new File();

    /**
     * Fills map with objects consisting of id (row number), ps id, short number, request text, response text (welcome notification).
     * @return
     */
    public Map<Integer, SubscribeRequest> findAll() {
        Map<Integer, SubscribeRequest> treeMap = new TreeMap<>();
        for(int i = 1; i < file.getLastRowId(file.getSheet("Подключение")); i++) {
            int psid = Integer.parseInt(file.getCellValue(i, file.getCellId("ps id", file.getSheet("Подключение")),
                    file.getSheet("Подключение")));
            if (psid == 0) {
                logger.warn("Empty ps id in row " + i + 1);
            } else {
                String shortNum = file.getCellValue(i, file.getCellId("Короткий номер", file.getSheet("Подключение")),
                        file.getSheet("Подключение"));
                String textRequest = file.getCellValue(i, file.getCellId("Текст сообщения", file.getSheet("Подключение")),
                        file.getSheet("Подключение"));
                String welcomeNotification = file.getCellValue(findRow(psid), file.getCellId("Уведомление при подключении", file.getSheet("Рассылки")),
                        file.getSheet("Рассылки"));
                String subscriptionName = file.getCellValue(findRow(psid), file.getCellId("Название рассылки", file.getSheet("Рассылки")),
                        file.getSheet("Рассылки"));
                treeMap.put(i, new SubscribeRequest(i, psid, subscriptionName, shortNum, textRequest, welcomeNotification));
            }
        }
        return treeMap;
    }

    /**
     * Get row number from subscription page
     * @param psId
     * @return
     */
    private int findRow(int psId) {
        int row = 0;
        for (int i = 1; i < file.getLastRowId(file.getSheet("Рассылки")); i++) {
            int id = Integer.valueOf(file.getCellValue(i, file.getCellId("ps id", file.getSheet("Рассылки")),
                    file.getSheet("Рассылки")));
            if(psId == id) {
                row = i;
                break;
            }
        }
        return row;
    }

    public void sout() {
        for(Map.Entry e : findAll().entrySet()) {
            System.out.println(e.getValue());
        }
    }
}

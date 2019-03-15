package com.a1s.subscribegeneratorapp.service;

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
    private int psIdColumn;
    private int shortNumColumn;
    private int textRequestColumn;
    private int welcomeNotificationColumn;

    @Autowired
    private File file;

    public ExcelReadService() {
        //getColumnsId();
    }

    /**
     * Получение id столбцов с необходимыми данными для теста
     */
    private void getColumnsId() {
        int row = 0;
        for(int i = 0; i < file.getLastCellId(file.getSheet("Подключение")); i++) {
            String text = file.getCellValue(row, i, file.getSheet("Подключение"));
            if(text.equals("ps id")) {
                psIdColumn = i;
            } else if(text.equals("Короткий номер")) {
                shortNumColumn = i;
            } else if(text.equals("Текст сообщения")) {
                textRequestColumn = i;
            }
        }
        for (int i = 0; i < file.getLastCellId(file.getSheet("Рассылки")); i++) {
            String text = file.getCellValue(row, i, file.getSheet("Рассылки"));
            if(text.equals("Уведомление при подключении")) {
                welcomeNotificationColumn = i;
                break;
            }
        }
    }

    //todo: тут получаем ConcurrentHashMap<Id, SubscribeRequest(id, psId, psIdName, shortNum, request, response)>
    public Map<Integer, SubscribeRequest> findAll() {
        Map<Integer, SubscribeRequest> treeMap = new TreeMap<>();
        for(int i = 1; i < file.getLastRowId(file.getSheet("Подключение")); i++) {
            int psid = Integer.parseInt(file.getCellValue(i, psIdColumn, file.getSheet("Подключение")));
            String shortNum = file.getCellValue(i, shortNumColumn, file.getSheet("Подключение"));
            String textRequest = file.getCellValue(i, textRequestColumn, file.getSheet("Подключение"));
            String welcomeNotification = file.getCellValue(i, welcomeNotificationColumn, file.getSheet("Рассылки"));
            if (psid == 0) {
                logger.warn("Empty ps id in row " + i + 1);
            } else {
                treeMap.put(i, new SubscribeRequest(i, psid, String.valueOf(psid), shortNum, textRequest, welcomeNotification));
            }
        }
        return treeMap;
    }

    public void sout() {
        for(Map.Entry e : findAll().entrySet()) {
            System.out.println(e.getValue());
        }
    }
}

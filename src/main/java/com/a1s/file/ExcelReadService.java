package com.a1s.file;

import com.a1s.subscribegeneratorapp.model.Subscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        getColumnsId();
    }
    //Подключение Рассылки

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
    private List<Subscription> read() {
        List<Subscription> subs = new ArrayList<>();
        for(int i = 1; i < file.getLastRowId(file.getSheet("Подключение")); i++) {
            int psid = Integer.parseInt(file.getCellValue(i, psIdColumn, file.getSheet("Подключение")));
            String shortNum = file.getCellValue(i, shortNumColumn, file.getSheet("Подключение"));
            String textRequest = file.getCellValue(i, textRequestColumn, file.getSheet("Подключение"));
            String welcomeNotification = file.getCellValue(i, welcomeNotificationColumn, file.getSheet("Рассылки"));
            if (psid == 0) {
                logger.warn("Empty ps id in row " + i + 1);
            } else {
                subs.add(new Subscription(psid, shortNum, textRequest, welcomeNotification));
            }
        }
        return subs;
    }

    public void sout() {
        for(Subscription sub : read()) {
            System.out.println(sub.getPsid() + " " + sub.getShortNum() + " " + sub.getTextRequest() + " " + sub.getWelcomeNotification());
        }
    }
}

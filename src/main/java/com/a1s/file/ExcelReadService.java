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

    private XSSFSheet connectionSheet;
    private XSSFSheet subscriptionSheet;

    private int lastSubscriptionColumn; //все эти 4 поля можно забрать через инъекцию File
    private int lastConnectionColumn;
    private int lastSubscriptionRow;
    private int lastConnectionRow;

    @Autowired
    private File file;

    public ExcelReadService() {
        //todo: убрать все, кроме строки 41, в методы инъектировать file.get() методы
        file = new File();
        connectionSheet = file.getConnectionSheet();
        subscriptionSheet = file.getSubscriptionSheet();
        lastConnectionColumn = file.getLastCellId(connectionSheet);
        lastSubscriptionColumn = file.getLastCellId(subscriptionSheet);
        lastSubscriptionRow = file.getLastRowId(subscriptionSheet);
        lastConnectionRow = file.getLastRowId(connectionSheet);
        getColumnsId();
    }

    /**
     * Получение id столбцов с необходимыми данными для теста
     */
    private void getColumnsId() {
        int row = 0;
        for(int i = 0; i < lastConnectionColumn; i++) {
            String text = file.getValue(row, i, connectionSheet);
            if(text.equals("ps id")) {
                psIdColumn = i;
            } else if(text.equals("Короткий номер")) {
                shortNumColumn = i;
            } else if(text.equals("Текст сообщения")) {
                textRequestColumn = i;
            }
        }
        for (int i = 0; i < lastSubscriptionColumn; i++) {
            String text = file.getValue(row, i, subscriptionSheet);
            if(text.equals("Уведомление при подключении")) {
                welcomeNotificationColumn = i;
                break;
            }
        }
    }

    //todo: тут получаем ConcurrentHashMap<Id, SubscribeRequest(id, psId, psIdName, shortNum, request, response)>
    private List<Subscription> read() {
        List<Subscription> subs = new ArrayList<>();
        for(int i = 1; i < lastConnectionRow; i++) {
            int psid = Integer.parseInt(file.getValue(i, psIdColumn, connectionSheet));
            String shortNum = file.getValue(i, shortNumColumn, connectionSheet);
            String textRequest = file.getValue(i, textRequestColumn, connectionSheet);
            String welcomeNotification = file.getValue(i, welcomeNotificationColumn, subscriptionSheet);
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

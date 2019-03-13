package com.a1s.file;

import com.a1s.subscribegeneratorapp.dao.SubscribeRequestDao;
import com.a1s.subscribegeneratorapp.model.SubscribeRequest;
import com.a1s.subscribegeneratorapp.model.Subscription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;
import java.util.List;

public class Read {
    private static final Log logger = LogFactory.getLog(Read.class);
    private int psIdColumn;
    private int shortNumColumn;
    private int textRequestColumn;
    private int welcomeNotificationColumn;

    private XSSFSheet connectionSheet;
    private XSSFSheet subscriptionSheet;
    private int lastSubscriptionColumn;
    private int lastConnectionColumn;
    private int lastSubscriptionRow;
    private int lastConnectionRow;

    private File file;

    public Read() {
        file = new File();
        connectionSheet = file.getConnectionSheet();
        subscriptionSheet = file.getSubscriptionSheet();
        lastConnectionColumn = file.setLastCell(connectionSheet);
        lastSubscriptionColumn = file.setLastCell(subscriptionSheet);
        lastSubscriptionRow = file.setLastRow(subscriptionSheet);
        lastConnectionRow = file.setLastRow(connectionSheet);
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

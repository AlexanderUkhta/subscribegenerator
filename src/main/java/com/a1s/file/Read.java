package com.a1s.file;

import org.apache.poi.xssf.usermodel.XSSFSheet;

public class Read {
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

    public void sout() {
        System.out.println("psIdColumn: " + psIdColumn);
        System.out.println("shortNumColumn: " + shortNumColumn);
        System.out.println("textRequestColumn: " + textRequestColumn);
        System.out.println("welcomeNotificationColumn: " + welcomeNotificationColumn);
    }
}

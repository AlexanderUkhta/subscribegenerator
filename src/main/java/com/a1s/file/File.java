package com.a1s.file;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class File {
    private static final String file = "src/main/resources/file.xlsx";
    private XSSFWorkbook book = setBook();
    private XSSFSheet connectionSheet = setConnectionSheet();
    private XSSFSheet subscriptionSheet = setSubscriptionSheet();

    /**
     * Получаем последнюю id последней ячейки для переданной страницы
     * @param sheet
     * @return
     */
    public int setLastCell(XSSFSheet sheet) {
        XSSFRow row = sheet.getRow(0);
        return row.getLastCellNum();
    }

    /**
     * Получаем последнюю id последней строки для переданной страницы
     * @param sheet
     * @return
     */
    public int setLastRow(XSSFSheet sheet) {
        return sheet.getLastRowNum();
    }

    /**
     * Получаем excel-книгу
     * @return
     */
    private XSSFWorkbook setBook() {
        XSSFWorkbook workbook;
        try {
            workbook = new XSSFWorkbook(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Can't get book");
        }
        return workbook;
    }

    /**
     * Получаем страницу с названием Подключение
     * @return
     */
    private XSSFSheet setConnectionSheet() {
        XSSFSheet sheet;
        try{
            sheet = book.getSheet("Подключение");
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Can't get connection sheet");
        }
        return sheet;
    }

    /**
     * Получаем страницу с названием Рассылки
     * @return
     */
    private XSSFSheet setSubscriptionSheet() {
        XSSFSheet sheet;
        try{
            sheet = book.getSheet("Рассылки");
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Can't get subscription sheet");
        }
        return sheet;
    }

    /**
     * Получаем ячейку
     * @param r
     * @param c
     * @param sheet
     * @return
     */
    private Cell getCell(int r, int c, XSSFSheet sheet) {
        Row row = sheet.getRow(r);
        if (row == null) {
            row = sheet.createRow(r);
        }
        Cell cell = row.getCell(c);
        if (cell == null) {
            cell = row.createCell(c);
        }
        cell.removeCellComment();
        return cell;
    }

    /**
     * Получаем значение ячейки
     * @param r
     * @param c
     * @param sheet
     * @return
     */
    public String getValue(int r, int c, XSSFSheet sheet) {
        Cell cell = getCell(r, c, sheet);
        if(cell == null) {
            cell = sheet.getRow(r).createCell(c);
        }
        cell.removeCellComment();
        String res;
        if(cell.getCellTypeEnum() == CellType.STRING){
            res = cell.getStringCellValue().trim();
        } else if(cell.getCellTypeEnum() == CellType.NUMERIC) {
            int n = (int) cell.getNumericCellValue();
            res = String.valueOf(n);
        } else {
            res = "0";
        }
        return res;
    }

    public XSSFSheet getConnectionSheet() {
        return connectionSheet;
    }

    public XSSFSheet getSubscriptionSheet() {
        return subscriptionSheet;
    }
}
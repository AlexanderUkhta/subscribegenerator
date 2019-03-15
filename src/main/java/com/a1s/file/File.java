package com.a1s.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class File {
    private static final Log logger = LogFactory.getLog(File.class);
    private static final String file = "src/main/resources/file.xlsx";
    private XSSFWorkbook book;

    public File() {
        book = getBook();
    }

    /**
     * Get the number of the last cell in the first row of the sheet
     * @param sheet excel sheet
     * @return
     */
    public int getLastCellId(XSSFSheet sheet) {
        XSSFRow row = sheet.getRow(0);
        return row.getLastCellNum();
    }

    /**
     * Get the last row id on the page
     * @param sheet  excel sheet
     * @return
     */
    public int getLastRowId(XSSFSheet sheet) {
        return sheet.getLastRowNum();
    }

    /**
     * Get excel book
     * @return
     */
    private XSSFWorkbook getBook() {
        XSSFWorkbook workbook;
        try {
            workbook = new XSSFWorkbook(new FileInputStream(file));
        } catch (IOException e) {
            logger.error("Can't get book", e);
            throw new RuntimeException("Can't get book");
        }
        return workbook;
    }

    /**
     * Get a page with a specific title from the book
     * @return
     */
    public XSSFSheet getSheet(String sheetName) {
        XSSFSheet sheet;
        try{
            sheet = book.getSheet(sheetName);
        } catch (Exception e){
            logger.error("Can't get connection sheet", e);
            throw new RuntimeException("Can't get connection sheet");
        }
        return sheet;
    }

    /**
     * Get cell by coordinates
     * @param r row id
     * @param c cell id
     * @param sheet apache sheet
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
     * Get cell value
     * @param r
     * @param c
     * @param sheet
     * @return
     */
    public String getCellValue(int r, int c, XSSFSheet sheet) {
        Cell cell = getCell(r, c, sheet);
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
}
package com.a1s.subscribegeneratorapp.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;


@Component("file")
public class Read {
    private static final Log logger = LogFactory.getLog(Read.class);
    private static final String file = "src/main/resources/file.xlsx";
    private XSSFWorkbook book = getBook();

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
        String res = "0";
        if(cell.getCellTypeEnum() == CellType.STRING){
            res = cell.getStringCellValue().trim();
        } else if(cell.getCellTypeEnum() == CellType.NUMERIC) {
            int n = (int) cell.getNumericCellValue();
            res = String.valueOf(n);
        } else if(cell.getCellTypeEnum() == CellType.FORMULA) {
            res = cell.getRichStringCellValue().getString();
        }
        return res;
    }

    /**
     * Get cell id
     * @param cellValue text in cell
     * @param sheet
     * @return
     */
    public int getCellId(String cellValue, XSSFSheet sheet) {
        int id = 0;
        for (int i = 0; i < getLastCellId(sheet); i++) {
            if(cellValue.equals(getCellValue(0, i, sheet))) {
                id = i;
                break;
            }
        }
        return id;
    }
}
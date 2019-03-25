package com.a1s.subscribegeneratorapp.excel;

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

import static com.a1s.ConfigurationConstantsAndMethods.INPUT_EXCEL_PATH;

/**
 * Represents methods for reading text data from excel workbook.
 */
@Component
public class ReadFromExcel {
    private static final Log logger = LogFactory.getLog(ReadFromExcel.class);
    private static final String file = INPUT_EXCEL_PATH;
    private XSSFWorkbook book = getBook();

    /**
     * Gets the number of last cell in the first row, that contains data.
     * @param sheet current excel sheet
     * @return the number of last cell
     */
    private int getLastCellId(XSSFSheet sheet) {
        XSSFRow row = sheet.getRow(0);
        return row.getLastCellNum();
    }

    /**
     * Gets the number of last row on current excel sheet.
     * @param sheet current excel sheet
     * @return the number of last row
     */
    public int getLastRowId(XSSFSheet sheet) {
        return sheet.getLastRowNum();
    }

    /**
     * Gets excel workbook from given FILE_PATH.
     * @return excel workbook
     */
    private XSSFWorkbook getBook() {
        XSSFWorkbook workbook;
        try {
            workbook = new XSSFWorkbook(new FileInputStream(file));
        } catch (IOException e) {
            logger.error("Cannot get workbook.", e);
            throw new RuntimeException("Cannot get workbook.");
        }
        return workbook;
    }

    /**
     * Gets an excel sheet with a specific title from the workbook.
     * @return excel sheet
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
     * Gets cell by coordinates.
     * @param r row id
     * @param c cell id
     * @param sheet excel sheet
     * @return cell with 'r' and 'c' coordinates
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
     * Gets cell value by coordinates.
     * @param r row id
     * @param c cell id
     * @param sheet excel sheet
     * @return cell value with 'r' and 'c' coordinates
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
     * Gets cell id by its value in the first row.
     * @param cellValue text value of current cell
     * @param sheet excel sheet
     * @return cell id
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
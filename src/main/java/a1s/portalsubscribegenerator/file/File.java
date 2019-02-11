package a1s.portalsubscribegenerator.file;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class File {
    private static final String file = "src/main/resources/Пример настроек рассылок.xlsx";
    private XSSFWorkbook book = setBook();
    private XSSFSheet connectionSheet = setConnectionSheet();
    private XSSFSheet subscriptionSheet = setSubscriptionSheet();

    private XSSFWorkbook setBook() {
        XSSFWorkbook workbook;
        try{
            workbook = new XSSFWorkbook(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Can't get book");
        }
        return workbook;
    }

    private XSSFSheet setConnectionSheet() {
        XSSFSheet sheet;
        try{
            sheet = book.getSheet("Подключение");
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Can't get info sheet");
        }
        return sheet;
    }

    private XSSFSheet setSubscriptionSheet() {
        XSSFSheet sheet;
        try{
            sheet = book.getSheet("Подключение");
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Can't get info sheet");
        }
        return sheet;
    }

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
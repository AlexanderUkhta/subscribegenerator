package a1s.portalsubscribegenerator.file;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class Read {
    private static final String file = "src/main/resources/Пример настроек рассылок.xlsx";
    private XSSFWorkbook book = setBook();
    private XSSFSheet connectionSheet = setConnectionSheet();
    private XSSFSheet subscriptionSheet = setConnectionSheet();

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

}
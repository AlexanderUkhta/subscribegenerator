package com.a1s.subscribegeneratorapp.file;

import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component()
public class Write {
    private static final Log logger = LogFactory.getLog(Write.class);
    private XSSFWorkbook book;
    private XSSFSheet sheet;
    private String sheetName;

    public Write() {
        sheetName = getDateWithHourAccuracy();
        book = new XSSFWorkbook();
        sheet = book.createSheet(sheetName);
    }

    /**
     * Receiving the date up to an hour
     * @return string like "2019-03-18 18-49"
     */
    private String getDateWithHourAccuracy() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm");
        System.out.println(dateFormat.format(new Date()));
        return dateFormat.format(new Date());
    }

    public void createRow(){

    }

    /**
     * Deletes the page with the report in case the report already exists.
     * @param sheetName
     */
    private void deleteExistReport(String sheetName) {
        try {
            book.removeSheetAt(book.getSheetIndex(sheetName));
        } catch(Exception e) {

        }
    }

    /**
     * Saves report in directory src/main/resources/
     */
    private void saveReport() {
        try {
            FileOutputStream out = new FileOutputStream("src/main/resources/subscribegeneratorreport.xlsx");
            book.write(out);
            out.close();
            book.close();
        } catch (FileNotFoundException e) {
            logger.error("Can't find file", e);
            e.printStackTrace();
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }
}

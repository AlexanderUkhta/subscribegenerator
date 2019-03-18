package com.a1s.subscribegeneratorapp.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Write {
    private static final Log logger = LogFactory.getLog(Write.class);
    private XSSFWorkbook book;

    public Write() {
        book = new XSSFWorkbook();
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

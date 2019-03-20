package com.a1s.subscribegeneratorapp.excel;

import com.a1s.subscribegeneratorapp.config.ReadExcelProperties;
import com.a1s.subscribegeneratorapp.model.ReportData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component()
public class WriteToExcel {
    private static final Log logger = LogFactory.getLog(WriteToExcel.class);
    private XSSFWorkbook book;
    private XSSFSheet sheet;
    private String sheetName;

    @Autowired
    private ReadExcelProperties readExcelProperties;

    @Autowired
    private CellStyle cellStyle;

    public WriteToExcel() {
        sheetName = getDateWithHourAccuracy();
        book = new XSSFWorkbook();
        sheet = book.createSheet(sheetName);
        createFirstRow();
    }

    /**
     * Receiving the date up to an hour
     * @return string like "2019-03-18 18-49"
     */
    private String getDateWithHourAccuracy() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm");
        return dateFormat.format(new Date());
    }

    /**
     * Added generation of column names in the report.
     */
    private void createFirstRow() {
        List<String> columnName = readExcelProperties.getExcelList();
        XSSFRow row = sheet.createRow(0);
        for (int i = 0; i < columnName.size(); i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(columnName.get(i));
        }
    }

    /**
     * Creates a row in the report and fills it with data from the ReportData
     * @param rowNumber
     * @param data
     */
    public void createRow(int rowNumber, ReportData data) {
        logger.info("Started putting report into new excel.");
        List<String> columnName = readExcelProperties.getExcelList();
        XSSFRow row = sheet.createRow(rowNumber);
        for (int i = 0; i < columnName.size(); i++) {
            XSSFCell cell = row.createCell(i);
            switch(columnName.get(i)) {
                case ("Название рассылки"):
                case ("Короткий номер"):
                case ("Текст сообщения"):
                case ("Нотификация при подключении"):
                case ("ps id"):
                case ("Ожидаемый результат"):
                    cell.setCellValue(data.getSubscribeRequestData().getPsIdName());
                    break;
                case("Действительный результат"):
                    String actualResponse = data.getActualResponse();
                    String expectedResult = data.getSubscribeRequestData().getResponseText();
                    cell.setCellValue(data.getActualResponse());
                    XSSFCell expectedCell = row.getCell(i-2);
                    if(actualResponse.equals(expectedResult)) {
                        cell.setCellStyle(cellStyle.greenBorderCell(book));
                        expectedCell.setCellStyle(cellStyle.greenBorderCell(book));
                    } else {
                        cell.setCellStyle(cellStyle.redBorderCell(book));
                        expectedCell.setCellStyle(cellStyle.redBorderCell(book));
                    }
                    break;
                case ("Ошибка"):
                    String errorMessage = data.getErrorMessage();
                    if(errorMessage != null) {
                        cell.setCellValue(data.getErrorMessage());
                        cell.setCellStyle(cellStyle.redBorderCell(book));
                        XSSFCell responseCell = row.getCell(i-1);
                        responseCell.setCellStyle(cellStyle.redBorderCell(book));
                        XSSFCell expectedResponse = row.getCell(i-2);
                        expectedResponse.setCellStyle(cellStyle.redBorderCell(book));
                    }
                    break;
            }
        }
        logger.info("Finished putting report into new excel, going to save document.");
        saveReport();
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
            logger.info("Report document is saved");
        } catch (FileNotFoundException e) {
            logger.error("Can't find excel", e);
            e.printStackTrace();
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }
}
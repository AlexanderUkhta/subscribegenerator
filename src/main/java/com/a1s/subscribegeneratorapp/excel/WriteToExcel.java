package com.a1s.subscribegeneratorapp.excel;

import com.a1s.subscribegeneratorapp.config.MsisdnAndExcelProperties;
import com.a1s.subscribegeneratorapp.model.ReportData;
import com.a1s.subscribegeneratorapp.model.SubscribeRequestData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.CellType;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WriteToExcel {
    private static final Log logger = LogFactory.getLog(WriteToExcel.class);
    private XSSFWorkbook book;
    private XSSFSheet sheet;
    private String sheetName;

    @Autowired
    private MsisdnAndExcelProperties msisdnAndExcelProperties;

    @Autowired
    private CellStyle cellStyle;

    public WriteToExcel() {
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
        return dateFormat.format(new Date());
    }

    /**
     * Added generation of column names in the report.
     */
    public void createFirstRow() {
        List<String> columnName = msisdnAndExcelProperties.getExcelColumns();
        XSSFRow row = sheet.createRow(0);
        for (int i = 0; i < columnName.size(); i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(columnName.get(i));
        }
    }

    /**
     * Creates a row in the report and fills it with data from the ReportData
     * @param reportDataMap
     */
    public int writeMap(Map<Integer, ReportData> reportDataMap) {
        AtomicInteger counter = new AtomicInteger();
        List<String> columnName = msisdnAndExcelProperties.getExcelColumns();
        logger.info("Started putting report into new excel.");
        try {
            reportDataMap.forEach((transactionId, reportData) -> {
                logger.info("Processing report data: " + counter.getAndIncrement());
                XSSFRow row = sheet.createRow(transactionId);
                for (int i = 0; i <= columnName.size(); i++) {
                    createCell(row, i, reportData);
                }
            });
        } finally {
            logger.info("Finished putting report into new excel, going to save document.");
            saveReport();
        }
        return counter.get();
    }

    private void createCell(XSSFRow row, int cellId, ReportData data) {
        XSSFCell cell = row.createCell(cellId, CellType.STRING);
        List<String> columnName = msisdnAndExcelProperties.getExcelColumns();
        switch(columnName.get(cellId)) {
            case ("Название рассылки"):
                cell.setCellValue(data.getSubscribeRequestData().getPsIdName());
                break;
            case ("Короткий номер"):
                cell.setCellValue(data.getSubscribeRequestData().getShortNum());
                break;
            case ("Текст сообщения"):
                cell.setCellValue(data.getSubscribeRequestData().getRequestText());
                break;
            case ("Нотификация при подключении"):
                cell.setCellValue(data.getSubscribeRequestData().getResponseText());
                break;
            case ("ps id"):
                cell.setCellValue(String.valueOf(data.getSubscribeRequestData().getPsId()));
                break;
            case ("Ожидаемый результат"):
                cell.setCellValue(data.getSubscribeRequestData().getResponseText());
                break;
            case("Действительный результат"):
                String actualResponse = data.getActualResponse();
                String expectedResult = data.getSubscribeRequestData().getResponseText();
                cell.setCellValue(data.getActualResponse());
                XSSFCell expectedCell = row.getCell(cellId-2);
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
                    XSSFCell responseCell = row.getCell(cellId-1);
                    responseCell.setCellStyle(cellStyle.redBorderCell(book));
                    XSSFCell expectedResponse = row.getCell(cellId-2);
                    expectedResponse.setCellStyle(cellStyle.redBorderCell(book));
                }
                break;
        }

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

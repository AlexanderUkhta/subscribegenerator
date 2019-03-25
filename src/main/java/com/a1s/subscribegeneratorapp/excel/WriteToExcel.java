package com.a1s.subscribegeneratorapp.excel;

import com.a1s.subscribegeneratorapp.config.MsisdnAndExcelProperties;
import com.a1s.subscribegeneratorapp.model.ReportData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.a1s.ConfigurationConstantsAndMethods.*;

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
        for (int i = 0; i < columnName.size() - 1; i++) { //No last 'welcome notification' column
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(columnName.get(i));
            cell.setCellStyle(cellStyle.greyBorderAndItalicFont(book));
        }
    }

    /**
     * Creates a row in the report and fills it with data from the ReportData
     * @param reportDataMap
     */
    public int writeMap(final Map<Integer, ReportData> reportDataMap) {
        AtomicInteger rowsProcessed = new AtomicInteger();
        List<String> columnName = new ArrayList<>(msisdnAndExcelProperties.getExcelColumns());
        logger.info("Started putting report into new excel.");

        try {
            reportDataMap.forEach((transactionId, reportData) -> {
                logger.info("Processing report data: " + rowsProcessed.incrementAndGet());
                XSSFRow row = sheet.createRow(transactionId);

                for (int cellId = 0; cellId < columnName.size(); cellId++) {
                    if (reportData.getSubscribeRequestData() != null) {
                        int result = createCell(row, cellId, reportData);
                        if (result == 1) {
                            reportData.setErrorMessage(RESPONSES_DONT_MATCH);
                            createCell(row, ERROR_COLUMN, reportData);
                            break;
                        }

                    } else {
                        for (int i = 0; i < columnName.size() - 1; i++) {
                            XSSFCell cell = row.createCell(i, CellType.STRING);
                            cell.setCellValue(EMPTY_CELL);
                        }
                        createCell(row, ERROR_COLUMN, reportData);
                    }
                }
            });

        } finally {
            logger.info("Finished putting report into new excel, going to save document.");
            saveReport();

        }
        return rowsProcessed.get();

    }

    private int createCell(XSSFRow row, int cellId, ReportData data) {
        XSSFCell cell = row.createCell(cellId, CellType.STRING);
        List<String> columnName = msisdnAndExcelProperties.getExcelColumns();

        switch (columnName.get(cellId)) {
            case ("PS_ID_NAME"): //название подписки/рассылки
                if (data.getSubscribeRequestData().getPsIdName() != null)
                    cell.setCellValue(data.getSubscribeRequestData().getPsIdName());
                else cell.setCellValue(EMPTY_CELL);
                break;
            case ("PS_ID"):
                if (data.getSubscribeRequestData().getPsId() != 0)
                    cell.setCellValue(String.valueOf(data.getSubscribeRequestData().getPsId()));
                else cell.setCellValue(EMPTY_CELL);
                break;
            case ("SHORT_NUM"):
                if (data.getSubscribeRequestData().getShortNum() != null)
                    cell.setCellValue(data.getSubscribeRequestData().getShortNum());
                else cell.setCellValue(EMPTY_CELL);
                break;
            case ("REQUEST"): //текст сообщения
                if (data.getSubscribeRequestData().getRequestText() != null)
                    cell.setCellValue(data.getSubscribeRequestData().getRequestText());
                else cell.setCellValue(EMPTY_CELL);
                break;
            case ("EXPECTED_RESPONSE"): //ожидаемый результат
                if (data.getSubscribeRequestData().getResponseText() != null)
                    cell.setCellValue(data.getSubscribeRequestData().getResponseText());
                else cell.setCellValue(EMPTY_CELL);
                break;
            case ("ACTUAL_RESPONSE"):
                if (data.getActualResponse() != null) {
                    String actualResponse = data.getActualResponse();
                    String expectedResult = data.getSubscribeRequestData().getResponseText();
                    cell.setCellValue(data.getActualResponse());
                    XSSFCell expectedCell = row.getCell(cellId - 1);

                    if (actualResponse.equals(expectedResult)) {
                        cell.setCellStyle(cellStyle.greenBorderCell(book));
                        expectedCell.setCellStyle(cellStyle.greenBorderCell(book));
                    } else {
                        cell.setCellStyle(cellStyle.redBorderCell(book));
                        expectedCell.setCellStyle(cellStyle.redBorderCell(book));
                        return 1;
                    }

                } else
                    cell.setCellValue(EMPTY_CELL);

                break;
            case ("ERROR_DATA"):
                String errorMessage = data.getErrorMessage();
                if (errorMessage != null) {
                    cell.setCellValue(data.getErrorMessage());
                    cell.setCellStyle(cellStyle.redBorderCell(book));
                }
                break;
//                case ("welcome notification"): //нотификация при подключении
//                    if (data.getSubscribeRequestData().getResponseText() != null)
//                        cell.setCellValue(data.getSubscribeRequestData().getResponseText());
//                    else cell.setCellValue(EMPTY_CELL);
//                    break;
            default:
                break;

        }
        return 0;

    }

    /**
     * Saves report in directory src/main/resources/
     */
    private void saveReport() {
        try {
            autosizeColumns();

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

    private void autosizeColumns() {
        for (int i  = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
            sheet.autoSizeColumn(i);
        }
    }
}

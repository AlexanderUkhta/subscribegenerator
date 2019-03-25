package com.a1s.subscribegeneratorapp.service;

import com.a1s.subscribegeneratorapp.excel.WriteToExcel;
import com.a1s.subscribegeneratorapp.model.ReportData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service, that makes excel report by reportDataMap, formed in requestQueueService.
 * The report is made by writeToExcel private methods.
 */
@Service
public class ExcelCreateService {
    private static final Log logger = LogFactory.getLog(ExcelCreateService.class);

    @Autowired
    private WriteToExcel writeToExcel;

    /**
     * Creates a report. Firstly, the first row with headers is put in excel sheet,
     * then remaining document is formed.
     * @param reportDataTreeMap map with report data, filled with transaction results
     * @return quantity of rows, that are finally put in excel report, except the first row
     */
    public int makeFullDataReport(final Map<Integer, ReportData> reportDataTreeMap) {
        writeToExcel.createFirstRow();
        int counter = writeToExcel.writeMap(reportDataTreeMap);
        logger.info("Data report has been processed");

        return counter;
    }

}

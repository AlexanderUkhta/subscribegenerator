package com.a1s.subscribegeneratorapp.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Contains color and font settings for a generated excel workbook.
 */
@Component("cellStyle")
class CellStyle {

    /**
     * Changes the cell background color to green.
     * @param book current excel workbook
     * @return
     */
    XSSFCellStyle greenBorderCell(XSSFWorkbook book) {
        XSSFCellStyle backgroundStyle = book.createCellStyle();
        backgroundStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        backgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return backgroundStyle;
    }

    /**
     * Changes the cell background color to red.
     * @param book current excel workbook
     * @return
     */
    XSSFCellStyle redBorderCell(XSSFWorkbook book) {
        XSSFCellStyle backgroundStyle = book.createCellStyle();
        backgroundStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
        backgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return backgroundStyle;
    }

    /**
     * Changes the cell background color to grey, switches font to italic.
     * @param book current excel workbook
     * @return
     */
    XSSFCellStyle greyBorderAndItalicFont(XSSFWorkbook book) {
        XSSFCellStyle backgroundStyle = book.createCellStyle();
        backgroundStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        backgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = book.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        backgroundStyle.setFont(font);
        return backgroundStyle;
    }

}

package com.a1s.subscribegeneratorapp.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class CellStyle {
    /**
     * Adds comments to cells
     * @param text
     * @param c
     * @param r
     * @param book
     * @param sheet
     * @return
     */
    public Comment setTextComment(String text, int c, int r, XSSFWorkbook book, XSSFSheet sheet){
        CreationHelper factory = book.getCreationHelper();
        Drawing drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = factory.createClientAnchor();
        Comment comment = drawing.createCellComment(anchor);
        RichTextString str = factory.createRichTextString(text);
        comment.setString(str);
        anchor.setCol1(c);
        anchor.setCol2(c + 1);
        anchor.setRow1(r);
        anchor.setRow2(r + 1);
        return comment;
    }

    /**
     * Changes the cell background color to green.
     * @param book
     * @return
     */
    public XSSFCellStyle greenBorderCell(XSSFWorkbook book) {
        XSSFCellStyle backgroundStyle = book.createCellStyle();
        backgroundStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        backgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return backgroundStyle;
    }

    /**
     * Changes the cell background color to yellow.
     * @param book
     * @return
     */
    public XSSFCellStyle yellowBorderCell(XSSFWorkbook book) {
        XSSFCellStyle backgroundStyle = book.createCellStyle();
        backgroundStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        backgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return backgroundStyle;
    }

    /**
     * Changes the cell background color to red.
     * @param book
     * @return
     */
    public XSSFCellStyle redBorderCell(XSSFWorkbook book) {
        XSSFCellStyle backgroundStyle = book.createCellStyle();
        backgroundStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        backgroundStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return backgroundStyle;
    }
}

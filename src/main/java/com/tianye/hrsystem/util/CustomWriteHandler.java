package com.tianye.hrsystem.util;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

public class CustomWriteHandler implements SheetWriteHandler
{
    private String title;


    public CustomWriteHandler(String title) {
        this.title = title;
    }

    @Override
    public void beforeSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {

    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Workbook workbook = writeWorkbookHolder.getWorkbook();
        Sheet sheet = workbook.getSheet(writeSheetHolder.getSheetName());
        Row row1 = sheet.getRow(0);
//        if (row1 == null) {
//            row1 = sheet.createRow(0);
//        }
//        row1.setHeight((short) 500);
//        Cell cell1 = row1.getCell(0);
//        if (cell1 == null) {
//            cell1 = row1.createCell(0);
//        }
//        cell1.setCellValue(title);
//        CellStyle cellStyle = workbook.createCellStyle();
//
//        Font font = workbook.createFont();
//        font.setBold(true);
//        font.setFontHeight((short) 220);
//        font.setFontName("宋体");
//
//        sheet.addMergedRegionUnsafe(new CellRangeAddress(0, 0, 0, 27));
//
//        CellStyle headStyle = workbook.createCellStyle();
//        headStyle.setFont(font);
//        headStyle.setAlignment(HorizontalAlignment.CENTER);
//        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//        // 设置表头背景色为灰色
//        headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
//        headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//
//        CellStyle contentStyle = workbook.createCellStyle();
//        contentStyle.setAlignment(HorizontalAlignment.CENTER);
//        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
//        contentStyle.setBorderTop(BorderStyle.THIN);
//        contentStyle.setBorderBottom(BorderStyle.THIN);
//        contentStyle.setBorderLeft(BorderStyle.THIN);
//        contentStyle.setBorderRight(BorderStyle.THIN);

        for(int i=4;i<=sheet.getLastRowNum();i++)
        {
            Row row = sheet.getRow(i);
            Cell cellXj = row.getCell(2);
            if("小计".equals(cellXj.getStringCellValue()))
            {
                //小计行，设置特定的样式
                Font font = workbook.createFont();
                font.setBold(true);
                font.setFontHeight((short) 15);
                font.setFontName("宋体");

                CellStyle headStyle = workbook.createCellStyle();
                headStyle.setFont(font);
                headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (Cell cell : row)
                {
                    cell.setCellStyle(headStyle);
                }
            }
        }
    }
}

package com.tianye.hrsystem.util;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.util.BooleanUtils;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import org.apache.poi.ss.usermodel.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomCellWriteHandler implements CellWriteHandler
{


    Map<String, CellStyle> cellStyleMap = new HashMap<>();

    /**
     * 小计行的索引
     */
    private List<Integer> xjRowList = new ArrayList<>();

    public CustomCellWriteHandler(List<Integer> xjRowList)
    {
        this.xjRowList = xjRowList;
    }

    @Override
    public void beforeCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Head head, Integer columnIndex, Integer relativeRowIndex, Boolean isHead) {

    }

    @Override
    public void afterCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        Cell cell = context.getCell();
        Row row = context.getRow();
        //int rowIndex = cell.getRowIndex();
        int cellIndex = cell.getColumnIndex();

        if (xjRowList.contains(cell.getRowIndex() - 4)) {
            // 设置合计行的样式
            CellStyle style = context.getWriteSheetHolder().getSheet().getWorkbook().createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);


            // 应用样式到合计行 ,小计行
            for (int index = 0; index < cell.getSheet().getRow(cell.getRowIndex()).getLastCellNum(); index++) {
                Cell totalCell = cell.getSheet().getRow(cell.getRowIndex()).getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                totalCell.setCellStyle(style);
                // 可选：设置合计行的字体样式
                Font font = context.getWriteSheetHolder().getSheet().getWorkbook().createFont();
                font.setFontName("宋体");
//                font.setBold(true);
                style.setFont(font);

//                row.setHeight((short) 200);
            }
        }
    }
}

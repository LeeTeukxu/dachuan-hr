package com.tianye.hrsystem.util;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.Cell;

import java.util.List;

public class RwhzCustemhandler extends AbstractColumnWidthStyleStrategy {
    private static final int MAX_COLUMN_WIDTH = 255;
    //the maximum column width in Excel is 255 characters

    public RwhzCustemhandler() {
    }

    @Override
    protected void setColumnWidth(WriteSheetHolder writeSheetHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        if (isHead) {
            int columnWidth = cell.getStringCellValue().getBytes().length;
            if (columnWidth > MAX_COLUMN_WIDTH) {
                columnWidth = MAX_COLUMN_WIDTH;
            } else {
//                if (cell.getColumnIndex() == 1 || cell.getColumnIndex() == 0) {
//                    columnWidth = columnWidth + 10;
//                } else {
//                    columnWidth = columnWidth + 10;
//                }
                if (cell.getStringCellValue().equals("应发工资") || cell.getStringCellValue().equals("加班工资") || cell.getStringCellValue().equals("代扣"))
                {
//                    columnWidth = columnWidth + 5;
//                    writeSheetHolder.getSheet().setColumnWidth(cell.getColumnIndex(), columnWidth);
                    return;
                }
                else
                {
                    columnWidth = columnWidth + 10;
                }

            }
            writeSheetHolder.getSheet().setColumnWidth(cell.getColumnIndex(), columnWidth * 256);
        }
    }
}

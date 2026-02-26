package com.tianye.hrsystem.util;

import com.alibaba.excel.write.handler.context.RowWriteHandlerContext;
import com.alibaba.excel.write.style.row.AbstractRowHeightStyleStrategy;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.util.Iterator;

public class CustomCellWriteHeightConfig extends AbstractRowHeightStyleStrategy
{

    /**
     * 默认高度
     */
    private static final Integer DEFAULT_HEIGHT = 180;

    @Override
    protected void setHeadColumnHeight(Row row, int relativeRowIndex) {

    }

    @Override
    protected void setContentColumnHeight(Row row, int relativeRowIndex) {
        Iterator<Cell> cellIterator = row.cellIterator();
        if (!cellIterator.hasNext()) {
            return;
        }
        // 默认为 1 行高度
        int maxHeight = 1;
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue();
                for (int i = 0; i < value.length(); i += 10) {
                    if (i + 10 < value.length()) {
                        value = value.substring(0, i) + "\n" + value.substring(i, i + 10) + value.substring(i + 10);
                    } else {
                        value = value.substring(0, i) + "\n" + value.substring(i);
                    }
                }
                if (value.contains("\n")) {
                    int length = value.split("\n").length;
                    maxHeight = Math.max(maxHeight, length);
                }
            }

        }
        row.setHeight((short) (maxHeight * DEFAULT_HEIGHT));
    }

    @Override
    public void afterRowDispose(RowWriteHandlerContext context) {
        if (context.getHead() != null) {
            if(ObjectUtils.isEmpty(context.getRelativeRowIndex())){
                return;
            }
            if (Boolean.TRUE.equals(context.getHead())) {
                this.setHeadColumnHeight(context.getRow(), context.getRelativeRowIndex());
            } else {
                this.setContentColumnHeight(context.getRow(), context.getRelativeRowIndex());
            }

        }
    }
}

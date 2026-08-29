package com.tianye.hrsystem.common;

import com.alibaba.excel.EasyExcel;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 流式读取 Excel（替代 {@code WorkbookFactory.create(inputStream)}）。
 *
 * <p>风险修复（内存溢出）：POI 的 {@code XSSFWorkbook} 会把整个 xlsx 的单元格模型加载进堆内存，
 * 超大文件直接 OOM。EasyExcel 基于 SAX 事件流式解析，文件读取期间内存占用恒定。</p>
 *
 * <p>为保证与原有基于 {@code DataFormatter.formatCellValue(cell)} 的导入逻辑行为一致，
 * 这里把每个单元格统一转换成与 {@code DataFormatter} 近似的字符串：
 * <ul>
 *   <li>字符串：trim；布尔：大写 TRUE/FALSE；</li>
 *   <li>日期：无时间分量则 {@code yyyy-MM-dd}，否则 {@code yyyy-MM-dd HH:mm:ss}；</li>
 *   <li>数字：按 {@code BigDecimal} 去尾零（如 100.0->"100"），与 Excel 常规数字显示一致。</li>
 * </ul>
 * 调用方拿到的仍是 {@code List<List<String>>}（行→列字符串），业务逻辑无需关心 POI 的 Row/Cell。</p>
 */
public final class ExcelImportUtil {

    private ExcelImportUtil() {
    }

    public static List<List<String>> readRows(InputStream inputStream) {
        List<List<String>> rows = new ArrayList<>();
        List<List<Object>> raw = EasyExcel.read(inputStream)
                .head(List.class)
                .autoTrim(true)
                .sheet(0)
                .headRowNumber(0)
                .doReadSync();
        if (raw == null) {
            return rows;
        }
        for (List<Object> rawRow : raw) {
            if (rawRow == null) {
                rows.add(new ArrayList<>());
                continue;
            }
            List<String> stringRow = new ArrayList<>(rawRow.size());
            for (Object cell : rawRow) {
                stringRow.add(toString(cell));
            }
            rows.add(stringRow);
        }
        return rows;
    }

    private static String toString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return ((String) value).trim();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "TRUE" : "FALSE";
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (value instanceof LocalDateTime) {
            LocalDateTime ldt = (LocalDateTime) value;
            return ldt.toLocalTime().equals(LocalTime.MIN)
                    ? ldt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (value instanceof Date) {
            LocalDateTime ldt = LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault());
            return ldt.toLocalTime().equals(LocalTime.MIN)
                    ? ldt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }
}

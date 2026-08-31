package com.tianye.hrsystem.common;

import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

public final class ExcelTemplateDownloadUtils {

    private ExcelTemplateDownloadUtils() {
    }

    public static void downloadClasspathTemplate(String resourcePath, String fileName, HttpServletResponse response)
            throws IOException {
        try (InputStream inputStream = ExcelTemplateDownloadUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, fileName + " not found");
                return;
            }
            response.setContentType(resolveContentType(fileName));
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            response.setHeader("Set-Cookie", "fileDownload=true; path=/");
            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        }
    }

    private static String resolveContentType(String fileName) {
        if (fileName != null && fileName.toLowerCase().endsWith(".xls")) {
            return "application/vnd.ms-excel";
        }
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
}

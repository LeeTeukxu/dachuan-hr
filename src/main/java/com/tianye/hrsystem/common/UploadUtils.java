package com.tianye.hrsystem.common;

import com.tianye.hrsystem.model.uploadFileResult;

import java.io.InputStream;

public class UploadUtils {
    FtpPath ftpPath;
    FTPUtil ftpUtil;
    private String companyId;

    private UploadUtils(String companyId) {
        this.companyId = companyId;
        ftpPath = new FtpPath(companyId);
        ftpUtil = new FTPUtil();
    }

    public static UploadUtils getInstance(String companyId) {
        return new UploadUtils(companyId);
    }

    public uploadFileResult uploadAttachment(String fileName, InputStream stream) {
        uploadFileResult result = new uploadFileResult();
        String path = ftpPath.getAttachment();
        boolean oo = false;
        try {
            oo = ftpUtil.upload(stream, fileName, path);
        } catch (Exception ax) {
            ax.printStackTrace();
            result.setMessage(ax.getMessage());
            oo = false;
        }
        result.setFullPath(path + fileName);
        result.setSuccess(oo);
        return result;
    }
}

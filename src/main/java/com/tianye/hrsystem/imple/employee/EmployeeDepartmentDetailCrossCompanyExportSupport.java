package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.springframework.beans.BeanUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

final class EmployeeDepartmentDetailCrossCompanyExportSupport {

    private static final List<String> COMPANY_IDS = Collections.unmodifiableList(
            Arrays.asList("0001", "0002", "0003", "0004", "0005"));
    private static final Pattern INVALID_FILE_NAME_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|]");

    private EmployeeDepartmentDetailCrossCompanyExportSupport() {
    }

    static List<String> companyIds() {
        return COMPANY_IDS;
    }

    static boolean isAdministrativeManager(LoginUserInfo loginUserInfo) {
        return loginUserInfo != null && "行政经理".equals(loginUserInfo.getRoleName());
    }

    static String companyDepartmentFileName(String companyName) {
        String safeCompanyName = companyName == null ? "" : companyName.trim();
        safeCompanyName = INVALID_FILE_NAME_CHARACTERS.matcher(safeCompanyName).replaceAll("");
        return safeCompanyName + "部门明细.xlsx";
    }

    static String generateArchivePassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    static <T> T withCompanyContext(LoginUserInfo originalContext,
                                    String companyId,
                                    CompanyWork<T> work) throws IOException {
        LoginUserInfo previousContext = CompanyContext.get();
        LoginUserInfo targetContext = copyContext(originalContext, companyId);
        CompanyContext.set(targetContext);
        try {
            return work.run();
        } finally {
            CompanyContext.set(previousContext);
        }
    }

    static byte[] buildEncryptedZip(Map<String, byte[]> files, String password) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IOException("压缩包没有可下载的文件");
        }
        if (password == null || password.isEmpty()) {
            throw new IOException("压缩包密码不能为空");
        }
        Path archivePath = Files.createTempFile("employee-department-detail-", ".zip");
        try {
            // try-with-resources 关闭 zip4j 句柄：Windows 下句柄未释放会导致下方 deleteIfExists 静默失败、临时 zip 堆积
            try (ZipFile zipFile = new ZipFile(archivePath.toFile(), password.toCharArray())) {
                ZipParameters parameters = new ZipParameters();
                parameters.setEncryptFiles(true);
                parameters.setEncryptionMethod(EncryptionMethod.AES);
                parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
                for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                    parameters.setFileNameInZip(entry.getKey());
                    byte[] bytes = entry.getValue() == null ? new byte[0] : entry.getValue();
                    zipFile.addStream(new ByteArrayInputStream(bytes), parameters);
                }
            }
            return Files.readAllBytes(archivePath);
        } finally {
            Files.deleteIfExists(archivePath);
        }
    }

    private static LoginUserInfo copyContext(LoginUserInfo source, String companyId) {
        LoginUserInfo target = new LoginUserInfo();
        if (source != null) {
            BeanUtils.copyProperties(source, target);
        }
        target.setCompanyId(companyId);
        if (source == null || !companyId.equals(source.getCompanyId())) {
            target.setCompanyName(null);
        }
        return target;
    }

    interface CompanyWork<T> {
        T run() throws IOException;
    }
}

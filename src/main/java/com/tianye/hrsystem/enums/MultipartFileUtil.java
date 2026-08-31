package com.tianye.hrsystem.enums;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 文件转换为MultipartFile工具类
 *
 * @author zhangzhiwei
 */
public class MultipartFileUtil {


    /**
     * 文件转换为MultipartFile
     *
     * <p>风险修复（内存溢出）：原实现用 {@code FileUtil.readBytes(file)} 把整个文件读进堆内存，
     * 超大文件(培训系统附件等)会直接 OOM。现改为磁盘持有、按需流式读取，仅在调用方真正需要
     * {@link #getBytes()} 时才惰性加载，导入/解析走 {@link #getInputStream()} 始终从磁盘流式读取。</p>
     *
     * @param file 文件对象
     * @return MultipartFile
     */
    public static MultipartFile getMultipartFile(File file) {
        return new MockMultipartFile(file.getName(), file.getName(), "application/octet-stream", file);
    }

    /**
     * 模拟的MultipartFile，用于服务间的文件上传（磁盘持有，避免整文件入堆）
     */
    public static class MockMultipartFile implements MultipartFile {

        private final String name;

        private final String originalFilename;

        private final String contentType;

        private final File file;

        private volatile byte[] content;

        /**
         * Create a new MockMultipartFile with the given content.
         *
         * @param name             the name of the file
         * @param originalFilename the original filename (as on the client's machine)
         * @param contentType      the content type (if known)
         * @param file             the backing file on disk
         */
        public MockMultipartFile(String name, String originalFilename, String contentType, File file) {
            Assert.hasLength(name, "Name must not be null");
            Assert.notNull(file, "File must not be null");
            this.name = name;
            this.originalFilename = (originalFilename != null ? originalFilename : "");
            this.contentType = contentType;
            this.file = file;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getOriginalFilename() {
            return this.originalFilename;
        }

        @Override
        public String getContentType() {
            return this.contentType;
        }

        @Override
        public boolean isEmpty() {
            return !file.exists() || file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            byte[] tmp = content;
            if (tmp == null) {
                synchronized (this) {
                    tmp = content;
                    if (tmp == null) {
                        tmp = Files.readAllBytes(file.toPath());
                        content = tmp;
                    }
                }
            }
            return tmp;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            if (!file.equals(dest)) {
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

    }
}

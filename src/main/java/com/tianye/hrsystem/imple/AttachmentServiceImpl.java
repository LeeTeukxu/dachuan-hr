package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.common.CompanyPathUtils;
import com.tianye.hrsystem.common.UploadUtils;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbattachment;
import com.tianye.hrsystem.model.uploadFileResult;
import com.tianye.hrsystem.repository.tbattachmentRepository;
import com.tianye.hrsystem.service.IAttachmentService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.UUID;

/**
 * @ClassName: AttachmentServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月09日 16:32
 **/
@Service
public class AttachmentServiceImpl implements IAttachmentService {

    @Autowired
    tbattachmentRepository attRep;

    private UploadUtils getUtils() throws Exception {
        LoginUserInfo info = CompanyContext.get();
        if (info == null) throw new Exception("登录失败，请重新登录!");
        String companyId = info.getCompanyId();
        return UploadUtils.getInstance(companyId);
    }

    @Override
    public tbattachment Upload(MultipartFile file) throws Exception {
        FileInputStream fileInputStream = null;
        LoginUserInfo loginInfo = CompanyContext.get();
        String fileName = file.getOriginalFilename();
        String[] exts = fileName.split("\\.");
        String extName = exts[exts.length - 1];
        String uuId = UUID.randomUUID().toString();
        String uploadFileName = uuId + "." + extName;
        String targetFile = CompanyPathUtils.getFullPath("Temp", uploadFileName);
        File fx = new File(targetFile);
        if (fx.exists()) {
            fx.delete();
        }
        // transferTo 流式落盘，避免 file.getBytes() 把整个上传文件一次性读入堆
        file.transferTo(fx);
        if (fx.exists()) {
            try {
                fileInputStream = new FileInputStream(fx);
                UploadUtils uploadUtils = getUtils();
                uploadFileResult rr = uploadUtils.uploadAttachment(uploadFileName, fileInputStream);
                if (rr.getSuccess() == true) {
                    tbattachment tb = new tbattachment();
                    tb.setId(uuId);
                    tb.setName(fileName);
                    tb.setSavePath(rr.getFullPath());
                    tb.setSize(Integer.parseInt(Long.toString(fx.length())));
                    tb.setCreateMan(Integer.parseInt(loginInfo.getUserId()));
                    tb.setCreateManName(loginInfo.getUserName());
                    tb.setCreateTime(new Date());
                    return attRep.save(tb);
                } else throw new Exception("上传文件:" + fileName + "失败!");

            } finally {
                try {
                    if (fileInputStream != null) fileInputStream.close();
                } catch (Exception ignore) {
                }
                // 上传结束立即删除本地临时文件；原先 deleteOnExit 要等 JVM 退出，Temp 目录无限膨胀
                if (!fx.delete()) {
                    fx.deleteOnExit();
                }
            }
        } else throw new Exception("保存上传文件失败!");
    }
}

package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.CompanyPathUtils;
import com.tianye.hrsystem.common.FTPUtil;
import com.tianye.hrsystem.common.WebFileUtils;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.model.tbattachment;
import com.tianye.hrsystem.repository.tbattachmentRepository;
import com.tianye.hrsystem.service.IAttachmentService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @ClassName: AttachmentController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月09日 15:13
 **/

@Controller
@RequestMapping("/attachment")
public class AttachmentController {

    @Autowired
    IAttachmentService attService;
    @Autowired
    tbattachmentRepository attRep;

    @RequestMapping("/upload")
    public successResult Upload(MultipartFile file) {
        successResult result = new successResult();
        try {
            tbattachment att = attService.Upload(file);
            result.setData(att);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @RequestMapping("/download")
    public void Download(String AttID, HttpServletResponse response) {
        try {
            Optional<tbattachment> findOne = attRep.findById(AttID);
            if (findOne.isPresent()) {
                tbattachment tb = findOne.get();
                String Path = tb.getSavePath();
                FTPUtil ftpUtil = new FTPUtil();
                String SavePath = CompanyPathUtils.getFullPath("Temp", tb.getName());
                if (ftpUtil.connect() == true) {
                    ftpUtil.download(Path, SavePath);
                    WebFileUtils.download(tb.getName(), new File(SavePath), response);
                } else response.getWriter().write("FTP登录失败!");
            } else response.getWriter().write("下载的文件不存在!");
        } catch (Exception ax) {
            try {
                response.getWriter().write(ax.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequestMapping("/getAttachmentByType")
    public successResult getAttachmentByType(String Type) {
        successResult result = new successResult();
        try {
            List<Map<String, Object>> rr = new ArrayList<>();
            if (StringUtils.isEmpty(Type) == false) {
                SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                List<tbattachment> atts = attRep.findAllByTypeOrderByCreateTimeDesc(Type);
                for (int i = 0; i < atts.size(); i++) {
                    tbattachment att = atts.get(i);
                    Map<String, Object> r = new HashMap<>();
                    r.put("ATTID", att.getId());
                    r.put("FILENAME", att.getName());
                    r.put("FILESIZE", att.getSize());
                    r.put("UPLOADTIME", simple.format(att.getCreateTime()));
                    r.put("UPLOADMAN", att.getCreateManName());
                    rr.add(r);
                }
                result.setData(rr);
            }
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }
}

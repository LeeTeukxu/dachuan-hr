package com.tianye.hrsystem.service;

import com.tianye.hrsystem.model.tbattachment;
import org.springframework.web.multipart.MultipartFile;

/**
 * @ClassName: IAttachmentService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月09日 16:07
 **/
public interface IAttachmentService {
    tbattachment Upload(MultipartFile file) throws Exception;
}

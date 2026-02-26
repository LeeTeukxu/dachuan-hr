package com.tianye.hrsystem.common;

import com.alibaba.fastjson.JSON;
import com.tianye.hrsystem.model.Postresultlog;
import com.tianye.hrsystem.repository.postresultlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @ClassName: DDResposeLogger
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年05月10日 17:41
 **/

@Component
public class DDTalkResposeLogger {
    @Autowired
    postresultlogRepository logRep;
    public void Info(Object Obj, String Url,Date begin,Class<?> classInfo){
        Postresultlog log=new Postresultlog();
        log.setBegin(begin);
        log.setContent(JSON.toJSONString(Obj));
        log.setPostUrl(Url);
        log.setClassName(classInfo.getName());
        log.setCreateTime(new Date());
        logRep.save(log);
    }
    public void Info(Object Obj, String Url,Date begin,Date end,Class<?> classInfo){
        Postresultlog log=new Postresultlog();
        log.setBegin(begin);
        log.setContent(JSON.toJSONString(Obj));
        log.setPostUrl(Url);
        log.setClassName(classInfo.getName());
        log.setCreateTime(new Date());
        logRep.save(log);
    }

}

package com.tianye.hrsystem.util;

import com.tianye.hrsystem.common.UpdateRecordTemplate;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName: AutoTaskUtil
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年04月28日 21:40
 **/
@Component
public class AutoTaskTokenUtil {
    @Autowired
    UpdateRecordTemplate redisRep;
    SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public boolean hasKey(Date date,Class<?>classInfo){
        LoginUserInfo Info= CompanyContext.get();
        String subKey=format.format(date);
        String mainKey=  Info.getCompanyId()+"::"+classInfo.getName();
        return redisRep.hasKey(mainKey,subKey);
    }
    public boolean hasKey(Date begin,Date end,Class<?> classInfo){
        LoginUserInfo Info= CompanyContext.get();
        String subKey=format.format(begin)+"::"+format.format(end);
        String mainKey=  Info.getCompanyId()+"::"+classInfo.getName();
        return redisRep.hasKey(mainKey,subKey);
    }
    public void  addOne(Date date,Class<?> classInfo){
        LoginUserInfo Info= CompanyContext.get();
        String subKey=format.format(date);
        String mainKey=  Info.getCompanyId()+"::"+classInfo.getName();
        redisRep.put(mainKey,subKey,"1");
    }

    public void  addOne(Date begin,Date end,Class<?> classInfo){
        LoginUserInfo Info= CompanyContext.get();
        String subKey=format.format(begin)+"::"+format.format(end);
        String mainKey=  Info.getCompanyId()+"::"+classInfo.getName();
        redisRep.put(mainKey,subKey,"1");
    }
    public String getLoggerText(Date begin,Date end,String method){
        String bText=format.format(begin);
        String eText=format.format(end);
        return "已完成同步:"+bText+"到:"+eText+"的"+method+"内容....";
    }
    public String getLoggerText(Date begin,String method){
        String bText=format.format(begin);
        return "已完成同步:"+bText+"的"+method+"内容....";
    }
}

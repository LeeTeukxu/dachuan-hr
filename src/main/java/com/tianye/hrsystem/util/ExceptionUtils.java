package com.tianye.hrsystem.util;

import cn.hutool.core.stream.StreamUtil;
import com.tianye.hrsystem.model.tbexception;
import com.tianye.hrsystem.repository.tbexceptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * @ClassName: ExceptionUtils
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年06月06日 15:12
 **/
@Component
public class ExceptionUtils {
    @Autowired
    tbexceptionRepository exceptRep;
    Logger logger= LoggerFactory.getLogger(ExceptionUtils.class);
    public void addOne(Class<?> classInfo,Exception ax){
        try {
            tbexception one=new tbexception();
            one.setClassName(classInfo.getName());
            one.setException(ax.getClass().getName());
            StringWriter writer=new StringWriter();
            ax.printStackTrace(new PrintWriter(writer));
            one.setDetail(writer.toString());
            one.setCreatetime(new Date());
            exceptRep.save(one);
        }
        catch(Exception bx){
            logger.info("记录异常发生了错误:"+ax.getMessage());
            bx.printStackTrace();
        }
    }
}

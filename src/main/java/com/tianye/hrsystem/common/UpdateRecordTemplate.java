package com.tianye.hrsystem.common;

import com.tianye.hrsystem.model.updateRecord;
import com.tianye.hrsystem.repository.updateRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName: UpdateRecordTemplate
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年07月22日 17:57
 **/
@Component
public class UpdateRecordTemplate {
    SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Autowired
    updateRecordRepository updateRep;
    public boolean hasKey(String mainKey,String subKey){
        return updateRep.countByMainKeyAndSubKey(mainKey,subKey)>0;
    }

    /**
     * 获取指定 mainKey+subKey 的 value
     * @return value 字符串，不存在时返回 null
     */
    public String getValue(String mainKey, String subKey) {
        updateRecord record = updateRep.findByMainKeyAndSubKey(mainKey, subKey);
        return record != null ? record.getValue() : null;
    }

    public boolean put(String mainKey,String subKey,String value){
        if(hasKey(mainKey,subKey)==false){
            updateRecord newOne=new updateRecord();
            newOne.setMainKey(mainKey);
            newOne.setSubKey(subKey);
            newOne.setValue(value);
            newOne.setCreateTime(new Date());
            updateRep.save(newOne);
        }
        return true;
    }

    /**
     * 写入或更新（支持覆盖已有值，如 FAILED → SUCCESS）
     */
    public void putOrUpdate(String mainKey, String subKey, String value) {
        updateRecord existing = updateRep.findByMainKeyAndSubKey(mainKey, subKey);
        if (existing != null) {
            existing.setValue(value);
            existing.setCreateTime(new Date());
            updateRep.save(existing);
        } else {
            updateRecord newOne = new updateRecord();
            newOne.setMainKey(mainKey);
            newOne.setSubKey(subKey);
            newOne.setValue(value);
            newOne.setCreateTime(new Date());
            updateRep.save(newOne);
        }
    }
}

package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.entity.po.HrmEmployeeChangeRecord;
import com.tianye.hrsystem.entity.po.HrmEmployeeFile;
import com.tianye.hrsystem.entity.vo.Content;
import com.tianye.hrsystem.enums.HrmActionBehaviorEnum;
import com.tianye.hrsystem.enums.LabelGroupEnum;

import java.util.Map;

/**
 * @ClassName: IHrmEmployeeActionRecordService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月26日 10:31
 **/
public interface IHrmEmployeeActionRecordService{
    Content employeeFixedFieldRecord(Map<String, Object> oldObj, Map<String, Object> newObj,
            LabelGroupEnum labelGroupEnum, Long employeeId) throws Exception;
    Content addOrDeleteRecord(HrmActionBehaviorEnum behaviorEnum, LabelGroupEnum labelGroupEnum, Long employeeId);
    Content entityUpdateRecord(LabelGroupEnum labelGroupEnum, Map<String, Object> oldRecord,
            Map<String, Object> newRecord, Long employeeId);

    Content changeRecord(HrmEmployeeChangeRecord hrmEmployeeChangeRecord);

    Content addFileRecord(HrmEmployeeFile employeeFile, HrmActionBehaviorEnum hrmActionBehaviorEnum);
}

package com.tianye.hrsystem.entity.vo;

import com.alibaba.fastjson.JSONObject;
import com.tianye.hrsystem.common.ApplyEnum;
import com.tianye.hrsystem.common.OperateObjectEnum;
import com.tianye.hrsystem.enums.BehaviorEnum;
import lombok.Data;

/**
 * @ClassName: OperationLog
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月27日 7:42
 **/
@Data
public class OperationLog {
    //操作对象
    private Object operationObject;
    //操作详情
    private String operationInfo;

    private BehaviorEnum behavior = null;

    private OperateObjectEnum applyObject = null;

    private ApplyEnum apply = null;

    public void setOperationObject(Object typeId, Object typeName) {
        if (operationObject == null) {
            this.operationObject = new JSONObject();
        }
        if (operationObject instanceof JSONObject) {
            ((JSONObject) operationObject).put("typeId", String.valueOf(typeId));
            ((JSONObject) operationObject).put("typeName", typeName);
        }

    }

    public void setOperationObject(String key, Object value) {
        if (operationObject == null) {
            operationObject = new JSONObject();
        }
        if (operationObject instanceof JSONObject) {
            ((JSONObject) operationObject).put(key, value);
        }
    }

}
package com.tianye.hrsystem.entity.param;

import com.tianye.hrsystem.common.PageEntity;

/**
 * @ClassName: QueryAttendanceShiftParameter
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月21日 15:32
 **/
public class QueryAttendanceShiftParameter extends PageEntity {
    private String shiftName;
    private  long shiftId;

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public long getShiftId() {
        return shiftId;
    }

    public void setShiftId(long shiftId) {
        this.shiftId = shiftId;
    }
}

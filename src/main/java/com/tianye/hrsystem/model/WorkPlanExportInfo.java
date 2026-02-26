package com.tianye.hrsystem.model;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName: WorkPlanExportInfo
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年08月09日 13:24
 **/
public class WorkPlanExportInfo implements Serializable {
    List<tbPlanListVo> planList;

    public List<tbPlanListVo> getPlanList() {
        return planList;
    }

    public void setPlanList(List<tbPlanListVo> planList) {
        this.planList = planList;
    }
}

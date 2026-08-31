package com.tianye.hrsystem.entity.vo;

import java.util.ArrayList;
import java.util.List;

public class WorkPlanImportPreviewVO {
    private Integer totalCount;
    private Integer validCount;
    private Integer errorCount;
    private List<WorkPlanImportPreviewRowVO> rows = new ArrayList<>();

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getValidCount() {
        return validCount;
    }

    public void setValidCount(Integer validCount) {
        this.validCount = validCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public List<WorkPlanImportPreviewRowVO> getRows() {
        return rows;
    }

    public void setRows(List<WorkPlanImportPreviewRowVO> rows) {
        this.rows = rows;
    }
}

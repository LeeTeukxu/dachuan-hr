package com.tianye.hrsystem.modules.workplan.bo;

import java.util.ArrayList;
import java.util.List;

public class SortWorkPlanPositionBO {

    private Long productId;
    private List<Long> positionIds = new ArrayList<>();

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public List<Long> getPositionIds() {
        return positionIds;
    }

    public void setPositionIds(List<Long> positionIds) {
        this.positionIds = positionIds;
    }
}

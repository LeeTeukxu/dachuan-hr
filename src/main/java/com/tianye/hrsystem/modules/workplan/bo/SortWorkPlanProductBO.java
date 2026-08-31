package com.tianye.hrsystem.modules.workplan.bo;

import java.util.ArrayList;
import java.util.List;

public class SortWorkPlanProductBO {

    private List<Long> productIds = new ArrayList<>();

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }
}

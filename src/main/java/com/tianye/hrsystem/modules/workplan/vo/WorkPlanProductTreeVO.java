package com.tianye.hrsystem.modules.workplan.vo;

import java.util.ArrayList;
import java.util.List;

public class WorkPlanProductTreeVO {

    private Long id;
    private String productName;
    private Integer sort;
    private List<WorkPlanPositionVO> positions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<WorkPlanPositionVO> getPositions() {
        return positions;
    }

    public void setPositions(List<WorkPlanPositionVO> positions) {
        this.positions = positions;
    }
}

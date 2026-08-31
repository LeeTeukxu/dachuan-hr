package com.tianye.hrsystem.modules.workplan.vo;

import java.util.ArrayList;
import java.util.List;

public class WorkPlanPositionVO {

    private Long id;
    private Long productId;
    private String positionName;
    private Integer sort;
    private List<WorkPlanPositionEmployeeVO> employees = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<WorkPlanPositionEmployeeVO> getEmployees() {
        return employees;
    }

    public void setEmployees(List<WorkPlanPositionEmployeeVO> employees) {
        this.employees = employees;
    }
}

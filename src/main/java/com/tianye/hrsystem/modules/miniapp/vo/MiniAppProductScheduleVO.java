package com.tianye.hrsystem.modules.miniapp.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 某天的产品排班（GET /mp/schedule/query），结构与保存入参对应，
 * employees 为渲染标签所需的快照（employeeId/name/depName）。
 */
public class MiniAppProductScheduleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String workDate;

    private List<ProductVO> products = new ArrayList<>();

    public static class ProductVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /** standard / custom */
        private String type;

        private Long productId;

        private String productName;

        private List<PositionVO> positions = new ArrayList<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public List<PositionVO> getPositions() {
            return positions;
        }

        public void setPositions(List<PositionVO> positions) {
            this.positions = positions;
        }
    }

    public static class PositionVO implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;

        private String shiftType;

        private String startTime;

        private String endTime;

        private Boolean endTimeUnknown;

        private Boolean isContinuous;

        private Boolean isCustom;

        private List<Long> employeeIds = new ArrayList<>();

        private List<EmployeeTagVO> employees = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShiftType() {
            return shiftType;
        }

        public void setShiftType(String shiftType) {
            this.shiftType = shiftType;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public Boolean getEndTimeUnknown() {
            return endTimeUnknown;
        }

        public void setEndTimeUnknown(Boolean endTimeUnknown) {
            this.endTimeUnknown = endTimeUnknown;
        }

        public Boolean getIsContinuous() {
            return isContinuous;
        }

        public void setIsContinuous(Boolean isContinuous) {
            this.isContinuous = isContinuous;
        }

        public Boolean getIsCustom() {
            return isCustom;
        }

        public void setIsCustom(Boolean isCustom) {
            this.isCustom = isCustom;
        }

        public List<Long> getEmployeeIds() {
            return employeeIds;
        }

        public void setEmployeeIds(List<Long> employeeIds) {
            this.employeeIds = employeeIds;
        }

        public List<EmployeeTagVO> getEmployees() {
            return employees;
        }

        public void setEmployees(List<EmployeeTagVO> employees) {
            this.employees = employees;
        }
    }

    public static class EmployeeTagVO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long employeeId;

        private String name;

        private String depName;

        public EmployeeTagVO() {
        }

        public EmployeeTagVO(Long employeeId, String name, String depName) {
            this.employeeId = employeeId;
            this.name = name;
            this.depName = depName;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDepName() {
            return depName;
        }

        public void setDepName(String depName) {
            this.depName = depName;
        }
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public List<ProductVO> getProducts() {
        return products;
    }

    public void setProducts(List<ProductVO> products) {
        this.products = products;
    }
}

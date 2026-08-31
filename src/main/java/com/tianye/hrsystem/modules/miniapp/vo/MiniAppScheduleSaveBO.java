package com.tianye.hrsystem.modules.miniapp.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 小程序「添加排班」保存入参（POST /mp/schedule/save，JSON body）。
 * 与前端 addschedule.vue 的提交结构同构；员工姓名/部门以数据库快照为准，不信任前端。
 */
public class MiniAppScheduleSaveBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** yyyy-MM-dd */
    private String workDate;

    private List<ProductItem> products = new ArrayList<>();

    public static class ProductItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /** standard / custom */
        private String type;

        /** 标准产品ID；自定义产品为空 */
        private Long productId;

        private String productName;

        private List<PositionItem> positions = new ArrayList<>();

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

        public List<PositionItem> getPositions() {
            return positions;
        }

        public void setPositions(List<PositionItem> positions) {
            this.positions = positions;
        }
    }

    public static class PositionItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;

        /** day / night / adjust / rest */
        private String shiftType;

        private String startTime;

        private String endTime;

        /** 下班时间不确定 */
        private Boolean endTimeUnknown;

        /** 是否连班（仅白班） */
        private Boolean isContinuous;

        /** 是否自定义岗位（只能调休/休息） */
        private Boolean isCustom;

        private List<Long> employeeIds = new ArrayList<>();

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
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public List<ProductItem> getProducts() {
        return products;
    }

    public void setProducts(List<ProductItem> products) {
        this.products = products;
    }
}

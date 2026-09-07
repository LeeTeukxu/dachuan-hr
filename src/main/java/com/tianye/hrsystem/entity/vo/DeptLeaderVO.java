package com.tianye.hrsystem.entity.vo;

import lombok.Data;

/**
 * 部门分管领导（员工新建/编辑选择部门时自动带出直属上级）
 */
@Data
public class DeptLeaderVO {
    private Long leaderEmployeeId;
    private String leaderName;
}

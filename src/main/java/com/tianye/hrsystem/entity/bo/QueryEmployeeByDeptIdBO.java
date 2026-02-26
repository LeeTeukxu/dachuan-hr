package com.tianye.hrsystem.entity.bo;

import com.tianye.hrsystem.base.PageEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryEmployeeByDeptIdBO extends PageEntity {

    @ApiModelProperty("部门名称")
    private Long deptId;

    @ApiModelProperty("搜索")
    private String search;

    @Override
    public String toString() {
        return "QueryEmployeeByDeptIdBO{" +
                "deptId=" + deptId +
                ", search='" + search + '\'' +
                '}';
    }
}

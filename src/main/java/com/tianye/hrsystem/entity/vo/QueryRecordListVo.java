package com.tianye.hrsystem.entity.vo;

import com.tianye.hrsystem.entity.po.HrmActionRecord;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class QueryRecordListVo extends HrmActionRecord {

    /**
     * private AdminUser adminUser
     */
    @ApiModelProperty("头像")
    private String img;

    @ApiModelProperty("真实姓名")
    private String realname;

    @Override
    public String toString() {
        return "QueryRecordListVO{" +
                "img='" + img + '\'' +
                ", realname='" + realname + '\'' +
                '}';
    }
}

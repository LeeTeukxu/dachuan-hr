package com.tianye.hrsystem.common;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * @author zhangzhiwei
 * 分页需继承的类
 */
@Getter
@Setter
public class PageEntity {

    @ApiModelProperty("当前页数")
    private Integer pageNum;

    @ApiModelProperty("每页展示条数")
    private Integer pageSize;

    @ApiModelProperty(value = "是否分页，0:不分页 1 分页", allowableValues = "0,1")
    private Integer pageType = 1;

    @ApiModelProperty(value="排序字段")
    private String sortField;

    @ApiModelProperty(value = "升或降序，1:升序 2:降序", allowableValues = "1,2")
    private String order;




}

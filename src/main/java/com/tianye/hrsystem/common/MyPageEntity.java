package com.tianye.hrsystem.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
public class MyPageEntity {

    @ApiModelProperty("当前页数")
    private Long page;

    @ApiModelProperty("每页展示条数")
    private Long limit;

    @ApiModelProperty(value = "是否分页，0:不分页 1 分页", allowableValues = "0,1")
    private Integer pageType = 1;

    public <T> Page<T> parse() {
        Page<T> page = new Page<T>(getPage(), getLimit());
        if (Objects.equals(0, pageType)) {
            page.setSize(10000);
        }
        return page;
    }

    public void setPageType(Integer pageType) {
        this.pageType = pageType;
        if (pageType == 0) {
            limit = 10000L;
        }
    }

    public Long getLimit() {
        if (limit == null || limit < 0L) {
            limit = 15L;
        }
        if (limit > 10000 && 1 == pageType) {
            limit = 10000L;
        }
        return limit;
    }

    public Long getPage() {
        if (page == null || page < 0L) {
            page = 1L;
        }
        return page;
    }
}

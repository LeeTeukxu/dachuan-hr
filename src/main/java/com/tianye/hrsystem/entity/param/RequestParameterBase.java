package com.tianye.hrsystem.entity.param;

/**
 * @ClassName: RequestParameterBase
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月20日 9:54
 **/
public class RequestParameterBase {
    Integer page;
    Integer limit;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}

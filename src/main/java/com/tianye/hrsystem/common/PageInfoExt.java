package com.tianye.hrsystem.common;

import com.github.pagehelper.PageInfo;

public class PageInfoExt<T> extends PageInfo
{

    /**
     * 额外数据
     */
    private Object extraData;

    public Object getExtraData() {
        return extraData;
    }

    public void setExtraData(Object extraData) {
        this.extraData = extraData;
    }

}

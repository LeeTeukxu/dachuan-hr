package com.tianye.hrsystem.model;

/**
 * @ClassName: uploadResult
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年08月18日 15:16
 **/
public class uploadResult {
    int errcode;
    String errmsg;
    String type;
    String media_id;

    public int getErrcode() {
        return errcode;
    }

    public void setErrcode(int errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMedia_id() {
        return media_id;
    }

    public void setMedia_id(String media_id) {
        this.media_id = media_id;
    }
}

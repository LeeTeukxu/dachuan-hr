package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("消息业务对象")
public class AdminMessageBO {

    @ApiModelProperty("消息类型")
    private Integer messageType;

    @ApiModelProperty("消息标题")
    private String title;

    @ApiModelProperty("消息内容")
    private String content;

    @ApiModelProperty("消息大类 1 任务 2 日志 3 oa审批 4公告 5 日程 6 crm消息 7 知识库 8 人资")
    private Integer label;

    @ApiModelProperty("消息类型 详见AdminMessageEnum")
    private Integer type;

    @ApiModelProperty("关联业务主键ID")
    private Long typeId;

    @ApiModelProperty("跳转链接")
    private String linkUrl;

    @ApiModelProperty("发送人")
    private Long userId;

    @ApiModelProperty("发送人邮箱")
    private String userEmail;

    @ApiModelProperty("接收人列表")
    private List<Long> ids;

    @ApiModelProperty("申请原因")
    private String examineReason;

    public void setIds(List<Long> ids) {
        //HashSet<Long> hashSet = new ArrayList<>(ids);
        this.ids = new ArrayList<>(ids);
    }
}

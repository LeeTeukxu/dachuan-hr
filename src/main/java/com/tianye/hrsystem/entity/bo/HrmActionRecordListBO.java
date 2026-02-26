package com.tianye.hrsystem.entity.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HrmActionRecordListBO {


    @ApiModelProperty("记录内容")
    private List<String> contentList;

    @ApiModelProperty("翻译记录内容")
    private List<String> transContentList;
	
	
}

package com.tianye.hrsystem.modules.dashboard.bo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 关键岗位设置保存参数
 */
@Getter
@Setter
public class KeyPostSaveBO {

    @ApiModelProperty("标记为关键岗位的岗位名称集合（全量替换）")
    private List<String> posts;
}

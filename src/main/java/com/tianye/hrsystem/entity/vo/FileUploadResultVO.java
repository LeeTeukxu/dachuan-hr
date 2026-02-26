package com.tianye.hrsystem.entity.vo;

import com.tianye.hrsystem.entity.po.UploadEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : zjj
 * @since : 2023/1/9
 */
@Getter
@Setter
public class FileUploadResultVO extends UploadEntity {

    @ApiModelProperty(value = "批次id")
    private String batchId;
}

package com.tianye.hrsystem.entity.bo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.tianye.hrsystem.common.MyPageEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictDataBO extends MyPageEntity {

    @TableId(value = "id")
    private Integer id;

    @ApiModelProperty(value = "名称")
    private String name;
}

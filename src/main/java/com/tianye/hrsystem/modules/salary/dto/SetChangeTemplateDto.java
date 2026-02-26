package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.modules.salary.vo.ChangeSalaryOptionVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SetChangeTemplateDto {

    private Long id;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty("定薪项")
    private List<ChangeSalaryOptionVO> value;

    @Override
    public String toString() {
        return "SetChangeTemplateBO{" +
                "id=" + id +
                ", templateName='" + templateName + '\'' +
                ", value=" + value +
                '}';
    }
}

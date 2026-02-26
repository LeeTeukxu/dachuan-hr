package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.common.PageEntity;
import lombok.Data;

@Data
public class PageQueryTestDto extends PageEntity
{
    private String keyWord;

    private String nf;

    private String name;
}

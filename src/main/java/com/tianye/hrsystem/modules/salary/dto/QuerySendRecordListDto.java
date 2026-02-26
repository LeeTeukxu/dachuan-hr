package com.tianye.hrsystem.modules.salary.dto;

import com.tianye.hrsystem.common.PageEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuerySendRecordListDto extends PageEntity {

    private Integer year;

    private Integer month;

    @Override
    public String toString() {
        return "QuerySendRecordListBO{" +
                "year=" + year +
                ", month=" + month +
                '}';
    }
}

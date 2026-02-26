package com.tianye.hrsystem.modules.salary.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangeSalaryOptionVO {
    private String name;

    private Integer code;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String value;


    public ChangeSalaryOptionVO(String name, Integer code, String value) {
        this.name = name;
        this.code = code;
        this.value = value;
    }

    @Override
    public String toString() {
        return "ChangeSalaryOptionVO{" +
                "name='" + name + '\'' +
                ", code=" + code +
                ", value='" + value + '\'' +
                '}';
    }
}

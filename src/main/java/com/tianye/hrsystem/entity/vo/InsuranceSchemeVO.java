package com.tianye.hrsystem.entity.vo;

import com.tianye.hrsystem.entity.bo.AddInsuranceSchemeBO;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class InsuranceSchemeVO extends AddInsuranceSchemeBO {

    public Map<String, String> languageKeyMap;

    @Override
    public String toString() {
        return super.toString();
    }
}

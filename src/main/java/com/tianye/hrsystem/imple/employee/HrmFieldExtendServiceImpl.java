package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.tianye.hrsystem.entity.vo.HrmFieldExtend;
import com.tianye.hrsystem.enums.Const;
import com.tianye.hrsystem.enums.FieldEnum;
import com.tianye.hrsystem.repository.hrmFieldExtendRepository;
import com.tianye.hrsystem.service.employee.IHrmFieldExtendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @ClassName: HrmFieldExtendServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月24日 8:16
 **/
@Service
public class HrmFieldExtendServiceImpl implements IHrmFieldExtendService {

    @Autowired
    hrmFieldExtendRepository extendRep;
    @Override
    public List<HrmFieldExtend> queryHrmFieldExtend(Long parentFieldId) {
        List<com.tianye.hrsystem.model.HrmFieldExtend> Fs =
                extendRep.findAllByParentFieldId(Math.toIntExact(parentFieldId));
        String VV=JSON.toJSONString(Fs);
        List<HrmFieldExtend> fieldExtends=JSON.parseArray(VV,HrmFieldExtend.class);
        fieldExtends.forEach(fieldExtend -> recordToFormType(fieldExtend, FieldEnum.parse(fieldExtend.getType())));
        return fieldExtends;
    }

    @Override
    public boolean saveOrUpdateHrmFieldExtend(List<HrmFieldExtend> hrmFieldExtendList, Long parentFieldId, boolean isUpdate) {
        return false;
    }

    @Override
    public boolean deleteHrmFieldExtend(Long parentFieldId) {
        return false;
    }

    private void recordToFormType(HrmFieldExtend record, FieldEnum typeEnum) {
        record.setFormType(typeEnum.getFormType());
        switch (typeEnum) {
            case CHECKBOX:
                record.setDefaultValue(StrUtil.splitTrim((CharSequence) record.getDefaultValue(), Const.SEPARATOR));
            case SELECT:
                if (Objects.equals(record.getRemark(), FieldEnum.OPTIONS_TYPE.getFormType())) {
                    LinkedHashMap<String, Object> optionsData = JSON.parseObject(record.getOptions(), LinkedHashMap.class);
                    record.setOptionsData(optionsData);
                    record.setSetting(new ArrayList<>(optionsData.keySet()));
                } else {
                    if (CollUtil.isEmpty(record.getSetting())) {
                        try {
                            String dtStr = Optional.ofNullable(record.getOptions()).orElse("").toString();
                            List<Object> jsonArrayList = JSON.parseObject(dtStr, List.class);
                            record.setSetting(jsonArrayList);
                        } catch (Exception e) {
                            record.setSetting(new ArrayList<>(StrUtil.splitTrim(record.getOptions(), Const.SEPARATOR)));
                        }
                    }
                }
                break;
            case DATE_INTERVAL:
                String dataValueStr = Optional.ofNullable(record.getDefaultValue()).orElse("").toString();
                if (StrUtil.isNotEmpty(dataValueStr)) {
                    record.setDefaultValue(StrUtil.split(dataValueStr, Const.SEPARATOR));
                }
                break;
            case USER:
            case STRUCTURE:
                record.setDefaultValue(new ArrayList<>(0));
                break;
            case AREA:
            case AREA_POSITION:
            case CURRENT_POSITION:
                String defaultValue = Optional.ofNullable(record.getDefaultValue()).orElse("").toString();
                record.setDefaultValue(JSON.parse(defaultValue));
                break;
            default:
                record.setSetting(new ArrayList<>());
                break;
        }
    }
}

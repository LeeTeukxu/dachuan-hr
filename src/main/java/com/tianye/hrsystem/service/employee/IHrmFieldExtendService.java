package com.tianye.hrsystem.service.employee;

import com.tianye.hrsystem.entity.vo.HrmFieldExtend;

import java.util.List;

public interface IHrmFieldExtendService  {
    /**
     * ??????????
     */
    List<HrmFieldExtend> queryHrmFieldExtend(Long parentFieldId);

    /**
     * ?????????????
     */
    boolean saveOrUpdateHrmFieldExtend(List<HrmFieldExtend> hrmFieldExtendList, Long parentFieldId, boolean isUpdate);

    /**
     * ?????????????
     */
    boolean deleteHrmFieldExtend(Long parentFieldId);
}


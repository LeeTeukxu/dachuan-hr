package com.tianye.hrsystem.modules.holiday.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.modules.holiday.bo.QueryRemainingVacationBO;
import com.tianye.hrsystem.modules.holiday.entity.HrmRemainingVacation;
import com.tianye.hrsystem.modules.holiday.vo.QueryRemainingVacationVO;
import org.apache.ibatis.annotations.Param;

public interface HrmRemainingVacationMapper extends BaseMapper<HrmRemainingVacation> {

    Page<QueryRemainingVacationVO> queryRemainingVacationList(Page<QueryRemainingVacationVO> parse,
                                                              @Param("data")QueryRemainingVacationBO queryRemainingVacationBO);
}

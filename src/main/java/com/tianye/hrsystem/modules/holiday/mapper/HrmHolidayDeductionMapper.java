package com.tianye.hrsystem.modules.holiday.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.modules.holiday.bo.QueryHolidayDeductionBO;
import com.tianye.hrsystem.modules.holiday.entity.HrmHolidayDeduction;
import com.tianye.hrsystem.modules.holiday.vo.QueryHolidayDeductionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HrmHolidayDeductionMapper extends BaseMapper<HrmHolidayDeduction> {

    Page<QueryHolidayDeductionVO> queryHolidayDeductionList(Page<QueryHolidayDeductionVO> parse,
                                                            @Param("data")QueryHolidayDeductionBO queryHolidayDeductionBO);

    List<QueryHolidayDeductionVO> queryHolidayDeduction(@Param("data") QueryHolidayDeductionBO queryHolidayDeductionBO);

    List<QueryHolidayDeductionVO> queryHolidayDeductionBatch(@Param("year") Integer year, @Param("month") Integer month);
}

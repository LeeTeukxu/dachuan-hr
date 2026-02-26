package com.tianye.hrsystem.mapper;


import org.springframework.data.domain.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.model.tbplanlist;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 附件表 Mapper 接口
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-04-27
 */
public interface WorkPlanMapper extends BaseMapper<tbplanlist> {
    List<tbplanlist>  getMaxDate(Map<String, Object> param);
}

package com.tianye.hrsystem.mapper;

import cn.hutool.core.lang.Dict;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.po.HrmActionRecord;
import com.tianye.hrsystem.entity.vo.HrmModelFiledVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * hrm员工操作记录表 Mapper 接口
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface HrmActionRecordMapper extends BaseMapper<HrmActionRecord> {
    /**
     * @param kv 字典值
     * @return
     */
    List<HrmModelFiledVO> queryFieldValue(@Param("data") Dict kv);
}

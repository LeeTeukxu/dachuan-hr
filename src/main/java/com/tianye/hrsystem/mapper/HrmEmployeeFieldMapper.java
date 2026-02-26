package com.tianye.hrsystem.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.entity.po.HrmEmployeeField;
import com.tianye.hrsystem.entity.vo.EmployeeArchivesFieldVO;
import com.tianye.hrsystem.entity.vo.EmployeeHeadFieldVO;
import com.tianye.hrsystem.entity.vo.FiledListVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 自定义字段表 Mapper 接口
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
public interface HrmEmployeeFieldMapper extends BaseMapper<HrmEmployeeField> {

    /**
     * 查询表头展示id
     *
     * @return
     */
    @Select("select field_id from `hrm_employee_field` where is_head_field = 1 order by label_group,sorting")
    List<Long> queryHeadFieldId();

    List<EmployeeHeadFieldVO> queryListHeads(Long userId);

    List<FiledListVO> queryFields();

    List<EmployeeArchivesFieldVO> queryEmployeeArchivesField();

    @InterceptorIgnore(tenantLine = "true")
    public List<Map<String, Object>> initData(Map<String, Object> map);
}

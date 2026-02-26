package com.tianye.hrsystem.modules.salary.mapper;


import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.model.tbdictdata;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryExport;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 工资导出表(HrmSalaryExport)表数据库访问层
 *
 * @author makejava
 * @since 2024-10-01 10:44:55
 */
public interface HrmSalaryExportMapper extends BaseMapper<HrmSalaryExport> {

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    HrmSalaryExport queryById(Integer id);

    /**
     * 统计总行数
     *
     * @param hrmSalaryExport 查询条件
     * @return 总行数
     */
    long count(HrmSalaryExport hrmSalaryExport);

    /**
     * 新增数据
     *
     * @param hrmSalaryExport 实例对象
     * @return 影响行数
     */
    int insert(HrmSalaryExport hrmSalaryExport);

    /**
     * 批量新增数据（MyBatis原生foreach方法）
     *
     * @param entities List<HrmSalaryExport> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<HrmSalaryExport> entities);

    /**
     * 批量新增或按主键更新数据（MyBatis原生foreach方法）
     *
     * @param entities List<HrmSalaryExport> 实例对象列表
     * @return 影响行数
     * @throws org.springframework.jdbc.BadSqlGrammarException 入参是空List的时候会抛SQL语句错误的异常，请自行校验入参
     */
    int insertOrUpdateBatch(@Param("entities") List<HrmSalaryExport> entities);

    /**
     * 修改数据
     *
     * @param hrmSalaryExport 实例对象
     * @return 影响行数
     */
    int update(HrmSalaryExport hrmSalaryExport);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Integer id);

    int deleteByYears(@Param("year") int year,@Param("month") int month);

    /**
     * 获取需要导出数据
     * @param year
     * @param month
     * @return
     */
    List<HrmSalaryExport> queryExportData(@Param("year") int year,@Param("month") int month);

}


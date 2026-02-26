package com.tianye.hrsystem.modules.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryArchivesListDto;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryArchives;
import com.tianye.hrsystem.modules.salary.vo.QueryChangeRecordListVO;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryArchivesListVO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 薪资档案表 Mapper 接口
 * </p>
 *
 * @author hmb
 * @since 2020-11-05
 */
public interface HrmSalaryArchivesMapper extends BaseMapper<HrmSalaryArchives> {

    BasePage<QuerySalaryArchivesListVO> querySalaryArchivesList(BasePage<QuerySalaryArchivesListVO> parse, @Param("data") QuerySalaryArchivesListDto querySalaryArchivesListDto,
                                                                @Param("employeeIds") Collection<Long> employeeIds,
                                                                @Param("year") Integer year,
                                                                @Param("month") Integer month);

    List<QuerySalaryArchivesListVO> queryEmpSalaryArchivesList(@Param("data") QuerySalaryArchivesListDto querySalaryArchivesListDto,
                                                                @Param("employeeIds") Collection<Long> employeeIds,
                                                                @Param("year") Integer year,
                                                                @Param("month") Integer month);

//    List<QueryChangeRecordListVO> queryChangeRecordList(@Param("employeeId") Long employeeId);
//
//    List<ChangeSalaryOptionVO> queryBatchChangeOption();
//
//    List<ExcelTemplateOption> queryFixSalaryExcelExportOption();
//
//    List<ExcelTemplateOption> queryChangeSalaryExcelExportOption();

    List<QueryChangeRecordListVO> queryChangeRecordList(@Param("employeeId") Long employeeId);

}

package com.tianye.hrsystem.modules.dashboard.mapper;

import com.tianye.hrsystem.common.BasePage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据看板聚合查询
 */
public interface DashboardAggMapper {

    Long findCompanyRootDeptId(@Param("companyName") String companyName);

    Map<String, Object> personnelOverview(@Param("end") String end,
                                          @Param("start") String start,
                                          @Param("prevEnd") String prevEnd,
                                          @Param("deptId") Long deptId,
                                          @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> structure(@Param("dim") String dim,
                                        @Param("end") String end,
                                        @Param("deptId") Long deptId,
                                        @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> deptStructure(@Param("end") String end,
                                            @Param("deptId") Long deptId,
                                            @Param("sex") Integer sex,
                                            @Param("edu") Integer edu,
                                            @Param("ageBand") String ageBand,
                                            @Param("tenureBand") String tenureBand,
                                            @Param("entryStatus") Integer entryStatus,
                                            @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> hireTrend(@Param("granule") String granule,
                                        @Param("start") String start,
                                        @Param("end") String end,
                                        @Param("deptId") Long deptId,
                                        @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> quitTrend(@Param("granule") String granule,
                                        @Param("start") String start,
                                        @Param("end") String end,
                                        @Param("deptId") Long deptId,
                                        @Param("quitType") Integer quitType,
                                        @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> keyQuitTrendByDept(@Param("granule") String granule,
                                                 @Param("start") String start,
                                                 @Param("end") String end,
                                                 @Param("deptId") Long deptId,
                                                 @Param("quitType") Integer quitType,
                                                 @Param("rootDeptId") Long rootDeptId);

    Long keyQuitCount(@Param("start") String start,
                      @Param("end") String end,
                      @Param("deptId") Long deptId,
                      @Param("quitType") Integer quitType,
                      @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> crossMatrix(@Param("xDim") String xDim,
                                          @Param("sDim") String sDim,
                                          @Param("end") String end,
                                          @Param("deptId") Long deptId,
                                          @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> flowDeptCompare(@Param("start") String start,
                                              @Param("end") String end,
                                              @Param("deptId") Long deptId,
                                              @Param("quitType") Integer quitType,
                                              @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> quitDist(@Param("dim") String dim,
                                       @Param("start") String start,
                                       @Param("end") String end,
                                       @Param("deptId") Long deptId,
                                       @Param("quitType") Integer quitType,
                                       @Param("rootDeptId") Long rootDeptId);

    BasePage<Map<String, Object>> personnelPageList(BasePage<Map<String, Object>> page,
                                                    @Param("deptId") Long deptId,
                                                    @Param("sex") Integer sex,
                                                    @Param("edu") Integer edu,
                                                    @Param("ageBand") String ageBand,
                                                    @Param("tenureBand") String tenureBand,
                                                    @Param("entryStatus") Integer entryStatus,
                                                    @Param("rootDeptId") Long rootDeptId);

    BasePage<Map<String, Object>> flowPageList(BasePage<Map<String, Object>> page,
                                               @Param("start") String start,
                                               @Param("end") String end,
                                               @Param("deptId") Long deptId,
                                               @Param("quitType") Integer quitType,
                                               @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> salaryOverview(@Param("startYear") Integer startYear,
                                             @Param("startMonth") Integer startMonth,
                                             @Param("endYear") Integer endYear,
                                             @Param("endMonth") Integer endMonth);

    Map<String, Object> bonusOverview(@Param("startYear") Integer startYear,
                                      @Param("startMonth") Integer startMonth,
                                      @Param("endYear") Integer endYear,
                                      @Param("endMonth") Integer endMonth);

    List<Map<String, Object>> salaryTrend(@Param("granule") String granule,
                                          @Param("startYear") Integer startYear,
                                          @Param("startMonth") Integer startMonth,
                                          @Param("endYear") Integer endYear,
                                          @Param("endMonth") Integer endMonth);

    List<Map<String, Object>> salaryInsuranceTrend(@Param("granule") String granule,
                                                   @Param("startYear") Integer startYear,
                                                   @Param("startMonth") Integer startMonth,
                                                   @Param("endYear") Integer endYear,
                                                   @Param("endMonth") Integer endMonth);

    List<Map<String, Object>> salaryDeptCompare(@Param("startYear") Integer startYear,
                                                @Param("startMonth") Integer startMonth,
                                                @Param("endYear") Integer endYear,
                                                @Param("endMonth") Integer endMonth,
                                                @Param("rootDeptId") Long rootDeptId);

    BasePage<Map<String, Object>> salaryEmpDetail(BasePage<Map<String, Object>> page,
                                                  @Param("year") Integer year,
                                                  @Param("month") Integer month,
                                                  @Param("deptId") Long deptId,
                                                  @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> perfTrend(@Param("indicatorType") Integer indicatorType,
                                        @Param("deptId") Long deptId,
                                        @Param("granule") String granule,
                                        @Param("startYear") Integer startYear,
                                        @Param("startMonth") Integer startMonth,
                                        @Param("endYear") Integer endYear,
                                        @Param("endMonth") Integer endMonth,
                                        @Param("rootDeptId") Long rootDeptId);

    List<Map<String, Object>> perfCompletion(@Param("deptId") Long deptId,
                                             @Param("startYear") Integer startYear,
                                             @Param("startMonth") Integer startMonth,
                                             @Param("endYear") Integer endYear,
                                             @Param("endMonth") Integer endMonth,
                                             @Param("rootDeptId") Long rootDeptId);
}

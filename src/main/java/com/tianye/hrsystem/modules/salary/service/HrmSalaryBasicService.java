package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.enums.SystemCodeEnum;
import com.tianye.hrsystem.exception.HrmException;
import com.tianye.hrsystem.mapper.HrmEmployeeMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryArchivesOption;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryBasicMapper;
import com.tianye.hrsystem.modules.salary.support.HrmSalaryBasicDefaults;
import com.tianye.hrsystem.modules.salary.dto.UpdateEmployeeFullAttendanceAmountDto;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryBasicVO;
import com.tianye.hrsystem.modules.salary.dto.QuerySalaryBasicDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * //基本工资表 服务实现类
 */
@Service
public class HrmSalaryBasicService extends BaseServiceImpl<HrmSalaryBasicMapper, HrmSalaryBasic>
{

    @Autowired
    private HrmSalaryBasicMapper hrmSalaryBasicMapper;

    @Autowired
    private HrmEmployeeMapper hrmEmployeeMapper;

    @Autowired
    private HrmSalaryArchivesOptionService hrmSalaryArchivesOptionService;

    private static final int BASIC_SALARY_ARCHIVES_OPTION_CODE = 10101;
    private static final List<Integer> BASIC_SALARY_ARCHIVES_IS_PRO_TYPES = Arrays.asList(0, 1);
    private static final String EXISTS_NOT_DELETED_EMPLOYEE_SQL =
            "exists (select 1 from hrm_employee e where e.employee_id = " +
                    "hrm_salary_archives_option.employee_id and e.is_del = 0)";
    private static final String FULL_ATTENDANCE_TYPE_ORDINARY = "ordinary";
    private static final String FULL_ATTENDANCE_TYPE_LEADER = "leader";

    /**
     * 基本工资列表
     * @param querySalaryBasicDto
     * @return
     */
    public Page<QuerySalaryBasicVO> querySalaryBasic(QuerySalaryBasicDto querySalaryBasicDto)
    {
        Page<QuerySalaryBasicVO> page = hrmSalaryBasicMapper.querySalaryBasic(querySalaryBasicDto.parse(), querySalaryBasicDto);
        return page;
    }

    /**
     * 根据主键ID删除基本工资
     * @param id
     * @return
     */
    public void deleteSalaryBasic(Long id) {
        hrmSalaryBasicMapper.deleteSalaryBasic(id);
    }

    /**
     * 保存基本工资
     * @param querySalaryBasicDto
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveSalaryBasic(QuerySalaryBasicDto querySalaryBasicDto) {
        HrmSalaryBasic salaryBasic = BeanUtil.copyProperties(querySalaryBasicDto, HrmSalaryBasic.class);
        HrmSalaryBasicDefaults.applyTo(salaryBasic);
        LoginUserInfo info = CompanyContext.get();
        LocalDateTime now = LocalDateTime.now();

        if (salaryBasic.getId() != null) {
            LambdaUpdateWrapper<HrmSalaryBasic> wrapper = new LambdaUpdateWrapper<HrmSalaryBasic>()
                    .eq(HrmSalaryBasic::getId, salaryBasic.getId());
            salaryBasic.setUpdateUserId(Long.parseLong(info.getUserId()));
            salaryBasic.setUpdateTime(now);
            update(salaryBasic, wrapper);
        }else {
            salaryBasic.setCreateUserId(Long.parseLong(info.getUserId()));
            salaryBasic.setCreateTime(now);
            save(salaryBasic);
        }
        if (salaryBasic.getSalaryBasic() != null) {
            syncBasicSalaryToEmployeeArchives(salaryBasic.getSalaryBasic(), info, now);
        }
    }

    protected int syncBasicSalaryToEmployeeArchives(BigDecimal salaryBasicAmount, LoginUserInfo info,
                                                    LocalDateTime now) {
        if (salaryBasicAmount == null) {
            return 0;
        }
        Long updateUserId = null;
        if (info != null && info.getUserId() != null) {
            updateUserId = Long.parseLong(info.getUserId());
        }
        return updateBasicSalaryArchiveOptions(salaryBasicAmount, updateUserId, now);
    }

    protected int updateBasicSalaryArchiveOptions(BigDecimal salaryBasicAmount, Long updateUserId, LocalDateTime now) {
        com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper<HrmSalaryArchivesOption> wrapper =
                hrmSalaryArchivesOptionService.lambdaUpdate()
                        .set(HrmSalaryArchivesOption::getValue, salaryBasicAmount.toPlainString())
                        .eq(HrmSalaryArchivesOption::getCode, BASIC_SALARY_ARCHIVES_OPTION_CODE)
                        .in(HrmSalaryArchivesOption::getIsPro, BASIC_SALARY_ARCHIVES_IS_PRO_TYPES)
                        .apply(EXISTS_NOT_DELETED_EMPLOYEE_SQL);
        if (updateUserId != null) {
            wrapper.set(HrmSalaryArchivesOption::getUpdateUserId, updateUserId);
        }
        if (now != null) {
            wrapper.set(HrmSalaryArchivesOption::getUpdateTime, now);
        }
        return wrapper.update() ? 1 : 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateEmployeeFullAttendanceAmount(UpdateEmployeeFullAttendanceAmountDto dto) {
        if (dto == null) {
            throw new HrmException(SystemCodeEnum.SYSTEM_NO_VALID.getCode(), "员工级全勤金额设置参数不能为空");
        }
        BigDecimal amount = dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new HrmException(SystemCodeEnum.SYSTEM_NO_VALID.getCode(), "全勤金额不能为空且不能小于0");
        }
        String amountType = dto.getAmountType() == null ? "" : dto.getAmountType().trim();
        HrmEmployee employee = new HrmEmployee();
        if (FULL_ATTENDANCE_TYPE_ORDINARY.equals(amountType)) {
            employee.setOrdinaryFullAttendanceAmount(amount);
        } else if (FULL_ATTENDANCE_TYPE_LEADER.equals(amountType)) {
            employee.setLeaderFullAttendanceAmount(amount);
        } else {
            throw new HrmException(SystemCodeEnum.SYSTEM_NO_VALID.getCode(), "全勤金额类型错误");
        }
        LoginUserInfo info = CompanyContext.get();
        if (info != null && info.getUserId() != null) {
            employee.setUpdateUserId(Long.parseLong(info.getUserId()));
        }
        employee.setUpdateTime(LocalDateTime.now());

        LambdaUpdateWrapper<HrmEmployee> wrapper = new LambdaUpdateWrapper<HrmEmployee>()
                .eq(HrmEmployee::getIsDel, 0);
        List<Long> employeeIds = normalizeEmployeeFullAttendanceIds(dto.getEmployeeIds());
        boolean explicitEmployeeScope = dto.getEmployeeIds() != null && !dto.getEmployeeIds().isEmpty();
        if (explicitEmployeeScope && employeeIds.isEmpty()) {
            throw new HrmException(SystemCodeEnum.SYSTEM_NO_VALID.getCode(), "员工范围不能为空");
        }
        if (!employeeIds.isEmpty()) {
            wrapper.in(HrmEmployee::getEmployeeId, employeeIds);
        }
        int updatedCount = hrmEmployeeMapper.update(employee, wrapper);
        if (explicitEmployeeScope && updatedCount <= 0) {
            throw new HrmException(SystemCodeEnum.SYSTEM_NO_VALID.getCode(), "所选员工不存在或已删除，未更新任何员工");
        }
        return updatedCount;
    }

    private List<Long> normalizeEmployeeFullAttendanceIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return employeeIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 根据id查询基本工资
     * @param id
     * @return
     */
    public QuerySalaryBasicVO queryById(String id) {
        QuerySalaryBasicVO querySalaryBasicVO = new QuerySalaryBasicVO();
        HrmSalaryBasic hrmSalaryBasic = getById(id);
        if (hrmSalaryBasic == null) {
            HrmSalaryBasicDefaults.applyTo(querySalaryBasicVO);
            return querySalaryBasicVO;
        }
        fillSalaryBasicVO(querySalaryBasicVO, hrmSalaryBasic);
        return querySalaryBasicVO;
    }

    /**
     * 查询基本工资
     * @param id
     * @return
     */
    public QuerySalaryBasicVO findAll() {
        QuerySalaryBasicVO querySalaryBasicVO = new QuerySalaryBasicVO();
        List<HrmSalaryBasic> findOne = list();
        if (findOne.size() <= 0) {
            HrmSalaryBasicDefaults.applyTo(querySalaryBasicVO);
            return querySalaryBasicVO;
        }
        HrmSalaryBasic latestSalaryBasic = selectLatestSalaryBasic(findOne);
        if (latestSalaryBasic == null) {
            HrmSalaryBasicDefaults.applyTo(querySalaryBasicVO);
            return querySalaryBasicVO;
        }
        fillSalaryBasicVO(querySalaryBasicVO, latestSalaryBasic);
        return querySalaryBasicVO;
    }

    private HrmSalaryBasic selectLatestSalaryBasic(List<HrmSalaryBasic> salaryBasics) {
        return salaryBasics.stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparing(HrmSalaryBasic::getCreateTime, Comparator.nullsFirst(LocalDateTime::compareTo))
                        .thenComparing(HrmSalaryBasic::getId, Comparator.nullsFirst(Long::compareTo)))
                .orElse(null);
    }

    private void fillSalaryBasicVO(QuerySalaryBasicVO querySalaryBasicVO, HrmSalaryBasic hrmSalaryBasic) {
        querySalaryBasicVO.setId(hrmSalaryBasic.getId());
        querySalaryBasicVO.setDeptId(hrmSalaryBasic.getDeptId());
        querySalaryBasicVO.setSalaryBasic(hrmSalaryBasic.getSalaryBasic());
        querySalaryBasicVO.setOvertimePay(hrmSalaryBasic.getOvertimePay());
        querySalaryBasicVO.setSubsidy(hrmSalaryBasic.getSubsidy());
        querySalaryBasicVO.setOrdinaryFullAttendanceAmount(hrmSalaryBasic.getOrdinaryFullAttendanceAmount());
        querySalaryBasicVO.setLeaderFullAttendanceAmount(hrmSalaryBasic.getLeaderFullAttendanceAmount());
        querySalaryBasicVO.setProductionMonthlyRestDays(hrmSalaryBasic.getProductionMonthlyRestDays());
        querySalaryBasicVO.setLargeMedicalInsuranceAmount(hrmSalaryBasic.getLargeMedicalInsuranceAmount());
        querySalaryBasicVO.setLongTermCareInsuranceAmount(hrmSalaryBasic.getLongTermCareInsuranceAmount());
        HrmSalaryBasicDefaults.applyTo(querySalaryBasicVO);
    }
}

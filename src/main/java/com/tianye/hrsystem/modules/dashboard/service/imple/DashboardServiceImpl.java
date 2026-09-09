package com.tianye.hrsystem.modules.dashboard.service.imple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.modules.dashboard.bo.DashboardQueryBO;
import com.tianye.hrsystem.modules.dashboard.bo.KeyPostSaveBO;
import com.tianye.hrsystem.modules.dashboard.bo.PerfIndicatorBO;
import com.tianye.hrsystem.modules.dashboard.entity.HrmDashboardRolePermission;
import com.tianye.hrsystem.modules.dashboard.entity.HrmKeyPostConfig;
import com.tianye.hrsystem.modules.dashboard.entity.HrmPerformanceIndicator;
import com.tianye.hrsystem.modules.dashboard.entity.HrmUserDashboardConfig;
import com.tianye.hrsystem.modules.dashboard.mapper.DashboardAggMapper;
import com.tianye.hrsystem.modules.dashboard.mapper.HrmDashboardRolePermissionMapper;
import com.tianye.hrsystem.modules.dashboard.mapper.HrmKeyPostConfigMapper;
import com.tianye.hrsystem.modules.dashboard.mapper.HrmPerformanceIndicatorMapper;
import com.tianye.hrsystem.modules.dashboard.mapper.HrmUserDashboardConfigMapper;
import com.tianye.hrsystem.modules.dashboard.service.DashboardPermissionSupport;
import com.tianye.hrsystem.modules.dashboard.service.DashboardService;
import com.tianye.hrsystem.modules.company.service.TbCompanyListService;
import com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO;
import com.tianye.hrsystem.modules.loginuser.service.TbLoginUserService;
import com.tianye.hrsystem.modules.role.bo.QueryRoleTypesBO;
import com.tianye.hrsystem.modules.role.service.TbRoleTypesService;
import com.tianye.hrsystem.modules.role.vo.QueryRoleTypesVO;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.tbmenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板实现
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String DASHBOARD_PERMISSION_MENU_PATH = "/hrm/system/dashboardPermission";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<Integer, String> EDU_NAMES = new HashMap<Integer, String>();

    private static final Map<Integer, String> REASON_NAMES = new HashMap<Integer, String>();

    @Autowired
    private TbLoginUserService loginUserService;

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    static {
        EDU_NAMES.put(1, "小学");
        EDU_NAMES.put(2, "初中");
        EDU_NAMES.put(3, "中专");
        EDU_NAMES.put(4, "中职");
        EDU_NAMES.put(5, "中技");
        EDU_NAMES.put(6, "高中");
        EDU_NAMES.put(7, "大专");
        EDU_NAMES.put(8, "本科");
        EDU_NAMES.put(9, "硕士");
        EDU_NAMES.put(10, "博士");
        EDU_NAMES.put(11, "博士后");
        EDU_NAMES.put(12, "其他");

        REASON_NAMES.put(1, "家庭原因");
        REASON_NAMES.put(2, "身体原因");
        REASON_NAMES.put(3, "薪资待遇");
        REASON_NAMES.put(4, "交通不便");
        REASON_NAMES.put(5, "工作压力");
        REASON_NAMES.put(6, "管理问题");
        REASON_NAMES.put(7, "无晋升空间");
        REASON_NAMES.put(8, "职业规划");
        REASON_NAMES.put(9, "合同到期放弃续签");
        REASON_NAMES.put(10, "其他个人原因");
        REASON_NAMES.put(11, "试用期辞退");
        REASON_NAMES.put(12, "违反条例");
        REASON_NAMES.put(13, "组织调整/裁员");
        REASON_NAMES.put(14, "绩效不达标辞退");
        REASON_NAMES.put(15, "合同到期不续签");
        REASON_NAMES.put(16, "其他被动原因");
    }

    @Resource
    private DashboardAggMapper dashboardAggMapper;

    @Resource
    private HrmPerformanceIndicatorMapper performanceIndicatorMapper;

    @Resource
    private HrmKeyPostConfigMapper keyPostConfigMapper;

    @Resource
    private HrmUserDashboardConfigMapper userDashboardConfigMapper;

    @Resource
    private TbCompanyListService companyService;

    @Resource
    private HrmDashboardRolePermissionMapper rolePermissionMapper;

    @Resource
    private DashboardPermissionSupport dashboardPermissionSupport;

    @Resource
    private TbRoleTypesService roleTypesService;

    /**
     * 如果 BO 指定了 companyId，临时切换公司上下文执行查询
     */
    private <T> T withCompanyContext(DashboardQueryBO bo, java.util.function.Supplier<T> work) {
        String cid = bo.getCompanyId();
        if (cid == null || cid.isEmpty()) {
            return work.get();
        }
        LoginUserInfo original = CompanyContext.get();
        CompanyContext.set(copyContextForCompany(original, cid));
        try {
            return work.get();
        } finally {
            CompanyContext.set(original);
        }
    }

    /**
     * 根据公司名查找部门树根节点 dept_id，用于过滤子公司数据
     */
    private Long resolveCompanyRootDeptId(String companyName) {
        if (companyName == null || companyName.isEmpty()) return null;
        return dashboardAggMapper.findCompanyRootDeptId(companyName);
    }

    @Override
    public Map<String, Object> personnelOverview(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            return dashboardAggMapper.personnelOverview(p.end, p.start, p.prevEnd, bo.getDeptId(), rootDeptId);
        });
    }

    @Override
    public Map<String, Object> flowOverview(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            Map<String, Object> result = dashboardAggMapper.personnelOverview(p.end, p.start, p.prevEnd, bo.getDeptId(), rootDeptId);
            long prevHired = sumCnt(dashboardAggMapper.hireTrend("%Y-%m", p.prevStart, p.prevEnd, bo.getDeptId(), rootDeptId));
            long prevQuit = sumCnt(dashboardAggMapper.quitTrend("%Y-%m", p.prevStart, p.prevEnd, bo.getDeptId(), bo.getQuitType(), rootDeptId));
            result.put("prevHiredCount", prevHired);
            result.put("prevQuitCount", prevQuit);
            Long keyLost = dashboardAggMapper.keyQuitCount(p.start, p.end, bo.getDeptId(), bo.getQuitType(), rootDeptId);
            result.put("keyLostCount", keyLost == null ? 0L : keyLost);
            Map<String, Object> typeDist = new HashMap<>();
            for (Map<String, Object> row : dashboardAggMapper.quitDist("type", p.start, p.end, bo.getDeptId(), null, rootDeptId)) {
                typeDist.put(String.valueOf(row.get("name")), ((Number) row.get("value")).longValue());
            }
            result.put("typeDist", typeDist);
            return result;
        });
    }

    private long sumCnt(List<Map<String, Object>> rows) {
        long total = 0L;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object c = row.get("cnt");
                if (c instanceof Number) {
                    total += ((Number) c).longValue();
                }
            }
        }
        return total;
    }

    @Override
    public List<Map<String, Object>> structure(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            String dim = normalizeDim(bo.getDim());
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            if ("dept".equals(dim)) {
                return dashboardAggMapper.deptStructure(Period.of(bo).end, bo.getDeptId(),
                        bo.getSex(), bo.getEdu(), bo.getAgeBand(), bo.getTenureBand(), bo.getEntryStatus(), rootDeptId);
            }
            List<Map<String, Object>> rows = dashboardAggMapper.structure(
                    dim, Period.of(bo).end, bo.getDeptId(), rootDeptId);
            if ("edu".equals(dim)) {
                for (Map<String, Object> row : rows) {
                    Object name = row.get("name");
                    if (name instanceof Number) {
                        row.put("name", EDU_NAMES.getOrDefault(((Number) name).intValue(), "未知"));
                    }
                }
            }
            return rows;
        });
    }

    @Override
    public Map<String, Object> flowTrend(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            String df = dfGranule(bo);
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            Map<String, Object> result = new HashMap<>();
            result.put("hiredRows", dashboardAggMapper.hireTrend(df, p.start, p.end, bo.getDeptId(), rootDeptId));
            result.put("quitRows", dashboardAggMapper.quitTrend(df, p.start, p.end, bo.getDeptId(), bo.getQuitType(), rootDeptId));
            result.put("prevQuitRows", dashboardAggMapper.quitTrend(df, p.prevStart, p.prevEnd, bo.getDeptId(), bo.getQuitType(), rootDeptId));
            result.put("keyDeptRows", dashboardAggMapper.keyQuitTrendByDept(df, p.start, p.end, bo.getDeptId(), bo.getQuitType(), rootDeptId));
            return result;
        });
    }

    @Override
    public Map<String, Object> crossMatrix(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            String xDim = normalizeCrossDim(bo.getDim());
            String sDim = normalizeCrossDim(bo.getStackDim());
            if (xDim.equals(sDim)) {
                sDim = "dept".equals(xDim) ? "edu" : "dept";
            }
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            List<Map<String, Object>> rows = dashboardAggMapper.crossMatrix(xDim, sDim, Period.of(bo).end, bo.getDeptId(), rootDeptId);
            for (Map<String, Object> row : rows) {
                row.put("kx", translateDimValue(xDim, row.get("kx")));
                row.put("ks", translateDimValue(sDim, row.get("ks")));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("xDim", xDim);
            result.put("sDim", sDim);
            result.put("rows", rows);
            return result;
        });
    }

    @Override
    public BasePage<Map<String, Object>> personnelPageList(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            if (bo.getPageType() == null) {
                bo.setPageType(1);
            }
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            return dashboardAggMapper.personnelPageList(page(bo),
                    bo.getDeptId(), bo.getSex(), bo.getEdu(), bo.getAgeBand(), bo.getTenureBand(), bo.getEntryStatus(), rootDeptId);
        });
    }

    @Override
    public List<Map<String, Object>> flowDeptCompare(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            return dashboardAggMapper.flowDeptCompare(p.start, p.end, bo.getDeptId(), bo.getQuitType(), rootDeptId);
        });
    }

    @Override
    public List<Map<String, Object>> quitDist(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            String dim = bo.getDim() == null ? "type" : bo.getDim();
            Period p = Period.of(bo);
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            List<Map<String, Object>> rows = dashboardAggMapper.quitDist(dim, p.start, p.end, bo.getDeptId(), bo.getQuitType(), rootDeptId);
            if ("reason".equals(dim)) {
                for (Map<String, Object> row : rows) {
                    Object name = row.get("name");
                    if (name instanceof Number) {
                        row.put("name", REASON_NAMES.getOrDefault(((Number) name).intValue(), "其他"));
                    }
                }
            }
            return rows;
        });
    }

    @Override
    public BasePage<Map<String, Object>> flowPageList(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            if (bo.getPageType() == null) {
                bo.setPageType(1);
            }
            Period p = Period.of(bo);
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            BasePage<Map<String, Object>> page = page(bo);
            dashboardAggMapper.flowPageList(page, p.start, p.end, bo.getDeptId(), bo.getQuitType(), rootDeptId);
            return page;
        });
    }

    @Override
    public Map<String, Object> salaryOverview(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            Map<String, Object> cur = single(dashboardAggMapper.salaryOverview(p.startYear, p.startMonth, p.endYear, p.endMonth));
            Map<String, Object> prev = single(dashboardAggMapper.salaryOverview(p.prevStartYear, p.prevStartMonth, p.prevEndYear, p.prevEndMonth));
            Map<String, Object> curBonus = dashboardAggMapper.bonusOverview(p.startYear, p.startMonth, p.endYear, p.endMonth);
            Map<String, Object> prevBonus = dashboardAggMapper.bonusOverview(p.prevStartYear, p.prevStartMonth, p.prevEndYear, p.prevEndMonth);
            cur.put("bonus", curBonus == null ? 0 : curBonus.get("bonus"));
            prev.put("bonus", prevBonus == null ? 0 : prevBonus.get("bonus"));
            cur.put("prev", prev);
            return cur;
        });
    }

    @Override
    public List<Map<String, Object>> salaryTrend(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            return dashboardAggMapper.salaryTrend(dfGranule(bo), p.startYear, p.startMonth, p.endYear, p.endMonth);
        });
    }

    @Override
    public List<Map<String, Object>> salaryInsuranceTrend(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            return dashboardAggMapper.salaryInsuranceTrend(dfGranule(bo), p.startYear, p.startMonth, p.endYear, p.endMonth);
        });
    }

    @Override
    public List<Map<String, Object>> salaryDeptCompare(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            return dashboardAggMapper.salaryDeptCompare(p.startYear, p.startMonth, p.endYear, p.endMonth, rootDeptId);
        });
    }

    @Override
    public BasePage<Map<String, Object>> salaryEmpDetail(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            if (bo.getPageType() == null) {
                bo.setPageType(1);
            }
            Integer year = bo.getYear() == null ? LocalDate.now().getYear() : bo.getYear();
            Integer month = bo.getMonth() == null ? LocalDate.now().getMonthValue() : bo.getMonth();
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            return dashboardAggMapper.salaryEmpDetail(page(bo), year, month, bo.getDeptId(), rootDeptId);
        });
    }

    @Override
    public Map<String, Object> perfTrend(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            Integer type = bo.getIndicatorType() == null ? 1 : bo.getIndicatorType();
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            List<Map<String, Object>> rows =
                    dashboardAggMapper.perfTrend(type, bo.getDeptId(), dfGranule(bo), p.startYear, p.startMonth, p.endYear, p.endMonth, rootDeptId);
            for (Map<String, Object> row : rows) {
                attachCompletion(row, type);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("indicatorType", type);
            result.put("rows", rows);
            return result;
        });
    }

    @Override
    public List<Map<String, Object>> perfCompletion(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            Period p = Period.of(bo);
            Long rootDeptId = resolveCompanyRootDeptId(bo.getCompanyName());
            List<Map<String, Object>> rows =
                    dashboardAggMapper.perfCompletion(bo.getDeptId(), p.startYear, p.startMonth, p.endYear, p.endMonth, rootDeptId);
            for (Map<String, Object> row : rows) {
                attachCompletion(row, ((Number) row.get("indicatorType")).intValue());
            }
            return rows;
        });
    }

    private void attachCompletion(Map<String, Object> row, int indicatorType) {
        Number t = (Number) row.get("targetValue");
        Number a = (Number) row.get("actualValue");
        double completion = 0D;
        double tv = t == null ? 0D : t.doubleValue();
        double av = a == null ? 0D : a.doubleValue();
        if (indicatorType == 4) {
            if (av != 0D) {
                completion = tv / av * 100;
            }
        } else if (tv != 0D) {
            completion = av / tv * 100;
        }
        row.put("completion", Math.round(completion * 10D) / 10D);
    }

    @Override
    public List<Map<String, Object>> perfPageList(DashboardQueryBO bo) {
        return withCompanyContext(bo, () -> {
            LambdaQueryWrapper<HrmPerformanceIndicator> qw = new LambdaQueryWrapper<>();
            if (bo.getYear() != null) {
                qw.eq(HrmPerformanceIndicator::getStatYear, bo.getYear());
            }
            if (bo.getMonth() != null && "m".equals(bo.getMode())) {
                qw.eq(HrmPerformanceIndicator::getStatMonth, bo.getMonth());
            }
            if (bo.getDeptId() != null) {
                qw.eq(HrmPerformanceIndicator::getDeptId, bo.getDeptId());
            }
            if (bo.getIndicatorType() != null) {
                qw.eq(HrmPerformanceIndicator::getIndicatorType, bo.getIndicatorType());
            }
            qw.orderByDesc(HrmPerformanceIndicator::getStatYear)
                    .orderByDesc(HrmPerformanceIndicator::getStatMonth)
                    .orderByAsc(HrmPerformanceIndicator::getDeptId)
                    .orderByAsc(HrmPerformanceIndicator::getIndicatorType);
            List<HrmPerformanceIndicator> list = performanceIndicatorMapper.selectList(qw);
        Map<Long, String> deptNames = deptNameMap();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (HrmPerformanceIndicator i : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i.getId());
            row.put("deptId", i.getDeptId());
            row.put("deptName", i.getDeptId() == null ? "全公司" : deptNames.getOrDefault(i.getDeptId(), "未知部门"));
            row.put("statYear", i.getStatYear());
            row.put("statMonth", i.getStatMonth());
            row.put("indicatorType", i.getIndicatorType());
            row.put("targetValue", i.getTargetValue());
            row.put("actualValue", i.getActualValue());
            row.put("remark", i.getRemark());
            int type = i.getIndicatorType() == null ? 1 : i.getIndicatorType();
            double tv = i.getTargetValue() == null ? 0D : i.getTargetValue().doubleValue();
            double av = i.getActualValue() == null ? 0D : i.getActualValue().doubleValue();
            double completion = 0D;
            if (type == 4) {
                if (av != 0D) {
                    completion = tv / av * 100;
                }
            } else if (tv != 0D) {
                completion = av / tv * 100;
            }
            row.put("completion", Math.round(completion * 10D) / 10D);
            rows.add(row);
        }
        return rows;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void perfSave(List<PerfIndicatorBO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (PerfIndicatorBO row : rows) {
            LambdaQueryWrapper<HrmPerformanceIndicator> qw = new LambdaQueryWrapper<>();
            qw.eq(HrmPerformanceIndicator::getStatYear, row.getStatYear())
                    .eq(HrmPerformanceIndicator::getStatMonth, row.getStatMonth())
                    .eq(HrmPerformanceIndicator::getIndicatorType, row.getIndicatorType());
            if (row.getDeptId() == null) {
                qw.isNull(HrmPerformanceIndicator::getDeptId);
            } else {
                qw.eq(HrmPerformanceIndicator::getDeptId, row.getDeptId());
            }
            HrmPerformanceIndicator exist = performanceIndicatorMapper.selectOne(qw);
            HrmPerformanceIndicator entity = new HrmPerformanceIndicator()
                    .setDeptId(row.getDeptId())
                    .setStatYear(row.getStatYear())
                    .setStatMonth(row.getStatMonth())
                    .setIndicatorType(row.getIndicatorType())
                    .setTargetValue(row.getTargetValue())
                    .setActualValue(row.getActualValue())
                    .setRemark(row.getRemark())
                    .setUpdateTime(java.time.LocalDateTime.now());
            if (exist == null) {
                performanceIndicatorMapper.insert(entity);
            } else {
                entity.setId(exist.getId());
                performanceIndicatorMapper.updateById(entity);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void perfDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            performanceIndicatorMapper.deleteById(id);
        }
    }

    @Override
    public List<Map<String, Object>> keyPostList() {
        return keyPostConfigMapper.listPostsWithFlag();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void keyPostSave(KeyPostSaveBO bo) {
        keyPostConfigMapper.delete(null);
        if (bo.getPosts() == null || bo.getPosts().isEmpty()) {
            return;
        }
        Long userId = currentUserId();
        LocalDate now = LocalDate.now();
        for (String post : bo.getPosts()) {
            if (post == null || post.trim().isEmpty()) {
                continue;
            }
            LambdaQueryWrapper<HrmKeyPostConfig> qw = new LambdaQueryWrapper<>();
            qw.eq(HrmKeyPostConfig::getPostName, post.trim());
            if (keyPostConfigMapper.selectCount(qw) > 0) {
                continue;
            }
            HrmKeyPostConfig config = new HrmKeyPostConfig()
                    .setPostName(post.trim())
                    .setCreateUserId(userId)
                    .setCreateTime(now.atStartOfDay());
            keyPostConfigMapper.insert(config);
        }
    }

    @Override
    public String userConfigGet(String boardKey) {
        HrmUserDashboardConfig config = userDashboardConfigMapper.selectOne(
                new LambdaQueryWrapper<HrmUserDashboardConfig>()
                        .eq(HrmUserDashboardConfig::getUserId, currentUserIdStr())
                        .eq(HrmUserDashboardConfig::getBoardKey, boardKey));
        return config == null ? null : config.getConfigJson();
    }

    @Override
    public void userConfigSave(String boardKey, String configJson) {
        String userId = currentUserIdStr();
        HrmUserDashboardConfig exist = userDashboardConfigMapper.selectOne(
                new LambdaQueryWrapper<HrmUserDashboardConfig>()
                        .eq(HrmUserDashboardConfig::getUserId, userId)
                        .eq(HrmUserDashboardConfig::getBoardKey, boardKey));
        HrmUserDashboardConfig entity = new HrmUserDashboardConfig()
                .setUserId(userId)
                .setBoardKey(boardKey)
                .setConfigJson(configJson)
                .setUpdateTime(java.time.LocalDateTime.now());
        if (exist == null) {
            entity.setCreateTime(java.time.LocalDateTime.now());
            userDashboardConfigMapper.insert(entity);
        } else {
            entity.setId(exist.getId());
            userDashboardConfigMapper.updateById(entity);
        }
    }

    private BasePage<Map<String, Object>> page(DashboardQueryBO bo) {
        long pn = bo.getPageNum() == null ? 1L : bo.getPageNum().longValue();
        long ps = bo.getPageSize() == null ? 15L : bo.getPageSize().longValue();
        return new BasePage<>(pn, ps);
    }

    private Long currentUserId() {
        try {
            String uid = CompanyContext.get().getUserId();
            return uid == null ? null : Long.parseLong(uid);
        } catch (Exception e) {
            return null;
        }
    }

    private String currentUserIdStr() {
        try {
            return CompanyContext.get().getUserId();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<Long, String> deptNameMap() {
        Map<Long, String> map = new HashMap<>();
        List<Map<String, Object>> depts = dashboardAggMapper.flowDeptCompare("1900-01-01", "2999-12-31", null, null, null);
        for (Map<String, Object> d : depts) {
            Object name = d.get("deptName");
            if (d.get("deptId") != null && name != null) {
                map.put(((Number) d.get("deptId")).longValue(), name.toString());
            }
        }
        return map;
    }

    private String normalizeDim(String dim) {
        if (dim == null) {
            return "dept";
        }
        switch (dim) {
            case "sex":
            case "edu":
            case "age":
            case "tenure":
            case "employment":
                return dim;
            default:
                return "dept";
        }
    }

    private String normalizeCrossDim(String dim) {
        if (dim == null) {
            return "dept";
        }
        switch (dim) {
            case "sex":
            case "edu":
            case "age":
            case "tenure":
                return dim;
            default:
                return "dept";
        }
    }

    private Object translateDimValue(String dim, Object value) {
        if ("edu".equals(dim) && value instanceof Number) {
            return EDU_NAMES.getOrDefault(((Number) value).intValue(), "未知");
        }
        return value;
    }

    private String dfGranule(DashboardQueryBO bo) {
        return "y".equals(bo.getMode()) ? "%Y" : "%Y-%m";
    }

    private Map<String, Object> single(List<Map<String, Object>> rows) {
        if (rows != null && !rows.isEmpty()) {
            return rows.get(0);
        }
        return new HashMap<>();
    }

    private LoginUserInfo copyContextForCompany(LoginUserInfo source, String companyId) {
        LoginUserInfo target = new LoginUserInfo();
        if (source != null) {
            org.springframework.beans.BeanUtils.copyProperties(source, target);
        }
        target.setCompanyId(companyId);
        if (source == null || !companyId.equals(source.getCompanyId())) {
            target.setCompanyName(null);
        }
        return target;
    }

    /**
     * 集团聚合的公司范围：若 BO 指定了 scopeCompanyIds 白名单则只遍历白名单内公司（员工入口权限收敛），
     * 否则遍历全量公司。
     */
    private List<QueryCompanyListVO> resolveScopeCompanies(DashboardQueryBO bo) {
        List<QueryCompanyListVO> all = companyService.getCompanyList();
        if (bo == null || bo.getScopeCompanyIds() == null || bo.getScopeCompanyIds().isEmpty()) {
            return all;
        }
        List<QueryCompanyListVO> scoped = new ArrayList<>();
        for (QueryCompanyListVO comp : all) {
            if (bo.getScopeCompanyIds().contains(comp.getCompanyId())) {
                scoped.add(comp);
            }
        }
        return scoped;
    }

    @Override
    public boolean isAdminManager() {
        LoginUserInfo user = CompanyContext.get();
        if (user == null || user.getRoleName() == null) {
            return false;
        }
        return "行政经理".equals(user.getRoleName().trim());
    }

    @Override
    public List<QueryCompanyListVO> companyList() {
        return new ArrayList<>(companyService.getCompanyList());
    }

    @Override
    public Map<String, Object> flowOverviewGroup(DashboardQueryBO bo) {
        Period p = Period.of(bo);
        LoginUserInfo original = CompanyContext.get();
        List<QueryCompanyListVO> companies = companyService.getCompanyList();
        Map<String, Object> merged = new HashMap<>();
        long totalHired = 0, totalQuit = 0, totalActive = 0, totalPrevHired = 0, totalPrevQuit = 0;
        double totalAge = 0;
        int ageCount = 0;
        List<Map<String, Object>> companyDetails = new ArrayList<>();

        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                Map<String, Object> one = dashboardAggMapper.personnelOverview(p.end, p.start, p.prevEnd, bo.getDeptId(), null);
                Map<String, Object> detail = new HashMap<>();
                detail.put("companyId", cid);
                detail.put("companyName", comp.getCompanyName());
                long hired = getLong(one, "hiredCount");
                long quit = getLong(one, "quitCount");
                long active = getLong(one, "activeCount");
                detail.put("hiredCount", hired);
                detail.put("quitCount", quit);
                detail.put("activeCount", active);
                totalHired += hired;
                totalQuit += quit;
                totalActive += active;
                companyDetails.add(detail);
            } finally {
                CompanyContext.set(original);
            }
        }

        // Previous period for comparison
        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                long prevHired = sumCnt(dashboardAggMapper.hireTrend("%Y-%m", p.prevStart, p.prevEnd, bo.getDeptId(), null));
                long prevQuit = sumCnt(dashboardAggMapper.quitTrend("%Y-%m", p.prevStart, p.prevEnd, bo.getDeptId(), bo.getQuitType(), null));
                totalPrevHired += prevHired;
                totalPrevQuit += prevQuit;
            } finally {
                CompanyContext.set(original);
            }
        }

        merged.put("hiredCount", totalHired);
        merged.put("quitCount", totalQuit);
        merged.put("activeCount", totalActive);
        merged.put("prevHiredCount", totalPrevHired);
        merged.put("prevQuitCount", totalPrevQuit);
        merged.put("companyDetails", companyDetails);
        return merged;
    }

    @Override
    public Map<String, Object> flowTrendGroup(DashboardQueryBO bo) {
        Period p = Period.of(bo);
        String df = dfGranule(bo);
        LoginUserInfo original = CompanyContext.get();
        List<QueryCompanyListVO> companies = companyService.getCompanyList();

        Map<String, Long> hiredMerged = new LinkedHashMap<>();
        Map<String, Long> quitMerged = new LinkedHashMap<>();

        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                for (Map<String, Object> row : dashboardAggMapper.hireTrend(df, p.start, p.end, bo.getDeptId(), null)) {
                    String period = String.valueOf(row.get("period"));
                    hiredMerged.merge(period, getLong(row, "cnt"), Long::sum);
                }
                for (Map<String, Object> row : dashboardAggMapper.quitTrend(df, p.start, p.end, bo.getDeptId(), bo.getQuitType(), null)) {
                    String period = String.valueOf(row.get("period"));
                    quitMerged.merge(period, getLong(row, "cnt"), Long::sum);
                }
            } finally {
                CompanyContext.set(original);
            }
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> hiredRows = new ArrayList<>();
        hiredMerged.forEach((k, v) -> { Map<String, Object> r = new HashMap<>(); r.put("period", k); r.put("cnt", v); hiredRows.add(r); });
        List<Map<String, Object>> quitRows = new ArrayList<>();
        quitMerged.forEach((k, v) -> { Map<String, Object> r = new HashMap<>(); r.put("period", k); r.put("cnt", v); quitRows.add(r); });
        result.put("hiredRows", hiredRows);
        result.put("quitRows", quitRows);

        // Previous period quit for comparison
        Map<String, Long> prevQuitMerged = new LinkedHashMap<>();
        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                for (Map<String, Object> row : dashboardAggMapper.quitTrend(df, p.prevStart, p.prevEnd, bo.getDeptId(), bo.getQuitType(), null)) {
                    String period = String.valueOf(row.get("period"));
                    prevQuitMerged.merge(period, getLong(row, "cnt"), Long::sum);
                }
            } finally {
                CompanyContext.set(original);
            }
        }
        List<Map<String, Object>> prevQuitRows = new ArrayList<>();
        prevQuitMerged.forEach((k, v) -> { Map<String, Object> r = new HashMap<>(); r.put("period", k); r.put("cnt", v); prevQuitRows.add(r); });
        result.put("prevQuitRows", prevQuitRows);

        return result;
    }

    @Override
    public List<Map<String, Object>> flowDeptCompareGroup(DashboardQueryBO bo) {
        Period p = Period.of(bo);
        LoginUserInfo original = CompanyContext.get();
        List<QueryCompanyListVO> companies = companyService.getCompanyList();
        Map<String, Map<String, Object>> byDept = new LinkedHashMap<>();

        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                for (Map<String, Object> row : dashboardAggMapper.flowDeptCompare(p.start, p.end, bo.getDeptId(), bo.getQuitType(), null)) {
                    String deptName = String.valueOf(row.get("deptName"));
                    byDept.computeIfAbsent(deptName, k -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("deptName", k);
                        m.put("hired", 0L);
                        m.put("quitCnt", 0L);
                        return m;
                    });
                    Map<String, Object> m = byDept.get(deptName);
                    m.put("hired", ((Number) m.get("hired")).longValue() + getLong(row, "hired"));
                    m.put("quitCnt", ((Number) m.get("quitCnt")).longValue() + getLong(row, "quitCnt"));
                }
            } finally {
                CompanyContext.set(original);
            }
        }
        return new ArrayList<>(byDept.values());
    }

    @Override
    public List<Map<String, Object>> flowDistGroup(DashboardQueryBO bo) {
        String dim = bo.getDim() == null ? "type" : bo.getDim();
        Period p = Period.of(bo);
        LoginUserInfo original = CompanyContext.get();
        List<QueryCompanyListVO> companies = companyService.getCompanyList();
        Map<String, Long> distMerged = new LinkedHashMap<>();

        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                for (Map<String, Object> row : dashboardAggMapper.quitDist(dim, p.start, p.end, bo.getDeptId(), bo.getQuitType(), null)) {
                    String name = String.valueOf(row.get("name"));
                    distMerged.merge(name, getLong(row, "value"), Long::sum);
                }
            } finally {
                CompanyContext.set(original);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        distMerged.forEach((k, v) -> { Map<String, Object> r = new HashMap<>(); r.put("name", k); r.put("value", v); result.add(r); });

        // Translate names for reason dim
        if ("reason".equals(dim)) {
            for (Map<String, Object> row : result) {
                Object name = row.get("name");
                if (name instanceof Number) {
                    row.put("name", REASON_NAMES.getOrDefault(((Number) name).intValue(), "其他"));
                }
            }
        }
        return result;
    }

    @Override
    public BasePage<Map<String, Object>> flowPageListGroup(DashboardQueryBO bo) {
        if (bo.getPageType() == null) {
            bo.setPageType(1);
        }
        Period p = Period.of(bo);
        LoginUserInfo original = CompanyContext.get();
        List<QueryCompanyListVO> companies = companyService.getCompanyList();
        List<Map<String, Object>> allRows = new ArrayList<>();

        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            String cname = comp.getCompanyName();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                BasePage<Map<String, Object>> tempPage = new BasePage<>(1L, 10000L);
                dashboardAggMapper.flowPageList(tempPage, p.start, p.end, bo.getDeptId(), bo.getQuitType(), null);
                List<Map<String, Object>> rows = tempPage.getList();
                if (rows != null) {
                    for (Map<String, Object> row : rows) {
                        row.put("companyId", cid);
                        row.put("companyName", cname);
                    }
                    allRows.addAll(rows);
                }
            } finally {
                CompanyContext.set(original);
            }
        }

        // Manual pagination on merged results
        long total = allRows.size();
        long pn = bo.getPageNum() == null ? 1L : bo.getPageNum().longValue();
        long ps = bo.getPageSize() == null ? 15L : bo.getPageSize().longValue();
        long from = (pn - 1) * ps;
        long to = Math.min(from + ps, total);
        List<Map<String, Object>> pageList = from < total ? allRows.subList((int) from, (int) to) : new ArrayList<>();

        BasePage<Map<String, Object>> result = new BasePage<>(pn, ps);
        result.setList(pageList);
        result.setTotal(total);
        return result;
    }

    // ========== 人员信息集团聚合 ==========

    @Override
    public Map<String, Object> personnelGroupOverview(DashboardQueryBO bo) {
        Period p = Period.of(bo);
        LoginUserInfo original = CompanyContext.get();
        List<QueryCompanyListVO> companies = resolveScopeCompanies(bo);

        long totalActive = 0, totalHired = 0, totalQuit = 0;
        List<Map<String, Object>> companyDetails = new ArrayList<>();

        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                Map<String, Object> ov = dashboardAggMapper.personnelOverview(p.end, p.start, p.prevEnd, bo.getDeptId(), null);
                long active = getLong(ov, "activeCount");
                long hired = getLong(ov, "hiredCount");
                long quit = getLong(ov, "quitCount");
                totalActive += active;
                totalHired += hired;
                totalQuit += quit;
                Map<String, Object> detail = new HashMap<>();
                detail.put("companyId", cid);
                detail.put("companyName", comp.getCompanyName());
                detail.put("activeCount", active);
                detail.put("hiredCount", hired);
                detail.put("quitCount", quit);
                companyDetails.add(detail);
            } finally {
                CompanyContext.set(original);
            }
        }

        Map<String, Object> merged = new HashMap<>();
        merged.put("activeCount", totalActive);
        merged.put("hiredCount", totalHired);
        merged.put("quitCount", totalQuit);
        merged.put("companyDetails", companyDetails);
        return merged;
    }

    // ========== 薪酬成本集团聚合 ==========

    @Override
    public Map<String, Object> salaryGroupOverview(DashboardQueryBO bo) {
        Period p = Period.of(bo);
        LoginUserInfo original = CompanyContext.get();
        List<QueryCompanyListVO> companies = resolveScopeCompanies(bo);

        double totalPay = 0, totalNet = 0, totalBonus = 0, totalTax = 0;
        List<Map<String, Object>> companyDetails = new ArrayList<>();

        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                Map<String, Object> cur = single(dashboardAggMapper.salaryOverview(p.startYear, p.startMonth, p.endYear, p.endMonth));
                Map<String, Object> curBonus = dashboardAggMapper.bonusOverview(p.startYear, p.startMonth, p.endYear, p.endMonth);
                // salaryOverview 实际返回 expectedPay/realPay/personalTax（应发/实发/个税），
                // 旧键名 totalPay/totalNet/totalTax 取不到恒为 0，此处修正为正确键名
                double pay = toDouble(cur.get("expectedPay"));
                double net = toDouble(cur.get("realPay"));
                double tax = toDouble(cur.get("personalTax"));
                double bonus = curBonus == null ? 0 : toDouble(curBonus.get("bonus"));
                totalPay += pay;
                totalNet += net;
                totalBonus += bonus;
                totalTax += tax;
                Map<String, Object> detail = new HashMap<>();
                detail.put("companyId", cid);
                detail.put("companyName", comp.getCompanyName());
                detail.put("totalPay", pay);
                detail.put("totalNet", net);
                detail.put("bonus", bonus);
                detail.put("totalTax", tax);
                companyDetails.add(detail);
            } finally {
                CompanyContext.set(original);
            }
        }

        Map<String, Object> merged = new HashMap<>();
        merged.put("totalPay", totalPay);
        merged.put("totalNet", totalNet);
        merged.put("bonus", totalBonus);
        merged.put("totalTax", totalTax);
        merged.put("companyDetails", companyDetails);
        return merged;
    }

    // ========== 绩效考核集团聚合 ==========

    @Override
    public List<Map<String, Object>> perfGroupCompletion(DashboardQueryBO bo) {
        Period p = Period.of(bo);
        LoginUserInfo original = CompanyContext.get();
        List<QueryCompanyListVO> companies = companyService.getCompanyList();

        Map<Integer, double[]> accum = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) accum.put(i, new double[]{0, 0});

        for (QueryCompanyListVO comp : companies) {
            String cid = comp.getCompanyId();
            LoginUserInfo ctx = copyContextForCompany(original, cid);
            CompanyContext.set(ctx);
            try {
                List<Map<String, Object>> rows = dashboardAggMapper.perfCompletion(bo.getDeptId(), p.startYear, p.startMonth, p.endYear, p.endMonth, null);
                for (Map<String, Object> row : rows) {
                    int type = ((Number) row.get("indicatorType")).intValue();
                    double tv = toDouble(row.get("targetValue"));
                    double av = toDouble(row.get("actualValue"));
                    double[] acc = accum.get(type);
                    if (acc != null) { acc[0] += tv; acc[1] += av; }
                }
            } finally {
                CompanyContext.set(original);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Map<Integer, String> typeNames = new HashMap<>();
        typeNames.put(1, "安全"); typeNames.put(2, "质量"); typeNames.put(3, "营收");
        typeNames.put(4, "成本"); typeNames.put(5, "产能");
        Map<Integer, String> typeUnits = new HashMap<>();
        typeUnits.put(1, "%"); typeUnits.put(2, "%"); typeUnits.put(3, "万元");
        typeUnits.put(4, "万元"); typeUnits.put(5, "万件");

        for (Map.Entry<Integer, double[]> e : accum.entrySet()) {
            int type = e.getKey();
            double tv = e.getValue()[0], av = e.getValue()[1];
            double completion = 0D;
            if (type == 4) { if (av != 0D) completion = tv / av * 100; }
            else if (tv != 0D) completion = av / tv * 100;
            Map<String, Object> row = new HashMap<>();
            row.put("indicatorType", type);
            row.put("indicatorName", typeNames.getOrDefault(type, ""));
            row.put("unit", typeUnits.getOrDefault(type, ""));
            row.put("targetValue", tv);
            row.put("actualValue", av);
            row.put("completion", Math.round(completion * 10D) / 10D);
            result.add(row);
        }
        return result;
    }

    private double toDouble(Object v) {
        return v instanceof Number ? ((Number) v).doubleValue() : 0D;
    }

    private long getLong(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    /**
     * 统计周期：按月=自然月；按年=自然年。prev 为上一个等长周期。
     */
    private static class Period {
        String start;
        String end;
        String prevStart;
        String prevEnd;
        Integer startYear;
        Integer startMonth;
        Integer endYear;
        Integer endMonth;
        Integer prevStartYear;
        Integer prevStartMonth;
        Integer prevEndYear;
        Integer prevEndMonth;

        static Period of(DashboardQueryBO bo) {
            int year = bo.getYear() == null ? LocalDate.now().getYear() : bo.getYear();
            boolean byYear = "y".equals(bo.getMode());
            int month = (!byYear && bo.getMonth() != null) ? Math.min(Math.max(bo.getMonth(), 1), 12) : LocalDate.now().getMonthValue();

            LocalDate start;
            LocalDate end;
            // 优先使用新的日期范围参数
            if (bo.getStartDate() != null && bo.getEndDate() != null) {
                start = LocalDate.parse(bo.getStartDate() + "-01");
                YearMonth endYm = YearMonth.parse(bo.getEndDate());
                end = endYm.atEndOfMonth();
            } else if (bo.getWinStartYear() != null && bo.getWinStartMonth() != null
                    && bo.getWinEndYear() != null && bo.getWinEndMonth() != null) {
                start = LocalDate.of(bo.getWinStartYear(), Math.min(Math.max(bo.getWinStartMonth(), 1), 12), 1);
                end = YearMonth.of(bo.getWinEndYear(), Math.min(Math.max(bo.getWinEndMonth(), 1), 12)).atEndOfMonth();
            } else if (byYear) {
                start = LocalDate.of(year, 1, 1);
                end = LocalDate.of(year, 12, 31);
            } else {
                YearMonth ym = YearMonth.of(year, month);
                start = ym.atDay(1);
                end = ym.atEndOfMonth();
            }

            byYear = start.getMonthValue() == 1 && end.getMonthValue() == 12
                    && start.getDayOfMonth() == 1 && end.getDayOfYear() == end.lengthOfYear()
                    && start.getYear() == end.getYear();
            LocalDate prevEnd = start.minusDays(1);
            LocalDate prevStart = byYear ? prevEnd.withDayOfYear(1) : prevEnd.withDayOfMonth(1);

            Period p = new Period();
            p.start = start.format(FMT);
            p.end = end.format(FMT);
            p.prevStart = prevStart.format(FMT);
            p.prevEnd = prevEnd.format(FMT);
            p.startYear = start.getYear();
            p.startMonth = start.getMonthValue();
            p.endYear = end.getYear();
            p.endMonth = end.getMonthValue();
            p.prevStartYear = prevStart.getYear();
            p.prevStartMonth = prevStart.getMonthValue();
            p.prevEndYear = prevEnd.getYear();
            p.prevEndMonth = prevEnd.getMonthValue();
            return p;
        }
    }

    /* ==================== 看板角色权限 ==================== */

    @Override
    public List<Map<String, Object>> dashboardPermissionList() {
        requireDashboardPermissionAdmin();
        Page<QueryRoleTypesVO> rolePage = roleTypesService.queryRoleTypesList(new QueryRoleTypesBO());
        List<QueryRoleTypesVO> roles = rolePage == null ? new ArrayList<>() : rolePage.getRecords();
        Map<String, String> configByRole = new HashMap<>();
        for (HrmDashboardRolePermission row : rolePermissionMapper.selectList(
                new LambdaQueryWrapper<HrmDashboardRolePermission>())) {
            configByRole.put(row.getRoleId(), row.getConfigJson());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (QueryRoleTypesVO role : roles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("roleId", String.valueOf(role.getId()));
            item.put("roleName", role.getName());
            item.put("canUse", role.getCanUse());
            item.put("configJson", configByRole.getOrDefault(String.valueOf(role.getId()), ""));
            list.add(item);
        }
        return list;
    }

    @Override
    public void saveDashboardPermission(String roleId, String configJson) {
        requireDashboardPermissionAdmin();
        if (roleId == null || roleId.isEmpty()) {
            throw new CrmException(400, "角色不能为空");
        }
        LoginUserInfo user = CompanyContext.get();
        String operator = user == null ? null : user.getUserId();
        LocalDateTime now = LocalDateTime.now();
        HrmDashboardRolePermission row = rolePermissionMapper.selectOne(
                new LambdaQueryWrapper<HrmDashboardRolePermission>()
                        .eq(HrmDashboardRolePermission::getRoleId, roleId));
        if (row == null) {
            row = new HrmDashboardRolePermission();
            row.setRoleId(roleId);
            row.setConfigJson(configJson);
            row.setUpdateBy(operator);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            rolePermissionMapper.insert(row);
        } else {
            row.setConfigJson(configJson);
            row.setUpdateBy(operator);
            row.setUpdateTime(now);
            rolePermissionMapper.updateById(row);
        }
        dashboardPermissionSupport.clearCache(roleId);
        // 修改看板权限成功后，强制该角色下所有用户下线（已有令牌立即失效）
        try {
            loginUserService.forceLogoutByRole(Integer.valueOf(roleId));
        } catch (Exception e) {
            log.warn("强制下线看板角色[{}]用户失败: {}", roleId, e.getMessage());
        }
    }

    @Override
    public String effectiveDashboardPermission() {
        LoginUserInfo user = CompanyContext.get();
        String roleId = user == null ? null : user.getRoleId();
        if (roleId == null || roleId.isEmpty()) {
            return "";
        }
        HrmDashboardRolePermission row = rolePermissionMapper.selectOne(
                new LambdaQueryWrapper<HrmDashboardRolePermission>()
                        .eq(HrmDashboardRolePermission::getRoleId, roleId));
        return row == null ? "" : row.getConfigJson();
    }

    private void requireDashboardPermissionAdmin() {
        LoginUserInfo user = CompanyContext.get();
        if (user == null || user.getMenuTree() == null) {
            throw new CrmException(403, "无权限配置看板权限");
        }
        // 拥有权限管理模块（看板权限菜单）的用户即可配置并持久化看板权限
        boolean hasMenu = user.getMenuTree().stream().anyMatch(m -> containsMenuPath(m, DASHBOARD_PERMISSION_MENU_PATH));
        if (!hasMenu) {
            throw new CrmException(403, "无权限配置看板权限");
        }
    }

    /** 递归匹配任意层级菜单 path（菜单层级可能不止两层） */
    private boolean containsMenuPath(tbmenu menu, String targetPath) {
        if (menu == null) {
            return false;
        }
        if (targetPath.equals(menu.getPath())) {
            return true;
        }
        List<tbmenu> children = menu.getChildren();
        if (children == null || children.isEmpty()) {
            return false;
        }
        for (tbmenu child : children) {
            if (containsMenuPath(child, targetPath)) {
                return true;
            }
        }
        return false;
    }
}

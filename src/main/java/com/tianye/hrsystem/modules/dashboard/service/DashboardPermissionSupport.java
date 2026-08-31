package com.tianye.hrsystem.modules.dashboard.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.modules.dashboard.entity.HrmDashboardRolePermission;
import com.tianye.hrsystem.modules.dashboard.mapper.HrmDashboardRolePermissionMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 看板元素权限守卫（元素唯一键 elKey = {board}:{kpi|chart|table|tab}:{key}）
 * 无配置行视为全部可见（向后兼容）；角色配置以隐藏清单形式存储。
 */
@Component
public class DashboardPermissionSupport {

    public static final String TYPE_KPI = "kpi";
    public static final String TYPE_CHART = "chart";
    public static final String TYPE_TABLE = "table";
    public static final String TYPE_TAB = "tab";

    private final ConcurrentHashMap<String, JSONObject> cache = new ConcurrentHashMap<>();

    @Resource
    private HrmDashboardRolePermissionMapper rolePermissionMapper;

    private JSONObject configOf(String roleId) {
        if (roleId == null || roleId.isEmpty()) {
            return null;
        }
        JSONObject cached = cache.get(roleId);
        if (cached != null) {
            return cached;
        }
        HrmDashboardRolePermission row = rolePermissionMapper.selectOne(
                new LambdaQueryWrapper<HrmDashboardRolePermission>()
                        .eq(HrmDashboardRolePermission::getRoleId, roleId));
        JSONObject config = null;
        if (row != null && StringUtils.hasText(row.getConfigJson())) {
            try {
                Object parsed = JSON.parse(row.getConfigJson());
                if (parsed instanceof JSONObject) {
                    config = (JSONObject) parsed;
                }
            } catch (Exception ignored) {
                config = null;
            }
        }
        if (config != null) {
            cache.put(roleId, config);
        } else {
            cache.remove(roleId);
        }
        return config;
    }

    private String currentRoleId() {
        LoginUserInfo user = CompanyContext.get();
        String roleId = user == null ? null : user.getRoleId();
        return roleId == null || roleId.isEmpty() ? null : roleId;
    }

    /** 默认全部可见：找不到配置或配置里不包含该元素时视为可见 */
    private boolean isHidden(List<Object> hiddenKeys, String key) {
        if (hiddenKeys == null || key == null) {
            return false;
        }
        return hiddenKeys.contains(key);
    }

    private List<Object> hiddenTabs(JSONObject config) {
        if (config == null) {
            return null;
        }
        JSONArray arr = config.getJSONArray("hiddenTabs");
        if (arr == null) {
            return null;
        }
        try {
            return arr.toJavaList(Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Object> hiddenList(JSONObject boards, String board, String type) {
        if (boards == null || board == null) {
            return null;
        }
        JSONObject b = boards.getJSONObject(board);
        if (b == null) {
            return null;
        }
        JSONArray arr = b.getJSONArray(type);
        if (arr == null) {
            return null;
        }
        try {
            return arr.toJavaList(Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** 判断当前用户是否被隐藏某个看板元素 */
    public boolean isElementHidden(String elKey) {
        String roleId = currentRoleId();
        JSONObject config = configOf(roleId);
        JSONObject boards = config == null ? null : config.getJSONObject("boards");
        String[] parts = elKey == null ? new String[0] : elKey.split(":");
        if (parts.length < 3) {
            return false;
        }
        String board = parts[0];
        String type = parts[1];
        String key = parts[2];
        if (TYPE_TAB.equals(type)) {
            return isHidden(hiddenTabs(config), key);
        }
        if (isHidden(hiddenTabs(config), board)) {
            return true;
        }
        if (TYPE_KPI.equals(type)) {
            return isHidden(hiddenList(boards, board, "hiddenKpis"), key);
        }
        if (TYPE_CHART.equals(type)) {
            return isHidden(hiddenList(boards, board, "hiddenCharts"), key);
        }
        if (TYPE_TABLE.equals(type)) {
            return isHidden(hiddenList(boards, board, "hiddenTables"), key);
        }
        return false;
    }

    /** 元素被隐藏则抛 403 */
    public void assertPermitted(String elKey) {
        if (isElementHidden(elKey)) {
            throw new CrmException(403, "无权限查看该看板数据");
        }
    }

    /** 一组元素全部被隐藏才抛 403 */
    public void assertAnyPermitted(String... elKeys) {
        if (elKeys == null || elKeys.length == 0) {
            return;
        }
        boolean anyVisible = false;
        for (String elKey : elKeys) {
            if (!isElementHidden(elKey)) {
                anyVisible = true;
                break;
            }
        }
        if (!anyVisible) {
            throw new CrmException(403, "无权限查看该看板数据");
        }
    }

    /** 当前用户某个 board 中被隐藏的 KPI id 集合 */
    public Set<String> hiddenKpis(String board) {
        String roleId = currentRoleId();
        JSONObject config = configOf(roleId);
        JSONObject boards = config == null ? null : config.getJSONObject("boards");
        List<Object> hidden = hiddenList(boards, board, "hiddenKpis");
        if (hidden == null || hidden.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return hidden.stream().map(String::valueOf).collect(Collectors.toSet());
    }

    public void clearCache(String roleId) {
        if (roleId != null) {
            cache.remove(roleId);
        } else {
            cache.clear();
        }
    }
}
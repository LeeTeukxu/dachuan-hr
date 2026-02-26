package com.tianye.hrsystem.imple;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.SetAttendanceRuleBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceGroup;
import com.tianye.hrsystem.entity.po.HrmAttendanceRule;
import com.tianye.hrsystem.entity.vo.AttendanceRulePageListVO;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.enums.HrmCodeEnum;
import com.tianye.hrsystem.enums.IsEnum;
import com.tianye.hrsystem.mapper.HrmAttendanceRuleMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.service.IHrmAttendanceGroupService;
import com.tianye.hrsystem.service.IHrmAttendanceRuleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 打卡规则表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-10
 */
@Service
public class HrmAttendanceRuleServiceImpl extends BaseServiceImpl<HrmAttendanceRuleMapper, HrmAttendanceRule> implements IHrmAttendanceRuleService {
    @Resource
    private HrmAttendanceRuleMapper attendanceRuleMapper;
    @Resource
    private IHrmAttendanceGroupService attendanceGroupService;

    @Override
    public BasePage<AttendanceRulePageListVO> queryAttendanceRulePageList(PageEntity pageEntity) {
        BasePage<HrmAttendanceRule> attendanceRuleBasePage = attendanceRuleMapper.selectPage(pageEntity.parse(), Wrappers.emptyWrapper());
        List<AttendanceRulePageListVO> attendanceRulePageListVOList = new ArrayList<>();
        attendanceRuleBasePage.getList().forEach(attendanceRule -> {
            AttendanceRulePageListVO attendanceRulePageListVO = new AttendanceRulePageListVO();
            BeanUtil.copyProperties(attendanceRule, attendanceRulePageListVO);
            attendanceRulePageListVOList.add(attendanceRulePageListVO);
        });
        BasePage<AttendanceRulePageListVO> page = new BasePage<>(attendanceRuleBasePage.getCurrent(),
                attendanceRuleBasePage.getSize(), attendanceRuleBasePage.getTotal());
        page.setList(attendanceRulePageListVOList);
        return page;
    }

    @Override
    public void setAttendanceRule(SetAttendanceRuleBO attendanceRule) {
        HrmAttendanceRule hrmAttendanceRule = BeanUtil.copyProperties(attendanceRule, HrmAttendanceRule.class);
        LoginUserInfo info = CompanyContext.get();
        if (attendanceRule.getAttendanceRuleId() == null) {
            hrmAttendanceRule.setCreateUserId(info.getUserIdValueL());
            hrmAttendanceRule.setCreateTime(LocalDateTime.now());
        }else {
            hrmAttendanceRule.setUpdateUserId(info.getUserIdValueL());
            hrmAttendanceRule.setUpdateTime(LocalDateTime.now());
        }
        saveOrUpdate(hrmAttendanceRule);
    }

    @Override
    public void verifyAttendanceRuleName(SetAttendanceRuleBO attendanceRule) {
        List<HrmAttendanceRule> attendanceRuleList = lambdaQuery().eq(HrmAttendanceRule::getAttendanceRuleName, attendanceRule.getAttendanceRuleName())
                .ne(attendanceRule.getAttendanceRuleId() != null, HrmAttendanceRule::getAttendanceRuleId, attendanceRule.getAttendanceRuleId()).list();
        if (attendanceRuleList.size() > 0) {
            throw new CrmException(HrmCodeEnum.RULE_NAME_ALREADY_EXISTS, attendanceRuleList.get(0).getAttendanceRuleName());
        }
    }

    @Override
    public OperationLog deleteAttendanceRule(Long attendanceRuleId) {
        HrmAttendanceRule hrmAttendanceRule = attendanceRuleMapper.selectById(attendanceRuleId);
        if (hrmAttendanceRule.getIsDefaultSetting() == IsEnum.YES.getValue()) {
            throw new CrmException(HrmCodeEnum.ATTEND_DEFAULT_CANNOT_BE_DELETED, "考勤规则");
        }
        List<HrmAttendanceGroup> hrmAttendanceGroupList = attendanceGroupService.lambdaQuery().eq(HrmAttendanceGroup::getAttendanceRuleId, attendanceRuleId).list();
        if (CollUtil.isNotEmpty(hrmAttendanceGroupList)) {
            throw new CrmException(HrmCodeEnum.ATTEND_USED_CANNOT_BE_DELETED, "考勤规则");
        }
        attendanceRuleMapper.deleteById(attendanceRuleId);

        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(hrmAttendanceRule.getAttendanceRuleName());
        operationLog.setOperationInfo("删除考勤扣款：" + hrmAttendanceRule.getAttendanceRuleName());
        return operationLog;
    }
}

package com.tianye.hrsystem.modules.attendanceinfo.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.attendanceinfo.bo.QueryAttendanceInfoBO;
import com.tianye.hrsystem.modules.attendanceinfo.entity.HrmAttendanceInfo;
import com.tianye.hrsystem.modules.attendanceinfo.mapper.HrmAttendanceInfoMapper;
import com.tianye.hrsystem.modules.attendanceinfo.vo.QueryAttendanceInfoVO;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryBasic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HrmAttendanceInfoService extends BaseServiceImpl<HrmAttendanceInfoMapper, HrmAttendanceInfo> {
    @Autowired
    HrmAttendanceInfoMapper attendanceInfoMapper;

    /**
     * 根据年月条件查询应出勤天数
     * @param QueryAttendanceInfoBO
     * @return
     */
    public List<QueryAttendanceInfoVO> queryAttendanceInfoByParam(QueryAttendanceInfoBO queryAttendanceInfoBO) {
        return attendanceInfoMapper.queryAttendanceInfoByParam(queryAttendanceInfoBO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAttendanceInfo(QueryAttendanceInfoBO queryAttendanceInfoBO) {
        List<QueryAttendanceInfoBO> queryAttendanceInfoBOS = queryAttendanceInfoBO.getQueryAttendanceInfoBOS();
        for (QueryAttendanceInfoBO attendanceInfoBO : queryAttendanceInfoBOS) {
            HrmAttendanceInfo attendanceInfo = BeanUtil.copyProperties(attendanceInfoBO, HrmAttendanceInfo.class);
            if (attendanceInfo.getAttendanceInfoId() != null) {
                LambdaUpdateWrapper<HrmAttendanceInfo> wrapper = new LambdaUpdateWrapper<HrmAttendanceInfo>()
                        .eq(HrmAttendanceInfo::getAttendanceInfoId, attendanceInfo.getAttendanceInfoId());
                update(attendanceInfo, wrapper);
            }else {
                save(attendanceInfo);
            }
        }
    }
}

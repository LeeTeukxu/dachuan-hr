package com.tianye.hrsystem.imple;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddAttendancePointBO;
import com.tianye.hrsystem.entity.po.HrmAttendancePoint;
import com.tianye.hrsystem.entity.vo.AttendancePointPageListVO;
import com.tianye.hrsystem.mapper.HrmAttendancePointMapper;
import com.tianye.hrsystem.service.IHrmAttendancePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 打卡地址表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-12
 */
@Service
public class HrmAttendancePointServiceImpl extends BaseServiceImpl<HrmAttendancePointMapper, HrmAttendancePoint> implements IHrmAttendancePointService {

    @Autowired
    private HrmAttendancePointMapper attendancePointMapper;

    @Override
    public BasePage<AttendancePointPageListVO> queryAttendancePointPageList(PageEntity pageEntity) {
        BasePage<HrmAttendancePoint> attendancePointBasePage = attendancePointMapper.selectPage(pageEntity.parse(), Wrappers.emptyWrapper());
        List<AttendancePointPageListVO> attendancePointPageListVOList = new ArrayList<>();
        attendancePointBasePage.getList().forEach(attendancePoint -> {
            AttendancePointPageListVO attendancePointPageListVO = new AttendancePointPageListVO();
            BeanUtil.copyProperties(attendancePoint, attendancePointPageListVO);
            attendancePointPageListVOList.add(attendancePointPageListVO);
        });
        BasePage<AttendancePointPageListVO> page = new BasePage<>(attendancePointBasePage.getCurrent(), attendancePointBasePage.getSize(), attendancePointBasePage.getTotal());
        page.setList(attendancePointPageListVOList);
        return page;
    }

    @Override
    public HrmAttendancePoint addAttendancePoint(AddAttendancePointBO attendancePoint) {
        HrmAttendancePoint hrmAttendancePoint = BeanUtil.copyProperties(attendancePoint, HrmAttendancePoint.class);
        save(hrmAttendancePoint);
        return hrmAttendancePoint;
    }
}

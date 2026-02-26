package com.tianye.hrsystem.imple;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.AddAttendanceWifiBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceWifi;
import com.tianye.hrsystem.entity.vo.AttendanceWifiPageListVO;
import com.tianye.hrsystem.mapper.HrmAttendanceWifiMapper;
import com.tianye.hrsystem.service.IHrmAttendanceWifiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 打卡wifi表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-12
 */
@Service
public class HrmAttendanceWifiServiceImpl extends BaseServiceImpl<HrmAttendanceWifiMapper, HrmAttendanceWifi> implements IHrmAttendanceWifiService {
    @Autowired
    private HrmAttendanceWifiMapper attendanceWifiMapper;

    @Override
    public BasePage<AttendanceWifiPageListVO> queryAttendanceWifiPageList(PageEntity pageEntity) {
        BasePage<HrmAttendanceWifi> attendanceWifiBasePage = attendanceWifiMapper.selectPage(pageEntity.parse(), Wrappers.emptyWrapper());
        List<AttendanceWifiPageListVO> attendanceWifiPageListVOList = new ArrayList<>();
        attendanceWifiBasePage.getList().forEach(attendanceWifi -> {
            AttendanceWifiPageListVO attendanceWifiPageListVO = new AttendanceWifiPageListVO();
            BeanUtil.copyProperties(attendanceWifi, attendanceWifiPageListVO);
            attendanceWifiPageListVOList.add(attendanceWifiPageListVO);
        });
        BasePage<AttendanceWifiPageListVO> page = new BasePage<>(attendanceWifiBasePage.getCurrent(),
                attendanceWifiBasePage.getSize(), attendanceWifiBasePage.getTotal());
        page.setList(attendanceWifiPageListVOList);
        return page;
    }

    @Override
    public HrmAttendanceWifi addAttendanceWifi(AddAttendanceWifiBO attendanceWifi) {
        HrmAttendanceWifi hrmAttendanceWifi = BeanUtil.copyProperties(attendanceWifi, HrmAttendanceWifi.class);
        save(hrmAttendanceWifi);
        return hrmAttendanceWifi;
    }
}

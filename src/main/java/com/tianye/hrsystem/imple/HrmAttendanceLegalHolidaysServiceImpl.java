package com.tianye.hrsystem.imple;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.HrmCacheKey;
import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.entity.bo.SetLegalHolidaysBO;
import com.tianye.hrsystem.entity.po.HrmAttendanceLegalHolidays;
import com.tianye.hrsystem.mapper.HrmAttendanceLegalHolidaysMapper;
import com.tianye.hrsystem.service.IHrmAttendanceLegalHolidaysService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 考勤法定节假日 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2021-08-23
 */
@Service
public class HrmAttendanceLegalHolidaysServiceImpl extends BaseServiceImpl<HrmAttendanceLegalHolidaysMapper, HrmAttendanceLegalHolidays> implements IHrmAttendanceLegalHolidaysService {
    @Autowired
    private Redis redis;
    private static final String KEY = HrmCacheKey.HRM_LEGAL_HOLIDAY_CACHE;

    @Override
    public void addLegalHolidays(SetLegalHolidaysBO setLegalHolidaysBo) {
        HrmAttendanceLegalHolidays hrmAttendanceLegalHolidays = BeanUtil.copyProperties(setLegalHolidaysBo,
                HrmAttendanceLegalHolidays.class);
        save(hrmAttendanceLegalHolidays);
        redis.del(KEY);
    }

    @Override
    public HrmAttendanceLegalHolidays checkIsLegalHolidays(LocalDateTime day) {
        HrmAttendanceLegalHolidays hrmAttendanceLegalHolidays = lambdaQuery().select().eq(HrmAttendanceLegalHolidays::getHolidayTime, day).orderByDesc(HrmAttendanceLegalHolidays::getCreateTime).one();
        return hrmAttendanceLegalHolidays;
    }

    /**
     * 查询法定节假日列表
     *
     * @return
     */
    @Override
    public List<HrmAttendanceLegalHolidays> queryLegalHolidayList() {
        List<HrmAttendanceLegalHolidays> list;
//        if (redis.exists(KEY)) {
//            String obj = redis.get(KEY);
//            list = JSON.parseArray(obj, HrmAttendanceLegalHolidays.class);
//            return list;
//        }
        list = lambdaQuery().eq(HrmAttendanceLegalHolidays::getType, 2).list();
//        if (CollUtil.isNotEmpty(list)) {
//            String jsonString = JSON.toJSONString(list);
//            //默认缓存一周
//            redis.setex(KEY, 604800, jsonString);
//        }
        return list;
    }
}

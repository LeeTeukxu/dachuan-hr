package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.po.HrmAttendanceExamine;
import com.tianye.hrsystem.mapper.HrmAttendanceExamineMapper;
import com.tianye.hrsystem.service.IHrmAttendanceExamineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 * <p>
 * 考勤审批设置 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2023-08-07
 */
@Service
public class HrmAttendanceExamineServiceImpl extends BaseServiceImpl<HrmAttendanceExamineMapper, HrmAttendanceExamine> implements IHrmAttendanceExamineService {

    /**
     * 查询字段配置
     *
     * @param id 主键ID
     * @return data
     * @author guomenghao
     * @since 2023-08-07
     */
    @Override
    public HrmAttendanceExamine queryById(Serializable id) {
        return getById(id);
    }

    /**
     * 保存或新增信息
     *
     * @param entity entity
     * @author guomenghao
     * @since 2023-08-07
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOrUpdate(HrmAttendanceExamine entity) {
        saveOrUpdate(entity);
    }

    /**
     * 查询请假审批配置
     *
     * @return
     */
    @Override
    public HrmAttendanceExamine queryHrmAttendanceExamine() {
        HrmAttendanceExamine hrmAttendanceExamine = lambdaQuery().orderByDesc(HrmAttendanceExamine::getCreateTime).last(" limit 1").one();
        return hrmAttendanceExamine;
    }
}

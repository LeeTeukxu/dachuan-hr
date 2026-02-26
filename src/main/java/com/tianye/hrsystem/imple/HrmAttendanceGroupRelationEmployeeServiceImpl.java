package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.base.PageEntity;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.po.HrmAttendanceGroupRelationEmployee;
import com.tianye.hrsystem.mapper.HrmAttendanceGroupRelationEmployeeMapper;
import com.tianye.hrsystem.service.IHrmAttendanceGroupRelationEmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 考勤组关联员工表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2021-11-26
 */
@Service
public class HrmAttendanceGroupRelationEmployeeServiceImpl extends BaseServiceImpl<HrmAttendanceGroupRelationEmployeeMapper, HrmAttendanceGroupRelationEmployee> implements IHrmAttendanceGroupRelationEmployeeService {

    /**
     * 查询字段配置
     *
     * @param id 主键ID
     * @return data
     * @author guomenghao
     * @since 2021-11-26
     */
    @Override
    public HrmAttendanceGroupRelationEmployee queryById(Serializable id) {
        return getById(id);
    }

    /**
     * 保存或新增信息
     *
     * @param entity entity
     * @author guomenghao
     * @since 2021-11-26
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOrUpdate(HrmAttendanceGroupRelationEmployee entity) {
        saveOrUpdate(entity);
    }


    /**
     * 查询所有数据
     *
     * @param search 搜索条件
     * @return list
     * @author guomenghao
     * @since 2021-11-26
     */
    @Override
    public BasePage<HrmAttendanceGroupRelationEmployee> queryPageList(PageEntity search) {
        return lambdaQuery().page(search.parse());
    }

    /**
     * 根据ID列表删除数据
     *
     * @param ids ids
     * @author guomenghao
     * @since 2021-11-26
     */
    @Override
    public void deleteByIds(List<Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        removeByIds(ids);
    }
}

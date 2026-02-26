package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryGroup;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalaryGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 薪资组 服务实现类
 * </p>
 *
 * @author zhangzhiwei
 * @since 2020-05-26
 */
@Service("salaryGroupService")
public class HrmSalaryGroupService extends BaseServiceImpl<HrmSalaryGroupMapper, HrmSalaryGroup> {


}

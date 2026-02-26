package com.tianye.hrsystem.modules.role.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.modules.role.bo.QueryRoleTypesBO;
import com.tianye.hrsystem.modules.role.mapper.TbRoleTypesMapper;
import com.tianye.hrsystem.modules.role.vo.QueryRoleTypesVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class TbRoleTypesService {
    @Autowired
    TbRoleTypesMapper tbRoleTypesMapper;

    public Page<QueryRoleTypesVO> queryRoleTypesList(@RequestBody QueryRoleTypesBO queryRoleTypesBO) {
        return tbRoleTypesMapper.queryRoleTypesList(queryRoleTypesBO.parse(), queryRoleTypesBO);
    }
}

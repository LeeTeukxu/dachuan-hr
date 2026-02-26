package com.tianye.hrsystem.modules.company.service;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.modules.company.mapper.TbCompanyListMapper;
import com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO;
import com.tianye.hrsystem.modules.company.entity.TbCompanyList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TbCompanyListService extends BaseServiceImpl<TbCompanyListMapper, TbCompanyList> {
    @Autowired
    TbCompanyListMapper tbCompanyListMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<QueryCompanyListVO> getCompanyList()
    {
        return tbCompanyListMapper.getCompanyList();
    }
}

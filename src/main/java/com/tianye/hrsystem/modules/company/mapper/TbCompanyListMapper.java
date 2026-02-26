package com.tianye.hrsystem.modules.company.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.company.vo.QueryCompanyListVO;
import com.tianye.hrsystem.modules.company.entity.TbCompanyList;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TbCompanyListMapper extends BaseMapper<TbCompanyList> {

    @Select(value = "Select * from hrsystem.tbcompanylist")
    List<QueryCompanyListVO> getCompanyList();
}

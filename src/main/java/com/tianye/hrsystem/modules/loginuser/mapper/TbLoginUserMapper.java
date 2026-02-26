package com.tianye.hrsystem.modules.loginuser.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.loginuser.bo.QueryLoginUserBO;
import com.tianye.hrsystem.modules.loginuser.entity.TbLoginUser;
import com.tianye.hrsystem.modules.loginuser.vo.QueryLoginUserVO;
import com.tianye.hrsystem.modules.menu.bo.QueryMenuBO;
import com.tianye.hrsystem.modules.menu.entity.TbMenu;
import com.tianye.hrsystem.modules.menu.vo.QueryMenuVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface TbLoginUserMapper extends BaseMapper<TbLoginUser> {
    Page<QueryLoginUserVO> queryLoginUserList(Page<QueryLoginUserVO> parse,
                                         @Param("data") QueryLoginUserBO queryLoginUserBO);

    int Add(QueryLoginUserBO queryLoginUserBO);
}

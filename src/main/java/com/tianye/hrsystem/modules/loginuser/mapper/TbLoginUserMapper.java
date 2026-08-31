package com.tianye.hrsystem.modules.loginuser.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.loginuser.bo.QueryLoginUserBO;
import com.tianye.hrsystem.modules.loginuser.entity.TbLoginUser;
import com.tianye.hrsystem.modules.loginuser.vo.QueryLoginUserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TbLoginUserMapper extends BaseMapper<TbLoginUser> {
    Page<QueryLoginUserVO> queryLoginUserList(Page<QueryLoginUserVO> parse,
                                         @Param("data") QueryLoginUserBO queryLoginUserBO);

    List<String> findSystemCompanyIdsByAccount(@Param("systemDatabase") String systemDatabase, @Param("account") String account);

    int saveSystemLoginAccount(@Param("systemDatabase") String systemDatabase, @Param("data") QueryLoginUserBO queryLoginUserBO);

    int deleteSystemLoginAccount(@Param("systemDatabase") String systemDatabase,
                                 @Param("companyId") String companyId,
                                 @Param("account") String account);
}

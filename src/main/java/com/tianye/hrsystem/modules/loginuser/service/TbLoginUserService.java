package com.tianye.hrsystem.modules.loginuser.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.MD5Utils;
import com.tianye.hrsystem.modules.insurance.entity.HrmInsuranceMonthEmpRecord;
import com.tianye.hrsystem.modules.insurance.mapper.HrmInsuranceMonthEmpRecordMapper;
import com.tianye.hrsystem.modules.loginuser.bo.QueryLoginUserBO;
import com.tianye.hrsystem.modules.loginuser.entity.TbLoginUser;
import com.tianye.hrsystem.modules.loginuser.mapper.TbLoginUserMapper;
import com.tianye.hrsystem.modules.loginuser.vo.QueryLoginUserVO;
import com.tianye.hrsystem.modules.menu.bo.QueryMenuBO;
import com.tianye.hrsystem.modules.menu.mapper.TbMenuMapper;
import com.tianye.hrsystem.modules.menu.vo.QueryMenuVO;
import org.apache.commons.codec.digest.Md5Crypt;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TbLoginUserService extends BaseServiceImpl<TbLoginUserMapper, TbLoginUser> {
    @Autowired
    TbLoginUserMapper tbLoginUserMapper;

    public Page<QueryLoginUserVO> queryLoginUserList(@RequestBody QueryLoginUserBO queryLoginUserBO) {
        return tbLoginUserMapper.queryLoginUserList(queryLoginUserBO.parse(), queryLoginUserBO);
    }

    @Transactional
    public Integer Add(@RequestBody QueryLoginUserBO queryLoginUserBO) {
        Optional<TbLoginUser> findOne = lambdaQuery().eq(TbLoginUser::getId, queryLoginUserBO.getId()).oneOpt();
        TbLoginUser tbLoginUser = new TbLoginUser();
        List<TbLoginUser> result = new ArrayList<>();
        if (findOne.isPresent()) {
            //如果已经添加

        }else {
            tbLoginUserMapper.Add(queryLoginUserBO);
        }
        BeanUtils.copyProperties(queryLoginUserBO, tbLoginUser);
        if (tbLoginUser.getPassword() != null) {
            tbLoginUser.setPassword(MD5Utils.enCode(tbLoginUser.getPassword()));
        }
        tbLoginUser.setAccount(tbLoginUser.getAccount().trim());
        result.add(tbLoginUser);
        saveOrUpdateBatch(result);
        return 0;
    }
}

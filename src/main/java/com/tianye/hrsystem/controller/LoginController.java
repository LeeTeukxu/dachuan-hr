package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.common.JWTTokenUtils;
import com.tianye.hrsystem.common.MD5Utils;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.mapper.LoginUserMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.modules.menu.bo.QueryRoleMenuBO;
import com.tianye.hrsystem.modules.menu.entity.TbMenu;
import com.tianye.hrsystem.model.tbrolemenu;
import com.tianye.hrsystem.modules.menu.service.TbMenuService;
import com.tianye.hrsystem.repository.rolemenuRepository;
import com.tianye.hrsystem.util.MyDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianye.hrsystem.repository.rolemenuRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName: LoginController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 14:27
 **/
@RestController
public class LoginController {
    @Autowired
    LoginUserMapper userMapper;
    @Value("${hrm.system.database}")
    String systemBase;
    @Value("${hrm.system.databasesuffix}")
    String databasesuffix;
    Logger logger= LoggerFactory.getLogger(LoginController.class);

    @Autowired
    rolemenuRepository rolemenuRepository;
    @Autowired
    TbMenuService tbMenuService;

    @PostMapping("/login")
    public successResult Login(String account, String password) {
        successResult result = new successResult();
        String RoleID = "";
        try {
            String CompanyID=userMapper.getCompanyIdByUserName(account,systemBase);
            if(StringUtils.isEmpty(CompanyID)) throw new Exception(account+"在系统中不存在!");

            LoginUserInfo Info= userMapper.getByAcountAndCompanyID(account,CompanyID,databasesuffix);
            if(Info==null){
                throw new Exception(account+"在系统中不存在!");
            }else {

                Boolean CanLogin=Info.getCanLogin();
                if(CanLogin==false){
                    throw  new Exception(account+"已被禁止登录系统!");
                }

                String savedPassword=Info.getPassword();
                String nowPassword= MD5Utils.enCode(password);
                if(nowPassword.equals(savedPassword)==false){
                    throw new Exception("登录密码不正确!");
                }
                Info.setSuffix(databasesuffix);
                Info.setPassword(null);
                String Token= JWTTokenUtils.getToken(Info);
                RoleID = Info.getRoleId();
                Info.setToken(Token);
                result.setData(Info);
            }
        }
        catch(Exception ax){
            result.raiseException(ax);
            ax.printStackTrace();
        }
        return result;
    }
    public successResult GetToken(String account){
        successResult result = new successResult();
        try {
            String CompanyID=userMapper.getCompanyIdByUserName(account,systemBase);
            if(StringUtils.isEmpty(CompanyID)) throw new Exception(account+"在系统中不存在!");

            LoginUserInfo Info= userMapper.getByAcountAndCompanyID(account,CompanyID,databasesuffix);
            if(Info==null){
                throw new Exception(account+"在系统中不存在!");
            }else {

                Boolean CanLogin=Info.getCanLogin();
                if(CanLogin==false){
                    throw  new Exception(account+"已被禁止登录系统!");
                }

                Info.setPassword(null);
                String Token= JWTTokenUtils.getToken(Info);
                Info.setToken(Token);
                result.setData(Info);
            }
        }
        catch(Exception ax){
            result.raiseException(ax);
            ax.printStackTrace();
        }
        return result;
    }
}

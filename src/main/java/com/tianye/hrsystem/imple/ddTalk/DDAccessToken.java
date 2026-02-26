package com.tianye.hrsystem.imple.ddTalk;

import cn.hutool.core.lang.func.Func0;
import com.alibaba.fastjson.JSON;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.ddAccount;
import com.tianye.hrsystem.mapper.ddAccountMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.dingtalk.api.DingTalkClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @ClassName: DDTokenCreator
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月14日 11:46
 **/
@Service
public class DDAccessToken implements IAccessToken {
    @Autowired
    StringRedisTemplate redisRep;
    @Autowired
    ddAccountMapper ddAMapper;
    @Value("${hrm.system.database}")
    String systemBase;
    @Value("${hrm.system.databasesuffix}")
    String databasesuffix;
    String tokenKey = "DDTALK_ACCESS_TOKEN";

    @Override
    public String Refresh() throws ApiException {
        String token = "";
        LoginUserInfo X=CompanyContext.get();
        try {

            String tokenKey1=X.getCompanyId()+"::"+tokenKey;
            if (redisRep.hasKey(tokenKey1) == false) {
                List<ddAccount> accountList= ddAMapper.getAllAccount(systemBase);
                Optional<ddAccount> findOnes= accountList.stream().filter(f->f.getCompanyId().equals(X.getCompanyId())).findFirst();

                if(findOnes.isPresent()){
                    ddAccount one=findOnes.get();
                    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
                    OapiGettokenRequest req = new OapiGettokenRequest();
                    req.setHttpMethod("GET");
                    req.setAppkey(one.getAppKey());
                    req.setAppsecret(one.getAppsecret());
                    OapiGettokenResponse rsp = client.execute(req);
                    token = rsp.getAccessToken();
                    redisRep.opsForValue().set(tokenKey1, token);
                    redisRep.expire(tokenKey1, 7000L, TimeUnit.SECONDS);
                    return token;
                } else throw new ApiException("不存在登录帐号无法进行数据获取!");
            } else {
                token = redisRep.opsForValue().get(tokenKey1);
            }
        } catch (Exception ax) {
            ax.printStackTrace();
            throw ax;
        } finally {
        }
        return token;
    }

    @Override
    public String Refresh(String CompanyID) throws ApiException {
        String token = "";
        try {
            String tokenKey1=CompanyID+"::"+tokenKey;
            if (redisRep.hasKey(tokenKey1) == false) {
                List<ddAccount> accountList= ddAMapper.getAllAccount(systemBase);
                Optional<ddAccount> findOnes= accountList.stream().filter(f->f.getCompanyId().equals(CompanyID)).findFirst();

                if(findOnes.isPresent()){
                    ddAccount one=findOnes.get();
                    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");
                    OapiGettokenRequest req = new OapiGettokenRequest();
                    req.setHttpMethod("GET");
                    req.setAppkey(one.getAppKey());
                    req.setAppsecret(one.getAppsecret());
                    OapiGettokenResponse rsp = client.execute(req);
                    token = rsp.getAccessToken();
                    redisRep.opsForValue().set(tokenKey1, token);
                    redisRep.expire(tokenKey1, 7000L, TimeUnit.SECONDS);
                    return token;
                } else throw new ApiException("不存在登录帐号无法进行数据获取!");
            } else {
                token = redisRep.opsForValue().get(tokenKey1);
            }
        } catch (Exception ax) {
            ax.printStackTrace();
            throw ax;
        } finally {

        }
        return token;
    }



    String AllCompanyKey="All::Company:List";
    public void EachCompany(Consumer<String> EachFun){
        try {
            if(redisRep.hasKey(AllCompanyKey)==false){
                LoginUserInfo Info = new LoginUserInfo();
                Info.setCompanyId("Default");
                Info.setSuffix(databasesuffix);
                CompanyContext.set(Info);
                List<ddAccount> accountList= ddAMapper.selectList(null);
                List<String> Keys=new ArrayList<>();
                for(int i=0;i<accountList.size();i++){
                    ddAccount dd=accountList.get(i);
                    String companyId=dd.getCompanyId();
                    Keys.add(companyId);
                    if(EachFun!=null){
                        EachFun.accept(companyId);
                    }
                }
                redisRep.opsForValue().set(AllCompanyKey,JSON.toJSONString(Keys));
                redisRep.expire(AllCompanyKey,360,TimeUnit.HOURS);
            } else {
                String VV=redisRep.opsForValue().get(AllCompanyKey);
                List<String> Keys= JSON.parseArray(VV,String.class);
                Keys.forEach(companyId->{
                    if(EachFun!=null){
                        EachFun.accept(companyId);
                    }
                });
            }
        }
        catch(Exception ax){
            ax.printStackTrace();
        }
    }

    @Override
    public String GetAdminUser(String CompanyID) throws ApiException {
        List<ddAccount> accountList= ddAMapper.getAllAccount(systemBase);
        Optional<ddAccount> findOnes= accountList.stream().filter(f->f.getCompanyId().equals(CompanyID)).findFirst();
        if(findOnes.isPresent()){
            ddAccount acc=findOnes.get();
            String X=acc.getAdminId();
            if(StringUtils.isEmpty(X)) throw new ApiException("请在HrSystem库的DDAccount指定管理员的专属ID"); else return X;
        } else {
            throw  new ApiException("请在HrSystem库的DDAccount指定管理员的专属ID");
        }
    }

    @Override
    public String GetMessageToken(String CompanyID) throws ApiException {
        List<ddAccount> accountList= ddAMapper.getAllAccount(systemBase);
        Optional<ddAccount> findOnes= accountList.stream().filter(f->f.getCompanyId().equals(CompanyID)).findFirst();
        if(findOnes.isPresent()){
            ddAccount acc=findOnes.get();
            String X=acc.getMessageToken();
            if(StringUtils.isEmpty(X)) throw new ApiException("请在HrSystem库的DDAccount指定信息发布Token"); else return X;
        } else {
            throw  new ApiException("请在HrSystem库的DDAccount指定信息发布Token！");
        }
    }
}

package com.tianye.hrsystem.service.ddTalk;

import com.taobao.api.ApiException;

import java.util.function.Consumer;

/**
 * @ClassName: ITokenRefresh
 * @Author: 肖新民
 * @*TODO:刷新DD的登录Token，获取后放入Redis,没有则获取。
 * @CreateTime: 2024年03月14日 11:34
 **/
public interface IAccessToken {
        String Refresh() throws ApiException;
        String Refresh(String CompanyID) throws ApiException;
        void EachCompany(Consumer<String> EachFun);
        String GetAdminUser(String CompanyID) throws ApiException;
        String GetMessageToken(String CompanyID) throws  ApiException;
}

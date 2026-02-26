package com.tianye.hrsystem.config;


import com.alibaba.fastjson.JSON;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.tianye.hrsystem.common.JWTTokenUtils;
import com.tianye.hrsystem.entity.vo.EmployeeInfo;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Component
public class CompanyInterceptor extends HandlerInterceptorAdapter {
    List<String> skipUrls = Arrays.asList("/hrsystem/login");
    @Value("${hrm.system.databasesuffix}")
    String databasesuffix;
    Logger logger= LoggerFactory.getLogger(CompanyInterceptor.class);
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView
            modelAndView) throws Exception {
        CompanyContext.clear();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String urlPath = request.getRequestURI();
        boolean hasLogin = false;
        if (skipUrls.contains(urlPath)) return true;
        String token = request.getParameter("token");
        if(org.apache.commons.lang.StringUtils.isEmpty(token))
        {
            token= request.getHeader("token");
        }
        successResult result = new successResult();
        response.setCharacterEncoding("utf-8");
        if (StringUtils.isEmpty(token) == false) {
            try {
                LoginUserInfo Info = JWTTokenUtils.GetByToken(token);

                EmployeeInfo Emp=new EmployeeInfo();
                Emp.setDeptId(Info.getDepIdValue());
                Emp.setDeptName(Info.getDepName());
                Emp.setEmployeeName(Info.getUserName());
                CompanyContext.set(Info);
                hasLogin = true;
                logger.info("Hr_"+Info.getCompanyId()+databasesuffix+"的用户:"+Info.getUserName()+"登录成功！");
            } catch (SignatureVerificationException e) {
                e.printStackTrace();
                result.raiseException(new Exception("无效签名！"));
            } catch (TokenExpiredException e) {
                e.printStackTrace();
                result.setMessage("token过期");
            } catch (AlgorithmMismatchException e) {
                e.printStackTrace();
                result.raiseException(new Exception("算法不一致"));
            } catch (Exception e) {
                e.printStackTrace();
                result.raiseException(new Exception("token无效！"));
            }
        } else result.raiseException(new Exception("请输入token"));
        if (hasLogin == false) {
            String V=JSON.toJSONString(result);
            System.out.println(V);
            System.out.println(response.getCharacterEncoding());
            response.getWriter().print(V);
            return false;
        } else return true;
//        return true;
    }
}

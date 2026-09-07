package com.tianye.hrsystem.modules.miniapp;

import com.tianye.hrsystem.controller.MiniAppController;
import com.tianye.hrsystem.modules.miniapp.service.IMiniAppService;
import com.tianye.hrsystem.modules.miniapp.support.MiniAppWxClient;
import com.tianye.hrsystem.modules.miniapp.mapper.MiniAppSystemMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

public class MiniAppLoginApiContractTest {

    @Test
    public void login_shouldAcceptOnlyWechatCode() throws Exception {
        Method controllerMethod = MiniAppController.class.getMethod("login", String.class);
        PostMapping mapping = controllerMethod.getAnnotation(PostMapping.class);
        Assert.assertArrayEquals(new String[]{"/login"}, mapping.value());
        Assert.assertNotNull(IMiniAppService.class.getMethod("login", String.class));
    }

    @Test
    public void bindEmployee_shouldExposeFirstBindingEndpoint() throws Exception {
        Method controllerMethod = MiniAppController.class.getMethod(
                "bindEmployee", String.class, String.class, String.class, String.class);
        PostMapping mapping = controllerMethod.getAnnotation(PostMapping.class);
        Assert.assertArrayEquals(new String[]{"/login/bindEmployee"}, mapping.value());
        Assert.assertNotNull(IMiniAppService.class.getMethod(
                "bindEmployee", String.class, String.class, String.class, String.class));
    }

    @Test
    public void wxClient_shouldNotExposePaidPhoneNumberCapability() {
        for (Method method : MiniAppWxClient.class.getDeclaredMethods()) {
            Assert.assertNotEquals("getPhoneByCode", method.getName());
        }
    }

    @Test
    public void systemMapper_shouldNotExposeLegacyPhoneBindingCapability() {
        for (Method method : MiniAppSystemMapper.class.getDeclaredMethods()) {
            Assert.assertNotEquals("findByOpenid", method.getName());
            Assert.assertNotEquals("findByEmployeeId", method.getName());
            Assert.assertNotEquals("insertBinding", method.getName());
        }
    }
}

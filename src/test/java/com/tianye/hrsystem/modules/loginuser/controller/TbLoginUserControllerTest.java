package com.tianye.hrsystem.modules.loginuser.controller;

import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.modules.loginuser.service.TbLoginUserService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

public class TbLoginUserControllerTest {

    @Test
    public void delete_shouldExposePathAndCallServiceDelete() throws Exception {
        Method method = TbLoginUserController.class.getDeclaredMethod("Delete", Integer.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        Assert.assertNotNull("登录用户控制器必须提供删除接口", postMapping);
        Assert.assertArrayEquals(new String[]{"/Delete/{id}"}, postMapping.value());

        TbLoginUserController controller = new TbLoginUserController();
        TbLoginUserService service = Mockito.mock(TbLoginUserService.class);
        Mockito.when(service.Delete(7)).thenReturn(0);
        ReflectionTestUtils.setField(controller, "tbLoginUserService", service);

        Result result = controller.Delete(7);

        Assert.assertTrue(result.hasSuccess());
        Assert.assertEquals(0, result.getData());
        Mockito.verify(service).Delete(7);
    }
}

package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.entity.bo.GenerateDeptCodeBO;
import com.tianye.hrsystem.entity.vo.Result;
import com.tianye.hrsystem.service.IHrmDeptService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HrmDeptControllerTest {

    @InjectMocks
    private HrmDeptController controller;

    @Mock
    private IHrmDeptService deptService;

    @Test
    public void generateCode_shouldExposePostMappingAndDelegateToService() throws Exception {
        Method method = HrmDeptController.class.getMethod("generateCode", GenerateDeptCodeBO.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        Assert.assertNotNull("组织编码生成接口必须暴露 POST 映射", postMapping);
        Assert.assertArrayEquals("组织编码生成接口路径必须保持前端约定", new String[]{"/generateCode"}, postMapping.value());

        GenerateDeptCodeBO generateDeptCodeBO = new GenerateDeptCodeBO();
        generateDeptCodeBO.setDeptId(1001L);
        when(deptService.generateCode(1001L)).thenReturn("3");

        Result<String> result = controller.generateCode(generateDeptCodeBO);

        Assert.assertEquals("3", result.getData());
        verify(deptService).generateCode(1001L);
    }
}

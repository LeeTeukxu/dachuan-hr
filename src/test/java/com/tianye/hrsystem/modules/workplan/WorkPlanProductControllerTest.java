package com.tianye.hrsystem.modules.workplan;

import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.modules.workplan.bo.SortWorkPlanPositionBO;
import com.tianye.hrsystem.modules.workplan.bo.SortWorkPlanProductBO;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanProductBO;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanPositionBO;
import com.tianye.hrsystem.modules.workplan.controller.WorkPlanProductController;
import com.tianye.hrsystem.modules.workplan.service.IWorkPlanProductService;
import com.tianye.hrsystem.modules.workplan.vo.WorkPlanProductTreeVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkPlanProductControllerTest {

    @InjectMocks
    private WorkPlanProductController controller;

    @Mock
    private IWorkPlanProductService productService;

    @Test
    public void queryTree_shouldReturnConfiguredProducts() {
        WorkPlanProductTreeVO product = new WorkPlanProductTreeVO();
        product.setId(10L);
        product.setProductName("椰子饼");
        when(productService.queryTree()).thenReturn(Collections.singletonList(product));

        successResult result = controller.queryTree();

        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals(1, ((java.util.List) result.getData()).size());
        verify(productService).queryTree();
    }

    @Test
    public void saveProductAndPosition_shouldDelegateToService() {
        SaveWorkPlanProductBO product = new SaveWorkPlanProductBO();
        product.setProductName("椰子饼");
        SaveWorkPlanPositionBO position = new SaveWorkPlanPositionBO();
        position.setProductId(10L);
        position.setPositionName("烘烤");

        Assert.assertTrue(controller.saveProduct(product).getSuccess());
        Assert.assertTrue(controller.savePosition(position).getSuccess());

        verify(productService).saveProduct(product);
        verify(productService).savePosition(position);
    }

    @Test
    public void sortPositions_shouldDelegateToService() {
        SortWorkPlanPositionBO request = new SortWorkPlanPositionBO();
        request.setProductId(10L);
        request.setPositionIds(Arrays.asList(22L, 21L));

        successResult result = controller.sortPositions(request);

        Assert.assertTrue(result.getSuccess());
        verify(productService).sortPositions(10L, Arrays.asList(22L, 21L));
    }

    @Test
    public void sortProducts_shouldDelegateToService() {
        SortWorkPlanProductBO request = new SortWorkPlanProductBO();
        request.setProductIds(Arrays.asList(12L, 10L, 11L));

        successResult result = controller.sortProducts(request);

        Assert.assertTrue(result.getSuccess());
        verify(productService).sortProducts(Arrays.asList(12L, 10L, 11L));
    }
}

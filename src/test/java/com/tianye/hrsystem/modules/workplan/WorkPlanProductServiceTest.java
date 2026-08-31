package com.tianye.hrsystem.modules.workplan;

import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanPositionBO;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanProductBO;
import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanPositionEmployee;
import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanProduct;
import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanProductPosition;
import com.tianye.hrsystem.modules.workplan.service.impl.WorkPlanProductServiceImpl;
import com.tianye.hrsystem.modules.workplan.vo.WorkPlanProductTreeVO;
import com.tianye.hrsystem.repository.HrmWorkPlanPositionEmployeeRepository;
import com.tianye.hrsystem.repository.HrmWorkPlanProductPositionRepository;
import com.tianye.hrsystem.repository.HrmWorkPlanProductRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkPlanProductServiceTest {

    @InjectMocks
    private WorkPlanProductServiceImpl service;

    @Mock
    private HrmWorkPlanProductRepository productRepository;

    @Mock
    private HrmWorkPlanProductPositionRepository positionRepository;

    @Mock
    private HrmWorkPlanPositionEmployeeRepository positionEmployeeRepository;

    @Mock
    private hrmEmployeeRepository employeeRepository;

    @Mock
    private tbattendanceuserRepository attendanceUserRepository;

    @Test
    public void queryTree_shouldReturnProductPositionsEmployeesAndAttendanceUserIds() {
        HrmWorkPlanProduct product = new HrmWorkPlanProduct();
        product.setId(10L);
        product.setProductName("凤梨酥");
        product.setSort(1);

        HrmWorkPlanProductPosition packer = new HrmWorkPlanProductPosition();
        packer.setId(20L);
        packer.setProductId(10L);
        packer.setPositionName("包装");
        packer.setSort(1);

        HrmWorkPlanPositionEmployee employee = new HrmWorkPlanPositionEmployee();
        employee.setId(30L);
        employee.setPositionId(20L);
        employee.setEmployeeId(1001L);
        employee.setEmployeeName("张三");
        employee.setSort(1);

        tbattendanceuser attendanceUser = new tbattendanceuser();
        attendanceUser.setEmpId(1001L);
        attendanceUser.setUserId("ding-zhang-san");
        attendanceUser.setUserName("张三");
        attendanceUser.setGroupId(5001L);

        when(productRepository.findAllByOrderBySortAscIdAsc()).thenReturn(Collections.singletonList(product));
        when(positionRepository.findAllByProductIdInOrderBySortAscIdAsc(Collections.singletonList(10L)))
                .thenReturn(Collections.singletonList(packer));
        when(positionEmployeeRepository.findAllByPositionIdInOrderBySortAscIdAsc(Collections.singletonList(20L)))
                .thenReturn(Collections.singletonList(employee));
        when(attendanceUserRepository.findAllByEmpIdIn(Collections.singletonList(1001L)))
                .thenReturn(Collections.singletonList(attendanceUser));

        List<WorkPlanProductTreeVO> tree = service.queryTree();

        Assert.assertEquals(1, tree.size());
        Assert.assertEquals("凤梨酥", tree.get(0).getProductName());
        Assert.assertEquals(1, tree.get(0).getPositions().size());
        Assert.assertEquals("包装", tree.get(0).getPositions().get(0).getPositionName());
        Assert.assertEquals(1, tree.get(0).getPositions().get(0).getEmployees().size());
        Assert.assertEquals("张三", tree.get(0).getPositions().get(0).getEmployees().get(0).getEmployeeName());
        Assert.assertEquals("ding-zhang-san", tree.get(0).getPositions().get(0).getEmployees().get(0).getUserId());
        Assert.assertEquals("5001", tree.get(0).getPositions().get(0).getEmployees().get(0).getGroupId());
    }

    @Test
    public void savePosition_shouldReplaceEmployeesAndKeepManualEmployeeNames() {
        SaveWorkPlanPositionBO request = new SaveWorkPlanPositionBO();
        request.setProductId(10L);
        request.setPositionName("装盒");

        SaveWorkPlanPositionBO.Employee selected = new SaveWorkPlanPositionBO.Employee();
        selected.setEmployeeId(1002L);
        selected.setEmployeeName("李四");
        selected.setMobile("13800001002");

        SaveWorkPlanPositionBO.Employee manual = new SaveWorkPlanPositionBO.Employee();
        manual.setEmployeeName("临时工A");

        request.setEmployees(Arrays.asList(selected, manual));

        HrmWorkPlanProductPosition savedPosition = new HrmWorkPlanProductPosition();
        savedPosition.setId(21L);
        savedPosition.setProductId(10L);
        savedPosition.setPositionName("装盒");
        when(positionRepository.save(org.mockito.ArgumentMatchers.any(HrmWorkPlanProductPosition.class)))
                .thenReturn(savedPosition);

        service.savePosition(request);

        verify(positionEmployeeRepository).deleteAllByPositionId(21L);
        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(positionEmployeeRepository).saveAll(saveCaptor.capture());
        List savedEmployees = saveCaptor.getValue();
        Assert.assertEquals(2, savedEmployees.size());
        HrmWorkPlanPositionEmployee first = (HrmWorkPlanPositionEmployee) savedEmployees.get(0);
        HrmWorkPlanPositionEmployee second = (HrmWorkPlanPositionEmployee) savedEmployees.get(1);
        Assert.assertEquals(Long.valueOf(1002L), first.getEmployeeId());
        Assert.assertEquals("李四", first.getEmployeeName());
        Assert.assertEquals("临时工A", second.getEmployeeName());
        Assert.assertNull(second.getEmployeeId());
    }

    @Test
    public void saveProduct_shouldAutoGenerateNextSortWhenCreatingWithoutSort() {
        SaveWorkPlanProductBO request = new SaveWorkPlanProductBO();
        request.setProductName("椰蓉卷");

        HrmWorkPlanProduct maxProduct = new HrmWorkPlanProduct();
        maxProduct.setSort(7);
        when(productRepository.findFirstByOrderBySortDescIdDesc()).thenReturn(maxProduct);
        when(productRepository.save(org.mockito.ArgumentMatchers.any(HrmWorkPlanProduct.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveProduct(request);

        ArgumentCaptor<HrmWorkPlanProduct> saveCaptor = ArgumentCaptor.forClass(HrmWorkPlanProduct.class);
        verify(productRepository).save(saveCaptor.capture());
        Assert.assertEquals(Integer.valueOf(8), saveCaptor.getValue().getSort());
    }

    @Test
    public void savePosition_shouldAutoGenerateNextSortWithinProductWhenCreatingWithoutSort() {
        SaveWorkPlanPositionBO request = new SaveWorkPlanPositionBO();
        request.setProductId(10L);
        request.setPositionName("封口");

        HrmWorkPlanProductPosition maxPosition = new HrmWorkPlanProductPosition();
        maxPosition.setSort(4);
        when(positionRepository.findFirstByProductIdOrderBySortDescIdDesc(10L)).thenReturn(maxPosition);

        HrmWorkPlanProductPosition savedPosition = new HrmWorkPlanProductPosition();
        savedPosition.setId(23L);
        savedPosition.setProductId(10L);
        savedPosition.setPositionName("封口");
        when(positionRepository.save(org.mockito.ArgumentMatchers.any(HrmWorkPlanProductPosition.class)))
                .thenReturn(savedPosition);

        service.savePosition(request);

        ArgumentCaptor<HrmWorkPlanProductPosition> saveCaptor = ArgumentCaptor.forClass(HrmWorkPlanProductPosition.class);
        verify(positionRepository).save(saveCaptor.capture());
        Assert.assertEquals(Integer.valueOf(5), saveCaptor.getValue().getSort());
    }

    @Test
    public void sortPositions_shouldPersistSubmittedOrderWithinProduct() {
        HrmWorkPlanProductPosition first = new HrmWorkPlanProductPosition();
        first.setId(21L);
        first.setProductId(10L);
        first.setPositionName("装盒");
        first.setSort(0);
        HrmWorkPlanProductPosition second = new HrmWorkPlanProductPosition();
        second.setId(22L);
        second.setProductId(10L);
        second.setPositionName("封口");
        second.setSort(1);
        when(positionRepository.findAllByProductIdOrderBySortAscIdAsc(10L))
                .thenReturn(Arrays.asList(first, second));

        service.sortPositions(10L, Arrays.asList(22L, 21L));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(positionRepository).saveAll(saveCaptor.capture());
        List savedPositions = saveCaptor.getValue();
        Assert.assertEquals(2, savedPositions.size());
        Assert.assertEquals(Long.valueOf(22L), ((HrmWorkPlanProductPosition) savedPositions.get(0)).getId());
        Assert.assertEquals(Integer.valueOf(0), ((HrmWorkPlanProductPosition) savedPositions.get(0)).getSort());
        Assert.assertEquals(Long.valueOf(21L), ((HrmWorkPlanProductPosition) savedPositions.get(1)).getId());
        Assert.assertEquals(Integer.valueOf(1), ((HrmWorkPlanProductPosition) savedPositions.get(1)).getSort());
    }

    @Test
    public void sortProducts_shouldPersistSubmittedOrderAndAppendUnsubmittedProducts() {
        HrmWorkPlanProduct first = new HrmWorkPlanProduct();
        first.setId(10L);
        first.setProductName("椰子饼");
        first.setSort(0);
        HrmWorkPlanProduct second = new HrmWorkPlanProduct();
        second.setId(11L);
        second.setProductName("凤梨酥");
        second.setSort(1);
        HrmWorkPlanProduct third = new HrmWorkPlanProduct();
        third.setId(12L);
        third.setProductName("椰蓉卷");
        third.setSort(2);
        when(productRepository.findAllByOrderBySortAscIdAsc())
                .thenReturn(Arrays.asList(first, second, third));

        service.sortProducts(Arrays.asList(12L, 10L));

        ArgumentCaptor<List> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(saveCaptor.capture());
        List savedProducts = saveCaptor.getValue();
        Assert.assertEquals(3, savedProducts.size());
        Assert.assertEquals(Long.valueOf(12L), ((HrmWorkPlanProduct) savedProducts.get(0)).getId());
        Assert.assertEquals(Integer.valueOf(0), ((HrmWorkPlanProduct) savedProducts.get(0)).getSort());
        Assert.assertEquals(Long.valueOf(10L), ((HrmWorkPlanProduct) savedProducts.get(1)).getId());
        Assert.assertEquals(Integer.valueOf(1), ((HrmWorkPlanProduct) savedProducts.get(1)).getSort());
        Assert.assertEquals(Long.valueOf(11L), ((HrmWorkPlanProduct) savedProducts.get(2)).getId());
        Assert.assertEquals(Integer.valueOf(2), ((HrmWorkPlanProduct) savedProducts.get(2)).getSort());
    }
}

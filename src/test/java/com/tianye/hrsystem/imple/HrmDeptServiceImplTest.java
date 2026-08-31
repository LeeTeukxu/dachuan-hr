package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.entity.bo.AddDeptBO;
import com.tianye.hrsystem.entity.po.HrmDept;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HrmDeptServiceImplTest {

    @Test
    public void generateCode_shouldReturnSmallestUnusedPositiveCodeFromDatabase() {
        TestableHrmDeptService service = new TestableHrmDeptService();
        service.existingDeptList = Arrays.asList(
                dept(1L, "1"),
                dept(2L, "2"),
                dept(3L, "4"),
                dept(4L, "abc"),
                dept(5L, "0")
        );

        String code = service.generateCode(null);

        Assert.assertEquals("组织编码应基于数据库已有正整数编码取最小未使用值", "3", code);
    }

    @Test
    public void generateCode_shouldExcludeEditingDeptWhenReusingCurrentCode() {
        TestableHrmDeptService service = new TestableHrmDeptService();
        service.existingDeptList = Arrays.asList(
                dept(10L, "2"),
                dept(20L, "1"),
                dept(30L, "3")
        );

        String code = service.generateCode(10L);

        Assert.assertEquals("编辑部门时应排除当前部门，当前编码可用时自动回填原编码", "2", code);
    }

    @Test
    public void addOrUpdate_shouldOverrideSubmittedCodeWithDatabaseGeneratedCode() {
        TestableHrmDeptService service = new TestableHrmDeptService();
        service.existingDeptList = Arrays.asList(
                dept(1L, "1"),
                dept(2L, "2")
        );
        AddDeptBO addDeptBO = new AddDeptBO();
        addDeptBO.setName("生产部");
        addDeptBO.setDeptType(2);
        addDeptBO.setParentId(0L);
        addDeptBO.setCode("99");

        service.addOrUpdate(addDeptBO);

        Assert.assertNotNull(service.savedDept);
        Assert.assertEquals("保存部门时后端必须重新生成编码，避免前端陈旧编码造成重复", "3", service.savedDept.getCode());
    }

    private static HrmDept dept(Long deptId, String code) {
        HrmDept dept = new HrmDept();
        dept.setDeptId(deptId);
        dept.setCode(code);
        return dept;
    }

    private static class TestableHrmDeptService extends HrmDeptServiceImpl {

        private List<HrmDept> existingDeptList = Collections.emptyList();
        private HrmDept savedDept;

        @Override
        public List<HrmDept> list() {
            return existingDeptList;
        }

        @Override
        public boolean saveOrUpdate(HrmDept entity) {
            savedDept = entity;
            return true;
        }
    }
}

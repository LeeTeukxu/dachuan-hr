package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.TreeNode;
import com.tianye.hrsystem.model.tbdictdata;
import com.tianye.hrsystem.repository.tbdictdataRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class tbDictDataServiceImplTest {

    @After
    public void clearContext() {
        CompanyContext.clear();
    }

    @Test
    public void getbyDtIdShouldExposeCustomTypeAndValueFromSnAndName() {
        tbdictdata root = new tbdictdata();
        root.setId(1);
        root.setPid(0);
        root.setSn("自定义类型");
        root.setName("自定义值");

        tbdictdataRepository repository = mock(tbdictdataRepository.class);
        when(repository.findAllByDtid(15)).thenReturn(Arrays.asList(root));

        tbDictDataServiceImpl service = new tbDictDataServiceImpl();
        service.dictRep = repository;

        List<TreeNode> nodes = service.getbyDtId(15, true);

        Assert.assertEquals(1, nodes.size());
        Assert.assertEquals("自定义类型", nodes.get(0).getType());
        Assert.assertEquals("自定义值", nodes.get(0).getText());
    }

    @Test
    public void addShouldPersistCustomTypeAndMergeExistingRowWithoutFixedBuckets() throws Exception {
        LoginUserInfo info = new LoginUserInfo();
        info.setUserId("1001");
        CompanyContext.set(info);

        tbdictdataRepository repository = mock(tbdictdataRepository.class);
        when(repository.save(any(tbdictdata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        tbdictdata existing = new tbdictdata();
        existing.setId(2);
        existing.setPid(8);
        existing.setDtid(16);
        existing.setSn("旧类型");
        existing.setName("旧值");
        existing.setCanUse(1);
        existing.setCreateMan(2002);
        existing.setCreateTime(new Date(1000L));
        when(repository.findById(2)).thenReturn(Optional.of(existing));

        tbDictDataServiceImpl service = new tbDictDataServiceImpl();
        service.dictRep = repository;

        tbdictdata create = new tbdictdata();
        create.setSn("自定义类型");
        create.setName("自定义值");
        service.add(create, null);

        tbdictdata edit = new tbdictdata();
        edit.setId(2);
        edit.setSn("新类型");
        edit.setName("新值");
        service.add(edit, null);

        ArgumentCaptor<tbdictdata> captor = ArgumentCaptor.forClass(tbdictdata.class);
        verify(repository, times(2)).save(captor.capture());
        tbdictdata savedCreate = captor.getAllValues().get(0);
        tbdictdata savedEdit = captor.getAllValues().get(1);

        Assert.assertEquals("自定义类型", savedCreate.getSn());
        Assert.assertEquals("自定义值", savedCreate.getName());
        Assert.assertEquals(Integer.valueOf(15), savedCreate.getDtid());
        Assert.assertEquals(Integer.valueOf(0), savedCreate.getPid());
        Assert.assertEquals(Integer.valueOf(1), savedCreate.getCanUse());
        Assert.assertEquals(Integer.valueOf(1001), savedCreate.getCreateMan());

        Assert.assertEquals("新类型", savedEdit.getSn());
        Assert.assertEquals("新值", savedEdit.getName());
        Assert.assertEquals(Integer.valueOf(16), savedEdit.getDtid());
        Assert.assertEquals(Integer.valueOf(8), savedEdit.getPid());
        Assert.assertEquals(Integer.valueOf(1), savedEdit.getCanUse());
        Assert.assertEquals(Integer.valueOf(2002), savedEdit.getCreateMan());
        Assert.assertEquals(new Date(1000L), savedEdit.getCreateTime());
    }
}

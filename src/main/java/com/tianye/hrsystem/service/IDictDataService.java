package com.tianye.hrsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.entity.bo.DictDataBO;
import com.tianye.hrsystem.entity.vo.DictDataVO;
import com.tianye.hrsystem.model.TreeNode;
import com.tianye.hrsystem.model.tbdictdata;
import com.tianye.hrsystem.model.tbplanlist;

import java.util.List;

/**
 * @ClassName: IDictDataService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月11日 10:56
 **/
public interface IDictDataService {
    List<TreeNode> getbyDtId(Integer DtId,boolean getAllChildren);
    List<TreeNode> getbyPId(Integer PId);
    Integer add(tbdictdata tbdictdata, String AddType) throws Exception;
    Page<DictDataVO> queryDictDataList(DictDataBO dictDataBO);
}

package com.tianye.hrsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.common.ResultCode;
import com.tianye.hrsystem.entity.bo.DictDataBO;
import com.tianye.hrsystem.entity.vo.DictDataVO;
import com.tianye.hrsystem.enums.Result;
import com.tianye.hrsystem.model.TreeNode;
import com.tianye.hrsystem.model.tbdictdata;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.service.IDictDataService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ClassName: DictDataController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月11日 11:27
 **/
@RestController
@RequestMapping("/dict")
public class DictDataController {
    @Autowired
    IDictDataService dictService;

    @RequestMapping("/getAllByDtId")
    public List<TreeNode>getAllByDtId(Integer DtId){
        return dictService.getbyDtId(DtId,true);
    }

    @RequestMapping("/getByDtId")
    public List<TreeNode> getByDtId(Integer DtId){
        return dictService.getbyDtId(DtId,false);
    }

    @RequestMapping("/getByPID")
    public List<TreeNode> getByPid(Integer PID){
        return dictService.getbyPId(PID);
    }

    @RequestMapping("/add")
    public Result add(@RequestBody tbdictdata tbdictdata, @RequestParam("AddType") String AddType) {
        try {
            Integer result = dictService.add(tbdictdata, AddType);
            return Result.ok(result);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"保存失败");
        }
    }

    @RequestMapping("/queryDictDataList")
    @ApiOperation("查询数据字典列表")
    public Result queryDictDataList(@RequestBody DictDataBO dictDataBO) {
        try {
            Page<DictDataVO> result = dictService.queryDictDataList(dictDataBO);
            return Result.ok(result);
        }catch (Exception ax) {
            ax.printStackTrace();
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.code(),"获取失败");
        }
    }
}

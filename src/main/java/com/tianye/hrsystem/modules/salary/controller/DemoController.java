package com.tianye.hrsystem.modules.salary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.model.tbdictdata;
import com.tianye.hrsystem.modules.salary.dto.DictDataQueryDto;
import com.tianye.hrsystem.modules.salary.dto.PageQueryTestDto;
import com.tianye.hrsystem.modules.salary.service.DemoService;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/demo")
@CrossOrigin
public class DemoController
{
    @Autowired
    private DemoService demoService;
//
//    /**
//     * json对象提交
//     * @param dto
//     * @param request
//     * @return
//     */
//    @ResponseBody
//    @RequestMapping(value="/pageTest1" ,method = {RequestMethod.POST})
//    public Result pageTest1(@RequestBody DictDataQueryDto dto, HttpServletRequest request)
//    {
//        Result result = new Result();
//        Page<tbdictdata> pageInfo = demoService.pageQueryTest1(dto);
//        result.setData(pageInfo);
//        result.setCode(ResultCode.SUCCESS);
//        result.setMessage("SUCCESS");
//
//        return result;
//    }
//
//
//    /**
//     * form-data 表单提交
//     * @param keyWord
//     * @param name
//     * @param nf
//     * @return
//     */
//    @PostMapping("/pageTest2")
//    public Result pageTest2(@ApiParam("关键字") @RequestParam("keyWord") String keyWord,
//                            @ApiParam("名称") @RequestParam("name") String name,
//                            @ApiParam("年份") @RequestParam("nf") String nf)
//    {
//        Result result = new Result();
//        PageQueryTestDto dto = new PageQueryTestDto();
//        dto.setKeyWord(keyWord);
//
////        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
////        PageInfo<PageQueryTestVo> pageInfo = demoService.pageQueryTest1(dto);
////        result.setData(pageInfo);
////        result.setCode(ResultCode.SUCCESS);
////        result.setMessage("SUCCESS");
//
//        return result;
//    }
}

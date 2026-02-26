package com.tianye.hrsystem.modules.tempworker.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.google.common.collect.Lists;
import com.tianye.hrsystem.modules.salary.service.SalaryComputeServiceNew;
import com.tianye.hrsystem.modules.salary.vo.QuerySalaryPageListVO;
import com.tianye.hrsystem.modules.tempworker.entity.*;
import com.tianye.hrsystem.modules.tempworker.mapper.TempWorkerExportMapper;
import com.tianye.hrsystem.util.CustomCellWriteHeightConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TempWorkerExportService
{
    @Autowired
    private TempWorkerExportMapper exportMapper;

    public void export(HttpServletResponse response) throws IOException
    {
        SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd");
        List<List<String>> headTitles = Lists.newArrayList();
        headTitles.add(Lists.newArrayList( "","","序号"));
        headTitles.add(Lists.newArrayList( "","","系统工号"));
        headTitles.add(Lists.newArrayList( "","","身份证号"));
        headTitles.add(Lists.newArrayList( "","","银行卡号"));
        headTitles.add(Lists.newArrayList( "","","姓名"));

        List<Tbbreed> tbbreedList = exportMapper.getTbbreedList(null);

        //获取其中的工厂列表
        List<String> workShopList = tbbreedList.stream().map(e -> e.getWorkShop()).collect(Collectors.toList());
        workShopList = workShopList.stream().distinct().collect(Collectors.toList());

        /**
         * 生成表头
         */
        for(String tbworkshop : workShopList)
        {
            //产品列表
             tbbreedList = exportMapper.getTbbreedList(tbworkshop);
            //产品列表
            for(int i=0;i<tbbreedList.size();i++)
            {
                String productName = tbbreedList.get(i).getBreed();
                Integer id = tbbreedList.get(i).getId();
                //根据id查询 做工日期,单价
                List<HashMap<String,Object>> dateList = exportMapper.getEmpWorkDateList(id);
                for(HashMap<String, Object> map : dateList)
                {
                    headTitles.add(Lists.newArrayList(tbworkshop, productName,map.get("WeightTime")+"("+map.get("Price")+"元/kg)"));
                }
                //重量合计
                headTitles.add(Lists.newArrayList(tbworkshop, productName,"重量合计"));

                //金额合计
                headTitles.add(Lists.newArrayList(tbworkshop, productName,"金额合计"));

            }
            //车间 金额总计
            headTitles.add(Lists.newArrayList(tbworkshop, "",tbworkshop+"金额合计"));
        }
        //所有车间总计
        headTitles.add(Lists.newArrayList("","","金额总计"));

        //加载的数据集
        List<List<String>> dataList = ListUtils.newArrayList();
        //加载数据
        List<Tblregister> workerList = exportMapper.getRegisterUserList();

        /**
         * 开始加载数据
         */
        if(CollectionUtil.isNotEmpty(workerList))
        {
            //遍历员工列表
            for(Tblregister worker : workerList)
            {
                Double totalPrice = 0.0d;
                //获取每个临时工的称重数据
                List<Tbweight> workerWeightList = exportMapper.getWorkerWeightList(worker.getIdcardno());
                if(CollectionUtil.isEmpty(workerWeightList))
                {
                    continue;
                }
                List<String> data = ListUtils.newArrayList();
                //遍历每一行的各个单元格
                //1,先处理 基础数据部分
                for(List<String> headTitleList : headTitles)
                {
                    //取出三级标题栏的 标题名称
                    String cellTitle1 = headTitleList.get(0);

                    String cellTitle2 = headTitleList.get(1);

                    String cellTitle3 = headTitleList.get(2);
                    if(cellTitle3.equals("序号"))
                    {
                        data.add(worker.getXh());
                    }
                    if(cellTitle3.equals("系统工号"))
                    {
                        data.add(worker.getWorkNo());
                    }
                    if(cellTitle3.equals("身份证号"))
                    {
                        data.add(worker.getIdcardno());
                    }
                    if(cellTitle3.equals("姓名"))
                    {
                        data.add(worker.getName());
                    }
                    if(cellTitle3.equals("银行卡号"))
                    {
                        data.add(worker.getBankNo());
                    }
                    if(StringUtils.isNotBlank(cellTitle2))
                    {
                        //第二行的标题不为空，开始处理每日称重数据
                        //取出生产线名称
                        String workShopNameTitle = cellTitle1;
                        //取出产品名称
                        String productNameTitle = cellTitle2;
                        //取出日期
                        String dateTitle = cellTitle3;
                        //根据生产线名称，产品名称，日期，取出对应的数值
                        if(CollectionUtil.isNotEmpty(workerWeightList))
                        {
                            Tbweight tbweight = workerWeightList.stream().filter(f -> f.getWorkshop().equals(workShopNameTitle) &&  f.getBreed().equals(productNameTitle) && f.getWeighttimeStr().equals(dateTitle)).findAny().orElse(null);
                            if(tbweight!=null && workShopNameTitle.equals(tbweight.getWorkshop()) && productNameTitle.equals(tbweight.getBreed()) && cellTitle3.equals(tbweight.getWeighttimeStr()))
                            {
                                data.add(String.valueOf(tbweight.getWeight()));
                            }
                            else
                            {
                                //每个产品金额总计
                                ProductWeightSummary weightSummary = exportMapper.getProductWeightSummary(worker.getIdcardno(),cellTitle2);
//                                if(cellTitle3.equals(cellTitle2+"重量合计"))
                                if(cellTitle3.equals("重量合计"))
                                {
                                    if(weightSummary!=null)
                                    {
                                        data.add(String.valueOf(weightSummary.getWeight()));
                                    }
                                    else
                                    {
                                        data.add("");
                                    }

                                }
//                                if(cellTitle3.equals(cellTitle2+"金额合计"))
                                if(cellTitle3.equals("金额合计"))
                                {
                                    if(weightSummary!=null)
                                    {
                                        data.add(String.valueOf(weightSummary.getProductprice()));
                                    }
                                    else
                                    {
                                        data.add("");
                                    }
                                }

                            }

                        }

                    }
                    else
                    {
                        if(cellTitle3.equals(cellTitle1+"金额合计"))
                        {
                            //每个车间金额总计
                            ProductWeightSummary weightSummary = exportMapper.getWorkShopWeightSummary(worker.getIdcardno(),cellTitle1);
                            if(weightSummary!=null)
                            {
                                data.add(String.valueOf(weightSummary.getProductprice()));
                                //累计总金额
                                totalPrice+=weightSummary.getProductprice();
                            }
                            else
                            {
                                data.add("");
                            }
                        }
                        if(cellTitle3.equals("金额总计"))
                        {
                            data.add(String.valueOf(totalPrice));
                        }
                    }

                }
                dataList.add(data);
            }
        }


        //内容样式策略
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        //垂直居中,水平居中
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        //设置 自动换行
        contentWriteCellStyle.setWrapped(false);

        // 字体策略
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short) 10);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        //头策略使用默认 设置字体大小
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontHeightInPoints((short) 10);
        headWriteCellStyle.setWriteFont(headWriteFont);


        String fileName = "称重_"+System.currentTimeMillis()+".xlsx";
        fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;fileName=" + fileName);
        // 需要写入文件夹就是用 DileOutPutStram
        // 如果是接口请求直接浏览器下载，就使用 response.getOutputStream()
//        InputStream is = SalaryComputeServiceNew.class.getResourceAsStream("/export/salary_export.xlsx");
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
        WriteSheet writeSheet = EasyExcel.
                writerSheet("Sheet1")
                .registerWriteHandler(new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle))
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .registerWriteHandler(new CustomCellWriteHeightConfig())
                .build();
        writeSheet.setRelativeHeadRowIndex(1);


        FillConfig fillConfig = FillConfig.builder()
                // 开启填充换行
                .forceNewRow(true)
                .build();

        writeSheet.setHead(headTitles);
        excelWriter.write(dataList, writeSheet);
        excelWriter.finish();

    }

}

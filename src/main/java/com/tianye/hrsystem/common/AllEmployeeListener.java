package com.tianye.hrsystem.common;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.tianye.hrsystem.entity.vo.EmployeeImportVO;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: AllEmployeeListener
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年05月16日 22:13
 **/
public class AllEmployeeListener extends AnalysisEventListener<EmployeeImportVO> {
    List<EmployeeImportVO> items=new ArrayList<>();
    @Override
    public void invoke(EmployeeImportVO data, AnalysisContext context) {
        if(StringUtils.isEmpty(data.getEmployeeName())==false)items.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {

    }
    public List<EmployeeImportVO> getItems(){
        return items;
    }
}

package com.tianye.hrsystem.controller;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.tianye.hrsystem.common.*;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.repository.tbPlanListRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.IWorkPlanService;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: WorkPlanListController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年08月02日 17:40
 **/

@Controller
@RequestMapping("/workPlan")
public class WorkPlanListController {

    @Autowired
    IWorkPlanService planService;

    @Autowired
    tbPlanListRepository planRep;

    @Autowired
    tbattendanceuserRepository userRep;

    @Autowired
    hrmAttendanceShiftRepository shiftRep;
    @Autowired
    IAccessToken accessToken;

    @RequestMapping("/saveAll")
    @ResponseBody
    public successResult Add(String Data){
        successResult result=new successResult();
        try {
            List<tbplanlist> list= JSON.parseArray(Data, tbplanlist.class);
            planService.AddAll(list);
        }
        catch(Exception ax){
            result.raiseException(ax);
        }
        return result;
    }

    @ResponseBody
    @RequestMapping("/getData")
    public PageObject<tbplanlist> getData(String  GroupID,String Begin,String End, Integer pageSize,Integer pageNum,
            String sortField,
            String sortOrder){
        Page<tbplanlist> datas=null;
        try {
            if(pageSize==null) pageSize=20;
            if(pageNum==null) pageNum=0;
            if(StringUtils.isEmpty(sortField)) sortField="createTime";
            if(StringUtils.isEmpty(sortOrder))sortOrder="asc";

            PageEntity entity=new PageEntity();
            entity.setOrder(sortOrder);
            entity.setSortField(sortField);
            entity.setPageNum(pageNum);
            entity.setPageSize(pageSize);
            Pageable pageable= PageableUtils.From(entity);

            if(StringUtils.isEmpty(Begin) && StringUtils.isEmpty(End)){
                datas=planRep.findAllByGroupId(GroupID,pageable);
            } else {
                if(StringUtils.isEmpty(Begin) || StringUtils.isEmpty(End)){
                    throw new Exception("请同时指明开始(Begin)和结束(End)时间!");
                }
                SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
                Date begin=format.parse(Begin);
                Date end=format.parse(End);
                if(StringUtils.isEmpty(GroupID)==false){
                    datas=planRep.findAllByGroupIdAndWorkDateBetween(GroupID,begin,end,pageable);
                } else {
                    datas=planRep.findAllByWorkDateBetween(begin,end,pageable);
                }
            }
        }
        catch(Exception ax){

            return PageObject.Error(ax.getMessage());
        }
        return PageObject.Of(datas);
    }

    @RequestMapping("/loadIsLast")
    @ResponseBody
    public PageObject<tbplanlist> loadIsLast(String SelectDate, String WorkDate, String Begin, Boolean LoadLast,
                                             Boolean isLast, Integer pageSize, Integer pageNum,
                                             String sortField, String sortOrder) {
        PageObject<tbplanlist> datas = null;
        try {
            String selectedDateText = SelectDate;
            if (StringUtils.isEmpty(selectedDateText)) {
                selectedDateText = WorkDate;
            }
            if (StringUtils.isEmpty(selectedDateText)) {
                selectedDateText = Begin;
            }

            Boolean loadLast = LoadLast != null ? LoadLast : isLast;
            if (loadLast == null) {
                loadLast = true;
            }

            if (StringUtils.isEmpty(selectedDateText)) {
                datas = planService.getMaxDate(pageSize, pageNum, sortField, sortOrder);
            } else {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                format.setLenient(false);
                Date selectedDate = format.parse(selectedDateText);
                datas = planService.loadBySelectedDate(selectedDate, loadLast, pageSize, pageNum, sortField, sortOrder);
            }
            datas.setLastPage(true);

        }catch (Exception ax) {
            return PageObject.Error(ax.getMessage());
        }
        return datas;
    }

    @RequestMapping("/removeAll")
    @ResponseBody
    public successResult RemoveAll(String IDS){
        successResult result=new successResult();
        try {
            List<Integer> IDArray= ListUtils.parse(IDS,Integer.class);
            planService.RemoveAll(IDArray);
        }
        catch(Exception ax){
            result.raiseException(ax);
        }
        return result;
    }
    @RequestMapping("/exportExcel")
    public void ExportToExcel(String GroupID,String Begin,String End, HttpServletResponse response){
        try {
            if(StringUtils.isEmpty(Begin) || StringUtils.isEmpty(End)){
                throw new Exception("请同时指明开始(Begin)和结束(End)时间!");
            }
            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd");
            Date begin=format.parse(Begin);
            Date end=format.parse(End);
            List<tbplanlist> tbplanlists=planRep.findAllByGroupIdAndWorkDateBetweenOrderByProductNameAsc(GroupID,begin,end);
            tbplanlists=tbplanlists.stream().sorted(Comparator.comparing(tbplanlist::getWorkDate)).collect(Collectors.toList());
            if(tbplanlists.size()>0){
                List<tbattendanceuser> users=userRep.findAll();
                List<HrmAttendanceShift>  shifts=shiftRep.findAll();
                List<tbPlanListVo> Vs=new ArrayList<>();
                for(int i=0;i<tbplanlists.size();i++){
                    tbplanlist plan=tbplanlists.get(i);
                    tbPlanListVo vo=new tbPlanListVo();
                    String classId=plan.getClassId();
                    String userId=plan.getUserId();
                    List<String> UserIDS=ListUtils.parse(userId,String.class);

                    vo.setProductName(plan.getProductName());
                    vo.setLinkName(plan.getLinkName());
                    vo.setWorkDate(format.format(plan.getWorkDate()));

                    Optional<HrmAttendanceShift> findShifts=
                            shifts.stream().filter(f->Long.toString(f.getShiftId()).equals(classId)).findFirst();
                    if(findShifts.isPresent()){
                        HrmAttendanceShift shift= findShifts.get();
                        String shiftName=shift.getShiftName();
                        String  start1=shift.getStart1();
                        String  end1  =shift.getEnd1();
                        if(StringUtils.isEmpty(start1)==false && StringUtils.isEmpty(end1)==false){
                            String[] s1=start1.split(":");
                            if(s1.length==3){
                                start1=s1[0]+":"+s1[1];
                            } else start1=s1[0];
                            String[] s2=end1.split(":");
                           if(s2.length==3) end1=s2[0]+":"+s2[1]; else end1=s2[0];
                            shiftName=shiftName+"("+start1+"~"+end1+")";
                        }
                        String  start2=shift.getStart2();
                        String  end2  =shift.getEnd2();
                        if(StringUtils.isEmpty(start2)==false && StringUtils.isEmpty(end2)==false){
                            String[] s1=start2.split(":");
                            if(s1.length==3){
                                start1=s1[0]+":"+s1[1];
                            } else start2=s1[0];
                            String[] s2=end2.split(":");
                            if(s2.length==3) end1=s2[0]+":"+s2[1]; else end2=s2[0];
                            shiftName=shiftName+"("+start2+"~"+end2+")";
                        }
                        vo.setClassName(shiftName);
                    } else continue;

                    if(UserIDS.size()>0){
                        List<tbattendanceuser> Us=
                                users.stream().filter(f->UserIDS.contains(f.getUserId())).collect(Collectors.toList());
                        if(Us.size()>0){
                            String Names=StringUtils.join(Us.stream().map(f->f.getUserName()).collect(Collectors.toList()), ',');
                            vo.setUserName(Names);
                        }
                    }
                    Vs.add(vo);
                }
                if(Vs.size()>0){
                    Map<String,Object>Info=new HashMap<>();
                    Info.put("planList",Vs);
                    String templateText=getTemplateByCode("workPlan");
                    String DD= StringUtilTool.createByTemplate(templateText,Info);
                    byte[] BB= DD.getBytes("utf-8");
                    WebFileUtils.download("生产排班("+Begin+"至"+End+")"+".xlsx",BB,response);
                } else {
                    throw new Exception("没有可导出的数据!");
                }
            } else throw new Exception("没有可导出的数据!");
        }
        catch(Exception ax){
            try {
                response.setCharacterEncoding("utf-8");
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(ax.getLocalizedMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public String getTemplateByCode(String code) throws Exception {
        String BaseDir = GlobalContext.getStaticUrl()+"\\" ;
        return StringUtilTool.readAll(BaseDir + code + ".ftl");
    }
    @RequestMapping("/publishPlan")
    @ResponseBody
    public successResult PublishPlan(String imgData,String fileName){
        successResult result=new successResult();
        try {
            LoginUserInfo Info= CompanyContext.get();
            byte[] BB=Base64.getDecoder().decode(imgData);
            String filePath=CompanyPathUtils.getTempPath(fileName);
            File fi=new File(filePath);
            FileUtils.writeByteArrayToFile(fi,BB);
            String Token=accessToken.GetMessageToken(Info.getCompanyId());
            uploadResult upResult=uploadOne(Token,fi);

            String Content="{\n" +
                    "    \"msgtype\": \"file\",\n" +
                    "    \"file\": {\n" +
                    "        \"media_id\": \""+upResult.getMedia_id()+"\"\n" +
                    "    }\n" +
                    "}";

            String Url="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key="+Token;
            String Vx= HttpUtil.post(Url,Content);
            uploadResult rr=JSON.parseObject(Vx,uploadResult.class);
            if(rr.getErrcode()!=0){
                throw new Exception(rr.getErrmsg());
            }
        }catch(Exception ax){
            result.raiseException(ax);
        }
        return result;
    }
    uploadResult uploadOne(String Token,File file){
        Map<String,Object> param=new HashMap<>();
        param.put("file", file);
        String Url="https://qyapi.weixin.qq.com/cgi-bin/webhook/upload_media?type=file&key="+Token;
        String request=HttpUtil.post(Url,param);
        return JSON.parseObject(request,uploadResult.class);
    }
}

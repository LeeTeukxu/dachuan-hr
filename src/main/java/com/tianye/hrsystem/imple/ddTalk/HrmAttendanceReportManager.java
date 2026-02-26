package com.tianye.hrsystem.imple.ddTalk;

import com.alibaba.fastjson.JSON;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceGetattcolumnsRequest;
import com.dingtalk.api.request.OapiAttendanceGetcolumnvalRequest;
import com.dingtalk.api.request.OapiAttendanceGetleavetimebynamesRequest;
import com.dingtalk.api.response.OapiAttendanceGetattcolumnsResponse;
import com.dingtalk.api.response.OapiAttendanceGetcolumnvalResponse;
import com.dingtalk.api.response.OapiAttendanceGetleavetimebynamesResponse;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.common.DDTalkResposeLogger;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.model.*;
import com.tianye.hrsystem.model.ddTalk.Ddtaskresult;
import com.tianye.hrsystem.repository.*;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.service.ddTalk.IHrmAttendanceReport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: HrmAttendanceReportManager
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年04月20日 9:23
 **/
@Service
public class HrmAttendanceReportManager implements IHrmAttendanceReport {

    @Autowired
    IAccessToken tokener;
    @Autowired
    hrmAttendanceReportFieldRepository fieldRep;
    @Autowired
    hrmAttendanceReportDataRepository dataRep;
    @Autowired
    hrmEmployeeLeaveRecordRepository leaveRep;
    @Autowired
    hrmEmployeeOverTimeRecordRepository overTimeRep;
    Long PreNum = 19000000L;
    SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat shortFormat = new SimpleDateFormat("yyyy-MM-dd");
    List<String> overTimes = Arrays.asList("工作日加班", "休息日加班", "节假日加班");

    Logger logger = LoggerFactory.getLogger(HrmAttendanceReportManager.class);
    @Autowired
    DDTalkResposeLogger ddLogger;
    @Autowired
    TransactionTemplate transactionTemplate;

    @Override
    public List<HrmAttendanceReportField> UpdateReportFields() throws ApiException {
        // 先调用API获取数据（在事务外执行，避免长事务）
        String password = tokener.Refresh();
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getattcolumns");
        OapiAttendanceGetattcolumnsRequest req = new OapiAttendanceGetattcolumnsRequest();
        OapiAttendanceGetattcolumnsResponse rsp = client.execute(req, password);
        
        if (rsp.isSuccess()) {
            // 先在内存中准备好数据
            List<HrmAttendanceReportField> fields = new ArrayList<>();
            List<OapiAttendanceGetattcolumnsResponse.ColumnForTopVo> columns = rsp.getResult().getColumns();
            for (int i = 0; i < columns.size(); i++) {
                OapiAttendanceGetattcolumnsResponse.ColumnForTopVo vo = columns.get(i);
                Long Id = vo.getId();
                Integer Type = 1;//
                if (Id == null) {
                    Type = 2;
                    Id = PreNum;
                    PreNum++;
                }
                String name = vo.getName();
                HrmAttendanceReportField newOne = new HrmAttendanceReportField();
                newOne.setFieldId(Id);
                newOne.setFieldName(name);
                newOne.setCreateTime(new Date());
                newOne.setType(Type);
                fields.add(newOne);
            }
            
            // 在短事务中执行数据库操作
            final List<HrmAttendanceReportField> fieldsToSave = fields;
            transactionTemplate.execute(status -> {
                fieldRep.deleteAll();
                if (!fieldsToSave.isEmpty()) {
                    fieldRep.saveAll(fieldsToSave);
                }
                return null;
            });
            logger.info("更新考勤报表字段完成，共 {} 个字段", fields.size());
        }
        return null;
    }
    @Override
    @Transactional
    public void UpdateAttendanceReport(String userId, Long empId, Date begin, Date end) throws ApiException {
        int pageIndex = 0;
        LoginUserInfo Info=CompanyContext.get();
        List<HrmAttendanceReportField> allFields = fieldRep.findAll();
        while (true) {
            List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 1).skip(pageIndex * 20L).limit(20L).collect(Collectors.toList());
            if (fields.size() > 0) {
                String password = tokener.Refresh();
                String Ids = String.join(",", fields.stream().map(f -> Long.toString(f.getFieldId())).collect(Collectors.toList()));
                for(int i=0;i<3;i++){
                    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getcolumnval");
                    OapiAttendanceGetcolumnvalRequest req = new OapiAttendanceGetcolumnvalRequest();
                    req.setFromDate(begin);
                    req.setToDate(end);
                    req.setColumnIdList(Ids);
                    req.setUserid(userId);
                    OapiAttendanceGetcolumnvalResponse rsp = client.execute(req, password);
                    if (rsp.isSuccess()) {
                        Ddtaskresult result=new Ddtaskresult();
                        result.setProcessed(0);
                        result.setClassName("UpdateAttendanceRepot");
                        result.setResult("未处理");
                        result.setCreatetime(new Date());
                        result.setCompanyId(Info.getCompanyId());
                        result.setContent(JSON.toJSONString(rsp));
                        result.setBegin(cimple.format(begin));
                        result.setEnd(cimple.format(end));
                        result.setEmpId(empId);
                        result.setUserId(userId);  // 【修复重名员工问题】：保存userId用于去重
                        ddRep.save(result);
                        logger.info("插入了一条"+shortFormat.format(begin)+"-"+shortFormat.format(end)+"的考勤报表数据!");
                        break;
                    } else {
                        if(i==2){
                            logger.info(rsp.getErrmsg());
                            throw new ApiException("超过最大重试次数，程序退出");
                        } else {
                            logger.info("拉取数据失败:"+rsp.getErrmsg()+"进行重试!");
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                pageIndex++;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else break;
        }
    }

    @Override
    public void UpdateAttendanceReportQuick(String userId, Long empId, Date begin, Date end) throws ApiException {
        int pageIndex = 0;
        LoginUserInfo Info=CompanyContext.get();
        List<HrmAttendanceReportField> allFields = fieldRep.findAll();
        while (true) {
            List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 1).skip(pageIndex * 20L).limit(20L).collect(Collectors.toList());
            if (fields.size() > 0) {
                String password = tokener.Refresh();
                String Ids = String.join(",", fields.stream().map(f -> Long.toString(f.getFieldId())).collect(Collectors.toList()));
                for(int i=0;i<3;i++){
                    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getcolumnval");
                    OapiAttendanceGetcolumnvalRequest req = new OapiAttendanceGetcolumnvalRequest();
                    req.setFromDate(begin);
                    req.setToDate(end);
                    req.setColumnIdList(Ids);
                    req.setUserid(userId);
                    OapiAttendanceGetcolumnvalResponse rsp = client.execute(req, password);
                    if (rsp.isSuccess()) {
                        SaveReportData(empId,rsp);
                        logger.info("插入了一条"+shortFormat.format(begin)+"-"+shortFormat.format(end)+"的考勤报表数据!");
                        break;
                    } else {
                        if(i==2){
                            logger.info(rsp.getErrmsg());
                            throw new ApiException("超过最大重试次数，程序退出");
                        } else {
                            logger.info("拉取数据失败:"+rsp.getErrmsg()+"进行重试!");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                pageIndex++;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else break;
        }
    }

    @Autowired
    ddtaskresultRepository ddRep;
    SimpleDateFormat cimple=new SimpleDateFormat("yyyyMMdd");
    @Override
    public void UpdateHolidayReport(String userId, Long empId, Date begin, Date end) throws ApiException {
        List<HrmAttendanceReportField> allFields = fieldRep.findAll();
        List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 2).collect(Collectors.toList());
        LoginUserInfo Info=CompanyContext.get();
        if (fields.size() > 0) {
            String password = tokener.Refresh();
            List<Long> idd=fields.stream().map(f->f.getFieldId()).collect(Collectors.toList());
            int count=dataRep.countAllByEmpIdAndWorkDateBetweenAndFieldIdIn(empId,begin,end,idd);
            if(count==0){
                String Ids = String.join(",", fields.stream().map(f -> f.getFieldName()).collect(Collectors.toList()));
                for(int i=0;i<3;i++){
                    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getleavetimebynames");
                    OapiAttendanceGetleavetimebynamesRequest req = new OapiAttendanceGetleavetimebynamesRequest();

                    req.setFromDate(begin);
                    req.setToDate(end);
                    req.setLeaveNames(Ids);
                    req.setUserid(userId);
                    OapiAttendanceGetleavetimebynamesResponse rsp = client.execute(req, password);
                    if(rsp.isSuccess()){
                        //ddLogger.Info(rsp,((DefaultDingTalkClient)client).getRequestUrl(),begin,end,HrmAttendanceReportManager.class);

                        Ddtaskresult result=new Ddtaskresult();
                        result.setProcessed(0);
                        result.setClassName("UpdateHolidayReport");
                        result.setResult("未处理");
                        result.setCreatetime(new Date());
                        result.setEmpId(empId);
                        result.setUserId(userId);  // 【修复重名员工问题】：保存userId用于去重
                        result.setBegin(cimple.format(begin));
                        result.setEnd(cimple.format(end));
                        result.setCompanyId(Info.getCompanyId());
                        result.setContent(JSON.toJSONString(rsp));
                        ddRep.save(result);
                        logger.info("插入了一条"+shortFormat.format(begin)+"-"+shortFormat.format(end)+"的请假报表数据!");
                        break;
                    } else {
                        if(i==2){
                            logger.info(rsp.getErrmsg());
                            throw new ApiException("超过最大重试次数，程序退出");
                        } else {
                            logger.info("拉取数据失败:"+rsp.getErrmsg()+"进行重试!");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }

            } else {
                logger.info(Long.toString(empId)+"已保存了："+Integer.toString(count)+"条记录!");
            }
        }
    }

    @Override
    public void UpdateHolidayReportQuick(String userId, Long empId, Date begin, Date end) throws ApiException {
        List<HrmAttendanceReportField> allFields = fieldRep.findAll();
        List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 2).collect(Collectors.toList());
        LoginUserInfo Info=CompanyContext.get();
        if (fields.size() > 0) {
            String password = tokener.Refresh();
            List<Long> idd=fields.stream().map(f->f.getFieldId()).collect(Collectors.toList());
            int count=dataRep.countAllByEmpIdAndWorkDateBetweenAndFieldIdIn(empId,begin,end,idd);
            if(count==0){
                String Ids = String.join(",", fields.stream().map(f -> f.getFieldName()).collect(Collectors.toList()));
                DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getleavetimebynames");
                OapiAttendanceGetleavetimebynamesRequest req = new OapiAttendanceGetleavetimebynamesRequest();

                req.setFromDate(begin);
                req.setToDate(end);
                req.setLeaveNames(Ids);
                req.setUserid(userId);
                OapiAttendanceGetleavetimebynamesResponse rsp = client.execute(req, password);
                if(rsp.isSuccess()){
                    SaveHolidayData(empId,rsp);
                    logger.info("插入了一条"+shortFormat.format(begin)+"-"+shortFormat.format(end)+"的请假报表数据!");
                }

            } else {
                logger.info(Long.toString(empId)+"已保存了："+Integer.toString(count)+"条记录!");
            }
        }
    }

    void SaveHolidayData(Long empId,OapiAttendanceGetleavetimebynamesResponse  rsp) throws  ApiException{
        if(rsp.isSuccess()){
            List<HrmAttendanceReportField> allFields = fieldRep.findAll();
            List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 2).collect(Collectors.toList());
            List<Long> idd=fields.stream().map(f->f.getFieldId()).collect(Collectors.toList());
            OapiAttendanceGetleavetimebynamesResponse.ColumnValListForTopVo V = rsp.getResult();
            List<OapiAttendanceGetleavetimebynamesResponse.ColumnValForTopVo> values = V.getColumns();
            List<HrmAttendanceReportData> Datas = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                OapiAttendanceGetleavetimebynamesResponse.ColumnValForTopVo value = values.get(i);
                List<OapiAttendanceGetleavetimebynamesResponse.ColumnDayAndVal> vs = value.getColumnvals();
                if (vs.size() > 0) {
                    OapiAttendanceGetleavetimebynamesResponse.ColumnForTopVo col = value.getColumnvo();
                    String fieldName = col.getName();
                    Optional<HrmAttendanceReportField> findFields =
                            fields.stream().filter(f -> f.getFieldName().equals(fieldName)).findFirst();
                    if (findFields.isPresent()) {
                        HrmAttendanceReportField ss = findFields.get();
                        for (int n = 0; n < vs.size(); n++) {
                            OapiAttendanceGetleavetimebynamesResponse.ColumnDayAndVal cVal = vs.get(n);

                            HrmAttendanceReportField field = findFields.get();
                            String fieldValue = cVal.getValue();
                            HrmAttendanceReportData sData = new HrmAttendanceReportData();
                            sData.setEmpId(empId);
                            sData.setValue(fieldValue);
                            sData.setFieldId(ss.getFieldId());
                            sData.setFieldName(field.getFieldName());
                            sData.setWorkDate(cVal.getDate());
                            sData.setCreateTime(new Date());
                            sData.setCreateUser(1L);
                            Datas.add(sData);
                        }
                    } else {
                        logger.info(fieldName + "没有找到对应的ID");
                        break;
                    }
                }
            }
            if (Datas.size() > 0) {
                dataRep.saveAll(Datas);
                logger.info("一共保存了" + Integer.toString(Datas.size()) + "条请假报表数据！");
            }
        }
    }
    void SaveReportData(Long empId, OapiAttendanceGetcolumnvalResponse rsp) throws ApiException{
        if (rsp.isSuccess()) {
            List<HrmAttendanceReportField> allFields = fieldRep.findAll();
            List<HrmAttendanceReportField> fields = allFields.stream().filter(f -> f.getType() == 1).collect(Collectors.toList());


            OapiAttendanceGetcolumnvalResponse.ColumnValListForTopVo V = rsp.getResult();
            List<OapiAttendanceGetcolumnvalResponse.ColumnValForTopVo> values = V.getColumnVals();
            List<HrmAttendanceReportData> Datas = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                OapiAttendanceGetcolumnvalResponse.ColumnValForTopVo value = values.get(i);
                List<OapiAttendanceGetcolumnvalResponse.ColumnDayAndVal> vs = value.getColumnVals();
                if (vs.size() > 0) {
                    OapiAttendanceGetcolumnvalResponse.ColumnForTopVo col = value.getColumnVo();
                    Long fieldId = col.getId();
                    Optional<HrmAttendanceReportField> findFields =
                            fields.stream().filter(f -> f.getFieldId().equals(fieldId)).findFirst();
                    if (findFields.isPresent()) {
                        for (int n = 0; n < vs.size(); n++) {
                            OapiAttendanceGetcolumnvalResponse.ColumnDayAndVal cVal = vs.get(n);
                            HrmAttendanceReportField field = findFields.get();
                            String fieldValue = cVal.getValue();
                            HrmAttendanceReportData sData = new HrmAttendanceReportData();
                            sData.setEmpId(empId);
                            sData.setValue(fieldValue);
                            sData.setFieldId(fieldId);
                            sData.setFieldName(field.getFieldName());
                            sData.setWorkDate(cVal.getDate());
                            sData.setCreateTime(new Date());
                            sData.setCreateUser(1L);
                            Datas.add(sData);
                        }
                    }
                }
            }
            if (Datas.size() > 0) {
                dataRep.saveAll(Datas);
                logger.info("一共保存了" + Long.toString(empId) + "的" + Integer.toString(Datas.size())+"条考勤及加班报表数据！");
            }
        }
    }

    @Transactional
    public void ProcessOne(String CompanyID) {
        Optional<Ddtaskresult> findOnes=ddRep.findFirstByProcessedAndCompanyIdOrderByCreatetime(0,CompanyID);
        if(findOnes.isPresent()){
            Ddtaskresult one=findOnes.get();
            Long T1=System.currentTimeMillis();
            try {
                String className=one.getClassName();
                String Content=one.getContent();
                Long empId=one.getEmpId();
                if(StringUtils.isEmpty(Content)==false){
                    if(className.equals("UpdateHolidayReport")){
                        OapiAttendanceGetleavetimebynamesResponse rsp=JSON.parseObject(Content,OapiAttendanceGetleavetimebynamesResponse.class);
                        SaveHolidayData(empId,rsp);
                    }else if(className.equals("UpdateAttendanceReport") || className.equals("UpdateAttendanceRepot")){
                        OapiAttendanceGetcolumnvalResponse rsp=JSON.parseObject(Content,OapiAttendanceGetcolumnvalResponse.class);
                        SaveReportData(empId,rsp);
                    }
                    one.setResult("处理完成!");
                    one.setSuccess(1);
                    one.setProcessed(200);
                } else throw new Exception("Content is empty!");
            }
            catch(Exception ax){
                one.setProcessed(500);
                one.setSuccess(0);
                one.setResult("处理出错:"+ax.getMessage());
            }
            finally {
                one.setProcesstime(new Date());
            }
            try {
                ddRep.save(one);
            }
            catch(Exception ax){
                ax.printStackTrace();
            }
            finally {
                Long T2=System.currentTimeMillis();
                logger.info("数据处理完成，用时:"+Long.toString(T2-T1));
            }
        } else {
            logger.info("CompanyID:"+CompanyID+"在DDTaskResult中未找到待处理的记录!");
        }
    }
}

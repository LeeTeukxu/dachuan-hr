package com.tianye.hrsystem.common;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.gson.Gson;
import com.tianye.hrsystem.imple.HrmSalaryMonthRecordServiceImpl;
import com.tianye.hrsystem.modules.salary.entity.HrmSalaryMonthRecord;
import com.tianye.hrsystem.modules.salary.entity.HrmSalarySlip;
import com.tianye.hrsystem.modules.salary.service.HrmSalarySlipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class aliyunMessage {
    private static Logger logger = Logger.getLogger(aliyunMessage.class.getName());

    @Autowired
    private HrmSalarySlipService iSalarySlipService;
    private static HrmSalarySlipService salarySlipService;

    @Autowired
    private HrmSalaryMonthRecordServiceImpl iSalaryMonthRecordService;
    private static HrmSalaryMonthRecordServiceImpl salaryMonthRecordService;

    @PostConstruct
    public void init() {
        salarySlipService = iSalarySlipService;
        salaryMonthRecordService = iSalaryMonthRecordService;
    }

//    public static class MyMessageListener implements MessageListener {
//        Gson gson=new Gson();
//
//        @Override
//        public boolean dealMessage(Message message) {
//
//            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            //消息的几个关键值
//            System.out.println("message receiver time from mns:" + format.format(new Date()));
//            System.out.println("message handle: " + message.getReceiptHandle());
//            System.out.println("message body: " + message.getMessageBodyAsString());
//            System.out.println("message id: " + message.getMessageId());
//            System.out.println("message dequeue count:" + message.getDequeueCount());
//            System.out.println("Thread:" + Thread.currentThread().getName());
//            try{
//                Map<String,Object> contentMap=gson.fromJson(message.getMessageBodyAsString(), HashMap.class);
//
//                //TODO 根据文档中具体的消息格式进行消息体的解析
//                String phoneNumber=(String)contentMap.get("phone_number");
//                Boolean success=(Boolean)contentMap.get("success");
//                String bizId=(String)contentMap.get("biz_id");
//                String outId=(String)contentMap.get("out_id");
//                String sendTime=(String)contentMap.get("send_time");
//                String reportTime=(String)contentMap.get("report_time");
//                String errCode=(String)contentMap.get("err_code");
//                String errMsg=(String)contentMap.get("err_msg");
//                String content = (String)contentMap.get("content");
//                String employeeId = (String)contentMap.get("employeeId");
//                String year = (String)contentMap.get("year");
//                String month = (String)contentMap.get("month");
//                System.out.println("回复的内容为：" + content + "，员工ID：" + employeeId + "，年份：" + year + "，月份" + month);
//
//                //TODO 这里开始编写您的业务代码
//                if (Integer.parseInt(content) == 1 || Integer.parseInt(content) == 2) {
////                    tempWorkerService.ModifyRenewalStatus(phoneNumber, Integer.parseInt(content));
//                    LambdaUpdateWrapper<HrmSalarySlip> wrapper = new LambdaUpdateWrapper<HrmSalarySlip>()
//                            .set(HrmSalarySlip::getReadStatus, content)
//                            .set(HrmSalarySlip::getSendTime, LocalDateTime.now())
//                            .eq(HrmSalarySlip::getEmployeeId,Long.valueOf(employeeId))
//                            .and(i -> i.eq(HrmSalarySlip::getYear, Integer.parseInt(year))).and(i -> i.eq(HrmSalarySlip::getMonth, Integer.parseInt(month)));
//                    salarySlipService.update(null, wrapper);
//
//                    LambdaUpdateWrapper<HrmSalaryMonthRecord> wrapper2 = new LambdaUpdateWrapper<HrmSalaryMonthRecord>()
//                            .set(HrmSalaryMonthRecord::getIsSend, 1)
//                            .eq(HrmSalaryMonthRecord::getYear, Integer.parseInt(year))
//                            .and(i -> i.eq(HrmSalaryMonthRecord::getMonth, month));
//                    salaryMonthRecordService.update(null, wrapper2);
//
//                    //如果全部为1则更新为员工确认完成
//                    List<HrmSalarySlip> lists = salarySlipService.lambdaQuery().eq(HrmSalarySlip::getYear, Integer.parseInt(year))
//                            .and(i ->i.eq(HrmSalarySlip::getMonth, Integer.parseInt(month))).list();
//                    Boolean IsPass = true;
//                    if (CollUtil.isNotEmpty(lists)) {
//                        for (HrmSalarySlip hrmSalarySlip : lists) {
//                            if (hrmSalarySlip.getReadStatus() != 1) {
//                                IsPass = false;
//                            }
//                        }
//                    }
//                    if (IsPass == true) {
//                        LambdaUpdateWrapper<HrmSalaryMonthRecord> wrapper1 = new LambdaUpdateWrapper<HrmSalaryMonthRecord>()
//                                .set(HrmSalaryMonthRecord::getCheckStatus, 12)
//                                .eq(HrmSalaryMonthRecord::getYear, Integer.parseInt(year))
//                                .and(i -> i.eq(HrmSalaryMonthRecord::getMonth, Integer.parseInt(month)));
//                        salaryMonthRecordService.update(null, wrapper1);
//                    }
//                }
//
//            }catch(com.google.gson.JsonSyntaxException e){
//                logger.log(Level.SEVERE, "error_json_format:"+message.getMessageBodyAsString(),e);
//                //理论上不会出现格式错误的情况，所以遇见格式错误的消息，只能先delete,否则重新推送也会一直报错
//                return true;
//            } catch (Throwable e) {
//                //您自己的代码部分导致的异常，应该return false,这样消息不会被delete掉，而会根据策略进行重推
//                return false;
//            }
//
//            //消息处理成功，返回true, SDK将调用MNS的delete方法将消息从队列中删除掉
//            return true;
//        }
//    }
}

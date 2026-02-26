package com.tianye.hrsystem.modules.salary.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.AdminMessageEnum;
import com.tianye.hrsystem.common.SMSUtils;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.AdminMessage;
import com.tianye.hrsystem.entity.vo.OperationLog;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.modules.salary.dto.ComputeSalaryDto;
import com.tianye.hrsystem.modules.salary.dto.QuerySlipEmployeePageListDto;
import com.tianye.hrsystem.modules.salary.dto.SendSalarySlipDto;
import com.tianye.hrsystem.modules.salary.dto.SmsUpDto;
import com.tianye.hrsystem.modules.salary.entity.*;
import com.tianye.hrsystem.modules.salary.mapper.HrmSalarySlipRecordMapper;
import com.tianye.hrsystem.service.employee.IHrmEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工资条服务类
 */
@Service
public class HrmSalarySlipRecordService  extends BaseServiceImpl<HrmSalarySlipRecordMapper, HrmSalarySlipRecord>
{
    @Autowired
    private HrmSalarySlipRecordMapper slipRecordMapper;

    @Autowired
    private HrmSalarySlipService hrmSalarySlipService;

    @Autowired
    private SalaryMonthRecordService_Bak salaryMonthRecordService;

    @Autowired
    private HrmSalaryMonthEmpRecordService salaryMonthEmpRecordService;

    @Autowired
    private HrmSalaryMonthOptionValueService salaryMonthOptionValueService;

    @Autowired
    private HrmSalarySlipOptionService salarySlipOptionService;

    @Autowired
    private IHrmEmployeeService hrmEmployeeService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private HrmSalarySlipService salarySlipService;


//    private IAdminMessageService adminMessageService =ApplicationContextHolder.getBean(IAdminMessageService.class);


    /**
     * 查询工资条选择发送员工列表
     * @param slipEmployeePageListBO
     * @return
     */
    public Page<SlipEmployeeVO> querySlipEmployeePageList(QuerySlipEmployeePageListDto slipEmployeePageListBO)
    {
        HrmSalaryMonthRecord salaryMonthRecord = salaryMonthRecordService.queryLastSalaryMonthRecord();
        Page<SlipEmployeeVO> page = slipRecordMapper.querySlipEmployeePageList(slipEmployeePageListBO.parse(), salaryMonthRecord.getSRecordId(), slipEmployeePageListBO);
        return page;
    }

    /**
     * 发送工资条给员工
     * @param sendSalarySlipBO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public OperationLog sendSalarySlip(SendSalarySlipDto sendSalarySlipBO)
    {
        Long userId = Long.parseLong(CompanyContext.get().getUserId());
        //获取最新的每月薪资记录
        HrmSalaryMonthRecord salaryMonthRecord = salaryMonthRecordService.queryLastSalaryMonthRecord();
        List<Long> sEmpRecordIds;
        if (sendSalarySlipBO.getIsAll())
        {
            sEmpRecordIds = slipRecordMapper.querySlipEmployeeIds(salaryMonthRecord.getSRecordId(), sendSalarySlipBO);
        } else {
            sEmpRecordIds = sendSalarySlipBO.getSEmpRecordIds();
        }

//        List<HrmSalarySlipTemplateOption> slipTemplateOption = sendSalarySlipBO.getSlipTemplateOption();
        Set<Integer> optionCodeList = new TreeSet<>();
        optionCodeList.add(240101);//实发工资
//        slipTemplateOption.forEach(option ->
//                optionCodeList.addAll(option.getOptionList().stream().map(HrmSalarySlipTemplateOption::getCode).collect(Collectors.toList())));
        //获取员工月薪资项表
        List<HrmSalaryMonthOptionValue> valueList = salaryMonthOptionValueService.lambdaQuery().select(HrmSalaryMonthOptionValue::getSEmpRecordId, HrmSalaryMonthOptionValue::getCode, HrmSalaryMonthOptionValue::getValue)
                .in(HrmSalaryMonthOptionValue::getCode, optionCodeList).in(HrmSalaryMonthOptionValue::getSEmpRecordId, sEmpRecordIds).list();
        Map<Long, Map<Integer, String>> empValueMap = valueList.stream().collect(Collectors.groupingBy(HrmSalaryMonthOptionValue::getSEmpRecordId, Collectors.toMap(HrmSalaryMonthOptionValue::getCode, HrmSalaryMonthOptionValue::getValue)));
        HrmSalarySlipRecord salarySlipRecord = new HrmSalarySlipRecord();
        salarySlipRecord.setSRecordId(salaryMonthRecord.getSRecordId());
        salarySlipRecord.setSalaryNum(salaryMonthRecord.getNum());
        salarySlipRecord.setPayNum(sEmpRecordIds.size());
        salarySlipRecord.setYear(salaryMonthRecord.getYear());
        salarySlipRecord.setMonth(salaryMonthRecord.getMonth());
        salarySlipRecord.setCreateTime(LocalDateTime.now());
        salarySlipRecord.setCreateUserId(Long.parseLong(CompanyContext.get().getUserId()));
        save(salarySlipRecord);

        for (Long sEmpRecordId : sEmpRecordIds)
        {

            //员工的工资的薪资项明细
            List<ComputeSalaryDto> list = salaryMonthOptionValueService.queryEmpSalaryOptionValueList(sEmpRecordId);

            //遍历员工的薪资记录
            Map<Integer, String> codeValueMap = empValueMap.get(sEmpRecordId);
            //创建工资条
            HrmSalarySlip hrmSalarySlip = new HrmSalarySlip();
            hrmSalarySlip.setRecordId(salarySlipRecord.getId());
            hrmSalarySlip.setSEmpRecordId(sEmpRecordId);
            Long employeeId = salaryMonthEmpRecordService.lambdaQuery().select(HrmSalaryMonthEmpRecord::getEmployeeId).eq(HrmSalaryMonthEmpRecord::getSEmpRecordId, sEmpRecordId).one().getEmployeeId();
            HrmEmployee employee = hrmEmployeeService.lambdaQuery().eq(HrmEmployee::getEmployeeId, employeeId).one();
            hrmSalarySlip.setEmployeeId(employeeId);
            hrmSalarySlip.setYear(salaryMonthRecord.getYear());
            hrmSalarySlip.setMonth(salaryMonthRecord.getMonth());
            //获取实发工资
            hrmSalarySlip.setRealSalary(Objects.isNull(codeValueMap.get(240101)) ? new BigDecimal(0) : new BigDecimal(codeValueMap.get(240101)));
            hrmSalarySlip.setCreateUserId(userId);
            hrmSalarySlip.setCreateTime(LocalDateTime.now());
            hrmSalarySlipService.save(hrmSalarySlip);
            List<HrmSalarySlipOption> batchSaveSlip = new ArrayList<>();

            String salaryContent="姓名="+employee.getEmployeeName()+";工号="+employee.getJobNumber();
            for(int i=0;i<list.size();i++)
            {
                ComputeSalaryDto computeSalaryDto = list.get(i);
                HrmSalarySlipOption option = new HrmSalarySlipOption();
                option.setSlipId(hrmSalarySlip.getId());
                option.setName(computeSalaryDto.getName());
                option.setType(2);
                option.setCode(computeSalaryDto.getCode());
                option.setValue(computeSalaryDto.getValue());
                option.setPid(0L);
                option.setRemark(null);
                option.setSort(i + 1);
                option.setCreateUserId(userId);
                batchSaveSlip.add(option);
                salaryContent+=computeSalaryDto.getName()+":"+computeSalaryDto.getValue()+";";
            }
//            //工资条模板项
//            for (int i = 0; i < slipTemplateOption.size(); i++) {
//                HrmSalarySlipTemplateOption categoryOptionTemplate = slipTemplateOption.get(i);
//                HrmSalarySlipOption categoryOption = new HrmSalarySlipOption();
//                categoryOption.setSlipId(hrmSalarySlip.getId());
//                categoryOption.setName(categoryOptionTemplate.getName());
//                categoryOption.setType(categoryOptionTemplate.getType());
//                categoryOption.setCode(categoryOptionTemplate.getCode());
//                categoryOption.setValue("");
//                categoryOption.setRemark(categoryOptionTemplate.getRemark());
//                categoryOption.setPid(0L);
//                categoryOption.setSort(i + 1);
//                categoryOption.setCreateUserId(userId);
//                salarySlipOptionService.save(categoryOption);
//                List<HrmSalarySlipTemplateOption> optionList = categoryOptionTemplate.getOptionList();
//                //工资项
//                for (int j = 0; j < optionList.size(); j++) {
//                    HrmSalarySlipTemplateOption optionTemplate = optionList.get(j);
//                    String value = Objects.isNull(codeValueMap.get(optionTemplate.getCode())) ? "0" : codeValueMap.get(optionTemplate.getCode());
//                    if (sendSalarySlipBO.getHideEmpty() == 1 && new BigDecimal(value).equals(BigDecimal.ZERO)) {
//                        continue;
//                    }
//                    HrmSalarySlipOption option = new HrmSalarySlipOption();
//                    option.setSlipId(hrmSalarySlip.getId());
//                    option.setName(optionTemplate.getName());
//                    option.setType(optionTemplate.getType());
//                    option.setCode(optionTemplate.getCode());
//                    option.setValue(value);
//                    option.setPid(categoryOption.getId());
//                    option.setRemark(optionTemplate.getRemark());
//                    option.setSort(j + 1);+
//                    option.setCreateUserId(userId);
//                    batchSaveSlip.add(option);
//                }
//            }
            salarySlipOptionService.saveBatch(batchSaveSlip, batchSaveSlip.size());
            //发送短消息
            AdminMessage adminMessage = new AdminMessage();
            AdminMessageEnum adminMessageEnum = AdminMessageEnum.HRM_SEND_SLIP;
            adminMessage.setTitle(adminMessageEnum.getRemarks());
            adminMessage.setContent(hrmSalarySlip.getYear() + "," + hrmSalarySlip.getMonth()+","+salaryContent);
            adminMessage.setLabel(adminMessageEnum.getLabel());
            adminMessage.setType(adminMessageEnum.getType());
            adminMessage.setTypeId(hrmSalarySlip.getId());
            adminMessage.setCreateUser(userId);
            //接收手机号码
            adminMessage .setRecipientPhone(employee.getMobile());
            adminMessage.setCreateTime(LocalDateTime.now());
            //保存消息到数据库
//            adminMessageService.save(adminMessage);
//            ApplicationContextHolder.getBean(IAdminMessageService.class).save(adminMessage);

            //发送短信(暂无实现)
            try {
                salaryContent = "测试工资明细短息";
                SendSmsResponse sendSmsResponse = SMSUtils.sendSms(employee.getMobile(), salaryContent, salaryMonthRecord.getYear(), salaryMonthRecord.getMonth(), employee.getEmployeeName());
                System.out.println("4104886806"+sendSmsResponse.getMessage());
                if (sendSmsResponse.getCode() != null && sendSmsResponse.getCode().equals("OK")) {
                    if (redisTemplate.hasKey(employee.getMobile()) == false) {
                        Map<String, String> maps = new HashMap<>();
                        maps.put("employeeId", String.valueOf(employee.getEmployeeId()));
                        maps.put("year", String.valueOf(salaryMonthRecord.getYear()));
                        maps.put("month", String.valueOf(salaryMonthRecord.getMonth()));
                        maps.put("system", "hrysystem");
                        maps.put("token", sendSalarySlipBO.getToken());
                        redisTemplate.opsForValue().set(employee.getMobile(), JSON.toJSONString(maps));
                        redisTemplate.expire(employee.getMobile(), 7, TimeUnit.DAYS);
                    }
                    System.out.println("短信发送成功！");
                } else {
                    System.out.println("短信发送失败！");
                }
            }catch (Exception ax) {
                ax.printStackTrace();
            }
        }
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(salaryMonthRecord.getYear() + "年" + salaryMonthRecord.getMonth() + "月工资条");
        operationLog.setOperationInfo("发送了" + salaryMonthRecord.getYear() + "年" + salaryMonthRecord.getMonth() + "月工资条");
        return operationLog;
    }

    /**
     * 短信上行确认工资
     * @param smsUpDto
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public OperationLog SmsUpModify(SmsUpDto smsUpDto){
        LambdaUpdateWrapper<HrmSalarySlip> wrapper = new LambdaUpdateWrapper<HrmSalarySlip>()
                .set(HrmSalarySlip::getReadStatus, 1)
                .set(HrmSalarySlip::getSendTime, LocalDateTime.now())
                .eq(HrmSalarySlip::getEmployeeId,Long.valueOf(smsUpDto.getEmployeeId()))
                .and(i -> i.eq(HrmSalarySlip::getYear, smsUpDto.getYear())).and(i -> i.eq(HrmSalarySlip::getMonth, smsUpDto.getMonth()));
        salarySlipService.update(null, wrapper);

        LambdaUpdateWrapper<HrmSalaryMonthRecord> wrapper2 = new LambdaUpdateWrapper<HrmSalaryMonthRecord>()
                .set(HrmSalaryMonthRecord::getIsSend, 1)
                .eq(HrmSalaryMonthRecord::getYear, smsUpDto.getYear())
                .and(i -> i.eq(HrmSalaryMonthRecord::getMonth, smsUpDto.getMonth()));
        salaryMonthRecordService.update(null, wrapper2);

        //如果全部为1则更新为员工确认完成
        List<HrmSalarySlip> lists = salarySlipService.lambdaQuery().eq(HrmSalarySlip::getYear, smsUpDto.getYear())
                .and(i ->i.eq(HrmSalarySlip::getMonth, smsUpDto.getMonth())).list();
        Boolean IsPass = true;
        if (CollUtil.isNotEmpty(lists)) {
            for (HrmSalarySlip hrmSalarySlip : lists) {
                if (hrmSalarySlip.getReadStatus() != 1) {
                    IsPass = false;
                }
            }
        }
        if (IsPass == true) {
            LambdaUpdateWrapper<HrmSalaryMonthRecord> wrapper1 = new LambdaUpdateWrapper<HrmSalaryMonthRecord>()
                    .set(HrmSalaryMonthRecord::getCheckStatus, 12)
                    .eq(HrmSalaryMonthRecord::getYear, smsUpDto.getYear())
                    .and(i -> i.eq(HrmSalaryMonthRecord::getMonth, smsUpDto.getMonth()));
            salaryMonthRecordService.update(null, wrapper1);
        }
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationObject(smsUpDto.getEmployeeId() + "的" + smsUpDto.getYear() + "年" + smsUpDto.getMonth() + "月工资条");
        operationLog.setOperationInfo(smsUpDto.getEmployeeId() + "更新了" + smsUpDto.getYear() + "年" + smsUpDto.getMonth() + "月工资条");
        return operationLog;
    }

    public List<HrmSalarySlipOption> querySlipDetail(Long id)
    {
        List<HrmSalarySlipOption> list = salarySlipOptionService.lambdaQuery().eq(HrmSalarySlipOption::getSlipId, id)
                .orderByAsc(HrmSalarySlipOption::getSort).list();
        Map<Long, List<HrmSalarySlipOption>> optionListMap = list.stream().filter(option -> option.getPid() != 0).collect(Collectors.groupingBy(HrmSalarySlipOption::getPid));
        return list.stream().filter(option -> option.getPid() == 0).peek(option -> {
                    //添加语言包key
//                    Map<String, String> keyMap = new HashMap<>();
//                    keyMap.put("name_resourceKey", "hrm." + option.getCode());
//                    option.setLanguageKeyMap(keyMap);
                    option.setOptionList(optionListMap.get(option.getId()));
                }
        ).collect(Collectors.toList());
    }
}

package com.tianye.hrsystem.imple;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.tianye.hrsystem.common.BeanUtils;
import com.tianye.hrsystem.common.DateTimeUtils;
import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.common.PageableUtils;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.param.QueryAttendanceShiftParameter;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.HrmAttendanceHistoryShift;
import com.tianye.hrsystem.model.HrmAttendanceShift;
import com.tianye.hrsystem.repository.hrmAttendanceHistoryShiftRepository;
import com.tianye.hrsystem.repository.hrmAttendanceShiftRepository;
import com.tianye.hrsystem.service.IAttendanceShiftService;
import com.tianye.hrsystem.entity.bo.SetAttendanceShiftBO;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * @ClassName: AttendanceShiftServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月20日 23:13
 **/

@Service
public class AttendanceShiftServiceImpl implements IAttendanceShiftService {

    @Autowired
    hrmAttendanceShiftRepository shiftRep;
    @Autowired
    hrmAttendanceHistoryShiftRepository hisRep;
    SimpleDateFormat hourFormat=new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat shortFormat=new SimpleDateFormat("yyyy-MM-dd");
    @Override
    @Transactional
    public void Save(SetAttendanceShiftBO vo) throws Exception {
        if(vo.getShiftType()!=1) throw new Exception("目前只支持上下班打卡类型的班次设置!");
        LoginUserInfo Info= CompanyContext.get();
        HrmAttendanceShift newOne= BeanUtils.Clone(vo, HrmAttendanceShift.class);
        Date Now=new Date();
        String toDay=shortFormat.format(Now);
        String yesterDay=shortFormat.format(DateUtils.addDays(Now,-1));
        String tomorRow=shortFormat.format(DateUtils.addDays(Now,1));

        Date startTime=dateFormat.parse(toDay+" "+vo.getStart1()+":00");
        Date endTime=dateFormat.parse(toDay+" "+vo.getEnd1()+":00");
        Date advanceTime=dateFormat.parse(toDay+" "+vo.getAdvanceCard1()+":00");
        Date ldateTime=dateFormat.parse(toDay+" "+vo.getLateCard1()+":00");
        Date earlyTime=dateFormat.parse(toDay+" "+vo.getEarlyCard1()+":00");
        Date postponeTime=dateFormat.parse(toDay+" "+vo.getPostponeCard1()+":00");
        if(startTime.getTime()>endTime.getTime()){
            endTime=dateFormat.parse(tomorRow+" "+vo.getEnd1()+":00");
        }
        if(advanceTime.after(startTime)){
            advanceTime=dateFormat.parse(yesterDay+" "+vo.getAdvanceCard1()+":00");
        }
        if(startTime.getTime()>postponeTime.getTime()){
            postponeTime=dateFormat.parse(tomorRow+" "+vo.getPostponeCard1()+":00");
        }
        if(advanceTime.getTime()>=startTime.getTime()) throw new Exception("下班打卡时间不能晚于上班时间!");
        if(DateUtil.between(startTime,advanceTime, DateUnit.MINUTE)>480L){
            throw new Exception("上班打卡开始时间不能设置在上班时间8小时前!");
        }
        if(advanceTime.getTime()>ldateTime.getTime()){
            throw new Exception("上班打卡开始时间不能晚于上班打卡结束时间!");
        }
        if(ldateTime.getTime()<=startTime.getTime()){
            throw new Exception("上班打卡结束时间不能早于上班时间!");
        }
        if(ldateTime.getTime()>=(earlyTime.getTime())){
            throw new Exception("上班打卡结束时间不能晚于下班打卡时间!");
        }
        if(earlyTime.getTime()>=postponeTime.getTime()){
            throw new Exception("下班打卡开始时间不能晚于下班打卡结时间!");
        }
        if(earlyTime.getTime()<=startTime.getTime()){
            throw new Exception("下班打卡开始时间不能早于上班时间!");
        }
        if(earlyTime.getTime()<=endTime.getTime()){
            throw new Exception("下班打卡开始时间不能晚于下班时间!");
        }
        if(postponeTime.getTime()<=(endTime.getTime())){
            throw  new Exception("下班打卡时间不能早于下班时间!");
        }
        Integer  shiftMinutes= Convert.toInt(DateUtil.between(endTime,startTime,DateUnit.MINUTE));
        if(shiftMinutes>1440){
            throw new Exception("工作时间不能超过24小时");
        }
        Integer restMinutes=0;
        if(vo.getRestTimeStatus()==1){
            Date restStart=dateFormat.parse(toDay+" "+vo.getRestStartTime()+":00");
            Date restEnd=dateFormat.parse(toDay+" "+vo.getRestEndTime()+":00");
            if(restEnd.getTime()< restStart.getTime()){
                restEnd=dateFormat.parse(tomorRow+" "+vo.getRestEndTime()+":00");
            }
            restMinutes=Convert.toInt(DateUtil.between(restEnd,restStart,DateUnit.MINUTE));
        }
        if(endTime.getTime()<startTime.getTime()){
            endTime=dateFormat.parse(tomorRow+" "+vo.getEnd1()+":00");
        }
        newOne.setShiftHours(shiftMinutes-restMinutes);
        if(newOne.getShiftId()!=null){
            if(postponeTime.getTime()<endTime.getTime()){
                postponeTime=dateFormat.parse(tomorRow+" "+vo.getPostponeCard1()+":00");
            }
            newOne.setEffectTime(DateUtils.addMinutes(postponeTime,1));

            newOne.setUpdateTime(new Date());
            newOne.setUpdateUserId(Info.getUserIdValueL());

            Date updateTime=newOne.getUpdateTime();
//            Optional<HrmAttendanceHistoryShift>findHis=
//                    hisRep.findFirstByUpdateTimeBetween(DateTimeUtils.CloneToFirst(updateTime),
//                            DateTimeUtils.CloneToEnd(updateTime));
//            if(findHis.isPresent()==false){
//                HrmAttendanceHistoryShift hisOne=BeanUtils.Clone(newOne, HrmAttendanceHistoryShift.class);
//                hisOne.setUpdateTime(null);
//                hisOne.setUpdateUserId(null);
//                hisOne.setShiftHistoryId(newOne.getShiftId());
//                hisOne.setCreateTime(new Date());
//                hisOne.setCreateUserId(Info.getUserIdValueL());
//                hisRep.save(hisOne);
//            }
        } else {
            newOne.setEffectTime(new Date());
            newOne.setCreateUserId(Info.getUserIdValueL());
            newOne.setCreateTime(new Date());
        }
        shiftRep.save(newOne);
    }

    @Override
    public PageObject<HrmAttendanceShift> GetPageList(QueryAttendanceShiftParameter param) {
        Pageable pageable= PageableUtils.From(param);
        Page<HrmAttendanceShift> page=null;
        if(StringUtils.isEmpty(param.getShiftName())){
            page=shiftRep.findAll(pageable);
        } //else page=shiftRep.getAllByShiftNameLike(param.getShiftName(),pageable);

        return PageObject.Of(page);
    }

    @Override
    @Transactional
    public void DeleteOne(Long  ID) throws Exception {
        Optional<HrmAttendanceShift> findOnes=shiftRep.findById(ID);
        if(findOnes.isPresent()){
            HrmAttendanceShift One=findOnes.get();
            if(One.getIsDefaultSetting()==1){
                throw new Exception("不能删除默认考勤班次!");
            }
            shiftRep.delete(One);
        } else throw new Exception("删除的记录不存在，请刷新!");
    }
}

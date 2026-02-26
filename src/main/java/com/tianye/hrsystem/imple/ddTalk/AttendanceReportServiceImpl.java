package com.tianye.hrsystem.imple.ddTalk;

import com.tianye.hrsystem.model.ddTalk.RptAttendanceDetail;
import com.tianye.hrsystem.model.ddTalk.singleDate;
import com.tianye.hrsystem.model.tbattendancedetail;
import com.tianye.hrsystem.model.tbholiday;
import com.tianye.hrsystem.repository.tbattendancedetailRepository;
import com.tianye.hrsystem.repository.tbholidayRepository;
import com.tianye.hrsystem.service.ddTalk.IAttendanceReport;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: AttendanceReportServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月19日 14:49
 **/
@Service
public class AttendanceReportServiceImpl implements IAttendanceReport {

    SimpleDateFormat simple=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat show=new  SimpleDateFormat("yyyy-MM-dd");
    GregorianCalendar gre=new GregorianCalendar();
    @Autowired
    tbattendancedetailRepository detailRep;
    @Autowired
    tbholidayRepository  hoRep;

    List<tbholiday> Hs=new ArrayList<>();
    @Override
    public List<RptAttendanceDetail> Create(Date Begin,Date End) throws Exception {
        List<RptAttendanceDetail> Res=new ArrayList<>();
        Integer Year =Begin.getYear()+1900;
        Integer Month=Begin.getMonth()+1;
        List<tbattendancedetail> Datas=detailRep.findAllByWorkDateBetween(Begin,End);
        List<String> IDS=Datas.stream().map(f->f.getUserId()).distinct().collect(Collectors.toList());
        Hs=hoRep.findAllByYear(Year);
        for(int n=0;n<IDS.size();n++){
            String UserID=IDS.get(n);
            RptAttendanceDetail detail=new RptAttendanceDetail();
            List<singleDate> Ss=new ArrayList<>();
            for(Date  TDate=Begin;TDate.before(End);DateUtils.addDays(TDate,1)){
                List<tbattendancedetail> finds=Datas.stream()
                        .filter(f->f.getUserId().equals(UserID))
                        .filter(f->f.getWorkDate().equals(TDate))
                        .collect(Collectors.toList());

                String DateText=show.format(TDate);
                boolean IsWorkDay=IsWorkDay(TDate);
                String Status="";
                if(finds.size()==0){
                    if(IsWorkDay) Status="旷工"; else Status="休息";
                } else {
                    //(Normal：正常,Early：早退,Late：迟到,SeriousLate：严重迟到,Absenteeism：旷工迟到,NotSigned：未打卡)
                   List<String> SS=finds.stream().map(f->f.getTimeResult()).distinct().collect(Collectors.toList());
                   if(SS.contains("Early")){
                       Status="早退";
                   }
                   else if(SS.contains("Late") || SS.contains("SeriousLate")){
                       Status="迟到";
                   }
                   else if(SS.contains("Absenteeism") || SS.contains("NotSigned")){
                       Status="旷工";
                   }
                }
                singleDate single=new singleDate();
                single.setDate(DateText);
                List<String> DD=new ArrayList<>();
                DD.add(Status);
                single.setTime(DD);
                Ss.add(single);
            }
            detail.setDateList(Ss);
            Res.add(detail);
        }
        return Res;
    }

    private String GetWeekDay(Date D){
        gre.setTime(D);
        int weekday=gre.get(Calendar.DAY_OF_WEEK)-1; //0是星期天
        String []s={"星期天","星期一","星期二","星期三","星期四","星期五","星期六",};
        return s[weekday];
    }
    private Boolean IsWorkDay(Date D){
        Optional<tbholiday> findHs=Hs.stream().filter(f->f.getDate().equals(D)).findFirst();
        if(findHs.isPresent()){
            //是否是节假日
            return findHs.get().getHoliday();
        } else {
            String WeekDay=GetWeekDay(D);
            if(WeekDay.equals("星期六")){
                return IsSpecialSaturday(D);
            }
            else if(WeekDay.equals("星期日")){
                return false;
            }
            else return true;
        }
    }
    /**
     * create by: mmzs
     * description: TODO
     * create time:
     * 
     是否是单双周中的双周。是休息日。否则上班。
     * @return 
     */
    private Boolean IsSpecialSaturday(Date D){
        return true;
    }
}

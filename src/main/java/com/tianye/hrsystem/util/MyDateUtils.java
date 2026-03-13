package com.tianye.hrsystem.util;

import cn.hutool.core.date.DateUtil;
import com.tianye.hrsystem.model.HrmAutoTaskList;
import com.tianye.hrsystem.repository.hrmAutoTaskListRepository;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.stream;

/**
 * @ClassName: DateUtils
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年04月25日 14:20
 **/
@Component
public class MyDateUtils {
    @Autowired
    hrmAutoTaskListRepository autoRep;
    public Date getCurrent(){
        List<Integer> IDS=new ArrayList<>();
        Date now=new Date();
        List<HrmAutoTaskList> taskLists=autoRep.findAll();
        for(int i=0;i< taskLists.size();i++){
            HrmAutoTaskList d= taskLists.get(i);
            Boolean Ea=
                    d.getUserProcess() && d.getGroupProcess() && d.getPlanProcess() && d.getDetailProcess() && d.getReportProcess() && d.getLeaveProcess();
            if(Ea==false){
                IDS.add(d.getId());
            }
        }
        if(IDS.size()==0){
            //return DateUtil.parse("2024-08-31 23:59:00","yyyy-MM-dd HH:mm:ss");
            return now;
        } else {
                Integer MinID= IDS.stream().mapToInt(f->f).min().getAsInt();
                Optional<HrmAutoTaskList> minOnes=
                        taskLists.stream().filter(f->f.getId().equals(MinID)).findFirst();
                if(minOnes.isPresent()){
                     HrmAutoTaskList one= minOnes.get();
                     Date endTime=one.getEndTime();
                     if(endTime.before(now)) return endTime;else return now;
                } else return now;
        }
    }

    SimpleDateFormat simple=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat ssss=new SimpleDateFormat("yyyy-MM-dd");
    /**
     * create by: mmzs
     * description: TODO
     * create time:
     *
     是否是一月最中间的一天
     * @return
     */
    public boolean isMidDayOfMonth(Date date) throws Exception{
        Date endOfMonth=getEndDayOfMonth(date);
        int Day=endOfMonth.getDay();
        if(Day%2!=0){
            Day--;
        }
        int X= Day/2;
        return date.getDay()==X;
    }
    public List<Date> rangeDate(Date begin,Date end){
        List<Date> res=new ArrayList<>();
        for(Date d =begin;(d.before(end) || d.equals(end));d= DateUtils.addDays(d,1)){
            res.add(d);
        }
        return res;
    }
    public boolean isEndOfMonth(Date date) throws Exception{
        String X=ssss.format(date);
        String Y=ssss.format(getEndDayOfMonth(date));
        return X.equals(Y);
    }
    public boolean isBeginOfMonth(Date date) throws Exception{
        Date begin=getBeginDayOfMonth(date);
        String s1=simple.format(begin);
        String s2=simple.format(date);
        return s1.equals(s2);
    }
    public Date getEndDayOfMonth(Date date) throws Exception{
        Integer YearNum=date.getYear()+1900;
        String  Year=Integer.toString(YearNum);
        Integer MonthNum=date.getMonth()+2;
        String  Month= StringUtils.leftPad(Integer.toString(MonthNum),2,'0');
        Date willDate=simple.parse(Year+"-"+Month+"-01 23:59:59");
        Date x= DateUtils.addDays(willDate,-1);
        x.setHours(23);
        x.setMinutes(59);
        x.setSeconds(59);
        return x;
    }
    public Date getBeginDayOfMonth(Date date) throws Exception{
        Integer YearNum=date.getYear()+1900;
        String  Year=Integer.toString(YearNum);
        Integer MonthNum=date.getMonth()+1;
        String  Month= StringUtils.leftPad(Integer.toString(MonthNum),2,'0');
        Date willDate=simple.parse(Year+"-"+Month+"-01 00:00:00");
        return willDate;
    }
    public Date getMiddleDayOfMonth(Date date) throws Exception{
        Date endOfMonth=getEndDayOfMonth(date);
        int Day=endOfMonth.getDate();
        if(Day%2!=0){
            Day--;
        }
        int X= Day/2;

        Integer YearNum=date.getYear()+1900;
        String  Year=Integer.toString(YearNum);
        Integer MonthNum=date.getMonth()+1;
        String  Month= StringUtils.leftPad(Integer.toString(MonthNum),2,'0');
        String DayNum=StringUtils.leftPad(Integer.toString(X),2,'0');
        Date willDate=simple.parse(Year+"-"+Month+"-"+DayNum+ " 23:59:59");
        return willDate;
    }
    public List<Date[]> getDateRangeByLimit(Date date,int limit) throws Exception{
        List<Date[]> res=new ArrayList<>();
        Date endMonthDay = getEndDayOfMonth(date);
        Date beginDate = setItBegin(getBeginDayOfMonth(date));
        Date cutoffDate = setItEnd(date);

        if (limit <= 0) {
            return res;
        }

        while(true){
            Date endDate = DateUtils.addDays(beginDate, limit - 1);
            endDate = setItEnd(endDate);

            if (endDate.after(endMonthDay)) {
                endDate = endMonthDay;
            }

            if (endDate.after(cutoffDate) && endDate.before(endMonthDay)) {
                break;
            }

            Date[] D = new Date[2];
            D[0] = beginDate;
            D[1] = endDate;
            res.add(D);

            if (endDate.equals(endMonthDay) || endDate.after(endMonthDay)) {
                break;
            }

            if(endDate.before(cutoffDate)){
                beginDate = setItBegin(DateUtils.addDays(endDate, 1));
                if(beginDate.after(cutoffDate)){
                    break;
                }
            } else {
                break;
            }
        }
        return res;
    }
    public List<Date[]> getDateRangeByLimit(Date beginDate,Date endDate,int limit) throws Exception{
        List<Date[]> res=new ArrayList<>();
        if (limit <= 0) {
            return res;
        }

        Date rangeStart = setItBegin(beginDate);
        Date rangeEnd = setItEnd(endDate);

        while(true){
            Date curData = DateUtils.addDays(rangeStart, limit - 1);
            curData = setItEnd(curData);

            if(curData.before(rangeEnd)){
                Date[] D = new Date[2];
                D[0] = rangeStart;
                D[1] = curData;
                res.add(D);
                rangeStart = setItBegin(DateUtils.addDays(curData,1));
            } else {
                Date[] D=new Date[2];
                D[0]=rangeStart;
                D[1]=rangeEnd;
                res.add(D);
                break;
            }
        }
        return res;
    }
    public Date setItBegin(Date date){
        Date d=Date.from(date.toInstant());
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        return d;
    }
    public Date setItEnd(Date date){

        Date d=Date.from(date.toInstant());
        d.setHours(23);
        d.setMinutes(59);
        d.setSeconds(59);
        return d;
    }

    public static boolean isWithinRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return date.isAfter(startDate) && date.isBefore(endDate) || date.isEqual(startDate) || date.isEqual(endDate);
    }

}

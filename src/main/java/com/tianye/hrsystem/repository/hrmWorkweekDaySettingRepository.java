package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.modules.workweek.entity.HrmWorkweekDaySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface hrmWorkweekDaySettingRepository extends JpaRepository<HrmWorkweekDaySetting, Long> {

    List<HrmWorkweekDaySetting> findAllBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThanOrderByWorkDateAsc(Integer settingYear, Date beginTime, Date endTime);

    void deleteBySettingYearAndWorkDateGreaterThanEqualAndWorkDateLessThan(Integer settingYear, Date beginTime, Date endTime);
}

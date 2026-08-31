package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.modules.workweek.entity.HrmWorkweekSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface hrmWorkweekSettingRepository extends JpaRepository<HrmWorkweekSetting, Long> {

    List<HrmWorkweekSetting> findAllBySettingYearOrderByWeekNoAsc(Integer settingYear);
}

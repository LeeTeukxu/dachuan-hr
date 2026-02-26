package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.HrmAttendanceWifi;
import com.tianye.hrsystem.model.HrmAutoTaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

/**
 * @ClassName: hrmAutoTaskListRepository
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年10月07日 11:44
 **/
@Repository
public interface hrmAutoTaskListRepository extends JpaRepository<HrmAutoTaskList,Integer> {
    Optional<HrmAutoTaskList> findFirstByEndTime(Date endTime);
}

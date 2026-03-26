package com.tianye.hrsystem.service;

import com.dingtalk.api.response.OapiAttendanceGetsimplegroupsResponse;
import com.tianye.hrsystem.common.PageObject;
import com.tianye.hrsystem.model.UserObject;
import org.springframework.data.domain.Page;
import com.tianye.hrsystem.model.tbplanlist;

import java.util.Date;
import java.util.List;

public interface IWorkPlanService {
    void AddAll(List<tbplanlist> planList)throws Exception;
    void RemoveAll(List<Integer> IDArray) throws Exception;
    PageObject<tbplanlist> getMaxDate(Integer pageSize, Integer pageNum, String sortField, String sortOrder);
    PageObject<tbplanlist> loadBySelectedDate(Date selectedDate, boolean loadLast, Integer pageSize, Integer pageNum,
                                              String sortField, String sortOrder);

    List<UserObject>getUsers(String CompanyID) throws Exception;
    List<OapiAttendanceGetsimplegroupsResponse.AtGroupForTopVo> getAllGroups() throws Exception;
}

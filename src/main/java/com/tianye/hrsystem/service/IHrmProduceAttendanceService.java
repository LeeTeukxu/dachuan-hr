package com.tianye.hrsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseService;
import com.tianye.hrsystem.entity.bo.QueryMonthAttendanceBO;
import com.tianye.hrsystem.entity.po.HrmProduceAttendance;
import com.tianye.hrsystem.entity.vo.QueryMonthAttendanceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface IHrmProduceAttendanceService extends BaseService<HrmProduceAttendance> {

    void resolveProduceAttendanceData(MultipartFile multipartFile, String dates) throws Exception;

    Page<QueryMonthAttendanceVO> queryProduceAttendanceList(QueryMonthAttendanceBO queryMonthAttendanceBO) throws Exception;
}

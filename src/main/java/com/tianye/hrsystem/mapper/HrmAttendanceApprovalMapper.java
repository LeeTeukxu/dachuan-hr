package com.tianye.hrsystem.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.common.BasePage;
import com.tianye.hrsystem.entity.bo.QueryAttendanceApprovalPageBO;
import com.tianye.hrsystem.entity.vo.QueryAttendanceApprovalPageVO;
import com.tianye.hrsystem.model.tbattendanceapprove;
import org.apache.ibatis.annotations.Param;

public interface HrmAttendanceApprovalMapper extends BaseMapper<tbattendanceapprove> {

    BasePage<QueryAttendanceApprovalPageVO> queryPageList(BasePage<QueryAttendanceApprovalPageVO> page,
            @Param("data") QueryAttendanceApprovalPageBO queryBO);
}

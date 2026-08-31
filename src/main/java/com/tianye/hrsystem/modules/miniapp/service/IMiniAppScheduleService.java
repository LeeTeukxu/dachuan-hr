package com.tianye.hrsystem.modules.miniapp.service;

import com.tianye.hrsystem.modules.miniapp.vo.MiniAppProductScheduleVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppScheduleEmployeeVO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppScheduleSaveBO;
import com.tianye.hrsystem.modules.miniapp.vo.MiniAppStandardProductVO;

import java.util.List;

/** 小程序「添加排班」（生产排班）接口：员工池 / 标准产品 / 按天保存与查询 */
public interface IMiniAppScheduleService {

    /** 可排班员工池：在职员工（isDel=0 且 entryStatus in (1,3,4)），与 PC 端排班员工口径一致 */
    List<MiniAppScheduleEmployeeVO> listSchedulableEmployees();

    /** 标准产品树（复用 PC 端 hrm_workplan_product / _position 数据），positions 为岗位名称列表 */
    List<MiniAppStandardProductVO> listStandardProducts();

    /** 按天整表替换式保存产品排班，返回保存明细行数；员工姓名/部门取数据库快照 */
    int saveProductSchedule(Long employeeId, MiniAppScheduleSaveBO request) throws Exception;

    /** 查询某天的产品排班（workDate yyyy-MM-dd），无数据返回空 products */
    MiniAppProductScheduleVO queryProductSchedule(Long employeeId, String workDate) throws Exception;
}

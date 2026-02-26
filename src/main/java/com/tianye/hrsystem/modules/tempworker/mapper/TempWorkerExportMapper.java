package com.tianye.hrsystem.modules.tempworker.mapper;

import com.tianye.hrsystem.base.BaseMapper;
import com.tianye.hrsystem.modules.salary.entity.HrmConfig;
import com.tianye.hrsystem.modules.tempworker.entity.*;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;

public interface TempWorkerExportMapper extends BaseMapper<Tbweight>
{
    List<Tbworkshop> getWorksShopList();

    List<Tbbreed> getTbbreedList(String workShop);


    List<Tblregister> getRegisterUserList();


    List<HashMap<String,Object>> getEmpWorkDateList(Integer productId);


    /**
     * 每个人的称重信息
     * @param idNo
     * @return
     */
    List<Tbweight> getWorkerWeightList(String idNo);


    /**
     * 获取员工对每个产品的称重数据
     * @param idNo
     * @param
     * @return
     */
    ProductWeightSummary getProductWeightSummary(@Param("idNo") String idNo, @Param("productName") String productName);


    /**
     * 每个员工在每个车间上的 总量
     * @param
     * @param workShop
     * @return
     */
    ProductWeightSummary getWorkShopWeightSummary(@Param("idNo") String idNo, @Param("workShop") String workShop);


}

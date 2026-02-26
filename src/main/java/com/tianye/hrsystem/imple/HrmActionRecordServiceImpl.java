package com.tianye.hrsystem.imple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import com.alibaba.fastjson.JSON;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.QueryRecordListBO;
import com.tianye.hrsystem.entity.po.HrmActionRecord;
import com.tianye.hrsystem.entity.vo.HrmModelFiledVO;
import com.tianye.hrsystem.entity.vo.QueryRecordListVo;
import com.tianye.hrsystem.enums.HrmActionBehaviorEnum;
import com.tianye.hrsystem.enums.HrmActionTypeEnum;
import com.tianye.hrsystem.mapper.HrmActionRecordMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.service.IHrmActionRecordService;
import com.tianye.hrsystem.util.TransferUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * hrm员工操作记录表 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Service
public class HrmActionRecordServiceImpl extends BaseServiceImpl<HrmActionRecordMapper, HrmActionRecord> implements IHrmActionRecordService {
    @Override
    public boolean saveRecord(HrmActionTypeEnum actionTypeEnum, HrmActionBehaviorEnum behaviorEnum, List<String> content, List<String> transContent, Long typeId) {
        if (CollUtil.isEmpty(content)) {
            return true;
        }
        LoginUserInfo Info= CompanyContext.get();
        if(Info!=null){
            HrmActionRecord hrmActionRecord = new HrmActionRecord();
            hrmActionRecord.setIpAddress("127.0.0.1");
            hrmActionRecord.setType(actionTypeEnum.getValue());
            hrmActionRecord.setBehavior(behaviorEnum.getValue());
            hrmActionRecord.setTypeId(typeId);
            hrmActionRecord.setContent(JSON.toJSONString(content));
            hrmActionRecord.setTransContent(JSON.toJSONString(transContent));
            hrmActionRecord.setCreateUserId(Info.getUserIdValueL());
            hrmActionRecord.setCreateTime(LocalDateTime.now());
            return save(hrmActionRecord);
        } else return false;
    }

    @Override
    public List<QueryRecordListVo> queryRecordList(QueryRecordListBO queryRecordListBO) {
        List<HrmActionRecord> list = lambdaQuery().eq(HrmActionRecord::getType, queryRecordListBO.getType())
                .eq(HrmActionRecord::getTypeId, queryRecordListBO.getTypeId())
                .orderByDesc(HrmActionRecord::getCreateTime).list();
        List<QueryRecordListVo> recordListVOS = TransferUtil.transferList(list, QueryRecordListVo.class);
        recordListVOS.forEach(record -> {
//            SimpleUser simpleUser = UserCacheUtil.getSimpleUser(record.getCreateUserId());
//            record.setRealname(simpleUser.getNickname());
        });
        return recordListVOS;
    }

    @Override
    public List<HrmModelFiledVO> queryFieldValue(Dict kv) {
        return baseMapper.queryFieldValue(kv);
    }
}

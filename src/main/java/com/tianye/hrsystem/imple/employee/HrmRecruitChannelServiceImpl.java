package com.tianye.hrsystem.imple.employee;

import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.mapper.HrmRecruitChannelMapper;
import com.tianye.hrsystem.repository.hrmRecruitChannelRepository;
import com.tianye.hrsystem.service.employee.IHrmRecruitChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tianye.hrsystem.entity.po.HrmRecruitChannel;
import java.util.Optional;

/**
 * @ClassName: HrmRecruitChannelServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月23日 22:19
 **/
@Service
public class HrmRecruitChannelServiceImpl extends BaseServiceImpl<HrmRecruitChannelMapper,HrmRecruitChannel> implements IHrmRecruitChannelService {
}

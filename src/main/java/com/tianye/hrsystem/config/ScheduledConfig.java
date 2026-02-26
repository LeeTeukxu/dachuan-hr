package com.tianye.hrsystem.config;

import com.tianye.hrsystem.entity.vo.HrmScheduledVo;
import com.tianye.hrsystem.mapper.HrmScheduledMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 描述:
 * 定时调度配置类
 * @author 闲走天涯
 * @create 2020/9/3 11:15
 */
@Slf4j
//@Component
public class ScheduledConfig implements ApplicationRunner
{
    @Autowired
    HrmScheduledMapper scheduledDao;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception
    {
        try {
            List<HrmScheduledVo> vos = scheduledDao.findStartingScheduled();
            for (HrmScheduledVo vo : vos)
            {
                MultiValueMap<String,Object> multiValueMap = new LinkedMultiValueMap<>();
                multiValueMap.add("code",vo.getCode());
                Map result = restTemplate.postForObject("http://localhost:9080/hrsystem/scheduled/startCron",multiValueMap,Map.class);
                log.info("【启动 定时调度】接口={},结果={}", vo.getCornLink(), result);
            }
        }catch (Exception e){
            log.error("【启动 定时调度】失败",e);
        }
    }
}



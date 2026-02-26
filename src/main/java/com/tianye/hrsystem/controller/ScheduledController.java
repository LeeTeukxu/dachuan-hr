package com.tianye.hrsystem.controller;

import com.alibaba.fastjson.JSONObject;
import com.tianye.hrsystem.entity.vo.HrmScheduledVo;
import com.tianye.hrsystem.mapper.HrmScheduledMapper;
import com.tianye.hrsystem.util.RestTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * 描述:
 * java 定时调度 控制 执行代码端
 *
 * @author 闲走天涯
 * @create 2020/9/2 11:23
 */
@Slf4j
@RestController
@RequestMapping("/scheduled")
public class ScheduledController {

//    @Autowired
//    private RestTemplate restTemplate;

    @Autowired
    private HrmScheduledMapper scheduledDao;

    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;


    private static SimpleClientHttpRequestFactory requestFactory = null;
    static
    {

        requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(60000); // 连接超时时间，单位=毫秒
        requestFactory.setReadTimeout(60000); // 读取超时时间，单位=毫秒
    }
    private static RestTemplate restTemplate = new RestTemplate(requestFactory);


    Map<String, ScheduledFuture> map = new HashMap<>();

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    /**
     * 启动定时器
     *
     * @param code 定时器编号
     * @return
     */
    @RequestMapping("/startCron")
    public Map startCron(String code)
    {
        Map result = new HashMap();
        result.put("code", "1");
        try
        {
            log.info("【定时调度】启动 参数-》 code={}", code);
            if (StringUtils.isEmpty(code))
            {
                result.put("code", "1");
                result.put("msg", "参数错误");
                return result;
            }
            HrmScheduledVo vo = scheduledDao.selectScheDuleByCode(code);
            log.info("【定时调度】启动 参数-》 vo={}", vo.toString());
            if (StringUtils.isEmpty(vo.getCorn()) || StringUtils.isEmpty(vo.getCode()) || StringUtils.isEmpty(vo.getCornName()) || StringUtils.isEmpty(vo.getCornLink()) || StringUtils.isEmpty(vo.getMethod()))
            {
                result.put("code", "1");
                result.put("msg", "参数错误");
                return result;
            }

            ScheduledFuture future = threadPoolTaskScheduler.schedule(new taskRunnable(vo), new CronTrigger(vo.getCorn()));
            map.put(vo.getCode(), future);
            log.info("【定时调度】启动 定时器编号={},名称={}", vo.getCode(), vo.getCornName());
            if (!"1".equals(vo.getStatus()))
            {
                HashMap<String,Object> params = new HashMap<>();
                params.put("code",code);
                params.put("status","1");
                int i = scheduledDao.updateScheduledStatus(params);
            }
        }
        catch (Exception e)
        {
            log.error("【定时调度】启动 失败", e);
            result.put("code", "1");
            result.put("msg", "系统错误");
            return result;
        }
        result.put("code", "3");
        result.put("msg", "启动成功");
        return result;
    }

    /**
     * 停止定时器
     *
     * @param code
     * @return
     */
    @RequestMapping("/stopCron")
    public Map stopCron(String code) {
        Map result = new HashMap();
        result.put("code", "1");
        try {
            log.info("【定时调度】停止 参数-》 code={}", code);
            if (StringUtils.isEmpty(code)) {
                result.put("code", "1");
                result.put("msg", "参数错误");
                return result;
            }
            HrmScheduledVo vo = scheduledDao.selectScheDuleByCode(code);
            log.info("【定时调度】停止 参数-》 vo={}", vo.toString());
            if (StringUtils.isEmpty(vo.getCorn()) || StringUtils.isEmpty(vo.getCode()) || StringUtils.isEmpty(vo.getCornName()) || StringUtils.isEmpty(vo.getCornLink()) || StringUtils.isEmpty(vo.getMethod())) {
                result.put("code", "1");
                result.put("msg", "参数错误");
                return result;
            }
            ScheduledFuture future = map.get(vo.getCode());
            if (future != null) {
                future.cancel(true);
                log.info("【定时调度】停止 定时器编号={},名称={}", vo.getCode(), vo.getCornName());
                map.remove(vo.getCode());
                if (!"2".equals(vo.getStatus())) {
                    HashMap<String,Object> params = new HashMap<>();
                    params.put("code",code);
                    params.put("status","2");
                    int i = scheduledDao.updateScheduledStatus(params);
                }
            } else {
                result.put("code", "2");
                result.put("msg", "查无定时器编号,错误");
                return result;
            }
        } catch (Exception e) {
            log.error("【定时调度】停止 失败", e);
            result.put("code", "1");
            result.put("msg", "系统错误");
            return result;
        }
        result.put("code", "3");
        result.put("msg", "停止成功");
        return result;
    }

    /**
     * 定时器执行方法
     *
     * @param vo
     */
    private void dotask(HrmScheduledVo vo)
    {
        log.info("【定时任务】" + vo.getCode() + ":" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        try {
            JSONObject result = new JSONObject();
            if ("get".equals(vo.getMethod().toLowerCase()))
            {
               // result = restTemplate.getForObject(vo.getCornLink(), JSONObject.class);
            }
            else if ("post".equals(vo.getMethod().toLowerCase()))
            {
                if (vo.getCornLink().indexOf("?") != -1)
                {
                    String link = vo.getCornLink().substring(0, vo.getCornLink().indexOf("?"));
                    MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
                    String params = vo.getCornLink().substring(vo.getCornLink().indexOf("?") + 1, vo.getCornLink().length());
                    if (params.indexOf("&") != -1)
                    {
                        String[] paramArr = params.split("&");
                        for (String param : paramArr)
                        {
                            multiValueMap.add(param.split("=")[0], param.split("=")[1]);
                        }
                    }
                    else
                    {
                        multiValueMap.add(params.split("=")[0], params.split("=")[1]);
                    }
                    result = restTemplate.postForObject(link, multiValueMap, JSONObject.class);
                }
                else
                {
                    result = restTemplate.postForObject(vo.getCornLink(), null, JSONObject.class);
                }
            }
            log.info("【定时任务】"+vo.getCode()+":result="+JSONObject.toJSONString(result));
        } catch (Exception e) {
            log.error("【定时任务】接口(" + vo.getCornLink() + ") 访问失败" + e.getMessage());
        }
    }

    //多线程
    class taskRunnable implements Runnable {

        private HrmScheduledVo vo;

        public taskRunnable(HrmScheduledVo vo) {
            this.vo = vo;
        }

        @Override
        public void run() {
            //编写你自己的业务逻辑
            dotask(vo);
        }
    }
}


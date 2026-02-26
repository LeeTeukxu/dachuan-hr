package com.tianye.hrsystem.imple.ddTalk;

import com.aliyun.dingtalkhrm_1_0.models.QueryDismissionStaffIdListResponse;
import com.aliyun.dingtalkhrm_1_0.models.QueryDismissionStaffIdListResponseBody;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiAttendanceGetusergroupRequest;
import com.dingtalk.api.request.OapiSmartworkHrmEmployeeQueryonjobRequest;
import com.dingtalk.api.request.OapiSmartworkHrmEmployeeV2ListRequest;
import com.dingtalk.api.response.OapiAttendanceGetusergroupResponse;
import com.dingtalk.api.response.OapiSmartworkHrmEmployeeQueryonjobResponse;
import com.dingtalk.api.response.OapiSmartworkHrmEmployeeV2ListResponse;
import com.github.pagehelper.util.StringUtil;
import com.taobao.api.ApiException;
import com.tianye.hrsystem.common.DDTalkResposeLogger;
import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.repository.hrmAttendanceGroupRelationEmployeeRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import com.tianye.hrsystem.repository.tbattendanceuserRepository;
import com.tianye.hrsystem.service.ddTalk.IAccessToken;
import com.tianye.hrsystem.service.ddTalk.IUserManager;
import com.tianye.hrsystem.util.MyDateUtils;
import org.apache.tomcat.util.buf.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tianye.hrsystem.common.RedisImpl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: AttendanceUserManager
 * @Author: 肖新民
 * @*TODO:建立打卡的用户和系统的用户的关系。
 * @CreateTime: 2024年03月14日 15:17
 **/
@Service
public class AttendanceUserManager implements IUserManager {
    @Autowired
    IAccessToken tokenCreator;
    @Value("${ddTalk.agentId}")
    String agentId;

    @Autowired
    hrmEmployeeRepository empRep;
    @Autowired
    tbattendanceuserRepository userRep;
    Logger logger= LoggerFactory.getLogger(AttendanceUserManager.class);  // 【修复】：之前写错了类名
    @Autowired
    MyDateUtils dateUtils;
    @Autowired
    DDTalkResposeLogger ddLogger;
    @Override
    @Transactional
    public void GetAndSave() throws ApiException {
        Long Offset=0L;
        Long Begin=System.currentTimeMillis();
        int Num=0;
        
        // 【性能优化1】：不再每次删除全表，改用增量更新策略
        // userRep.deleteAll();  // 注释掉全表删除
        logger.info("开始增量同步考勤用户信息...");
        
        // 【性能优化2】：预加载现有映射关系到内存，只查询一次
        Map<String, tbattendanceuser> existingUserMap = userRep.findAll()
            .stream()
            .collect(Collectors.toMap(u -> u.getUserId(), u -> u, (u1, u2) -> u1));
        logger.info("预加载完成: 现有考勤用户映射{}个", existingUserMap.size());
        
        // 【性能优化3】：预加载员工数据到内存，只查询一次
        List<HrmEmployee> allEmployees = empRep.findAll();
        Map<String, HrmEmployee> employeeByMobileMap = allEmployees.stream()
            .filter(e -> StringUtil.isNotEmpty(e.getMobile()))
            .collect(Collectors.toMap(e -> e.getMobile().trim(), e -> e, (e1, e2) -> e1));
        Map<String, List<HrmEmployee>> employeeByNameMap = allEmployees.stream()
            .collect(Collectors.groupingBy(HrmEmployee::getEmployeeName));
        logger.info("预加载完成: 员工{}个，手机号索引{}个", allEmployees.size(), employeeByMobileMap.size());
        
        // 【性能优化4】：预加载考勤组关系，只查询一次
        Map<Long, Long> empIdToGroupIdMap = empRelRep.findAll()
            .stream()
            .collect(Collectors.toMap(
                r -> r.getEmployeeId(), 
                r -> r.getAttendanceGroupId(), 
                (g1, g2) -> g1
            ));
        logger.info("预加载完成: 考勤组关系{}个", empIdToGroupIdMap.size());
        
        // 【性能优化A】：收集所有要保存的用户数据，最后一次性批量保存
        List<tbattendanceuser> allUsersToSave = new ArrayList<>();
        
        // ============ 第一步：处理在职用户 ============
        logger.info("开始获取在职用户数据...");
        while (true) {
            String password = tokenCreator.Refresh();
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/queryonjob");
            OapiSmartworkHrmEmployeeQueryonjobRequest req = new OapiSmartworkHrmEmployeeQueryonjobRequest();
            req.setStatusList("2,3,5");
            req.setOffset(Offset);
            req.setSize(50L);  // 【API限制修复】：钉钉API限制size最大为50
            OapiSmartworkHrmEmployeeQueryonjobResponse rsp = client.execute(req, password);
            Date begin=dateUtils.getCurrent();
            ddLogger.Info(rsp,((DefaultDingTalkClient)client).getRequestUrl(),begin,AttendanceUserManager.class);
            
            // 【空指针修复】：先检查rsp是否为null
            if (rsp == null) {
                logger.error("钉钉API返回null，跳过本次循环");
                break;
            }
            
            if (rsp.getSuccess() == true) {
                OapiSmartworkHrmEmployeeQueryonjobResponse.PageResult PP= rsp.getResult();
                
                // 【空指针修复】：检查PP和DataList是否为null
                if (PP == null) {
                    logger.error("钉钉API返回的PageResult为null");
                    break;
                }
                
                List<String> IDS = PP.getDataList();
                if (IDS == null) {
                    logger.warn("钉钉API返回的DataList为null，跳过本次循环");
                    // 检查是否还有下一页
                    if(PP.getNextCursor()==null){
                        break;
                    }
                    Offset=PP.getNextCursor();
                    continue;
                }
                
                if(IDS.size()>0){
                    // 【性能优化5】：传递预加载的数据，避免重复查询
                    List<tbattendanceuser> Users=GetUserNameByID(IDS, existingUserMap, employeeByMobileMap, 
                        employeeByNameMap, empIdToGroupIdMap);
                    
                    // 【性能优化A】：先收集数据，不立即保存
                    allUsersToSave.addAll(Users);
                    Num += Users.size();
                    logger.debug("本批次获取{}个用户，累计{}个", Users.size(), Num);
                }
                if(PP.getNextCursor()==null){
                    logger.info("在职用户数据获取完成，共{}个用户", Num);
                    break;
                }
                Offset=PP.getNextCursor();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // ============ 第二步：处理离职用户 ============
        logger.info("开始获取离职用户数据...");
        Long NextToken=0L;
        int dismissedCount = 0;
        while(true){
            try {
                String password = tokenCreator.Refresh();
                com.aliyun.dingtalkhrm_1_0.Client client = createClient();
                com.aliyun.dingtalkhrm_1_0.models.QueryDismissionStaffIdListHeaders queryDismissionStaffIdListHeaders = new com.aliyun.dingtalkhrm_1_0.models.QueryDismissionStaffIdListHeaders();
                queryDismissionStaffIdListHeaders.xAcsDingtalkAccessToken = password;
                com.aliyun.dingtalkhrm_1_0.models.QueryDismissionStaffIdListRequest queryDismissionStaffIdListRequest = new com.aliyun.dingtalkhrm_1_0.models.QueryDismissionStaffIdListRequest()
                        .setNextToken(NextToken)
                        .setMaxResults(50);
                QueryDismissionStaffIdListResponse rsp=
                        client.queryDismissionStaffIdListWithOptions(queryDismissionStaffIdListRequest,
                         queryDismissionStaffIdListHeaders,
                    new com.aliyun.teautil.models.RuntimeOptions());
                    
               // 【空指针修复】：先检查rsp和body是否为null
               if (rsp == null || rsp.getBody() == null) {
                   logger.error("钉钉离职员工API返回null，跳过本次循环");
                   break;
               }
               
               QueryDismissionStaffIdListResponseBody body= rsp.getBody();
               List<String> disUsers= body.getUserIdList();
               if(disUsers==null ||  disUsers.size()<=0)break;

                // 【性能优化7】：离职用户也使用预加载数据
                List<tbattendanceuser> Users=GetUserNameByID(disUsers, existingUserMap, employeeByMobileMap, 
                    employeeByNameMap, empIdToGroupIdMap);
                
                // 【性能优化A】：先收集数据，不立即保存
                allUsersToSave.addAll(Users);
                dismissedCount += Users.size();
                Num += Users.size();
                logger.debug("本批次获取{}个离职用户，累计{}个", Users.size(), dismissedCount);
                
                if(body.getHasMore()==false){
                    logger.info("离职用户数据获取完成，共{}个用户", dismissedCount);
                    break;
                }
                NextToken=body.getNextToken();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            catch(Exception ax){
                ax.printStackTrace();
            }
        }
        
        // ============ 第三步：批量持久化（关键优化点）============
        Long saveBegin = System.currentTimeMillis();
        logger.info("开始批量保存，总共{}个用户数据...", allUsersToSave.size());
        
        if (allUsersToSave.size() > 0) {
            batchSaveUsers(allUsersToSave);
        }
        
        Long saveEnd = System.currentTimeMillis();
        Long totalEnd = System.currentTimeMillis();
        
        logger.info("==========================================");
        logger.info("同步完成统计:");
        logger.info("  - 总用户数: {}", Num);
        logger.info("  - 持久化耗时: {} 毫秒", saveEnd - saveBegin);
        logger.info("  - 总耗时: {} 毫秒", totalEnd - Begin);
        logger.info("==========================================");
    }
    
    /**
     * 批量保存用户数据（分批处理，避免内存溢出）
     * 
     * 性能优化说明：
     * 1. 分批持久化，每批500条
     * 2. 每批次flush后清理一级缓存，避免内存压力
     * 3. 使用JPA的saveAll，利用批处理机制
     * 
     * @param users 要保存的用户列表
     */
    private void batchSaveUsers(List<tbattendanceuser> users) {
        if (users == null || users.isEmpty()) {
            logger.warn("没有需要保存的用户数据");
            return;
        }
        
        int batchSize = 500;  // 每批次处理500条
        int totalBatches = (int) Math.ceil((double) users.size() / batchSize);
        
        logger.info("开始分批保存: 总数{}, 分{}批, 每批{}条", users.size(), totalBatches, batchSize);
        
        for (int i = 0; i < users.size(); i += batchSize) {
            int end = Math.min(i + batchSize, users.size());
            List<tbattendanceuser> batch = users.subList(i, end);
            
            int currentBatch = (i / batchSize) + 1;
            logger.debug("正在保存第{}/{}批，本批{}条", currentBatch, totalBatches, batch.size());
            
            try {
                // 批量保存
                userRep.saveAll(batch);
                
                // 强制flush到数据库
                if (entityManager != null) {
                    entityManager.flush();
                    entityManager.clear();  // 清理一级缓存，释放内存
                }
                
                logger.debug("第{}/{}批保存成功", currentBatch, totalBatches);
            } catch (Exception e) {
                logger.error("第{}/{}批保存失败，错误: {}", currentBatch, totalBatches, e.getMessage(), e);
                throw new RuntimeException("批量保存失败", e);
            }
        }
        
        logger.info("批量保存完成，总共保存{}条数据", users.size());
    }
    
    @Autowired
    hrmAttendanceGroupRelationEmployeeRepository empRelRep;
    
    @PersistenceContext
    private EntityManager entityManager;
    /**
     * create by: mmzs
     * description: TODO
     * create time:
     * <p>
     * 通过UserID查询用户名称
     *
     * @return
     */
    /**
     * 通过钉钉UserID查询用户信息并建立映射关系（性能优化版）
     * 
     * 优化说明：
     * 1. 接收预加载的数据，避免每次都查询数据库（从N次查询优化到1次）
     * 2. 优先使用已有映射关系（减少钉钉API调用）
     * 3. 新用户通过手机号匹配系统员工（解决同名员工问题）
     * 4. 手机号匹配失败，尝试姓名匹配
     * 5. 利用Redis缓存考勤组信息（减少钉钉API调用）
     * 
     * @param IDS 钉钉用户ID列表
     * @param existingUserMap 现有映射关系（预加载）
     * @param employeeByMobileMap 手机号索引（预加载）
     * @param employeeByNameMap 姓名索引（预加载）
     * @param empIdToGroupIdMap 考勤组关系（预加载）
     * @return 用户映射列表
     */
    public List<tbattendanceuser> GetUserNameByID(
            List<String> IDS,
            Map<String, tbattendanceuser> existingUserMap,
            Map<String, HrmEmployee> employeeByMobileMap,
            Map<String, List<HrmEmployee>> employeeByNameMap,
            Map<Long, Long> empIdToGroupIdMap) throws ApiException {
        
        List<tbattendanceuser> Users = new ArrayList<>();
        
        // 调用钉钉API获取用户信息
        String password = tokenCreator.Refresh();
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/smartwork/hrm/employee/v2/list");
        OapiSmartworkHrmEmployeeV2ListRequest req = new OapiSmartworkHrmEmployeeV2ListRequest();
        req.setUseridList(StringUtils.join(IDS, ','));
        req.setAgentid(Long.parseLong(agentId));
        
        // 【修复字段提取问题】：不设置fieldFilterList，让钉钉返回所有字段
        // 原因：设置字段过滤后，钉钉返回的FieldDataList可能为空
        OapiSmartworkHrmEmployeeV2ListResponse rsp = client.execute(req, password);
        
        // 【空指针修复】：先检查rsp是否为null
        if (rsp == null) {
            logger.error("钉钉用户详情API返回null，UserIDs: {}", StringUtils.join(IDS, ','));
            return Users;
        }
        
        // 空指针风险检查
        if (rsp.getResult() == null || rsp.getResult().size() == 0) {
            logger.warn("钉钉API返回空数据，请检查UserIDs: {}", StringUtils.join(IDS, ','));
            return Users;
        }
        
        List<OapiSmartworkHrmEmployeeV2ListResponse.EmpRosterFieldVo> Rs = rsp.getResult();
        logger.info("钉钉API返回了 {} 个用户", Rs != null ? Rs.size() : 0);
        
        // 【性能优化】：统计复用和新建的数量
        int reuseCount = 0;
        int newCount = 0;
        int skipCount = 0;
        
        // 遍历处理（从内存Map查询，不调用数据库）
        for (OapiSmartworkHrmEmployeeV2ListResponse.EmpRosterFieldVo V : Rs) {
            String userId = V.getUserid();
            
            try {
                // 【性能优化】：从内存Map查询已有映射，不查数据库
                tbattendanceuser existingUser = existingUserMap.get(userId);
                
                if (existingUser != null) {
                    // 已有映射，直接使用（不更新考勤组，节省API调用）
                    Users.add(existingUser);
                    reuseCount++;
                    logger.debug("复用已有映射: {} (userId={}, empId={})", 
                        existingUser.getUserName(), userId, existingUser.getEmpId());
                    continue;
                }
                
                // 新用户，提取基本信息
                // 【修复字段提取问题】：尝试多种字段名称
                String userName = extractFieldValue(V, "姓名");
                if (userName == null) userName = extractFieldValue(V, "name");
                if (userName == null) userName = extractFieldValue(V, "sys01-name");
                
                String mobile = extractFieldValue(V, "手机号码");
                if (mobile == null) mobile = extractFieldValue(V, "mobile");
                if (mobile == null) mobile = extractFieldValue(V, "sys02-mobile");
                if (mobile == null) mobile = extractFieldValue(V, "手机号");
                
                // 【修复手机号格式问题】：标准化手机号格式
                // 钉钉可能返回：+86-13800001111、+8613800001111、86-13800001111
                // 系统存储：13800001111
                // 需要统一去除国际区号和特殊字符
                if (mobile != null) {
                    mobile = normalizePhoneNumber(mobile);
                }
                
                if (StringUtil.isEmpty(userName)) {
                    logger.warn("钉钉用户姓名为空，跳过处理，userId: {}", userId);
                    skipCount++;
                    continue;
                }
                
                logger.info("处理新用户: 姓名={}, 手机号={}, userId={}", userName, 
                    StringUtil.isNotEmpty(mobile) ? mobile : "无", userId);
                
                HrmEmployee employee = null;
                String matchMethod = "未匹配";
                
                // 优先级1：手机号精确匹配
                if (StringUtil.isNotEmpty(mobile)) {
                    employee = employeeByMobileMap.get(mobile.trim());
                    if (employee != null) {
                        matchMethod = "手机号";
                    }
                }
                
                // 优先级2：姓名+手机号组合匹配
                if (employee == null && StringUtil.isNotEmpty(mobile)) {
                    List<HrmEmployee> employeesByName = employeeByNameMap.get(userName);
                    if (employeesByName != null && !employeesByName.isEmpty()) {
                        for (HrmEmployee emp : employeesByName) {
                            if (StringUtil.isNotEmpty(emp.getMobile()) && 
                                emp.getMobile().trim().equals(mobile.trim())) {
                                employee = emp;
                                matchMethod = "姓名+手机号组合";
                                break;
                            }
                        }
                        
                        if (employee == null) {
                            logger.warn("姓名+手机号组合匹配失败: 姓名={}, 手机号={}", userName, mobile.trim());
                        }
                    }
                }
                
                // 优先级3：姓名匹配（仅限唯一姓名）
                if (employee == null) {
                    List<HrmEmployee> employeesByName = employeeByNameMap.get(userName);
                    if (employeesByName != null && !employeesByName.isEmpty()) {
                        if (employeesByName.size() == 1) {
                            employee = employeesByName.get(0);
                            matchMethod = "姓名(唯一)";
                        } else {
                            logger.error("无法处理重名员工：姓名={}, userId={}, 系统中有{}个同名员工但手机号匹配失败", 
                                userName, userId, employeesByName.size());
                            skipCount++;
                            employee = null;
                        }
                    } else {
                        logger.warn("姓名匹配失败，系统中无此员工: 姓名={}", userName);
                        skipCount++;
                    }
                }
                
                if (employee == null) {
                    logger.error("无法匹配系统员工: 姓名={}, 手机号={}, 钉钉userId={}", 
                        userName, mobile != null ? mobile : "无", userId);
                    continue;
                }
                
                // 从内存Map获取考勤组
                Long groupId = empIdToGroupIdMap.get(employee.getEmployeeId());
                
                if (groupId == null) {
                    try {
                        groupId = GetGroupIDByUserID(userId);
                    } catch (Exception e) {
                        logger.warn("获取考勤组失败，userId: {}", userId);
                    }
                }
                
                // 创建映射关系
                tbattendanceuser user = new tbattendanceuser();
                user.setUserId(userId);
                user.setUserName(userName);
                user.setGroupId(groupId);
                user.setCreateMan(1);
                user.setEmpId(employee.getEmployeeId());
                user.setDepId(employee.getDeptId());
                user.setCreateTime(new Date());
                Users.add(user);
                newCount++;
                
            } catch (Exception e) {
                logger.error("处理钉钉用户异常，userId: {}, 错误: {}", userId, e.getMessage(), e);
                skipCount++;
            }
        }
        
        logger.info("批次处理完成: 复用{}个, 新建{}个, 跳过{}个", reuseCount, newCount, skipCount);
        return Users;
    }
    
    /**
     * 标准化手机号格式
     * 去除国际区号(+86、86)和特殊字符(-, 空格等)，保留纯数字
     * 
     * @param phone 原始手机号
     * @return 标准化后的手机号（纯数字）
     */
    private String normalizePhoneNumber(String phone) {
        if (StringUtil.isEmpty(phone)) {
            return phone;
        }
        
        String normalized = phone.trim();
        normalized = normalized.replaceAll("[\\s\\-\\(\\)\\+]", "");
        
        if (normalized.startsWith("86") && normalized.length() == 13) {
            normalized = normalized.substring(2);
        }
        
        return normalized;
    }
    
    /**
     * 从钉钉字段列表中提取指定字段的值
     */
    private String extractFieldValue(OapiSmartworkHrmEmployeeV2ListResponse.EmpRosterFieldVo V, String fieldName) {
        if (V.getFieldDataList() == null || V.getFieldDataList().isEmpty()) {
            return null;
        }
        
        for (OapiSmartworkHrmEmployeeV2ListResponse.EmpFieldDataVo fieldData : V.getFieldDataList()) {
            if (fieldData.getFieldName() != null && fieldData.getFieldName().equals(fieldName)) {
                if (fieldData.getFieldValueList() != null && !fieldData.getFieldValueList().isEmpty()) {
                    return fieldData.getFieldValueList().get(0).getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * 匹配系统员工
     * 
     * 匹配优先级：
     * 1. 手机号匹配（优先，解决同名问题）
     * 2. 姓名匹配（降级处理）
     */
    private Optional<HrmEmployee> matchEmployee(String userName, String mobile) {
        if (StringUtil.isNotEmpty(mobile)) {
            String cleanMobile = mobile.trim();
            Optional<HrmEmployee> empByMobile = empRep.findFirstByMobile(cleanMobile);
            
            if (empByMobile.isPresent()) {
                return empByMobile;
            }
        }
        
        Optional<HrmEmployee> empByName = empRep.findFirstByEmployeeName(userName);
        if (empByName.isPresent()) {
            return empByName;
        }
        
        return Optional.empty();
    }

    @Autowired
    private RedisImpl redisImpl;
    
    /**
     * 获取用户的考勤组ID
     * 使用Redis缓存减少API调用
     */
    public Long GetGroupIDByUserID(String UserID) throws ApiException{
        String cacheKey = "attendance:groupId:" + UserID;
        
        // 先查缓存
        Long cachedGroupId = (Long) redisImpl.get(cacheKey);
        if (cachedGroupId != null) {
            logger.debug("从缓存获取考勤组: userId={}, groupId={}", UserID, cachedGroupId);
            return cachedGroupId;
        }
        
        // 缓存未命中，调用API
        String password = tokenCreator.Refresh();
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/attendance/getusergroup");
        OapiAttendanceGetusergroupRequest req = new OapiAttendanceGetusergroupRequest();
        req.setUserid(UserID);
        OapiAttendanceGetusergroupResponse rsp = client.execute(req, password);
        
        if (rsp.getResult() == null) {
            logger.warn("获取用户考勤组失败，userId={}", UserID);
            return null;
        }
        
        Long groupId = rsp.getResult().getGroupId();
        
        // 缓存24小时
        redisImpl.setex(cacheKey, 24 * 60 * 60, groupId);  // 24小时 = 24*60*60秒
        logger.info("查询并缓存考勤组: userId={}, groupId={}", UserID, groupId);
        
        return groupId;
    }
    public static com.aliyun.dingtalkhrm_1_0.Client createClient() throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkhrm_1_0.Client(config);
    }
}

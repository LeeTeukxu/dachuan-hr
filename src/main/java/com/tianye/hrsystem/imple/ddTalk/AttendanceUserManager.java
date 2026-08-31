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
import org.springframework.transaction.support.TransactionTemplate;
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
    @Autowired
    TransactionTemplate transactionTemplate;
    @Override
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
            .filter(u -> u.getUserId() != null && !u.getUserId().trim().isEmpty())
            .collect(Collectors.toMap(u -> u.getUserId(), u -> u, this::pickPreferredUserRecord));
        logger.info("预加载完成: 现有考勤用户映射{}个", existingUserMap.size());
        
        // 【性能优化3】：预加载员工数据到内存，只查询一次
        List<HrmEmployee> allEmployees = empRep.findAll();
        Map<String, HrmEmployee> employeeByMobileMap = allEmployees.stream()
            .filter(e -> StringUtil.isNotEmpty(e.getMobile()))
            .collect(Collectors.toMap(e -> e.getMobile().trim(), e -> e, (e1, e2) -> e1));
        Map<String, List<HrmEmployee>> employeeByNameMap = allEmployees.stream()
            .collect(Collectors.groupingBy(HrmEmployee::getEmployeeName));
        Map<Long, HrmEmployee> employeeByIdMap = allEmployees.stream()
            .filter(e -> e.getEmployeeId() != null)
            .collect(Collectors.toMap(HrmEmployee::getEmployeeId, e -> e, (e1, e2) -> e1));
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
        Set<String> invalidUserIds = new LinkedHashSet<>();
        Set<String> remoteVisibleUserIds = new LinkedHashSet<>();
        
        // ============ 第一步：处理在职用户 ============
        logger.info("开始获取在职用户数据...");
        final int maxApiRetry = 3;
        int onJobFailureCount = 0;
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

            // 钉钉失败(null/false)时重试后快速失败：否则同参数立即重入，CPU 空转并放大钉钉调用量。
            // 不能带半截数据继续落库——remoteVisibleUserIds 不全会导致 deleteStaleMappings 误删在职用户映射。
            if (rsp == null || rsp.getSuccess() != true) {
                onJobFailureCount++;
                String errInfo = rsp == null ? "返回null" : ("errcode=" + rsp.getErrcode() + ",errmsg=" + rsp.getErrmsg());
                if (onJobFailureCount >= maxApiRetry) {
                    logger.error("在职用户拉取连续失败{}次({})，中止本次同步", onJobFailureCount, errInfo);
                    throw new ApiException("钉钉在职用户接口连续失败: " + errInfo);
                }
                logger.warn("在职用户拉取第{}次失败({})，1000ms后重试", onJobFailureCount, errInfo);
                sleepQuietly(1000L);
                continue;
            }
            onJobFailureCount = 0;

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
                remoteVisibleUserIds.addAll(IDS.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                // 【性能优化5】：传递预加载的数据，避免重复查询
                List<tbattendanceuser> Users=GetUserNameByID(IDS, existingUserMap, employeeByMobileMap,
                    employeeByNameMap, employeeByIdMap, empIdToGroupIdMap, invalidUserIds);

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
            sleepQuietly(100L);
        }

        // ============ 第二步：处理离职用户 ============
        logger.info("开始获取离职用户数据...");
        Long NextToken=0L;
        int dismissedCount = 0;
        int dismissFailureCount = 0;
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

               // 钉钉返回null时按失败重试：直接 break 会带着不完整的离职名单落库，
               // deleteStaleMappings 会把仍在职但未拉到的用户映射误删
               if (rsp == null || rsp.getBody() == null) {
                   throw new IllegalStateException("钉钉离职员工API返回null");
               }

               QueryDismissionStaffIdListResponseBody body= rsp.getBody();
               List<String> disUsers= body.getUserIdList();
               if(disUsers==null ||  disUsers.size()<=0)break;
               remoteVisibleUserIds.addAll(disUsers.stream().filter(Objects::nonNull).collect(Collectors.toList()));
                dismissFailureCount = 0;

                // 【性能优化7】：离职用户也使用预加载数据
                List<tbattendanceuser> Users=GetUserNameByID(disUsers, existingUserMap, employeeByMobileMap,
                    employeeByNameMap, employeeByIdMap, empIdToGroupIdMap, invalidUserIds);

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
                sleepQuietly(100L);
            }
            catch(Exception ax){
                // 吞异常后继续循环=同参数永久重放(100% CPU)；重试上限后快速失败，由断点续传机制恢复
                dismissFailureCount++;
                logger.error("钉钉离职员工接口第{}次调用失败", dismissFailureCount, ax);
                if (dismissFailureCount >= maxApiRetry) {
                    throw new ApiException("钉钉离职员工接口连续失败" + dismissFailureCount + "次: " + ax.getMessage());
                }
                sleepQuietly(1000L);
            }
        }
        
        // ============ 第三步：批量持久化（关键优化点）============
        // 事务只包住落库段：原先 @Transactional 包住整个方法，钉钉分页拉取+sleep 期间一直占用
        // 租户库连接；现在预加载和钉钉调用在事务外逐条短连接执行，落库段在事务内保证原子性
        Long saveBegin = System.currentTimeMillis();
        List<tbattendanceuser> deduplicatedUsers = deduplicateUsersByUserId(allUsersToSave);
        logger.info("开始批量保存，原始{}条，按userId去重后{}条...", allUsersToSave.size(), deduplicatedUsers.size());

        transactionTemplate.execute(status -> {
            if (deduplicatedUsers.size() > 0) {
                batchSaveUsers(deduplicatedUsers);
            }
            deleteStaleMappings(existingUserMap, deduplicatedUsers, employeeByIdMap, invalidUserIds, remoteVisibleUserIds);
            return null;
        });
        
        Long saveEnd = System.currentTimeMillis();
        Long totalEnd = System.currentTimeMillis();
        
        logger.info("==========================================");
        logger.info("同步完成统计:");
        logger.info("  - 总用户数: {}", Num);
        logger.info("  - 持久化耗时: {} 毫秒", saveEnd - saveBegin);
        logger.info("  - 总耗时: {} 毫秒", totalEnd - Begin);
        logger.info("==========================================");
    }

    /** 分页间休眠：中断时恢复中断标记并抛出，终止整次同步 */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("同步线程被中断", e);
        }
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
            Map<Long, HrmEmployee> employeeByIdMap,
            Map<Long, Long> empIdToGroupIdMap,
            Set<String> invalidUserIds) throws ApiException {
        
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
        collectMissingUserIds(IDS, Rs, invalidUserIds);
        
        // 【性能优化】：统计复用和新建的数量
        int reuseCount = 0;
        int newCount = 0;
        int skipCount = 0;
        
        // 遍历处理（从内存Map查询，不调用数据库）
        for (OapiSmartworkHrmEmployeeV2ListResponse.EmpRosterFieldVo V : Rs) {
            String userId = V.getUserid();
            
            try {
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
                
                tbattendanceuser existingUser = existingUserMap.get(userId);
                if (StringUtil.isEmpty(userName) && existingUser != null) {
                    userName = existingUser.getUserName();
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
                
                // 兜底：无法实时匹配时，复用历史 empId 映射，避免本次同步把员工丢失
                if (employee == null && existingUser != null && existingUser.getEmpId() != null) {
                    HrmEmployee mappedEmp = employeeByIdMap.get(existingUser.getEmpId());
                    if (mappedEmp != null) {
                        employee = mappedEmp;
                        matchMethod = "历史映射兜底";
                    }
                }

                if (employee == null) {
                    logger.error("无法匹配系统员工: 姓名={}, 手机号={}, 钉钉userId={}", 
                        userName, mobile != null ? mobile : "无", userId);
                    if (existingUser != null) {
                        logger.warn("保留原映射但跳过更新: userId={}, 原empId={}", userId, existingUser.getEmpId());
                    }
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
                
                // 创建/更新映射关系
                tbattendanceuser user = existingUser != null ? existingUser : new tbattendanceuser();
                user.setUserId(userId);
                user.setUserName(userName);
                if (groupId != null) {
                    user.setGroupId(groupId);
                }
                if (user.getCreateMan() == null) {
                    user.setCreateMan(1);
                }
                user.setEmpId(employee.getEmployeeId());
                user.setDepId(employee.getDeptId());
                if (user.getCreateTime() == null) {
                    user.setCreateTime(new Date());
                }
                Users.add(user);
                existingUserMap.put(userId, user);

                if (existingUser != null) {
                    reuseCount++;
                } else {
                    newCount++;
                }
                logger.debug("映射完成: userId={}, empId={}, 姓名={}, 匹配方式={}",
                        userId, employee.getEmployeeId(), userName, matchMethod);
                
            } catch (Exception e) {
                logger.error("处理钉钉用户异常，userId: {}, 错误: {}", userId, e.getMessage(), e);
                skipCount++;
            }
        }
        
        logger.info("批次处理完成: 复用{}个, 新建{}个, 跳过{}个", reuseCount, newCount, skipCount);
        return Users;
    }

    private void collectMissingUserIds(List<String> requestedIds,
                                       List<OapiSmartworkHrmEmployeeV2ListResponse.EmpRosterFieldVo> responseRows,
                                       Set<String> invalidUserIds) {
        if (requestedIds == null || requestedIds.isEmpty() || invalidUserIds == null) {
            return;
        }
        Set<String> existingIds = responseRows == null ? Collections.emptySet() :
                responseRows.stream()
                        .map(OapiSmartworkHrmEmployeeV2ListResponse.EmpRosterFieldVo::getUserid)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(item -> !item.isEmpty())
                        .collect(Collectors.toSet());
        for (String requestedId : requestedIds) {
            if (requestedId == null) {
                continue;
            }
            String normalized = requestedId.trim();
            if (!normalized.isEmpty() && !existingIds.contains(normalized)) {
                invalidUserIds.add(normalized);
            }
        }
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

    private tbattendanceuser pickPreferredUserRecord(tbattendanceuser left, tbattendanceuser right) {
        boolean leftHasEmp = left.getEmpId() != null && left.getEmpId() > 0;
        boolean rightHasEmp = right.getEmpId() != null && right.getEmpId() > 0;
        if (leftHasEmp != rightHasEmp) {
            return leftHasEmp ? left : right;
        }
        Date leftTime = left.getCreateTime();
        Date rightTime = right.getCreateTime();
        if (leftTime != null && rightTime != null && !leftTime.equals(rightTime)) {
            return leftTime.after(rightTime) ? left : right;
        }
        if (leftTime == null && rightTime != null) {
            return right;
        }
        if (leftTime != null && rightTime == null) {
            return left;
        }
        Integer leftId = left.getId();
        Integer rightId = right.getId();
        if (leftId != null && rightId != null && !leftId.equals(rightId)) {
            return leftId > rightId ? left : right;
        }
        return left;
    }

    private List<tbattendanceuser> deduplicateUsersByUserId(List<tbattendanceuser> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, tbattendanceuser> deduplicated = new LinkedHashMap<>();
        for (tbattendanceuser user : users) {
            if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
            tbattendanceuser existing = deduplicated.get(user.getUserId());
            if (existing == null) {
                deduplicated.put(user.getUserId(), user);
            } else {
                deduplicated.put(user.getUserId(), pickPreferredUserRecord(existing, user));
            }
        }
        return new ArrayList<>(deduplicated.values());
    }

    private void deleteStaleMappings(Map<String, tbattendanceuser> existingUserMap,
                                     List<tbattendanceuser> syncedUsers,
                                     Map<Long, HrmEmployee> employeeByIdMap,
                                     Set<String> invalidUserIds,
                                     Set<String> remoteVisibleUserIds) {
        List<tbattendanceuser> staleMappings = collectStaleMappingsToDelete(
                existingUserMap == null ? Collections.emptyList() : new ArrayList<>(existingUserMap.values()),
                syncedUsers,
                employeeByIdMap,
                invalidUserIds,
                remoteVisibleUserIds,
                syncedUsers != null && !syncedUsers.isEmpty()
        );
        if (staleMappings.isEmpty()) {
            return;
        }
        List<Integer> idsToDelete = staleMappings.stream()
                .map(tbattendanceuser::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (idsToDelete.isEmpty()) {
            return;
        }
        userRep.deleteAllByIdIn(idsToDelete);
        logger.info("已清理{}条失效考勤用户映射: {}", idsToDelete.size(), idsToDelete);
    }

    private List<tbattendanceuser> collectStaleMappingsToDelete(List<tbattendanceuser> existingUsers,
                                                                List<tbattendanceuser> syncedUsers,
                                                                Map<Long, HrmEmployee> employeeByIdMap,
                                                                Set<String> invalidUserIds,
                                                                Set<String> remoteVisibleUserIds,
                                                                boolean allowReplaceBySyncedUsers) {
        if (existingUsers == null || existingUsers.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<tbattendanceuser>> existingByEmpId = existingUsers.stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getEmpId() != null)
                .collect(Collectors.groupingBy(tbattendanceuser::getEmpId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, String> syncedUserIdByEmpId = syncedUsers == null ? Collections.emptyMap() :
                syncedUsers.stream()
                        .filter(Objects::nonNull)
                        .filter(user -> user.getEmpId() != null)
                        .filter(user -> StringUtil.isNotEmpty(user.getUserId()))
                        .collect(Collectors.toMap(tbattendanceuser::getEmpId, tbattendanceuser::getUserId, (left, right) -> left, LinkedHashMap::new));
        Set<String> invalidIds = invalidUserIds == null ? Collections.emptySet() : invalidUserIds;
        Set<String> visibleIds = remoteVisibleUserIds == null ? Collections.emptySet() : remoteVisibleUserIds;
        List<tbattendanceuser> result = new ArrayList<>();
        for (Map.Entry<Long, List<tbattendanceuser>> entry : existingByEmpId.entrySet()) {
            Long empId = entry.getKey();
            List<tbattendanceuser> mappings = entry.getValue();
            HrmEmployee employee = employeeByIdMap == null ? null : employeeByIdMap.get(empId);
            String preferredUserId = resolvePreferredUserId(employee, syncedUserIdByEmpId.get(empId), allowReplaceBySyncedUsers);
            for (tbattendanceuser mapping : mappings) {
                String userId = mapping.getUserId();
                if (StringUtil.isEmpty(userId)) {
                    continue;
                }
                if (StringUtil.isNotEmpty(preferredUserId) && !preferredUserId.equals(userId)) {
                    result.add(mapping);
                    continue;
                }
                if (invalidIds.contains(userId) && shouldRemoveInvalidMapping(employee, preferredUserId)) {
                    result.add(mapping);
                    continue;
                }
                if (shouldRemoveOrphanedMapping(mapping, employee, preferredUserId, visibleIds)) {
                    result.add(mapping);
                }
            }
        }
        return result.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(tbattendanceuser::getId, user -> user, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private String resolvePreferredUserId(HrmEmployee employee, String syncedUserId, boolean allowReplaceBySyncedUsers) {
        if (employee != null && StringUtil.isNotEmpty(employee.getDingtalkUserId())) {
            return employee.getDingtalkUserId().trim();
        }
        if (allowReplaceBySyncedUsers && StringUtil.isNotEmpty(syncedUserId)) {
            return syncedUserId.trim();
        }
        return null;
    }

    private boolean shouldRemoveInvalidMapping(HrmEmployee employee, String preferredUserId) {
        if (StringUtil.isNotEmpty(preferredUserId)) {
            return true;
        }
        if (employee == null) {
            return false;
        }
        Integer isDel = employee.getIsDel();
        Integer entryStatus = employee.getEntryStatus();
        return (isDel == null || isDel == 0) && (entryStatus == null || entryStatus == 1);
    }

    private boolean shouldRemoveOrphanedMapping(tbattendanceuser mapping,
                                                HrmEmployee employee,
                                                String preferredUserId,
                                                Set<String> remoteVisibleUserIds) {
        if (mapping == null || StringUtil.isEmpty(mapping.getUserId())) {
            return false;
        }
        if (StringUtil.isNotEmpty(preferredUserId)) {
            return false;
        }
        if (remoteVisibleUserIds == null || remoteVisibleUserIds.isEmpty()) {
            return false;
        }
        if (remoteVisibleUserIds.contains(mapping.getUserId())) {
            return false;
        }
        if (employee == null) {
            return false;
        }
        Integer isDel = employee.getIsDel();
        Integer entryStatus = employee.getEntryStatus();
        return (isDel == null || isDel == 0) && (entryStatus == null || entryStatus == 1);
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

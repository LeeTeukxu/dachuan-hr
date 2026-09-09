package com.tianye.hrsystem.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 「批处理任务」通用断点续传登记簿。
 *
 * <p>职责（只登记断点，不碰业务）：
 * <ul>
 *   <li>登记本次任务的「指纹」：标识两次运行是不是同一个任务（判断能否续传）；</li>
 *   <li>登记本次任务的「发起参数 payload」：可反解的原始请求参数（支持「通知中心→继续」用同参重发）；</li>
 *   <li>登记「已完成工作项集合 processed」：判断上次干到哪、续传时跳过哪些；</li>
 *   <li>登记上次收尾状态 status：RUNNING/FAILED/SUCCESS。</li>
 * </ul>
 *
 * <p>续传判定规则（与考勤现有语义一致）：
 * <ul>
 *   <li>上次 fingerprint 与本次<strong>不一致</strong>（换了任务/换了条件）→ 从头，重置 processed；</li>
 *   <li>一致且 status ∈ {FAILED, RUNNING} → <strong>续传</strong>：remaining = allItems − completed，从 remaining 继续；</li>
 *   <li>一致且 status == SUCCESS（上次已完成）→ 默认从头（用户主动再跑=想刷新），重置 processed。</li>
 * </ul>
 *
 * <p>Redis 异常一律不阻断业务：begin 在读写异常时当“无断点从头跑”，保证同步功能永远可用。
 *
 * <p>key 规则：{@code resumable:{biz}:{companyId}:{suffix}}，全部带 TTL（默认 24h），防陈旧断点。
 * 业务类型 biz：approval=审批获取、attendance=考勤同步（参照 {@link MonthlyFullSyncGuard}）。</p>
 *
 * <p>工作项 item 以字符串 id 表示（如员工 id）；processed 以 CSV 存 Redis，稳定去重。</p>
 */
@Component
public class ResumableJobCheckpoint {

    private static final Logger logger = LoggerFactory.getLogger(ResumableJobCheckpoint.class);

    /** 审批获取断点 */
    public static final String BIZ_APPROVAL = "approval";
    /** 考勤同步断点 */
    public static final String BIZ_ATTENDANCE = "attendance";

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SUCCESS = "SUCCESS";

    private static final String KEY_PREFIX = "resumable";
    private static final String SUFFIX_FINGERPRINT = "fingerprint";
    private static final String SUFFIX_PAYLOAD = "payload";
    private static final String SUFFIX_PROCESSED = "processed";
    private static final String SUFFIX_STATUS = "status";

    /** 断点默认过期时间：24 小时 */
    private static final int TTL_SECONDS = 86400;

    @Autowired
    private Redis redis;

    // ==================== 对外数据结构 ====================

    /** 一次断点会话：由 {@link #begin} 开启，携带续传所需的完成/剩余集合。 */
    public static class ResumeContext {
        public final String biz;
        public final String companyId;
        public final String fingerprint;
        /** 已确认完成的工作项 id 集合（续传时跳过它们） */
        public final Set<String> completed;
        /** 本次仍需处理的工作项 id 集合（从头=全部，续传=剩余） */
        public final List<String> remaining;
        /** 是否从断点恢复（false=从头） */
        public final boolean resumed;

        public ResumeContext(String biz, String companyId, String fingerprint,
                             Set<String> completed, List<String> remaining, boolean resumed) {
            this.biz = biz;
            this.companyId = companyId;
            this.fingerprint = fingerprint;
            this.completed = completed;
            this.remaining = remaining;
            this.resumed = resumed;
        }
    }

    // ==================== 主流程方法 ====================

    /**
     * 开启（或继续）一个任务，返回应从哪个集合继续。
     *
     * @param biz         业务类型（BIZ_APPROVAL / BIZ_ATTENDANCE）
     * @param companyId   租户公司 id
     * @param fingerprint 任务指纹（判等；两次一致才可能续传）
     * @param payload     可反解的发起参数（可为 null；供「继续/重发」时重建本次请求）
     * @param allItems    本次任务的全体工作项 id（去重后参与续传计算）
     */
    public ResumeContext begin(String biz, String companyId, String fingerprint,
                               String payload, List<String> allItems) {
        String cid = normalizeCompanyId(companyId);
        List<String> uniqueItems = dedupe(allItems);
        try {
            String lastFp = redis.get(key(biz, cid, SUFFIX_FINGERPRINT));
            String lastStatus = redis.get(key(biz, cid, SUFFIX_STATUS));
            boolean sameTask = fingerprint != null && fingerprint.equals(lastFp);
            boolean resumable = sameTask
                    && (STATUS_FAILED.equals(lastStatus) || STATUS_RUNNING.equals(lastStatus));

            Set<String> completed = new LinkedHashSet<>();
            List<String> remaining = new ArrayList<>(uniqueItems);
            boolean resumed = false;

            if (resumable) {
                // 续传：跳过已完成集合
                String processedStr = redis.get(key(biz, cid, SUFFIX_PROCESSED));
                completed.addAll(parseCsv(processedStr));
                remaining = filterOut(uniqueItems, completed);
                resumed = true;
                logger.info("[断点续传] {} 公司{} 任务一致且上次{}，从断点恢复：共{}项，已完成{}项，剩余{}项",
                        biz, cid, lastStatus, uniqueItems.size(), completed.size(), remaining.size());
            } else {
                if (!sameTask) {
                    logger.info("[断点续传] {} 公司{} 任务指纹不一致或无断点，从头开始：共{}项",
                            biz, cid, uniqueItems.size());
                } else {
                    logger.info("[断点续传] {} 公司{} 任务上次已完成({})，本次从头(刷新语义)：共{}项",
                            biz, cid, lastStatus, uniqueItems.size());
                }
                // 从头：清旧 processed，写入本次指纹+payload，置 RUNNING
                clearProcessed(biz, cid);
                if (fingerprint != null) {
                    redis.setex(key(biz, cid, SUFFIX_FINGERPRINT), TTL_SECONDS, fingerprint);
                }
                if (payload != null) {
                    redis.setex(key(biz, cid, SUFFIX_PAYLOAD), TTL_SECONDS, payload);
                }
                redis.setex(key(biz, cid, SUFFIX_STATUS), TTL_SECONDS, STATUS_RUNNING);
            }
            return new ResumeContext(biz, cid, fingerprint, completed, remaining, resumed);
        } catch (Exception e) {
            logger.warn("[断点续传] {} 公司{} begin 读 Redis 异常，按从头兜底: {}", biz, cid, e.getMessage());
            return new ResumeContext(biz, cid, fingerprint, new LinkedHashSet<>(), uniqueItems, false);
        }
    }

    /**
     * 登记一个工作项已完成（追加进 processed）。
     *
     * @param ctx  {@link #begin} 返回的会话
     * @param item 已完成的工作项 id
     */
    public void markProcessed(ResumeContext ctx, String item) {
        if (ctx == null || item == null || item.trim().isEmpty()) {
            return;
        }
        try {
            String processedStr = redis.get(key(ctx.biz, ctx.companyId, SUFFIX_PROCESSED));
            Set<String> set = parseCsv(processedStr);
            set.add(item.trim());
            redis.setex(key(ctx.biz, ctx.companyId, SUFFIX_PROCESSED), TTL_SECONDS, String.join(",", set));
            redis.setex(key(ctx.biz, ctx.companyId, SUFFIX_STATUS), TTL_SECONDS, STATUS_RUNNING);
        } catch (Exception e) {
            logger.warn("[断点续传] {} 公司{} markProcessed 写 Redis 异常(忽略): {}", ctx.biz, ctx.companyId, e.getMessage());
        }
    }

    /** 整轮成功：置 SUCCESS 并清空 processed（已完成无保留必要）。 */
    public void complete(ResumeContext ctx) {
        if (ctx == null) {
            return;
        }
        try {
            clearProcessed(ctx.biz, ctx.companyId);
            redis.setex(key(ctx.biz, ctx.companyId, SUFFIX_STATUS), TTL_SECONDS, STATUS_SUCCESS);
            logger.info("[断点续传] {} 公司{} 整轮成功，断点置 SUCCESS", ctx.biz, ctx.companyId);
        } catch (Exception e) {
            logger.warn("[断点续传] {} 公司{} complete 写 Redis 异常(忽略): {}", ctx.biz, ctx.companyId, e.getMessage());
        }
    }

    /** 整轮失败(异常)：置 FAILED，保留 processed/fingerprint/payload 供下次续传。 */
    public void fail(ResumeContext ctx) {
        if (ctx == null) {
            return;
        }
        markTerminalFailed(ctx.biz, ctx.companyId);
    }

    /**
     * 按公司置「终态 FAILED」（供自动重试最终放弃后调用，无需持有 ResumeContext）。
     * 仅当该公司该业务确已开启过一个真实任务(fingerprint 存在)才置 FAILED；
     * 否则(如参数校验就失败、尚未真正开跑)不落 FAILED，避免产生无意义的"可继续"断点。
     * 保留 processed/fingerprint/payload 供手动「继续」续传。
     */
    public void markTerminalFailed(String biz, String companyId) {
        String cid = normalizeCompanyId(companyId);
        try {
            String fp = redis.get(key(biz, cid, SUFFIX_FINGERPRINT));
            if (fp == null || fp.isEmpty()) {
                logger.info("[断点续传] {} 公司{} 无真实运行断点，忽略终态 FAILED", biz, cid);
                return;
            }
            redis.setex(key(biz, cid, SUFFIX_STATUS), TTL_SECONDS, STATUS_FAILED);
            logger.info("[断点续传] {} 公司{} 终态失败，断点置 FAILED 可续传", biz, cid);
        } catch (Exception e) {
            logger.warn("[断点续传] {} 公司{} markTerminalFailed 写 Redis 异常(忽略): {}", biz, cid, e.getMessage());
        }
    }

    // ==================== 查询/清理 ====================

    /**
     * 该公司该业务当前是否存在「可续传」断点（上次 FAILED/RUNNING 且留了 processed/fingerprint）。
     * 供通知中心「继续」按钮判断是否需要展示。
     */
    public boolean hasResumable(String biz, String companyId) {
        String cid = normalizeCompanyId(companyId);
        try {
            String status = redis.get(key(biz, cid, SUFFIX_STATUS));
            if (STATUS_FAILED.equals(status) || STATUS_RUNNING.equals(status)) {
                String fp = redis.get(key(biz, cid, SUFFIX_FINGERPRINT));
                return fp != null && !fp.isEmpty();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 读取该公司该业务保存的发起参数 payload（供「继续/重发」重建请求）。
     * 无则返回 null。
     */
    public String getSavedPayload(String biz, String companyId) {
        String cid = normalizeCompanyId(companyId);
        try {
            return redis.get(key(biz, cid, SUFFIX_PAYLOAD));
        } catch (Exception e) {
            return null;
        }
    }

    /** 强制清理该公司该业务的全部断点状态。 */
    public void clearAll(String biz, String companyId) {
        String cid = normalizeCompanyId(companyId);
        try {
            redis.del(key(biz, cid, SUFFIX_FINGERPRINT), key(biz, cid, SUFFIX_PAYLOAD),
                    key(biz, cid, SUFFIX_PROCESSED), key(biz, cid, SUFFIX_STATUS));
        } catch (Exception e) {
            logger.warn("[断点续传] {} 公司{} clearAll 写 Redis 异常(忽略): {}", biz, cid, e.getMessage());
        }
    }

    // ==================== 私有工具 ====================

    private String key(String biz, String companyId, String suffix) {
        return KEY_PREFIX + ":" + biz + ":" + companyId + ":" + suffix;
    }

    private String normalizeCompanyId(String companyId) {
        return (companyId == null || companyId.trim().isEmpty()) ? "unknown" : companyId;
    }

    private void clearProcessed(String biz, String companyId) {
        try {
            redis.del(key(biz, companyId, SUFFIX_PROCESSED));
        } catch (Exception e) {
            logger.warn("[断点续传] {} 公司{} clearProcessed 异常(忽略): {}", biz, companyId, e.getMessage());
        }
    }

    private Set<String> parseCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        return set;
    }

    private List<String> dedupe(List<String> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(new LinkedHashSet<>(items));
    }

    private List<String> filterOut(List<String> items, Set<String> completed) {
        List<String> result = new ArrayList<>();
        for (String item : items) {
            if (item != null && !completed.contains(item.trim())) {
                result.add(item);
            }
        }
        return result;
    }
}

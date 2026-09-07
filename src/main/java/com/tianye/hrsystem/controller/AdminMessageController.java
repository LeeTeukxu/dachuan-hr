package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.po.AdminMessage;
import com.tianye.hrsystem.imple.AdminMessageServiceImpl;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.successResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统消息通知 Controller
 */
@Controller
@RequestMapping("/adminMessage")
public class AdminMessageController {

    private static final Logger logger = LoggerFactory.getLogger(AdminMessageController.class);

    @Autowired
    private AdminMessageServiceImpl adminMessageService;

    @Autowired
    private com.tianye.hrsystem.autoTask.RetirementReminderTask retirementReminderTask;

    /**
     * 获取通知列表
     * @param pageNum 页码，默认1
     * @param pageSize 每页数量，默认10
     * @param isRead 已读筛选：null=全部，0=未读，1=已读
     */
    @RequestMapping("/list")
    @ResponseBody
    public successResult list(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Integer isRead) {
        successResult result = new successResult();
        try {
            LoginUserInfo userInfo = CompanyContext.get();
            Long userId = userInfo != null && userInfo.getUserIdValue() != null ? userInfo.getUserIdValue() : 0L;
            
            int offset = (pageNum - 1) * pageSize;
            List<AdminMessage> messages = adminMessageService.selectList(userId, pageSize, offset, isRead);
            int total = adminMessageService.countByUser(userId, isRead);
            int unreadCount = adminMessageService.countUnread(userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", messages);
            data.put("total", total);
            data.put("unreadCount", unreadCount);
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            
            result.setData(data);
        } catch (Exception e) {
            logger.error("获取通知列表失败", e);
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 获取未读数量
     */
    @RequestMapping("/unreadCount")
    @ResponseBody
    public successResult unreadCount() {
        successResult result = new successResult();
        try {
            LoginUserInfo userInfo = CompanyContext.get();
            // 顺带做一次到龄退休检查（每公司每天仅一次，见 RetirementReminderTask），
            // 因全项目 scheduling.enabled=false，定时任务不运行，靠本入口保证提醒能发出
            retirementReminderTask.checkCompanyQuietly(userInfo != null ? userInfo.getCompanyId() : null);
            Long userId = userInfo != null && userInfo.getUserIdValue() != null ? userInfo.getUserIdValue() : 0L;
            
            int count = adminMessageService.countUnread(userId);
            result.setData(count);
        } catch (Exception e) {
            logger.error("获取未读数量失败", e);
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 标记单条已读
     */
    @RequestMapping("/read")
    @ResponseBody
    public successResult markAsRead(@RequestParam Long messageId) {
        successResult result = new successResult();
        try {
            adminMessageService.markAsRead(messageId);
            result.setMessage("标记已读成功");
        } catch (Exception e) {
            logger.error("标记已读失败", e);
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 标记所有已读
     */
    @RequestMapping("/readAll")
    @ResponseBody
    public successResult markAllAsRead() {
        successResult result = new successResult();
        try {
            LoginUserInfo userInfo = CompanyContext.get();
            Long userId = userInfo != null && userInfo.getUserIdValue() != null ? userInfo.getUserIdValue() : 0L;
            
            adminMessageService.markAllAsRead(userId);
            result.setMessage("全部标记已读成功");
        } catch (Exception e) {
            logger.error("全部标记已读失败", e);
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 删除单条通知
     */
    @RequestMapping("/delete")
    @ResponseBody
    public successResult delete(@RequestParam Long messageId) {
        successResult result = new successResult();
        try {
            adminMessageService.deleteById(messageId);
            result.setMessage("删除成功");
        } catch (Exception e) {
            logger.error("删除通知失败", e);
            result.raiseException(e);
        }
        return result;
    }

    /**
     * 删除所有通知
     */
    @RequestMapping("/deleteAll")
    @ResponseBody
    public successResult deleteAll() {
        successResult result = new successResult();
        try {
            LoginUserInfo userInfo = CompanyContext.get();
            Long userId = userInfo != null && userInfo.getUserIdValue() != null ? userInfo.getUserIdValue() : 0L;
            
            adminMessageService.deleteAll(userId);
            result.setMessage("清空所有通知成功");
        } catch (Exception e) {
            logger.error("清空所有通知失败", e);
            result.raiseException(e);
        }
        return result;
    }
}

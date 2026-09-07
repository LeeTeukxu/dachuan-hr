package com.tianye.hrsystem.imple;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.entity.bo.AdminMessageBO;
import com.tianye.hrsystem.entity.bo.AdminMessageQueryBO;
import com.tianye.hrsystem.entity.po.AdminMessage;
import com.tianye.hrsystem.mapper.AdminMessageMapper;
import com.tianye.hrsystem.modules.salary.vo.AdminMessageVO;
import com.tianye.hrsystem.service.IAdminMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统消息表 服务实现类
 */
@Service
public class AdminMessageServiceImpl extends BaseServiceImpl<AdminMessageMapper, AdminMessage> implements IAdminMessageService {

    private static final Logger logger = LoggerFactory.getLogger(AdminMessageServiceImpl.class);

    @Autowired
    private AdminMessageMapper adminMessageMapper;

    @Override
    public Long saveOrUpdateMessage(AdminMessage message) {
        if (message.getMessageId() == null) {
            save(message);
        } else {
            updateById(message);
        }
        return message.getMessageId();
    }

    @Override
    public Page<AdminMessage> queryList(AdminMessageQueryBO adminMessageBO) {
        // 使用MyBatis-Plus的分页
        Page<AdminMessage> page = new Page<>(adminMessageBO.getPageNum(), adminMessageBO.getPageSize());
        // 这里可以使用MyBatis-Plus的条件构造器，或者调用自定义SQL
        return page;
    }

    @Override
    public AdminMessageVO queryUnreadCount() {
        AdminMessageVO vo = new AdminMessageVO();
        // 查询当前用户的未读数量
        // 这里需要从CompanyContext获取当前用户ID
        // 由于是系统级通知，所有用户都能看到，所以查询recipient_user=0的未读数量
        int count = adminMessageMapper.countUnread(0L);
        vo.setAllCount(count);
        return vo;
    }

    @Override
    public void addMessage(AdminMessageBO adminMessageBO) {
        AdminMessage message = new AdminMessage();
        message.setTitle(adminMessageBO.getTitle());
        message.setContent(adminMessageBO.getContent());
        message.setLabel(adminMessageBO.getLabel());
        message.setType(adminMessageBO.getType());
        message.setTypeId(adminMessageBO.getTypeId());
        message.setCreateUser(0L); // 系统创建
        message.setRecipientUser(0L); // 系统级通知
        message.setCreateTime(LocalDateTime.now());
        message.setIsRead(0);
        message.setLinkUrl(adminMessageBO.getLinkUrl());
        save(message);
    }

    @Override
    public void deleteEventMessage(Integer eventId) {
        // 根据事件ID删除消息
        // 这里可以扩展实现
    }

    @Override
    public void deleteById(Long messageId) {
        adminMessageMapper.deleteById(messageId);
    }

    @Override
    public void deleteByLabel(Integer label) {
        // 根据label删除消息
        // 这里可以扩展实现
    }

    /**
     * 查询通知列表（系统级+个人）
     */
    public List<AdminMessage> selectList(Long userId, int limit, int offset, Integer isRead) {
        return adminMessageMapper.selectList(userId, limit, offset, isRead);
    }

    /**
     * 查询未读数量
     */
    public int countUnread(Long userId) {
        return adminMessageMapper.countUnread(userId);
    }

    /**
     * 查询用户消息总数
     */
    public int countByUser(Long userId, Integer isRead) {
        return adminMessageMapper.countByUser(userId, isRead);
    }

    /**
     * 标记单条已读
     */
    @Transactional
    public void markAsRead(Long messageId) {
        adminMessageMapper.markAsRead(messageId);
    }

    /**
     * 标记所有已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        adminMessageMapper.markAllAsRead(userId);
    }

    /**
     * 删除所有通知
     */
    @Transactional
    public void deleteAll(Long userId) {
        adminMessageMapper.deleteAll(userId);
    }

    /**
     * 按类型删除通知
     */
    @Transactional
    public void deleteByType(Integer type, Long userId) {
        adminMessageMapper.deleteByType(type, userId);
    }

    /**
     * 更新通知内容（用于进度更新）
     */
    @Transactional
    public void updateContent(Long messageId, String content) {
        if (messageId == null || content == null) {
            return;
        }
        AdminMessage msg = new AdminMessage();
        msg.setMessageId(messageId);
        msg.setContent(content);
        msg.setUpdateTime(LocalDateTime.now());
        adminMessageMapper.updateById(msg);
    }
}

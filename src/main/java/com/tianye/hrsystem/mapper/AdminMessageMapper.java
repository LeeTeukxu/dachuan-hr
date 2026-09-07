package com.tianye.hrsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianye.hrsystem.entity.po.AdminMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统消息表 Mapper 接口
 */
@Mapper
public interface AdminMessageMapper extends BaseMapper<AdminMessage> {

    /**
     * 查询通知列表（系统级+个人）
     */
    List<AdminMessage> selectList(@Param("userId") Long userId, 
                                  @Param("limit") int limit, 
                                  @Param("offset") int offset,
                                  @Param("isRead") Integer isRead);

    /**
     * 查询未读数量
     */
    int countUnread(@Param("userId") Long userId);

    /**
     * 查询用户消息总数
     */
    int countByUser(@Param("userId") Long userId, @Param("isRead") Integer isRead);

    /**
     * 标记单条已读
     */
    int markAsRead(@Param("messageId") Long messageId);

    /**
     * 标记所有已读
     */
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 删除单条
     */
    int deleteById(@Param("messageId") Long messageId);

    /**
     * 删除所有
     */
    int deleteAll(@Param("userId") Long userId);

    /**
     * 按类型删除
     */
    int deleteByType(@Param("type") Integer type, @Param("userId") Long userId);
}

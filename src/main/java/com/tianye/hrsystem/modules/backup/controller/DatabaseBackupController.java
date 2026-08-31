package com.tianye.hrsystem.modules.backup.controller;

import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.modules.backup.service.DatabaseBackupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据库备份管理接口：手动备份、记录查询、还原、清理
 */
@RestController
@RequestMapping("/backup")
@Api(tags = "数据库备份")
@Slf4j
public class DatabaseBackupController {

    @Autowired
    private DatabaseBackupService backupService;

    @PostMapping("/create")
    @ApiOperation("手动立即备份")
    public successResult create() {
        successResult result = new successResult();
        try {
            int count = backupService.createBackup();
            result.setData(count);
            result.setMessage("备份完成，共备份 " + count + " 个数据库");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/list")
    @ApiOperation("备份记录列表")
    public successResult list() {
        successResult result = new successResult();
        try {
            result.setData(backupService.listRecords());
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/restore")
    @ApiOperation("还原备份")
    public successResult restore(@RequestParam Long id) {
        successResult result = new successResult();
        try {
            result.setMessage(backupService.restoreBackup(id));
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/cleanup")
    @ApiOperation("清理过期备份")
    public successResult cleanup() {
        successResult result = new successResult();
        try {
            int count = backupService.cleanupExpired();
            result.setData(count);
            result.setMessage("已清理 " + count + " 条过期备份");
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }
}

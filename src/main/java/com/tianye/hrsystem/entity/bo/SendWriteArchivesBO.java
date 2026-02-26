package com.tianye.hrsystem.entity.bo;

import lombok.Data;

import java.util.List;

@Data
public class SendWriteArchivesBO {
    private List<Long> userIds;

    private List<Long> deptIds;
}

package com.tianye.hrsystem.modules.miniapp.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 标准产品（GET /mp/schedule/standardProducts），positions 为岗位名称列表，按 sort 排序 */
public class MiniAppStandardProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private List<String> positions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPositions() {
        return positions;
    }

    public void setPositions(List<String> positions) {
        this.positions = positions;
    }
}

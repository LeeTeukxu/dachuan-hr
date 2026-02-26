package com.tianye.hrsystem.model;


import java.io.Serializable;
import java.util.List;

/**
 * @ClassName: TreeNode
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月11日 10:57
 **/
public class TreeNode implements Serializable {
    private Integer id;
    private Integer pid;
    private String text;
    private List<TreeNode> children;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }
}

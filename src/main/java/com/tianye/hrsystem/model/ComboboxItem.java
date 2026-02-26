package com.tianye.hrsystem.model;

import java.io.Serializable;

/**
 * @ClassName: ComboboxItem
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2025年07月24日 22:15
 **/
public class ComboboxItem implements Serializable {
    private String id;
    private String text;

    String begin1;
    String end1;

    String begin2;
    String end2;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getBegin1() {
        return begin1;
    }

    public void setBegin1(String begin1) {
        this.begin1 = begin1;
    }

    public String getEnd1() {
        return end1;
    }

    public void setEnd1(String end1) {
        this.end1 = end1;
    }

    public String getBegin2() {
        return begin2;
    }

    public void setBegin2(String begin2) {
        this.begin2 = begin2;
    }

    public String getEnd2() {
        return end2;
    }

    public void setEnd2(String end2) {
        this.end2 = end2;
    }
}

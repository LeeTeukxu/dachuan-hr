package com.tianye.hrsystem.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: PageObject
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月19日 21:03
 **/
public class PageObject<T> {
    boolean firstPage;
    boolean lastPage;
    Integer pageNumber;
    Integer pageSize;
    Integer totalPage;
    Integer totalRow;
    List<T> list;
    String message;

    public boolean isFirstPage() {
        return firstPage;
    }

    public void setFirstPage(boolean firstPage) {
        this.firstPage = firstPage;
    }

    public boolean isLastPage() {
        return lastPage;
    }

    public void setLastPage(boolean lastPage) {
        this.lastPage = lastPage;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public Integer getTotalRow() {
        return totalRow;
    }

    public void setTotalRow(Integer totalRow) {
        this.totalRow = totalRow;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static <T> PageObject<T> Of(Page<T> p){
        PageObject<T> res=new PageObject<>();
        res.setList(p.getContent());
        Pageable pageable=p.getPageable();
        boolean FirstPage=pageable.getPageNumber()==1;
        res.setPageNumber(pageable.getPageNumber());
        res.setFirstPage(FirstPage);
        res.setLastPage(!FirstPage);
        res.setTotalPage(p.getTotalPages());
        res.setMessage("");
        res.setTotalRow(Math.toIntExact(p.getTotalElements()));
        return res;
    }
    public static <T> PageObject<T> Error(String Error){
        PageObject<T>   res=new PageObject<>();
        res.setList(new ArrayList<>());
        res.setMessage(Error);
        return res;
    }
}

package com.tianye.hrsystem.service;

import com.tianye.hrsystem.model.tbmenulist;

import java.util.List;

/**
 * @ClassName: IMenuService
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月10日 15:35
 **/
public interface IMenuService {

    List<tbmenulist>  GetAll();

    tbmenulist Save(tbmenulist Menu);
    void Delete(Integer IDS);
}

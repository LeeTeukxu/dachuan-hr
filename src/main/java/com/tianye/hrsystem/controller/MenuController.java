package com.tianye.hrsystem.controller;

import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.model.tbmenulist;
import com.tianye.hrsystem.repository.tbmenulistRepository;
import com.tianye.hrsystem.service.IMenuService;
import com.tianye.hrsystem.util.MyDateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: MenuController
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月10日 13:35
 **/

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    tbmenulistRepository menuRep;
    @Autowired
    IMenuService menuService;
    @RequestMapping("/getAll")
    public List<tbmenulist> getAll() {
        return menuService.GetAll();
    }

    @PostMapping("/save")
    public successResult Save(@RequestBody tbmenulist Data) {
        successResult result = new successResult();
        try {
            tbmenulist list = menuService.Save(Data);
            result.setData(list);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    @PostMapping("/delete")
    public successResult Delete(Integer ID) {
        successResult result = new successResult();
        try {
            menuService.Delete(ID);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

}

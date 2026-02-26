package com.tianye.hrsystem.imple;

import com.tianye.hrsystem.model.tbmenulist;
import com.tianye.hrsystem.repository.tbmenulistRepository;
import com.tianye.hrsystem.service.IMenuService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: MenuServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月10日 15:46
 **/
@Service
public class MenuServiceImpl implements IMenuService {

    @Autowired
    tbmenulistRepository menuRep;

    @Override
    public List<tbmenulist> GetAll() {
        List<tbmenulist> Alls=menuRep.findAll();
        List<tbmenulist> res=Alls.stream().filter(f->f.getPid()==0).collect(Collectors.toList());
        res.forEach(f->EachOne(f,Alls));
        return res;
    }
    private void EachOne(tbmenulist one,List<tbmenulist> alls){
        Integer ID=one.getId();
        List<tbmenulist> finds=alls.stream().filter(f->f.getPid()==ID).collect(Collectors.toList());
        if(finds.size()>0){
            one.setChildren(finds);
            finds.forEach(f->EachOne(f,alls));
        }
    }

    @Override
    public tbmenulist Save(tbmenulist Menu) {
        List<tbmenulist>menus=menuRep.findAllByName(Menu.getName());
        if(menus.size()==0){
            if(Menu.getPid()==null)Menu.setPid(0);
            if(Menu.getId()==null){
                Menu.setCreateTime(new Date());
                Menu.setCanUse(1);
            }
        }  else {
            tbmenulist m=menus.get(0);
            if(Menu.getPid()!=null){
                m.setPid(Menu.getPid());
            }
            m.setUrl(Menu.getUrl());
            m.setName(Menu.getName());
            m.setCanUse(Menu.getCanUse());
            Menu=m;
        }
        return menuRep.save(Menu);
    }

    @Transactional
    @Override
    public void Delete(Integer ID) {
        menuRep.deleteById(ID);
    }
}

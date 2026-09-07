package com.tianye.hrsystem.imple;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.config.CompanyContext;
import com.tianye.hrsystem.entity.bo.DictDataBO;
import com.tianye.hrsystem.entity.vo.DictDataVO;
import com.tianye.hrsystem.mapper.DictDataMapper;
import com.tianye.hrsystem.model.LoginUserInfo;
import com.tianye.hrsystem.model.TreeNode;
import com.tianye.hrsystem.model.tbdictdata;
import com.tianye.hrsystem.model.tbplanlist;
import com.tianye.hrsystem.repository.tbdictdataRepository;
import com.tianye.hrsystem.service.IDictDataService;
import org.apache.commons.math3.analysis.function.Add;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @ClassName: tbDictDataServiceImpl
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月11日 11:08
 **/
@Service
public class tbDictDataServiceImpl implements IDictDataService {

    @Autowired
    tbdictdataRepository dictRep;
    @Autowired
    DictDataMapper dictDataMapper;
    @Override
    public List<TreeNode> getbyDtId(Integer DtID,boolean getAllChildren) {
        List<tbdictdata> datas=new ArrayList<>();
        if(getAllChildren) datas=dictRep.findAllByDtid(DtID);else datas=dictRep.findAllByDtidAndPid(DtID,0);
        List<TreeNode> Nodes=datas.stream().map(f->{
            TreeNode node=new TreeNode();
            node.setId(f.getId());
            node.setPid(f.getPid());
            node.setType(f.getSn());
            node.setText(f.getName());
            return node;
        }).collect(Collectors.toList());
        if(getAllChildren==true){
            List<TreeNode> res=Nodes.stream().filter(f->f.getPid()==0).collect(Collectors.toList());
            res.forEach(f->EachOne(f,Nodes));
            return res;
        } else return Nodes;
    }

    @Override
    public List<TreeNode> getbyPId(Integer PID) {
        List<tbdictdata> datas=dictRep.findAllByPid(PID);
        List<TreeNode> Nodes=datas.stream().map(f->{
            TreeNode node=new TreeNode();
            node.setId(f.getId());
            node.setPid(f.getPid());
            node.setType(f.getSn());
            node.setText(f.getName());
            return node;
        }).collect(Collectors.toList());
        return Nodes;
    }

    private void EachOne(TreeNode one,List<TreeNode> alls){
        Integer ID=one.getId();
        List<TreeNode> finds=alls.stream().filter(f->f.getPid()==ID).collect(Collectors.toList());
        if(finds.size()>0){
            one.setChildren(finds);
            finds.forEach(f->EachOne(f,alls));
        }
    }

    @Override
    @Transactional
    public Integer add(tbdictdata tbdictdata, String AddType) throws Exception {
        if (tbdictdata != null) {
            LoginUserInfo Info = CompanyContext.get();
            if (tbdictdata.getId() != null) {
                Optional<tbdictdata> findOne = dictRep.findById(tbdictdata.getId());
                if (findOne.isPresent()) {
                    tbdictdata dictdata = findOne.get();
                    if (tbdictdata.getDtid() == null) {
                        tbdictdata.setDtid(dictdata.getDtid());
                    }
                    if (tbdictdata.getPid() == null) {
                        tbdictdata.setPid(dictdata.getPid());
                    }
                    if (tbdictdata.getCanUse() == null) {
                        tbdictdata.setCanUse(dictdata.getCanUse());
                    }
                    if (tbdictdata.getCreateMan() == null) {
                        tbdictdata.setCreateMan(dictdata.getCreateMan());
                    }
                    if (tbdictdata.getCreateTime() == null) {
                        tbdictdata.setCreateTime(dictdata.getCreateTime());
                    }
                    if (tbdictdata.getSn() == null) {
                        tbdictdata.setSn(dictdata.getSn());
                    }
                    if (tbdictdata.getName() == null) {
                        tbdictdata.setName(dictdata.getName());
                    }
                } else if (tbdictdata.getDtid() == null) {
                    tbdictdata.setDtid(15);
                }
            } else {
                if (tbdictdata.getPid() == null) {
                    tbdictdata.setPid(0);
                }
                if (tbdictdata.getCanUse() == null) {
                    tbdictdata.setCanUse(1);
                }
                if (Info != null && tbdictdata.getCreateMan() == null) {
                    Long userIdVal = Info.getUserIdValue();
                    tbdictdata.setCreateMan(userIdVal == null ? null : userIdVal.intValue());
                }
                if (tbdictdata.getCreateTime() == null) {
                    tbdictdata.setCreateTime(DateTime.now());
                }
                if (tbdictdata.getDtid() == null) {
                    if ("chejian".equals(AddType)) {
                        tbdictdata.setDtid(16);
                    } else {
                        tbdictdata.setDtid(15);
                    }
                }
            }
            dictRep.save(tbdictdata);
        }
        return 0;
    }

    @Override
    public Page<DictDataVO> queryDictDataList(DictDataBO dictDataBO) {
        return dictDataMapper.queryDictDataList(dictDataBO.parse(), dictDataBO);
    }
}

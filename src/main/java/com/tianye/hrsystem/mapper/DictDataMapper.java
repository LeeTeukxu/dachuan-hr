package com.tianye.hrsystem.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianye.hrsystem.entity.bo.DictDataBO;
import com.tianye.hrsystem.entity.vo.DictDataVO;
import org.apache.ibatis.annotations.Param;

public interface DictDataMapper {
    Page<DictDataVO> queryDictDataList(Page<DictDataVO> parse,
                                   @Param("data") DictDataBO dictDataBO);
}

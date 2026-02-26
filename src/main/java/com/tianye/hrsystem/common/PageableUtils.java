package com.tianye.hrsystem.common;

import com.tianye.hrsystem.entity.param.RequestParameterBase;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * @ClassName: PageableUtils
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月21日 15:39
 **/
public class PageableUtils {

    public static Pageable From(PageEntity rqt){
        Pageable pageable= PageRequest.of(rqt.getPageNum(),rqt.getPageSize());
        return pageable;
    }
}

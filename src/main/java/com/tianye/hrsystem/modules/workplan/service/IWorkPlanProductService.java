package com.tianye.hrsystem.modules.workplan.service;

import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanPositionBO;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanProductBO;
import com.tianye.hrsystem.modules.workplan.vo.WorkPlanProductTreeVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IWorkPlanProductService {

    List<WorkPlanProductTreeVO> queryTree();

    Long saveProduct(SaveWorkPlanProductBO request);

    Long savePosition(SaveWorkPlanPositionBO request);

    void sortProducts(List<Long> productIds);

    void sortPositions(Long productId, List<Long> positionIds);

    void deleteProduct(Long id);

    void deletePosition(Long id);

    void importExcel(MultipartFile file) throws Exception;
}

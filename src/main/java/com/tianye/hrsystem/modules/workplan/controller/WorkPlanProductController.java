package com.tianye.hrsystem.modules.workplan.controller;

import com.tianye.hrsystem.common.ExcelTemplateDownloadUtils;
import com.tianye.hrsystem.model.successResult;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanPositionBO;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanProductBO;
import com.tianye.hrsystem.modules.workplan.bo.SortWorkPlanPositionBO;
import com.tianye.hrsystem.modules.workplan.bo.SortWorkPlanProductBO;
import com.tianye.hrsystem.modules.workplan.service.IWorkPlanProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/workPlanProduct")
public class WorkPlanProductController {

    @Autowired
    private IWorkPlanProductService productService;

    @RequestMapping("/queryTree")
    public successResult queryTree() {
        successResult result = new successResult();
        try {
            result.setData(productService.queryTree());
        } catch (Exception ex) {
            result.raiseException(ex);
        }
        return result;
    }

    @RequestMapping("/saveProduct")
    public successResult saveProduct(@RequestBody SaveWorkPlanProductBO request) {
        successResult result = new successResult();
        try {
            result.setData(productService.saveProduct(request));
        } catch (Exception ex) {
            result.raiseException(ex);
        }
        return result;
    }

    @RequestMapping("/savePosition")
    public successResult savePosition(@RequestBody SaveWorkPlanPositionBO request) {
        successResult result = new successResult();
        try {
            result.setData(productService.savePosition(request));
        } catch (Exception ex) {
            result.raiseException(ex);
        }
        return result;
    }

    @RequestMapping("/sortPositions")
    public successResult sortPositions(@RequestBody SortWorkPlanPositionBO request) {
        successResult result = new successResult();
        try {
            productService.sortPositions(request == null ? null : request.getProductId(),
                    request == null ? null : request.getPositionIds());
            result.setData(0);
        } catch (Exception ex) {
            result.raiseException(ex);
        }
        return result;
    }

    @RequestMapping("/sortProducts")
    public successResult sortProducts(@RequestBody SortWorkPlanProductBO request) {
        successResult result = new successResult();
        try {
            productService.sortProducts(request == null ? null : request.getProductIds());
            result.setData(0);
        } catch (Exception ex) {
            result.raiseException(ex);
        }
        return result;
    }

    @RequestMapping("/deleteProduct")
    public successResult deleteProduct(@RequestParam("id") Long id) {
        successResult result = new successResult();
        try {
            productService.deleteProduct(id);
            result.setData(0);
        } catch (Exception ex) {
            result.raiseException(ex);
        }
        return result;
    }

    @RequestMapping("/deletePosition")
    public successResult deletePosition(@RequestParam("id") Long id) {
        successResult result = new successResult();
        try {
            productService.deletePosition(id);
            result.setData(0);
        } catch (Exception ex) {
            result.raiseException(ex);
        }
        return result;
    }

    @GetMapping("/downloadTemplate")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        ExcelTemplateDownloadUtils.downloadClasspathTemplate("export/workplan_product.xlsx", "生产产品模版.xlsx", response);
    }

    @PostMapping("/importExcel")
    public successResult importExcel(@RequestParam("file") MultipartFile file) {
        successResult result = new successResult();
        try {
            productService.importExcel(file);
            result.setData(0);
        } catch (Exception ex) {
            result.raiseException(ex);
        }
        return result;
    }
}

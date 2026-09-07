package com.tianye.hrsystem.modules.workplan.service.impl;

import com.tianye.hrsystem.model.HrmEmployee;
import com.tianye.hrsystem.model.tbattendanceuser;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanPositionBO;
import com.tianye.hrsystem.modules.workplan.bo.SaveWorkPlanProductBO;
import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanPositionEmployee;
import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanProduct;
import com.tianye.hrsystem.modules.workplan.entity.HrmWorkPlanProductPosition;
import com.tianye.hrsystem.modules.workplan.service.IWorkPlanProductService;
import com.tianye.hrsystem.modules.workplan.vo.WorkPlanPositionEmployeeVO;
import com.tianye.hrsystem.modules.workplan.vo.WorkPlanPositionVO;
import com.tianye.hrsystem.modules.workplan.vo.WorkPlanProductTreeVO;
import com.tianye.hrsystem.repository.HrmWorkPlanPositionEmployeeRepository;
import com.tianye.hrsystem.repository.HrmWorkPlanProductPositionRepository;
import com.tianye.hrsystem.repository.HrmWorkPlanProductRepository;
import com.tianye.hrsystem.repository.hrmEmployeeRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.tianye.hrsystem.common.ExcelImportUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkPlanProductServiceImpl implements IWorkPlanProductService {

    @Autowired
    private HrmWorkPlanProductRepository productRepository;

    @Autowired
    private HrmWorkPlanProductPositionRepository positionRepository;

    @Autowired
    private HrmWorkPlanPositionEmployeeRepository positionEmployeeRepository;

    @Autowired
    private hrmEmployeeRepository employeeRepository;

    @Override
    public List<WorkPlanProductTreeVO> queryTree() {
        List<HrmWorkPlanProduct> products = productRepository.findAllByOrderBySortAscIdAsc();
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> productIds = products.stream()
                .map(HrmWorkPlanProduct::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<HrmWorkPlanProductPosition> positions = productIds.isEmpty()
                ? Collections.emptyList()
                : positionRepository.findAllByProductIdInOrderBySortAscIdAsc(productIds);
        List<Long> positionIds = positions.stream()
                .map(HrmWorkPlanProductPosition::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<HrmWorkPlanPositionEmployee> employees = positionIds.isEmpty()
                ? Collections.emptyList()
                : positionEmployeeRepository.findAllByPositionIdInOrderBySortAscIdAsc(positionIds);
        Map<Long, tbattendanceuser> attendanceUserMap = buildAttendanceUserMap(employees);

        Map<Long, List<WorkPlanPositionEmployeeVO>> employeesByPosition = employees.stream()
                .map(item -> toEmployeeVO(item, attendanceUserMap.get(item.getEmployeeId())))
                .collect(Collectors.groupingBy(WorkPlanPositionEmployeeVO::getPositionId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<WorkPlanPositionVO>> positionsByProduct = positions.stream()
                .map(item -> toPositionVO(item, employeesByPosition.get(item.getId())))
                .collect(Collectors.groupingBy(WorkPlanPositionVO::getProductId, LinkedHashMap::new, Collectors.toList()));

        return products.stream()
                .map(item -> toProductVO(item, positionsByProduct.get(item.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveProduct(SaveWorkPlanProductBO request) {
        if (request == null || StringUtils.isBlank(request.getProductName())) {
            throw new IllegalArgumentException("生产产品不能为空");
        }
        HrmWorkPlanProduct product = request.getId() == null
                ? new HrmWorkPlanProduct()
                : productRepository.findById(request.getId()).orElseGet(HrmWorkPlanProduct::new);
        Date now = new Date();
        product.setId(request.getId());
        product.setProductName(request.getProductName().trim());
        product.setSort(resolveProductSort(request, product));
        if (product.getCreateTime() == null) {
            product.setCreateTime(now);
        }
        product.setUpdateTime(now);
        return productRepository.save(product).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePosition(SaveWorkPlanPositionBO request) {
        if (request == null || request.getProductId() == null) {
            throw new IllegalArgumentException("生产产品不能为空");
        }
        if (StringUtils.isBlank(request.getPositionName())) {
            throw new IllegalArgumentException("岗位不能为空");
        }
        HrmWorkPlanProductPosition position = request.getId() == null
                ? new HrmWorkPlanProductPosition()
                : positionRepository.findById(request.getId()).orElseGet(HrmWorkPlanProductPosition::new);
        Date now = new Date();
        position.setId(request.getId());
        position.setProductId(request.getProductId());
        position.setPositionName(request.getPositionName().trim());
        position.setSort(resolvePositionSort(request, position));
        if (position.getCreateTime() == null) {
            position.setCreateTime(now);
        }
        position.setUpdateTime(now);
        HrmWorkPlanProductPosition savedPosition = positionRepository.save(position);
        Long positionId = savedPosition.getId();
        positionEmployeeRepository.deleteAllByPositionId(positionId);
        List<HrmWorkPlanPositionEmployee> employees = buildPositionEmployees(positionId, request.getEmployees(), now);
        if (!employees.isEmpty()) {
            positionEmployeeRepository.saveAll(employees);
        }
        return positionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<HrmWorkPlanProduct> products = Optional
                .ofNullable(productRepository.findAllByOrderBySortAscIdAsc())
                .orElse(Collections.emptyList());
        if (products.isEmpty()) {
            return;
        }
        Map<Long, HrmWorkPlanProduct> productsById = products.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(HrmWorkPlanProduct::getId, item -> item, (first, second) -> first, LinkedHashMap::new));
        List<HrmWorkPlanProduct> sorted = new ArrayList<>();
        Set<Long> usedIds = new LinkedHashSet<>();
        for (Long productId : productIds) {
            HrmWorkPlanProduct product = productsById.get(productId);
            if (product == null || usedIds.contains(productId)) {
                continue;
            }
            product.setSort(sorted.size());
            product.setUpdateTime(new Date());
            sorted.add(product);
            usedIds.add(productId);
        }
        for (HrmWorkPlanProduct product : products) {
            if (product.getId() != null && usedIds.contains(product.getId())) {
                continue;
            }
            product.setSort(sorted.size());
            product.setUpdateTime(new Date());
            sorted.add(product);
        }
        if (!sorted.isEmpty()) {
            productRepository.saveAll(sorted);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortPositions(Long productId, List<Long> positionIds) {
        if (productId == null) {
            throw new IllegalArgumentException("生产产品不能为空");
        }
        if (positionIds == null || positionIds.isEmpty()) {
            return;
        }
        List<HrmWorkPlanProductPosition> positions = Optional
                .ofNullable(positionRepository.findAllByProductIdOrderBySortAscIdAsc(productId))
                .orElse(Collections.emptyList());
        if (positions.isEmpty()) {
            return;
        }
        Map<Long, HrmWorkPlanProductPosition> positionsById = positions.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(HrmWorkPlanProductPosition::getId, item -> item, (first, second) -> first, LinkedHashMap::new));
        List<HrmWorkPlanProductPosition> sorted = new ArrayList<>();
        Set<Long> usedIds = new LinkedHashSet<>();
        for (Long positionId : positionIds) {
            HrmWorkPlanProductPosition position = positionsById.get(positionId);
            if (position == null || usedIds.contains(positionId)) {
                continue;
            }
            position.setSort(sorted.size());
            position.setUpdateTime(new Date());
            sorted.add(position);
            usedIds.add(positionId);
        }
        for (HrmWorkPlanProductPosition position : positions) {
            if (position.getId() != null && usedIds.contains(position.getId())) {
                continue;
            }
            position.setSort(sorted.size());
            position.setUpdateTime(new Date());
            sorted.add(position);
        }
        if (!sorted.isEmpty()) {
            positionRepository.saveAll(sorted);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        if (id == null) {
            return;
        }
        List<HrmWorkPlanProductPosition> positions = Optional.ofNullable(positionRepository.findAllByProductId(id))
                .orElse(Collections.emptyList());
        List<Long> positionIds = positions.stream()
                .map(HrmWorkPlanProductPosition::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!positionIds.isEmpty()) {
            positionEmployeeRepository.deleteAllByPositionIdIn(positionIds);
        }
        positionRepository.deleteAllByProductId(id);
        productRepository.deleteById(id);
    }

    private Integer resolveProductSort(SaveWorkPlanProductBO request, HrmWorkPlanProduct product) {
        if (request.getSort() != null) {
            return request.getSort();
        }
        if (request.getId() != null && product.getSort() != null) {
            return product.getSort();
        }
        HrmWorkPlanProduct maxProduct = productRepository.findFirstByOrderBySortDescIdDesc();
        return maxProduct == null || maxProduct.getSort() == null ? 0 : maxProduct.getSort() + 1;
    }

    private Integer resolvePositionSort(SaveWorkPlanPositionBO request, HrmWorkPlanProductPosition position) {
        if (request.getSort() != null) {
            return request.getSort();
        }
        if (request.getId() != null && position.getSort() != null) {
            return position.getSort();
        }
        HrmWorkPlanProductPosition maxPosition = positionRepository
                .findFirstByProductIdOrderBySortDescIdDesc(request.getProductId());
        return maxPosition == null || maxPosition.getSort() == null ? 0 : maxPosition.getSort() + 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePosition(Long id) {
        if (id == null) {
            return;
        }
        positionEmployeeRepository.deleteAllByPositionId(id);
        positionRepository.deleteById(id);
    }

    /**
     * 员工身份直接从档案构建（2026-09 决议：不再读写 tbattendanceuser）。
     * userId 取 dingtalk_user_id，缺失时回退 employeeId 字符串（读侧双键兼容）。
     */
    private Map<Long, tbattendanceuser> buildAttendanceUserMap(List<HrmWorkPlanPositionEmployee> employees) {
        List<Long> employeeIds = Optional.ofNullable(employees).orElse(Collections.emptyList()).stream()
                .map(HrmWorkPlanPositionEmployee::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, tbattendanceuser> result = new LinkedHashMap<>();
        for (HrmEmployee employee : employeeRepository.findAllByEmployeeIdIn(employeeIds)) {
            if (employee == null || employee.getEmployeeId() == null) {
                continue;
            }
            String userId = StringUtils.trimToEmpty(employee.getDingtalkUserId());
            if (StringUtils.isBlank(userId)) {
                userId = String.valueOf(employee.getEmployeeId());
            }
            tbattendanceuser identity = new tbattendanceuser();
            identity.setEmpId(employee.getEmployeeId());
            identity.setUserId(userId);
            identity.setUserName(employee.getEmployeeName());
            identity.setDepId(employee.getDeptId());
            result.put(employee.getEmployeeId(), identity);
        }
        return result;
    }

    private List<HrmWorkPlanPositionEmployee> buildPositionEmployees(Long positionId,
                                                                     List<SaveWorkPlanPositionBO.Employee> requestEmployees,
                                                                     Date now) {
        List<HrmWorkPlanPositionEmployee> employees = new ArrayList<>();
        List<SaveWorkPlanPositionBO.Employee> safeEmployees = Optional.ofNullable(requestEmployees)
                .orElse(Collections.emptyList());
        for (int index = 0; index < safeEmployees.size(); index += 1) {
            SaveWorkPlanPositionBO.Employee source = safeEmployees.get(index);
            if (source == null || (source.getEmployeeId() == null && StringUtils.isBlank(source.getEmployeeName()))) {
                continue;
            }
            HrmWorkPlanPositionEmployee target = new HrmWorkPlanPositionEmployee();
            target.setPositionId(positionId);
            target.setEmployeeId(source.getEmployeeId());
            target.setEmployeeName(StringUtils.trimToEmpty(source.getEmployeeName()));
            target.setMobile(StringUtils.trimToNull(source.getMobile()));
            target.setDeptName(StringUtils.trimToNull(source.getDeptName()));
            target.setPost(StringUtils.trimToNull(source.getPost()));
            target.setSort(source.getSort() == null ? index : source.getSort());
            target.setCreateTime(now);
            target.setUpdateTime(now);
            employees.add(target);
        }
        return employees;
    }

    private WorkPlanProductTreeVO toProductVO(HrmWorkPlanProduct product, List<WorkPlanPositionVO> positions) {
        WorkPlanProductTreeVO vo = new WorkPlanProductTreeVO();
        vo.setId(product.getId());
        vo.setProductName(product.getProductName());
        vo.setSort(product.getSort());
        vo.setPositions(positions == null ? new ArrayList<>() : positions);
        return vo;
    }

    private WorkPlanPositionVO toPositionVO(HrmWorkPlanProductPosition position,
                                            List<WorkPlanPositionEmployeeVO> employees) {
        WorkPlanPositionVO vo = new WorkPlanPositionVO();
        vo.setId(position.getId());
        vo.setProductId(position.getProductId());
        vo.setPositionName(position.getPositionName());
        vo.setSort(position.getSort());
        vo.setEmployees(employees == null ? new ArrayList<>() : employees);
        return vo;
    }

    private WorkPlanPositionEmployeeVO toEmployeeVO(HrmWorkPlanPositionEmployee employee,
                                                    tbattendanceuser attendanceUser) {
        WorkPlanPositionEmployeeVO vo = new WorkPlanPositionEmployeeVO();
        vo.setId(employee.getId());
        vo.setPositionId(employee.getPositionId());
        vo.setEmployeeId(employee.getEmployeeId());
        vo.setEmployeeName(employee.getEmployeeName());
        vo.setMobile(employee.getMobile());
        vo.setDeptName(employee.getDeptName());
        vo.setPost(employee.getPost());
        vo.setSort(employee.getSort());
        if (attendanceUser != null) {
            vo.setUserId(attendanceUser.getUserId());
            vo.setGroupId(attendanceUser.getGroupId() == null ? null : String.valueOf(attendanceUser.getGroupId()));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("请选择产品导入Excel文件");
        }

        List<String[]> rows = readImportRows(file);
        if (rows.isEmpty()) {
            throw new Exception("Excel文件中没有数据");
        }

        Date now = new Date();

        // 按产品名称分组，保持顺序
        LinkedHashMap<String, List<String[]>> productGroups = new LinkedHashMap<>();
        for (String[] row : rows) {
            String productName = row[0];
            if (StringUtils.isBlank(productName)) {
                continue;
            }
            productGroups.computeIfAbsent(productName.trim(), k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<String[]>> entry : productGroups.entrySet()) {
            String productName = entry.getKey();
            List<String[]> productRows = entry.getValue();

            // 获取或创建产品
            HrmWorkPlanProduct product = productRepository.findByProductName(productName);
            if (product == null) {
                product = new HrmWorkPlanProduct();
                product.setProductName(productName);
                product.setCreateTime(now);
            }
            product.setUpdateTime(now);
            product.setSort(resolveProductSortFromRows(productRows));
            product = productRepository.save(product);

            Long productId = product.getId();

            // 删除该产品下所有岗位（导入覆盖）
            positionEmployeeRepository.deleteAllByPositionIdIn(
                    positionRepository.findAllByProductId(productId).stream()
                            .map(HrmWorkPlanProductPosition::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
            positionRepository.deleteAllByProductId(productId);

            // 保存岗位
            int positionSort = 0;
            for (String[] row : productRows) {
                String positionName = row[1];
                if (StringUtils.isBlank(positionName)) {
                    continue;
                }

                HrmWorkPlanProductPosition position = new HrmWorkPlanProductPosition();
                position.setProductId(productId);
                position.setPositionName(positionName.trim());
                position.setSort(row.length > 2 && StringUtils.isNotBlank(row[2])
                        ? Integer.parseInt(row[2].trim())
                        : positionSort++);
                position.setCreateTime(now);
                position.setUpdateTime(now);
                positionRepository.save(position);
            }
        }
    }

    private List<String[]> readImportRows(MultipartFile file) throws Exception {
        List<String[]> rows = new ArrayList<>();
        // 流式读取（EasyExcel），避免超大 xlsx 把整张表的单元格模型加载进堆内存导致 OOM
        List<List<String>> all = ExcelImportUtil.readRows(file.getInputStream());
        if (all.size() < 2) {
            return rows;
        }
        // 第 1 行起为数据（跳过表头）
        for (int rowIndex = 1; rowIndex < all.size(); rowIndex++) {
            List<String> sheetRow = all.get(rowIndex);
            String productName = safeGet(sheetRow, 0).trim();
            String positionName = safeGet(sheetRow, 1).trim();
            if (StringUtils.isBlank(productName) && StringUtils.isBlank(positionName)) {
                continue;
            }
            String productSort = safeGet(sheetRow, 2);
            String positionSort = safeGet(sheetRow, 3);
            rows.add(new String[]{productName, positionName, productSort, positionSort});
        }
        return rows;
    }

    private static String safeGet(List<String> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return "";
        }
        return row.get(index);
    }

    private Integer resolveProductSortFromRows(List<String[]> rows) {
        for (String[] row : rows) {
            if (row.length > 2 && StringUtils.isNotBlank(row[2])) {
                try {
                    return Integer.parseInt(row[2].trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        HrmWorkPlanProduct maxProduct = productRepository.findFirstByOrderBySortDescIdDesc();
        return maxProduct == null || maxProduct.getSort() == null ? 0 : maxProduct.getSort() + 1;
    }
}

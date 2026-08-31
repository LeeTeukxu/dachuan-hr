package com.tianye.hrsystem.imple.employee;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianye.hrsystem.base.BaseServiceImpl;
import com.tianye.hrsystem.common.CrmException;
import com.tianye.hrsystem.entity.po.FileEntity;
import com.tianye.hrsystem.entity.po.HrmEmployee;
import com.tianye.hrsystem.entity.po.HrmEmployeeContract;
import com.tianye.hrsystem.entity.vo.ContractInformationVO;
import com.tianye.hrsystem.enums.EmployeeContractStatus;
import com.tianye.hrsystem.enums.EmployeeContractType;
import com.tianye.hrsystem.enums.HrmCodeEnum;
import com.tianye.hrsystem.enums.HrmActionBehaviorEnum;
import com.tianye.hrsystem.enums.LabelGroupEnum;
import com.tianye.hrsystem.mapper.HrmEmployeeContractMapper;
import com.tianye.hrsystem.mapper.HrmEmployeeMapper;
import com.tianye.hrsystem.service.AdminFileService;
import com.tianye.hrsystem.service.employee.IHrmEmployeeContractService;
import com.tianye.hrsystem.util.TransferUtil;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.tianye.hrsystem.common.ExcelImportUtil;

import javax.annotation.Resource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 员工合同 服务实现类
 * </p>
 *
 * @author huangmingbo
 * @since 2020-05-12
 */
@Service
public class HrmEmployeeContractServiceImpl extends BaseServiceImpl<HrmEmployeeContractMapper, HrmEmployeeContract> implements IHrmEmployeeContractService {

    @Autowired
    private AdminFileService adminFileService;

    @Resource
    private EmployeeActionRecordServiceImpl employeeActionRecordService;

    @Resource
    private HrmEmployeeMapper employeeMapper;

    @Override
    public List<ContractInformationVO> contractInformation(Long employeeId) {
        List<HrmEmployeeContract> contractList = lambdaQuery().eq(HrmEmployeeContract::getEmployeeId, employeeId).orderByAsc(HrmEmployeeContract::getSort).list();
        List<ContractInformationVO> contractInformationVOList = TransferUtil.transferList(contractList, ContractInformationVO.class);
        LocalDate today = LocalDate.now();
        contractInformationVOList.forEach(contractInformationVO -> {
            if (StrUtil.isNotEmpty(contractInformationVO.getBatchId())) {
                List<FileEntity> listResult = adminFileService.queryFileList(contractInformationVO.getBatchId());
                contractInformationVO.setFileList(listResult);
            }
            // 合同状态按日期动态纠正:到期自动变"已到期"、未到开始日期为"未执行";无固定期限合同(contractType=2)不判到期
            if (!Integer.valueOf(2).equals(contractInformationVO.getContractType()) && contractInformationVO.getEndTime() != null) {
                if (contractInformationVO.getEndTime().isBefore(today)) {
                    contractInformationVO.setStatus(2);
                } else if (contractInformationVO.getStartTime() != null
                        && contractInformationVO.getStartTime().isAfter(today)
                        && !Integer.valueOf(0).equals(contractInformationVO.getStatus())) {
                    contractInformationVO.setStatus(0);
                } else if (Integer.valueOf(2).equals(contractInformationVO.getStatus())) {
                    contractInformationVO.setStatus(1);
                }
            }
        });
        return contractInformationVOList;
    }

    @Override
    public void addOrUpdateContract(HrmEmployeeContract employeeContract) {
        normalizeContractTerm(employeeContract);
        if (employeeContract.getContractId() == null) {
            employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.ADD, LabelGroupEnum.CONTRACT, employeeContract.getEmployeeId());
        } else {
            HrmEmployeeContract old = getById(employeeContract.getContractId());
            employeeActionRecordService.entityUpdateRecord(LabelGroupEnum.CONTRACT, BeanUtil.beanToMap(old), BeanUtil.beanToMap(employeeContract), employeeContract.getEmployeeId());
        }
        saveOrUpdate(employeeContract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer importContracts(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "请选择合同导入文件");
        }
        Map<String, HrmEmployee> employeeMap = buildContractImportEmployeeMap();
        // 流式读取（EasyExcel），避免超大 xlsx 把整张表的单元格模型加载进堆内存导致 OOM
        List<List<String>> all = ExcelImportUtil.readRows(file.getInputStream());
        if (all.isEmpty()) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "合同导入文件缺少表头");
        }
        List<String> headerRow = all.get(0);
        Map<String, Integer> headerIndexes = buildHeaderIndexes(headerRow);
        requireHeader(headerIndexes, "姓名");
        requireHeader(headerIndexes, "电话");
        requireHeader(headerIndexes, "合同类型");
        requireHeader(headerIndexes, "合同开始日期");
        requireHeader(headerIndexes, "合同结束日期");
        requireHeader(headerIndexes, "合同状态");

        int importedCount = 0;
        for (int rowIndex = 1; rowIndex < all.size(); rowIndex++) {
            List<String> row = all.get(rowIndex);
            if (isContractImportRowEmpty(row, headerIndexes)) {
                continue;
            }
            HrmEmployeeContract contract = buildImportContract(row, headerIndexes, employeeMap, rowIndex + 1);
            addOrUpdateContract(contract);
            importedCount++;
        }
        if (importedCount == 0) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "导入文件没有可导入的合同数据");
        }
        return importedCount;
    }

    @Override
    public void deleteContract(Long contractId) {
        HrmEmployeeContract contract = getById(contractId);
        employeeActionRecordService.addOrDeleteRecord(HrmActionBehaviorEnum.DELETE, LabelGroupEnum.CONTRACT, contract.getEmployeeId());
        removeById(contractId);
    }

    @Override
    public List<Long> queryToExpireContractCount() {
        return getBaseMapper().queryToExpireContractCount();
    }

    protected List<HrmEmployee> listContractImportCandidates() {
        return employeeMapper.selectList(Wrappers.<HrmEmployee>lambdaQuery()
                .select(HrmEmployee::getEmployeeId, HrmEmployee::getEmployeeName, HrmEmployee::getMobile, HrmEmployee::getIsDel)
                .eq(HrmEmployee::getIsDel, 0));
    }

    private Map<String, HrmEmployee> buildContractImportEmployeeMap() {
        Map<String, HrmEmployee> employeeMap = new HashMap<>();
        List<HrmEmployee> employeeList = listContractImportCandidates();
        if (employeeList == null) {
            return employeeMap;
        }
        for (HrmEmployee employee : employeeList) {
            if (employee == null) {
                continue;
            }
            String name = normalizeHeader(employee.getEmployeeName());
            String mobile = normalizePhone(employee.getMobile());
            if (StrUtil.isEmpty(name) || StrUtil.isEmpty(mobile)) {
                continue;
            }
            String uniqueKey = contractImportEmployeeKey(name, mobile);
            if (employeeMap.containsKey(uniqueKey)) {
                throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "系统中存在重复员工：" + employee.getEmployeeName() + " + " + mobile + "，请先处理后再导入合同");
            }
            employeeMap.put(uniqueKey, employee);
        }
        return employeeMap;
    }

    private Map<String, Integer> buildHeaderIndexes(List<String> headerRow) {
        Map<String, Integer> headerIndexes = new HashMap<>();
        for (int cellIndex = 0; cellIndex < headerRow.size(); cellIndex++) {
            String header = normalizeHeader(getCellText(headerRow, cellIndex));
            if (StrUtil.isNotEmpty(header) && !headerIndexes.containsKey(header)) {
                headerIndexes.put(header, cellIndex);
            }
        }
        return headerIndexes;
    }

    private void requireHeader(Map<String, Integer> headerIndexes, String header) {
        if (!headerIndexes.containsKey(normalizeHeader(header))) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "合同导入文件缺少必填列：" + header);
        }
    }

    private HrmEmployeeContract buildImportContract(List<String> row, Map<String, Integer> headerIndexes, Map<String, HrmEmployee> employeeMap, int displayRow) {
        String employeeName = getImportText(row, headerIndexes, "姓名");
        String phone = normalizePhone(getImportText(row, headerIndexes, "电话"));
        if (StrUtil.isEmpty(employeeName) || StrUtil.isEmpty(phone)) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + displayRow + "行姓名和电话不能为空");
        }
        HrmEmployee employee = employeeMap.get(contractImportEmployeeKey(employeeName, phone));
        if (employee == null) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + displayRow + "行未找到员工：" + employeeName + " + " + phone);
        }
        LocalDate startTime = getRequiredImportDate(row, headerIndexes, "合同开始日期", displayRow);
        Integer contractType = parseContractType(getImportText(row, headerIndexes, "合同类型"), displayRow);
        LocalDate endTime = isOpenEndedContract(contractType)
                ? null
                : getRequiredImportDate(row, headerIndexes, "合同结束日期", displayRow);
        HrmEmployeeContract contract = new HrmEmployeeContract();
        contract.setContractId(null);
        contract.setEmployeeId(employee.getEmployeeId());
        contract.setContractNum(getImportText(row, headerIndexes, "合同编号"));
        contract.setContractType(contractType);
        contract.setStartTime(startTime);
        contract.setEndTime(endTime);
        contract.setStatus(parseContractStatus(getImportText(row, headerIndexes, "合同状态"), displayRow));
        contract.setSignCompany(getImportText(row, headerIndexes, "签约公司"));
        contract.setSignTime(getImportDate(row, headerIndexes, "合同签订日期"));
        contract.setRemarks(getImportText(row, headerIndexes, "合同备注"));
        contract.setSort(displayRow - 1);
        return contract;
    }

    private Integer parseContractType(String value, int displayRow) {
        String text = cleanText(value);
        Integer numberValue = parseEnumNumber(text);
        if (numberValue != null && numberValue >= 1 && numberValue <= 9) {
            return numberValue;
        }
        int enumValue = EmployeeContractType.valueOfType(text);
        if (enumValue != -1) {
            return enumValue;
        }
        for (EmployeeContractType type : EmployeeContractType.values()) {
            if (StrUtil.isNotEmpty(text) && text.contains(type.getName())) {
                return type.getValue();
            }
        }
        throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + displayRow + "行合同类型无效：" + value);
    }

    private Integer parseContractStatus(String value, int displayRow) {
        String text = cleanText(value);
        Integer numberValue = parseEnumNumber(text);
        if (numberValue != null && numberValue >= 0 && numberValue <= 2) {
            return numberValue;
        }
        int enumValue = EmployeeContractStatus.valueOfType(text);
        if (enumValue != -1) {
            return enumValue;
        }
        for (EmployeeContractStatus status : EmployeeContractStatus.values()) {
            if (StrUtil.isNotEmpty(text) && text.contains(status.getName())) {
                return status.getValue();
            }
        }
        throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + displayRow + "行合同状态无效：" + value);
    }

    private Integer parseEnumNumber(String text) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        String firstNumber = text.replaceAll("^([0-9]+).*$", "$1");
        if (!firstNumber.matches("[0-9]+")) {
            return null;
        }
        try {
            return Integer.parseInt(firstNumber);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isContractImportRowEmpty(List<String> row, Map<String, Integer> headerIndexes) {
        if (row == null) {
            return true;
        }
        for (Integer cellIndex : headerIndexes.values()) {
            if (cellIndex != null && StrUtil.isNotEmpty(getCellText(row, cellIndex))) {
                return false;
            }
        }
        return true;
    }

    private String getImportText(List<String> row, Map<String, Integer> headerIndexes, String header) {
        Integer cellIndex = headerIndexes.get(normalizeHeader(header));
        if (cellIndex == null || row == null) {
            return "";
        }
        return cleanText(getCellText(row, cellIndex));
    }

    private LocalDate getImportDate(List<String> row, Map<String, Integer> headerIndexes, String header) {
        Integer cellIndex = headerIndexes.get(normalizeHeader(header));
        if (cellIndex == null || row == null) {
            return null;
        }
        return parseLocalDate(getCellText(row, cellIndex));
    }

    private LocalDate getRequiredImportDate(List<String> row, Map<String, Integer> headerIndexes, String header, int displayRow) {
        Integer cellIndex = headerIndexes.get(normalizeHeader(header));
        if (cellIndex == null || row == null) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + displayRow + "行" + header + "不能为空");
        }
        String text = getCellText(row, cellIndex);
        if (StrUtil.isEmpty(text)) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + displayRow + "行" + header + "不能为空");
        }
        LocalDate date = parseLocalDate(text);
        if (date == null) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "第" + displayRow + "行" + header + "无效：" + text);
        }
        return date;
    }

    private String getCellText(List<String> row, int cellIndex) {
        if (row == null || cellIndex < 0 || cellIndex >= row.size()) {
            return "";
        }
        return cleanText(row.get(cellIndex));
    }

    private LocalDate parseLocalDate(String value) {
        String cleanValue = cleanText(value);
        if (StrUtil.isEmpty(cleanValue)) {
            return null;
        }
        cleanValue = cleanValue.replace("年", "-").replace("月", "-").replace("日", "");
        cleanValue = cleanValue.replace('/', '-').replace('.', '-');
        if (cleanValue.matches("\\d{8}")) {
            try {
                return LocalDate.of(Integer.parseInt(cleanValue.substring(0, 4)), Integer.parseInt(cleanValue.substring(4, 6)), Integer.parseInt(cleanValue.substring(6, 8)));
            } catch (Exception ignored) {
                return null;
            }
        }
        if (cleanValue.matches("\\d{5}")) {
            try {
                return DateUtil.getLocalDateTime(new BigDecimal(cleanValue).doubleValue()).toLocalDate();
            } catch (Exception ignored) {
            }
        }
        String[] parts = cleanValue.split("[^0-9]+");
        if (parts.length >= 3) {
            try {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                if (year < 100) {
                    year += 2000;
                }
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String contractImportEmployeeKey(String employeeName, String mobile) {
        return normalizeHeader(employeeName) + "|" + normalizePhone(mobile);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return cleanText(phone).replaceAll("[\\s\\-]", "");
    }

    private String normalizeHeader(String text) {
        return cleanText(text).replace(" ", "").replace("　", "");
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r", "").replace("\n", "").trim();
    }

    private void normalizeContractTerm(HrmEmployeeContract employeeContract) {
        if (employeeContract == null) {
            return;
        }
        if (isOpenEndedContract(employeeContract.getContractType())) {
            if (employeeContract.getStartTime() == null) {
                throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "合同开始日期不能为空");
            }
            employeeContract.setEndTime(null);
            employeeContract.setTerm(null);
            return;
        }
        employeeContract.setTerm(calculateContractTerm(employeeContract.getStartTime(), employeeContract.getEndTime()));
    }

    private boolean isOpenEndedContract(Integer contractType) {
        return Integer.valueOf(EmployeeContractType.NO_FIXED_TERM_LABOR_CONTRACT.getValue()).equals(contractType);
    }

    private Integer calculateContractTerm(LocalDate startTime, LocalDate endTime) {
        if (startTime == null || endTime == null) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "合同开始日期和合同结束日期不能为空");
        }
        if (endTime.isBefore(startTime)) {
            throw new CrmException(HrmCodeEnum.TEMPLATE_SAVE_PARAM_ERROR, "合同结束日期不能早于合同开始日期");
        }
        long months = ChronoUnit.MONTHS.between(startTime, endTime.plusDays(1));
        if (startTime.plusMonths(months).isBefore(endTime.plusDays(1))) {
            months++;
        }
        return (int) Math.max(1, (months + 11) / 12);
    }
}

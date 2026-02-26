package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_field")
public class HrmEmployeeField implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 主键ID
	 */
  @Column(name = "field_id")
  private Long fieldId;
	/**
	 * 自定义字段英文标识
	 */
  @Column(name = "field_name")
  private String fieldName;
	/**
	 * 字段名称
	 */
  @Column(name = "name")
  private String name;
	/**
	 * 字段类型 1 单行文本 2 多行文本 3 单选 4日期 5 数字 6 小数 7 手机  8 文件 9 多选   10 日期时间 11 邮箱 12 籍贯地区
	 */
  @Column(name = "type")
  private Integer type;
	/**
	 * 关联表类型 0 不需要关联 1 hrm员工 2 hrm部门 3 hrm职位 4 系统用户 5 系统部门 6 招聘渠道
	 */
  @Column(name = "component_type")
  private Integer componentType;
	/**
	 * 标签 1 个人信息 2 岗位信息 3 合同 4 工资社保
	 */
  @Column(name = "label")
  private Integer label;
	/**
	 * 标签分组 * 1 员工个人信息 2 通讯信息 3 教育经历 4 工作经历 5 证书/证件 6 培训经历 7 联系人
        * 11 岗位信息 12 离职信息 
        * 21 合同信息 
        * 31 工资卡信息 32 社保信息
	 */
  @Column(name = "label_group")
  private Integer labelGroup;
	/**
	 * 字段说明
	 */
  @Column(name = "remark")
  private String remark;
	/**
	 * 输入提示
	 */
  @Column(name = "input_tips")
  private String inputTips;
	/**
	 * 最大长度
	 */
  @Column(name = "max_length")
  private Integer maxLength;
	/**
	 * 默认值
	 */
  @Column(name = "default_value")
  private String defaultValue;
	/**
	 * 是否唯一 1 是 0 否
	 */
  @Column(name = "is_unique")
  private Integer isUnique;
	/**
	 * 是否必填 1 是 0 否
	 */
  @Column(name = "is_null")
  private Integer isNull;
	/**
	 * 排序 从小到大
	 */
  @Column(name = "sorting")
  private Integer sorting;
	/**
	 * 如果类型是选项，此处不能为空，使用kv格式
	 */
  @Column(name = "options")
  private String options;
	/**
	 * 是否固定字段 0 否 1 是
	 */
  @Column(name = "is_fixed")
  private Integer isFixed;
	/**
	 * 000000 (1:标题,2:选项,3:必填,4:唯一,5:隐藏,6:删除)
	 */
  @Column(name = "operating")
  private String operating;
	/**
	 * 是否隐藏  0不隐藏 1隐藏
	 */
  @Column(name = "is_hidden")
  private Integer isHidden;
	/**
	 * 是否可以修改值 0 否 1 是
	 */
  @Column(name = "is_update_value")
  private Integer isUpdateValue;
	/**
	 * 是否在列表头展示 0 否 1 是
	 */
  @Column(name = "is_head_field")
  private Integer isHeadField;
	/**
	 * 是否需要导入字段 0 否 1 是
	 */
  @Column(name = "is_import_field")
  private Integer isImportField;
	/**
	 * 是否员工可见 0 否 1 是
	 */
  @Column(name = "is_employee_visible")
  private Integer isEmployeeVisible;
	/**
	 * 是否员工可修改 0 否 1 是 2 禁用否
	 */
  @Column(name = "is_employee_update")
  private Integer isEmployeeUpdate;
	/**
	 * 样式百分比%
	 */
  @Column(name = "style_percent")
  private Integer stylePercent;
	/**
	 * 精度，允许的最大小数位
	 */
  @Column(name = "precisions")
  private Integer precisions;
	/**
	 * 表单定位 坐标格式： 1,1
	 */
  @Column(name = "form_position")
  private String formPosition;
	/**
	 * 限制的最大数值
	 */
  @Column(name = "max_num_restrict")
  private String maxNumRestrict;
	/**
	 * 限制的最小数值
	 */
  @Column(name = "min_num_restrict")
  private String minNumRestrict;
	/**
	 * 表单辅助id，前端生成
	 */
  @Column(name = "form_assist_id")
  private Long formAssistId;
	/**
	 * 创建人id
	 */
  @Column(name = "create_user_id")
  private Long createUserId;
	/**
	 * 创建时间
	 */
  @Column(name = "create_time")
  private Date createTime;
	/**
	 * 更新人id
	 */
  @Column(name = "update_user_id")
  private Long updateUserId;
	/**
	 * 最后修改时间
	 */
  @Column(name = "update_time")
  private Date updateTime;

  public Long getFieldId() {
    return fieldId;
  }
  public void setFieldId(Long fieldId) {
    this.fieldId = fieldId;
  }


  public String getFieldName() {
    return fieldName;
  }
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }


  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  public Integer getType() {
    return type;
  }
  public void setType(Integer type) {
    this.type = type;
  }


  public Integer getComponentType() {
    return componentType;
  }
  public void setComponentType(Integer componentType) {
    this.componentType = componentType;
  }


  public Integer getLabel() {
    return label;
  }
  public void setLabel(Integer label) {
    this.label = label;
  }


  public Integer getLabelGroup() {
    return labelGroup;
  }
  public void setLabelGroup(Integer labelGroup) {
    this.labelGroup = labelGroup;
  }


  public String getRemark() {
    return remark;
  }
  public void setRemark(String remark) {
    this.remark = remark;
  }


  public String getInputTips() {
    return inputTips;
  }
  public void setInputTips(String inputTips) {
    this.inputTips = inputTips;
  }


  public Integer getMaxLength() {
    return maxLength;
  }
  public void setMaxLength(Integer maxLength) {
    this.maxLength = maxLength;
  }


  public String getDefaultValue() {
    return defaultValue;
  }
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }


  public Integer getIsUnique() {
    return isUnique;
  }
  public void setIsUnique(Integer isUnique) {
    this.isUnique = isUnique;
  }


  public Integer getIsNull() {
    return isNull;
  }
  public void setIsNull(Integer isNull) {
    this.isNull = isNull;
  }


  public Integer getSorting() {
    return sorting;
  }
  public void setSorting(Integer sorting) {
    this.sorting = sorting;
  }


  public String getOptions() {
    return options;
  }
  public void setOptions(String options) {
    this.options = options;
  }


  public Integer getIsFixed() {
    return isFixed;
  }
  public void setIsFixed(Integer isFixed) {
    this.isFixed = isFixed;
  }


  public String getOperating() {
    return operating;
  }
  public void setOperating(String operating) {
    this.operating = operating;
  }


  public Integer getIsHidden() {
    return isHidden;
  }
  public void setIsHidden(Integer isHidden) {
    this.isHidden = isHidden;
  }


  public Integer getIsUpdateValue() {
    return isUpdateValue;
  }
  public void setIsUpdateValue(Integer isUpdateValue) {
    this.isUpdateValue = isUpdateValue;
  }


  public Integer getIsHeadField() {
    return isHeadField;
  }
  public void setIsHeadField(Integer isHeadField) {
    this.isHeadField = isHeadField;
  }


  public Integer getIsImportField() {
    return isImportField;
  }
  public void setIsImportField(Integer isImportField) {
    this.isImportField = isImportField;
  }


  public Integer getIsEmployeeVisible() {
    return isEmployeeVisible;
  }
  public void setIsEmployeeVisible(Integer isEmployeeVisible) {
    this.isEmployeeVisible = isEmployeeVisible;
  }


  public Integer getIsEmployeeUpdate() {
    return isEmployeeUpdate;
  }
  public void setIsEmployeeUpdate(Integer isEmployeeUpdate) {
    this.isEmployeeUpdate = isEmployeeUpdate;
  }


  public Integer getStylePercent() {
    return stylePercent;
  }
  public void setStylePercent(Integer stylePercent) {
    this.stylePercent = stylePercent;
  }


  public Integer getPrecisions() {
    return precisions;
  }
  public void setPrecisions(Integer precisions) {
    this.precisions = precisions;
  }


  public String getFormPosition() {
    return formPosition;
  }
  public void setFormPosition(String formPosition) {
    this.formPosition = formPosition;
  }


  public String getMaxNumRestrict() {
    return maxNumRestrict;
  }
  public void setMaxNumRestrict(String maxNumRestrict) {
    this.maxNumRestrict = maxNumRestrict;
  }


  public String getMinNumRestrict() {
    return minNumRestrict;
  }
  public void setMinNumRestrict(String minNumRestrict) {
    this.minNumRestrict = minNumRestrict;
  }


  public Long getFormAssistId() {
    return formAssistId;
  }
  public void setFormAssistId(Long formAssistId) {
    this.formAssistId = formAssistId;
  }


  public Long getCreateUserId() {
    return createUserId;
  }
  public void setCreateUserId(Long createUserId) {
    this.createUserId = createUserId;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Long getUpdateUserId() {
    return updateUserId;
  }
  public void setUpdateUserId(Long updateUserId) {
    this.updateUserId = updateUserId;
  }


  public Date getUpdateTime() {
    return updateTime;
  }
  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

}

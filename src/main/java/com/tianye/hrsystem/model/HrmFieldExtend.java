package com.tianye.hrsystem.model;

import java.util.Date;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hrm_field_extend")
public class HrmFieldExtend implements Serializable {
  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
	/**
	 * 主键ID
	 */
  @Column(name = "id")
  private Integer id;
	/**
	 * 对应主字段id
	 */
  @Column(name = "parent_field_id")
  private Integer parentFieldId;
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
	 * 字段类型
	 */
  @Column(name = "type")
  private Integer type;
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
	 * 如果类型是选项，此处不能为空，多个选项以，隔开
	 */
  @Column(name = "options")
  private String options;
	/**
	 * 是否允许编辑
	 */
  @Column(name = "operating")
  private Integer operating;
	/**
	 * 是否隐藏  0不隐藏 1隐藏
	 */
  @Column(name = "is_hidden")
  private Integer isHidden;
	/**
	 * 字段来源  0.自定义 1.原始固定 2原始字段但值存在扩展表中
	 */
  @Column(name = "field_type")
  private Integer fieldType;
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
  private Integer formAssistId;
	/**
	 * 创建人id
	 */
  @Column(name = "create_user_id")
  private Integer createUserId;
	/**
	 * 创建时间
	 */
  @Column(name = "create_time")
  private Date createTime;
	/**
	 * 更新人id
	 */
  @Column(name = "update_user_id")
  private Integer updateUserId;
	/**
	 * 最后修改时间
	 */
  @Column(name = "update_time")
  private Date updateTime;

  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }


  public Integer getParentFieldId() {
    return parentFieldId;
  }
  public void setParentFieldId(Integer parentFieldId) {
    this.parentFieldId = parentFieldId;
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


  public Integer getOperating() {
    return operating;
  }
  public void setOperating(Integer operating) {
    this.operating = operating;
  }


  public Integer getIsHidden() {
    return isHidden;
  }
  public void setIsHidden(Integer isHidden) {
    this.isHidden = isHidden;
  }


  public Integer getFieldType() {
    return fieldType;
  }
  public void setFieldType(Integer fieldType) {
    this.fieldType = fieldType;
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


  public Integer getFormAssistId() {
    return formAssistId;
  }
  public void setFormAssistId(Integer formAssistId) {
    this.formAssistId = formAssistId;
  }


  public Integer getCreateUserId() {
    return createUserId;
  }
  public void setCreateUserId(Integer createUserId) {
    this.createUserId = createUserId;
  }


  public Date getCreateTime() {
    return createTime;
  }
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }


  public Integer getUpdateUserId() {
    return updateUserId;
  }
  public void setUpdateUserId(Integer updateUserId) {
    this.updateUserId = updateUserId;
  }


  public Date getUpdateTime() {
    return updateTime;
  }
  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

}

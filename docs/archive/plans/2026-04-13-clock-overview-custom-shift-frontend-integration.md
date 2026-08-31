# 打卡概况单员工单日自定义排班前端接入说明

## 目标
- 在“打卡概况”页面支持点击任一员工任一天的单元格。
- 弹出“修改自定义排班”弹窗，允许录入 `HH:mm` 格式的开始/结束时间。
- 保存后仅修改该员工当天排班，不影响同一天其他员工。

## 后端已就绪接口

### 1. 查询当前员工当天排班
- 路径：`POST /api/hrsystem/workPlan/queryEmployeeDayShift`
- 参数：
  - `employeeId`
  - `workDate`
- 示例：

```js
axios.postForm(
  `/api/hrsystem/workPlan/queryEmployeeDayShift?employeeId=${employeeId}&workDate=${workDate}`
)
```

- `workDate` 支持：
  - `yyyy-MM-dd`
  - `yyyy-MM-dd HH:mm:ss`

### 2. 保存员工当天自定义排班
- 路径：`POST /api/hrsystem/workPlan/saveEmployeeDayCustomShift`
- 参数：
  - `employeeId`
  - `workDate`
  - `customStart`
  - `customEnd`
- 示例：

```js
const query = new URLSearchParams({
  employeeId: String(employeeId),
  workDate,
  customStart,
  customEnd
})
axios.postForm(`/api/hrsystem/workPlan/saveEmployeeDayCustomShift?${query.toString()}`)
```

## 返回结构
- 两个接口都返回统一结构：

```json
{
  "success": true,
  "code": 200,
  "message": "",
  "data": {
    "employeeId": 102,
    "userId": "u1",
    "planId": 41,
    "workDate": "2026-04-13",
    "source": "local",
    "currentShiftType": "custom",
    "currentShiftLabel": "自定义班次(09:00~18:00)",
    "currentStart": "09:00",
    "currentEnd": "18:00",
    "currentCrossDay": false,
    "groupId": "2001",
    "classId": null,
    "customShiftId": 902,
    "customStart": "09:00",
    "customEnd": "18:00",
    "customCrossDay": false
  }
}
```

## 关键字段说明
- `source`
  - `local`: 已有本地排班，优先显示这个。
  - `attendance`: 本地无排班，回退显示同步考勤链路里的当天班次。
  - `empty`: 当天查不到任何排班。
- `currentShiftType`
  - `custom`: 当前是本地自定义班次。
  - `standard`: 当前是标准班次。
  - `rest`: 当前是休息。
  - `empty`: 当前未设置。
- `currentShiftLabel`
  - 当前排班展示文案，可直接展示在弹窗里。
- `customStart/customEnd`
  - 可作为弹窗输入框初始值。
- `customCrossDay/currentCrossDay`
  - `true` 表示下班是次日。

## 前端建议修改文件
- `../hr_web/src/api/hrm/attendance/workPlan.js`
- `../hr_web/src/views/hrm/attendance/clock/components/Overview.vue`

## API 层建议新增方法

```js
export const queryEmployeeDayShift = (employeeId, workDate, config = {}) =>
  axios.postForm(
    `${workPlanApi}/queryEmployeeDayShift?employeeId=${encodeURIComponent(employeeId || '')}&workDate=${encodeURIComponent(workDate || '')}`,
    undefined,
    config
  )

export const saveEmployeeDayCustomShift = (params = {}, config = {}) => {
  const query = new URLSearchParams({
    employeeId: String(params.employeeId || ''),
    workDate: params.workDate || '',
    customStart: params.customStart || '',
    customEnd: params.customEnd || ''
  })
  return axios.postForm(`${workPlanApi}/saveEmployeeDayCustomShift?${query.toString()}`, undefined, config)
}
```

## Overview.vue 建议改动

### 1. 单元格增加点击行为
- 当前 `clock-cell` 只有展示。
- 需要在每个日期单元格上增加点击事件：
  - 传入 `row.employeeId`
  - 传入当前列日期 `date`

示意：

```vue
<div
  class="clock-cell"
  :class="`clock-cell--${getCellView(row, date).tone}`"
  @click="handleEditDayShift(row, date)"
>
```

### 2. 新增弹窗状态

```js
const dayShiftDialogVisible = ref(false)
const dayShiftLoading = ref(false)
const dayShiftSaving = ref(false)
const dayShiftForm = reactive({
  employeeId: null,
  workDate: '',
  currentShiftLabel: '',
  currentShiftType: '',
  customStart: '',
  customEnd: '',
  customCrossDay: false
})
```

### 3. 查询并打开弹窗

```js
async function handleEditDayShift(row, date) {
  dayShiftLoading.value = true
  try {
    const res = await queryEmployeeDayShift(row.employeeId, date)
    if (!res?.success) {
      throw new Error(res?.message || '加载当天排班失败')
    }
    const data = res.data || {}
    dayShiftForm.employeeId = row.employeeId
    dayShiftForm.workDate = date
    dayShiftForm.currentShiftLabel = data.currentShiftLabel || '--'
    dayShiftForm.currentShiftType = data.currentShiftType || 'empty'
    dayShiftForm.customStart = data.customStart || data.currentStart || ''
    dayShiftForm.customEnd = data.customEnd || data.currentEnd || ''
    dayShiftForm.customCrossDay = !!(data.customCrossDay || data.currentCrossDay)
    dayShiftDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error?.message || '加载当天排班失败')
  } finally {
    dayShiftLoading.value = false
  }
}
```

### 4. 保存逻辑

```js
async function handleSaveDayShift() {
  if (!isValidCustomTime(dayShiftForm.customStart)) {
    ElMessage.warning('请输入正确的开始时间，格式为 HH:mm')
    return
  }
  if (!isValidCustomTime(dayShiftForm.customEnd)) {
    ElMessage.warning('请输入正确的结束时间，格式为 HH:mm')
    return
  }

  dayShiftSaving.value = true
  try {
    const res = await saveEmployeeDayCustomShift({
      employeeId: dayShiftForm.employeeId,
      workDate: dayShiftForm.workDate,
      customStart: dayShiftForm.customStart,
      customEnd: dayShiftForm.customEnd
    })
    if (!res?.success) {
      throw new Error(res?.message || '保存自定义排班失败')
    }
    ElMessage.success('自定义排班已保存')
    dayShiftDialogVisible.value = false
    await getList()
  } catch (error) {
    ElMessage.error(error?.message || '保存自定义排班失败')
  } finally {
    dayShiftSaving.value = false
  }
}
```

## 弹窗 UI 建议
- 标题：`修改自定义排班`
- 展示项：
  - 当前排班：`dayShiftForm.currentShiftLabel`
  - 自定义时间：
    - 开始时间输入框
    - 结束时间输入框
  - 若 `customEnd <= customStart`，显示 `次日下班`
- 提示语：
  - `保存后仅修改该员工当天的排班，不影响其他员工`

## 文案建议
- 加载中：`正在加载当天排班...`
- 加载失败：`加载当天排班失败`
- 空排班提示：`当天暂无本地排班，可为该员工单独设置自定义时间`
- 开始时间校验：`请输入正确的开始时间，格式为 HH:mm`
- 结束时间校验：`请输入正确的结束时间，格式为 HH:mm`
- 保存中：`正在保存自定义排班...`
- 保存成功：`自定义排班已保存`
- 保存失败：`保存自定义排班失败`

## 交互细节
- 点击单元格即打开，不需要区分“正常/异常/空白”状态。
- 即使当天原本是标准班次或休息，也允许保存成自定义时间。
- 保存后直接刷新当前表格，不需要整页刷新。
- 后端已经保证：
  - 同一条共享排班记录会自动拆分；
  - 只改当前员工；
  - 不影响同记录里的其他员工。

## 验收点
- 点击任一员工任一天都能打开弹窗。
- 打开时能看到当前排班文案。
- 输入 `HH:mm` 后可保存。
- 跨天时间如 `20:00` 到 `08:00` 能正常保存。
- 保存后当前月列表刷新，目标日期单元格能反映新排班。
- 保存单人后，同天其他员工排班不被改动。

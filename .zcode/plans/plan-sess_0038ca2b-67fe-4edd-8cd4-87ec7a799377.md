### 目标
1. 新增"个人信息"页:展示所属公司、账号角色等登录信息,并把修改密码、退出登录集成到页面内(右上角下拉替换为"个人信息"入口,已确认)。
2. 顶栏"数据统计"tab 左侧的用户名文字去掉,只留方形 Grid 图标。
3. 按项目规范更新 hainan/docs 文档。

### 现状结论(已完成探索)
- 前端项目在 `/Users/jiangyongming/Project/hr_copy/hr_web`(Vue3 + Element Plus + Vuex + Vue Router hash 模式),本次**只改前端,不动后端 Java 代码**。
- 登录快照 `user_info`(localStorage/Vuex)已含 userName、account、companyName、depName、roleName、companyId、employeeId 等,页面数据直接取用;角色/凭证变更会触发 session-seed 强制重新登录,快照不会失真,无需新增后端接口。
- 后端已有 `POST /hrsystem/changePassword`(原密码错 5 次锁号、BCrypt 存储)和 `POST /hrsystem/logout`(token 参数,jti 写 Redis 黑名单),直接复用。当前右上角"退出登录"只清了本地 token、未调后端,本次顺带修复。
- 权限机制:`permission.js` 的 `DEFAULT_ALLOWED_PATHS` 白名单 + 路由 `hidden+checkPath` 模式(同 `schedulingDetail` 用法)可让新页面对所有登录用户可访问且不出现在任何菜单/tab。

### 前端改动清单(hr_web)
1. **新建页面 `src/views/hrm/profile/Index.vue`**
   - `wk-head` 页头"个人信息"(样式对齐 dataConfig 等现有页面)。
   - "基本信息"卡片:`el-descriptions` 展示 姓名、登录账号、所属公司、所属部门、账号角色(取自 `$store.state.user_info`)。
   - "安全设置"卡片:"修改密码"按钮复用全局 `RestPassword.vue` 弹窗(`store.commit('changRestPassword', true)`,自动继承首次登录强制改密逻辑);"退出登录"红色按钮。
   - 退出登录流程:调 `authLogOut` 吊销令牌 → 清除 localStorage `token`/`user_info` → 重置 store → 提示后跳 `/login`(与 `requset.js` 会话失效清理逻辑一致)。
2. **路由 `src/router/config.js`**:`/hrm` children 新增 `{ path: 'profile', meta: { parentName: 'hrm', name: '个人信息', hidden: true, checkPath: '/hrm/profile' }, component: ... }`。`hidden` 保证不出现在顶栏 tab 和侧边菜单。
3. **权限 `src/utils/permission.js`**:`DEFAULT_ALLOWED_PATHS` 增加 `'/hrm/profile'`,所有登录用户可访问(不依赖角色 menuTree)。
4. **API `src/api/login/user.js`**:新增 `authLogOut(token)` → `POST /api/hrsystem/logout?token=...`(token 走现有拦截器 header 之外需按后端契约显式传参)。
5. **`src/components/layout/TopHeader.vue`**
   - 删除"数据统计"tab 左侧的 `<span>{{ userName }}</span>`,只留 Grid 图标(点击仍回首页)。
   - 下拉菜单改为单项"个人信息"→ `router.push('/hrm/profile')`;删除不再使用的 `logOut`、`openRestPassword` 死代码及多余 import。
6. **`src/components/RestPassword.vue` 小改进**:非强制改密成功后提示"密码修改成功,请重新登录"并清凭证跳登录页(后端改密后本就 bumpSessionSeed 使所有会话失效,与其等下一次请求被动弹回,不如主动明确跳转);首次登录强制改密流程不变。

### 文档更新(hainan 仓库)
- `docs/requirements.md`:新增本轮需求小节(个人信息页、右上角入口调整、退出登录真正吊销令牌、顶栏只留图标)。
- `docs/development.md`:追加实现说明(涉及文件、权限白名单机制、退出登录链路)。

### 验证
- `npm run build`(或 dev 启动)确认编译通过。
- 联调验证:登录 → 右上角"个人信息"进入页面 → 信息正确展示;修改密码(错误原密码报错、成功后跳登录页并用新密码可登录);退出登录后用旧 token 请求接口应返回"登录已失效";顶栏首页图标旁不再显示用户名文字;各角色账号(含无 menuTree)均可进入个人信息页。
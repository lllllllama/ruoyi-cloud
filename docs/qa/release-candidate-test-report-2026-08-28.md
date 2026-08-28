# RuoYi-Cloud 任务调度与资金管理 Release Candidate 测试报告

## 1. 结论

- 测试分支：`fan`
- 被测提交：`d6c730a`（包含产品修复 `040ff5a`）
- 测试日期：2026-08-28
- 自动化结论：**核心功能、权限、金额、并发、浏览器、性能、迁移和回滚均通过**。
- 发布结论：**CONDITIONAL PASS（有条件通过，暂不标记最终 Release GO）**。

尚未关闭的发布前检查：

1. 当前终端没有 Windows 服务控制权限，Redis 停止/恢复场景无法自动执行。
2. OWASP ZAP 官方发行包在当前网络下下载速度过低，未完成 ZAP baseline；已由 Semgrep、HTTP 攻击用例和浏览器 XSS 用例覆盖主要风险，但不能等同于 ZAP PASS。
3. 10,000 任务/50,000 submission/50,000 fund record 大数据量测试以及 60 分钟 soak 属于计划中的建议项，本轮未构造该规模数据。
4. 自动化无法代替最终的人眼交互、文案和实际业务口径验收，操作步骤见第 8 节。

## 2. 兼容环境

| 组件 | 实际版本 | 结果 |
| --- | --- | --- |
| JDK | 1.8.0_504 | PASS |
| Maven | 3.6.3 | PASS |
| Spring Boot | 2.6.3 | PASS |
| Node | 12.14.0 | PASS，正式前端基线 |
| npm | 6.13.4 | PASS |
| Vue | 2.6.12 | PASS |
| MySQL | 5.7.44，端口 3307 | PASS |
| Nacos | 2.0.4，端口 8848 | PASS |
| Redis | 本地 Windows 服务，端口 6379 | 功能 PASS；重启场景 BLOCKED |

未升级 JDK、Spring Boot、Vue 或 Node；未改变用户原有全局环境配置。

## 3. 自动化结果汇总

| 测试层 | 结果 | 证据摘要 |
| --- | --- | --- |
| JDK8 全量构建 | PASS | `mvn clean verify`，23/23 模块成功 |
| Service 单元测试 | PASS | Fund 25/25，Research 35/35，共 60/60 |
| Node12 生产构建 | PASS | Node 12.14.0 + npm 6.13.4，`npm ci`、`npm run build:prod` 成功；仅原框架资源体积警告 |
| 全新数据库安装 | PASS | 17/17 schema 检查通过 |
| Migration 幂等 | PASS | 第二次执行 12/12 检查通过 |
| 旧库升级 | PASS | 升级前后原表行数、金额汇总、业务主键不变，topic 到 group 映射正确 |
| Fund 真实 MySQL | PASS | 35/35，金额、锁、结束、审计、附件和 20 线程热点竞争通过 |
| Research 真实 MySQL | PASS | 65/65，课题、三级树、提交审核、自动完成及回退通过 |
| 固定角色登录 | PASS | 11/11 账号经 Gateway 登录及路由正确 |
| Fund 多角色 HTTP | PASS | 48/48，公开拨付、受限使用、责任人、跨课题、附件权限通过 |
| Chrome E2E | PASS | 41/41，1366×768 与 1920×1080，无控制台、页面和请求错误 |
| Edge E2E | PASS | 41/41，1366×768 与 1920×1080，无控制台、页面和请求错误 |
| 安全 HTTP | PASS | 26/26，匿名、IDOR、提权、注入、文件名与双扩展名场景通过 |
| 浏览器 XSS | PASS | 任务、提交、说明按文本渲染，无弹窗或页面错误 |
| Semgrep | PASS/LOW | 764 个跟踪文件、200 条规则；无 Critical/High，1 个 UUID v3 MD5 提示为非密码用途且仓库未调用 |
| 50 用户性能基线 | PASS | 15 分钟、42,978 请求、47.70 rps、错误率 0.1885% |
| 20 线程热点竞争 | PASS | 拨付/使用记录与关闭、重复关闭均无超额、脏状态或重复关闭日志 |
| 服务故障恢复 | PARTIAL PASS | 可执行的 9/9 通过；Redis 重启 1 项因服务控制权限 BLOCKED |
| 发布回滚 | PASS | 当前版 → `a3b8ff8` 旧 JAR → 当前版；两版均 11/11 登录，数据库快照不变 |
| 最终 T69 | PASS | 课题 → 资金 → 三级任务 → 成果提交审核 → 归档及递归完成全链路成功 |

性能详细数据：

- 查询 p95：140.52 ms（门槛 1000 ms）
- 普通写 p95：49.45 ms（门槛 1500 ms）
- 关键操作 p95：40.12 ms（门槛 2000 ms）
- 81 次超时主要集中在 submission 列表和草稿创建，整体错误率仍低于 0.5%；建议作为下一轮容量优化观察项。
- 报告：`D:\ruoyi\dev-tools\runtime\performance-artifacts\performance-baseline-20260828172201.json`

安全扫描报告：`D:\ruoyi\dev-tools\runtime\security-artifacts\semgrep-security.json`

回滚制品：

- `D:\ruoyi\dev-tools\rollback-artifacts\ruoyi-modules-fund-a3b8ff8.jar`
- `D:\ruoyi\dev-tools\rollback-artifacts\ruoyi-modules-fund-current.jar`

## 4. 本轮发现并修复的问题

1. 任务责任人下拉框在未指定单位时错误使用 `dept_id = null`，导致没有可选成员。已改为返回当前课题全部有效成员，并补单元/E2E 回归。
2. 资金责任人仅比较本地 `responsible_user_id`，成员被移出课题或 Research 不可用时仍可能操作。已要求所有非管理员责任人操作先重新校验当前课题成员；FAIL-01 和 Fund 25/25、真实角色 48/48 均通过。
3. 金额超过两位小数会被数据库静默舍入。现已在后端拒绝，并有回归测试。
4. 登录用户读取公开拨付数据的权限入口与需求不一致。现已允许登录用户读取拨付公开数据，同时保持使用数据和附件的课题权限隔离。

对应关键提交：

- `752eabc fix(research): list all active members for task assignment`
- `e2b041a fix(fund): reject monetary values beyond two decimals`
- `93126c1 fix(fund): expose allocation reads to authenticated users`
- `040ff5a fix(fund): revalidate responsible group membership`

## 5. 故障恢复结果

已通过：

- Research 下线时 Fund 敏感写操作 fail-closed，恢复后正常。
- Fund 重启后开放计划仍可执行，已完成计划仍锁定。
- File 下线时上传失败且不产生虚假附件元数据，恢复后上传正常。
- Nacos 下线时已运行的 System/Fund 进程未崩溃，恢复后 Research 重新注册为健康实例。
- MySQL 在未提交事务中断后测试行数为 0，业务恢复且无半成功数据。

未自动执行：

- Redis 服务停止/启动被 Windows 拒绝：`Cannot open Redis service on computer '.'`。
- 可复跑脚本：`scripts/qa/failure-recovery.ps1`。该脚本会真实停止本地服务，只应在本机测试环境中以管理员 PowerShell 运行。

## 6. Migration 与回滚

Migration 验证同时覆盖全新部署和旧库升级：未 DROP 原 RuoYi 表，未修改 `sys_user/sys_dept/sys_role` 核心结构，资金迁移保持原业务 ID 和金额汇总，重复执行不会重复插入。

回滚使用上一稳定提交 `a3b8ff8` 的独立 Fund JAR，验证结果：

- 旧版启动、注册、11 个固定账号登录全部成功。
- 恢复当前版后 11 个账号再次全部成功。
- 回滚前后预算快照均为 2 条、1100.00；使用计划均为 2 条、计划 330.00、实际 30.00。
- 当前 Fund JAR SHA-256：`BC128CFE2D7AC35FC4A505F39BE63BA7CAFD19B55DE88B312C1E371F83DBB880`。

## 7. 当前运行状态

测试结束时以下端口均正常监听：

```text
MySQL 3307    Redis 6379    Nacos 8848
Gateway 8080  Auth 9200     System 9201
Research 9204 Fund 9205     File 9300
UI 81
```

## 8. 需要人工完成的实机验收

以下操作仅在本地测试库进行。先确认地址 `http://127.0.0.1:81` 可打开。QA 账号密码均与本地 admin 密码一致，当前测试环境为 `admin123`。

### 8.1 人眼 UI 与完整业务流

1. 使用 `admin/admin123` 登录，确认左侧出现“课题管理、任务调度、资金管理”，页面没有乱码、遮挡和横向溢出。
2. 在 `/research-group/group` 新建编码唯一的课题，例如 `MANUAL-20260828`；选择负责单位、参与单位并保存。
3. 给课题添加 `a_leader`（LEADER）、`a_core`（CORE）、`a_member`（MEMBER）、`a_expert`（EXPERT），配置一个单位负责人；刷新详情确认成员、单位和角色未丢失。
4. 在 `/fund/budget` 为新课题配置总资金 100.00。
5. 在 `/fund/allocation` 建立 60.00 和 40.00 两个计划，应成功；再新增 1.00 应被拒绝。录入实际拨付并结束，确认实际金额、差额、结束原因、完成时间和操作历史。
6. 使用 `a_leader` 登录，在 `/fund/use` 建立 60.00 和 40.00 使用计划，责任人选择 `a_member`；再新增 1.00 应被拒绝。
7. 使用 `a_member` 登录，给自己的使用计划录入记录并上传一个 PDF/图片附件；从业务附件入口下载并核对内容。完成计划后再次尝试修改金额或新增记录，应被拒绝。
8. 使用 `a_leader` 在 `/research/framework` 创建年度框架，在 `/research/task` 创建一级 → 二级 → 三级任务；给叶子任务添加 required_num=1 的必交成果，责任人选择 `a_member`。
9. 使用 `a_member` 在 `/research/my-task` 创建草稿、上传多个附件并提交；提交后普通编辑应被锁定。
10. 使用 `a_leader` 在 `/research/audit` 审核通过；在 `/research/archive` 检查归档人、归档时间、附件，并确认成果、叶子任务及父任务自动完成。
11. 取消审核，确认成果和三级任务状态自动回退；重新提交并审核，确认再次完成。

### 8.2 人工权限负向检查

1. 使用 `outsider/admin123` 登录，直接输入 `http://127.0.0.1:81/fund/use`，应跳走或显示 403/404，不能看到课题使用数据。
2. 使用 `b_leader/admin123` 尝试访问 A 课题的任务、提交、使用计划和附件，应被后端拒绝；仅隐藏按钮不算通过。
3. 使用 `a_member` 尝试审核成果、修改总资金、分配拨付责任人，应被拒绝。
4. 将 `a_member` 暂时从课题成员中停用，但保留其资金责任人字段；其再次记账应失败。恢复成员后应可继续操作。
5. 复制一条使用附件下载地址，在退出登录后的新隐私窗口打开，应返回 401；使用 `outsider` 登录后打开应返回 403。

### 8.3 Redis 重启（必须使用管理员 PowerShell）

1. 先在浏览器登录 `a_member`，保留当前页面。
2. 以管理员身份打开 PowerShell，执行：

```powershell
Get-Service Redis
Stop-Service -Name Redis -Force
```

3. 刷新已登录页面并尝试新登录。允许出现明确的受控错误，但页面不应永久卡死，数据库不应产生半条业务记录。
4. 恢复 Redis：

```powershell
Start-Service -Name Redis
Get-Service Redis
```

5. 等待 3～5 秒，重新登录并访问课题、任务、资金页面；均应恢复正常。把停止期间和恢复后的页面截图、时间及错误信息附到验收记录。

### 8.4 OWASP ZAP baseline

1. 在测试环境安装 ZAP，确认所有本地服务启动，不要对生产地址运行主动扫描。
2. 先以报告模式扫描 `http://127.0.0.1:81` 和 `http://127.0.0.1:8080`。
3. 导入已登录会话或配置 Form Authentication 后，再爬取 Research、Fund 和附件下载路由；匿名扫描不能替代已认证扫描。
4. 导出 HTML/JSON 报告。Critical/High 必须逐条复核并作为发布阻断；Medium/Low 记录接受、修复或误报理由。
5. 重点确认 Token、CORS、安全响应头、路径遍历、上传文件和受控附件下载告警。

### 8.5 建议的容量补测

如本次直接面向生产数据量，上线前再准备脱敏测试数据：10,000 任务、50,000 submission、50,000 fund record，重复列表、树、我的任务、未审、归档和 overview 场景；随后执行 30 并发 60 分钟 soak，观察 Heap、CPU、线程、数据库连接、Redis 连接和慢 SQL 是否回落。

## 9. 最终判定条件

完成以下三项后可把本报告由 CONDITIONAL PASS 更新为 Release GO：

1. 管理员 PowerShell 完成 Redis 重启并通过。
2. ZAP baseline 无未处置 Critical/High。
3. 第 8.1～8.2 的人工业务与权限检查通过，并保留截图或验收记录。


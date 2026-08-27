# 资金管理模块接入说明

本次按上传的 **RuoYi-Cloud 3.4.0** 目录开发，新增独立微服务 `ruoyi-modules/ruoyi-fund`，服务名 `ruoyi-fund`，默认端口 `9205`。前端仍并入原 `ruoyi-ui`，通过 `/ruoyi-fund/**` 访问资金服务。

## 已实现业务规则

- 系统管理员维护课题引用、参与单位和课题总资金。
- **拨付计划总额强制不能超过课题总资金**。
  - 新增拨付计划：`已有有效计划总额 + 本次金额 <= 课题总资金`。
  - 修改拨付计划：`除本计划外的有效计划总额 + 修改后金额 <= 课题总资金`。
  - 修改课题总资金：新总资金不得低于已有有效拨付计划总额。
  - 校验位于后端事务中，并锁定同一课题的总资金记录，避免并发创建计划绕过总额限制。
- 拨付单位负责人只能给本单位成员指定拨付责任人。
- 指定拨付责任人后，仅责任人提交拨付记录/结束计划；未指定时，拨付单位成员可操作。
- 拨付计划可有多条实际拨付记录；结束时由后端重新汇总金额，有差异必须二次确认；结束后锁定。
- 课题负责人创建资金使用计划；责任人可选。指定后仅责任人操作，未指定时课题负责/参与单位成员可操作。
- 使用计划可有多条记录；未用完可确认结束，超支可强制确认结束；结束后锁定。
- 拨付计划/记录面向登录用户可查；使用计划/记录由后端按课题成员关系校验。
- 金额统一使用 `BigDecimal` / `DECIMAL(18,2)`。
- 凭证复用现有 `/file/upload` 服务和 Vue `FileUpload` 组件。

## 当前仓库没有“课题管理”模块的处理

上传仓库是原始 RuoYi-Cloud 结构，未发现课题/参与单位业务表。因此资金服务内新增：

- `fund_topic`：课题引用、负责单位、课题负责人；
- `fund_topic_dept`：负责/参与单位映射。

它们是**资金模块的课题主数据适配层**，不是要求替代你正式系统中的课题主表。后续合并到已有课题模块时，建议保留 `IFundTopicService` 作为边界，将课题信息和成员关系的数据源切换到正式课题服务；拨付、使用业务表不需要因此重构。

## 主要目录

```text
ruoyi-modules/ruoyi-fund/                 # 独立资金微服务
ruoyi-api/ruoyi-api-system/.../fund*      # 系统服务组织/用户内部只读 DTO + Feign
ruoyi-modules/ruoyi-system/.../SysFundSupportController.java
ruoyi-ui/src/api/fund/                    # Vue2 API
ruoyi-ui/src/views/fund/                  # 资金总览 / 拨付 / 使用页面
sql/ry_fund.sql                           # 业务库 + 菜单权限
sql/ry_fund_config.sql                    # Nacos 配置
```

## 安装

1. 备份数据库后执行 `sql/ry_fund.sql`：创建独立业务库 `ry-fund`，并向 `ry-cloud.sys_menu` 增加资金菜单/权限。
2. 根据实际 MySQL/Redis 地址修改 `sql/ry_fund_config.sql` 中的连接信息，再执行该脚本写入 Nacos `ruoyi-fund-dev.yml`。
3. 当前网关配置启用了 discovery locator，前端使用 `/ruoyi-fund/**` 可自动发现 `ruoyi-fund`，无需新增固定 gateway route。
4. 在项目根目录执行 Maven 打包，启动 `ruoyi-modules-fund.jar`。
5. 在 `ruoyi-ui` 重新安装依赖并构建前端。
6. 使用若依角色管理给“系统管理员 / 拨付单位负责人 / 课题负责人 / 责任人”等实际角色分配按钮权限。SQL 只自动处理 PPT 要求的页面可见性，不擅自扩大写权限。

## 合并到另一套同源代码

本次尽量采用“新增文件、少改旧文件”的方式：

- 主要业务全部位于新增 `ruoyi-fund` 目录；
- 系统服务只新增资金专用的组织/用户内部只读接口；
- 旧模块主要修改 `ruoyi-modules/pom.xml`、Docker 编排/复制脚本；
- 前端资金文件全部在独立 `fund` namespace 下。

如使用 `FUND_MODULE.patch`，在另一套同源项目根目录先创建干净分支并备份，然后执行：

```bash
patch -p2 < FUND_MODULE.patch
```

不同同源版本若已有自定义代码，建议优先人工合并 `pom.xml`、Docker 文件和 SQL，新增目录通常可直接复制。

## 验证状态

当前环境已做：XML 解析、Mapper 接口/XML 方法一致性、Vue/JS 语法检查、Java 源文件语法扫描及关键业务逻辑静态检查。

当前执行环境没有 Maven，且 Java/Node 版本不是项目目标的 JDK 8 / Node 12，因此**未宣称完成真实 `mvn package` 和 `npm run build:prod`**。合入测试环境后应使用项目实际工具链执行完整编译、数据库联调和权限场景测试。

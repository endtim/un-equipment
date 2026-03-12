# 高校大型仪器共享平台

本项目采用前后端分离结构：

```text
un-equipment/
  frontend/    # Vue 3 + Element Plus
  backend/     # Spring Boot + MyBatis
  docs/
    sql/       # 数据库结构与初始化数据
    api/       # 接口与业务规则文档
```

## 1. 环境要求

- JDK 8+
- Maven 3.8+
- Node.js 18+
- MySQL 8.x

## 2. 数据库初始化

1. 创建数据库：`instrument_share_platform`
2. 执行建表脚本：`docs/sql/un-equipment.sql`
3. 执行基础数据脚本：`docs/sql/seed-basic.sql`
4. 若为已有库升级，执行增量脚本：`docs/sql/upgrade_20260310_instrument_fields.sql`

说明：`seed-basic.sql` 中演示账号密码使用 BCrypt 哈希存储，默认明文密码为 `123456`。

## 3. 后端启动

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8080`  
Swagger：`http://localhost:8080/swagger-ui.html`

### 3.1 推荐环境变量

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `APP_JWT_EXPIRE_SECONDS`

参考：`backend/src/main/resources/application-local.yml.example`

## 4. 前端启动

```bash
cd frontend
npm install
npm run serve
```

默认地址：`http://localhost:8081`

## 5. 演示账号

- `admin / 123456`（平台管理员）
- `owner_zhang / 123456`（仪器负责人）
- `teacher_wang / 123456`（校内用户）
- `external_li / 123456`（校外用户）

## 6. 关键文档

- 结构基线：`docs/sql/un-equipment.sql`
- 基础数据：`docs/sql/seed-basic.sql`
- 状态机规则：`docs/api/order-state-machine.md`
- 功能与流程说明：`docs/现有功能与业务流程说明.md`
- 整改进度：`docs/整改实施进度.md`

## 7. 常见问题

### 7.1 Maven 依赖下载失败

若出现私有仓库连接超时（如 `10.200.0.5:8081`），请检查本地 `settings.xml` 镜像配置，或切换可用公共仓库镜像后再编译。

### 7.2 登录失败（账号密码正确）

请确认数据库 `sys_user.password` 为 BCrypt 哈希格式（以 `$2a$` / `$2b$` / `$2y$` 开头），且账号状态为 `ENABLED`。

## 8. P2/P3 回归检查清单

- 预约状态机：
  - `MACHINE`：`PENDING_AUDIT -> WAITING_USE -> IN_USE -> WAITING_SETTLEMENT -> COMPLETED`
  - `SAMPLE`：`PENDING_AUDIT -> WAITING_RECEIVE -> TESTING -> RESULT_UPLOADED -> WAITING_SETTLEMENT -> COMPLETED`
- 取消规则：仅允许 `PENDING_AUDIT`、`APPROVED`、`WAITING_USE`、`WAITING_RECEIVE` 执行取消。
- 结算幂等：同一订单并发结算只允许一次成功，重复请求应返回状态不允许。
- 价格口径：统一按用户类型取价（校内 `priceInternal`，校外 `priceExternal`），下单、完单重算、结算记录一致。
- 开放规则：`weekDay` 必须在 1~7，`maxReserveMinutes` 必须大于等于 `stepMinutes` 且可整除。
- 已预约时段：仪器详情页切换日期可刷新占用列表；与后端冲突校验口径一致。
- 前端异常处理：业务错误不出现 Vue 红屏，401/403 统一清理登录态并跳转登录页。

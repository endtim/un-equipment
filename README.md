# 高校大型仪器共享平台

项目已调整为前后端分离目录结构：

```text
un-equipment/
  frontend/
    src/
    public/
    package.json
    vue.config.js
  backend/
    src/main/java/
    src/main/resources/
    pom.xml
  docs/
    sql/
    api/
  README.md
```

## 目录说明

- `frontend/`：Vue 3 + Element Plus 前端工程
- `backend/`：Spring Boot + MyBatis 后端工程
- `docs/sql/`：数据库建表脚本
- `docs/api/`：接口文档目录

## 本地运行

### 后端

```bash
cd backend
mvn spring-boot:run
```

- 默认地址：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`

### 前端

```bash
cd frontend
npm install
npm run serve
```

- 默认地址：`http://localhost:8081`

## 开发建议

- 后端开发与构建命令统一在 `backend/` 执行。
- 前端开发与构建命令统一在 `frontend/` 执行。
- 根目录仅保留项目总览与文档入口。
- 根目录 `pom.xml` 为聚合工程，可在根目录执行 `mvn -pl backend spring-boot:run`。

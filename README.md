# Coffee Manager

## 项目简介

咖啡豆管理项目是一个面向个人使用的咖啡豆档案库，用来记录咖啡豆基础信息、包装封面、主观评价和实际冲煮复盘，帮助用户形成可回溯的饮用经验。

当前项目处于 MVP 准交付阶段：coffee / file / review / brew 主流程已完成前后端闭环，全流程 bugfix 和 MVP 收口检查已通过。本 README 只记录当前已实现能力、启动方式、配置方式和后续边界，不包含延后功能的实现承诺。

## 功能概览

已实现：

- 咖啡豆档案：新增、编辑、删除、详情、列表、搜索、筛选、分页。
- 咖啡豆页面：`/coffee` 作为当前主页面，新增 / 编辑采用弹窗交互。
- 包装封面上传：支持 jpg / png / webp，单文件默认最大 5MB。
- 封面静态访问：MVP 标准 URL 固定为 `/uploads/coffee-covers/{filename}`。
- 主观评价：围绕指定咖啡豆新增、编辑、删除、分页查看评价。
- 评价评分：综合评分必填，维度评分可选；评分范围 0.0-5.0，步进 0.5。
- 冲煮记录：围绕指定咖啡豆新增、编辑、删除、分页查看冲煮复盘。
- 临时默认登录：前端当前会使用 `admin/admin123456` 自动登录。

未实现或延后内容见下方“延后事项”。

## 技术栈

后端：

- Java 17+
- Spring Boot 3.3.5
- Spring Web / Spring Security / Spring Validation
- MyBatis-Plus 3.5.10.1
- MySQL 8
- JJWT 0.12.6
- Lombok
- Redis 连接配置已预留，MVP 当前主流程不依赖 Redis

前端：

- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia
- Axios

## 项目结构

```text
coffee-manager/
  backend/                         Spring Boot 后端
    src/main/resources/application.yml
    src/main/resources/db/init.sql 数据库初始化脚本
    src/main/java/com/example/coffeebean/
      auth/                        临时登录与 JWT
      coffee/                      咖啡豆档案
      file/                        封面上传
      review/                      评价
      brew/                        冲煮记录
      config/                      安全、静态资源、文件配置
  frontend/                        Vue 3 前端
    src/api/                       前端 API 封装
    src/views/CoffeeView.vue       当前 MVP 主页面
    vite.config.ts                 Vite 代理配置
  docs/                            需求、数据库、接口文档与项目状态索引
  uploads/                         运行时上传目录，已 gitignore
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.9 或更高版本
- Node.js 20 或更高版本
- npm
- MySQL 8

本仓库没有提交 Maven Wrapper，后端启动依赖本机 `mvn` 命令。

可先检查版本：

```powershell
java -version
mvn -version
node -v
npm -v
mysql --version
```

## 数据库准备

MVP 准交付环境使用 MySQL。当前项目实际库名为 `coffee_manager`，以 `backend/src/main/resources/db/init.sql` 和 `application.yml` 中的默认 JDBC 地址为准。

数据库初始化脚本位于：

```text
backend/src/main/resources/db/init.sql
```

该脚本会：

- 创建 `coffee_manager` 数据库。
- 创建 `users`、`coffee_beans`、`coffee_reviews`、`brew_records` 表。
- 创建 `flavor_tags`、`coffee_bean_flavor_tags`、`review_flavor_tags` 表作为后续风味标签阶段的结构预留；MVP 当前没有风味标签业务闭环。
- 写入默认用户 `admin`。
- 写入一批风味标签基础数据；当前仅作为预留数据，前端和后端业务接口尚未接入风味标签功能。

应用配置中 `spring.sql.init.mode=never`，所以后端启动时不会自动执行 SQL。首次启动前需要手动执行：

```powershell
mysql -uroot -p123456 < backend\src\main\resources\db\init.sql
```

如果本机 MySQL 密码不是 `123456`，请改用自己的账号执行脚本，例如：

```powershell
mysql -uroot -p < backend\src\main\resources\db\init.sql
```

执行后确认数据库存在：

```powershell
mysql -uroot -p123456 -e "SHOW DATABASES LIKE 'coffee_manager';"
```

准交付环境建议使用独立数据库账号，不建议长期使用 `root` 账号作为应用连接账号。账号创建和授权可由数据库管理员按项目环境执行，应用侧只需要保证该账号可以访问 `coffee_manager` 库并具备当前 MVP 所需的表读写权限。

## 后端配置

主要配置文件：

```text
backend/src/main/resources/application.yml
```

常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端服务端口 |
| `DB_URL` | `jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true` | MySQL JDBC 地址 |
| `DB_USERNAME` | `root` | MySQL 用户名；本地默认可用，准交付环境建议替换为独立账号 |
| `DB_PASSWORD` | `123456` | MySQL 密码；准交付环境必须通过环境变量替换 |
| `FILE_UPLOAD_PATH` | `uploads` | 上传根目录，默认相对后端进程启动目录 |
| `JWT_SECRET` | 开发默认值 | JWT 签名密钥，准交付 / 部署环境必须替换 |
| `JWT_EXPIRATION_SECONDS` | `604800` | JWT 过期时间，默认 7 天 |
| `REDIS_HOST` | `localhost` | Redis 主机，仅后续预留，当前 MVP 不作为启动必需依赖 |
| `REDIS_PORT` | `6379` | Redis 端口，仅后续预留，当前 MVP 不作为启动必需依赖 |
| `REDIS_DATABASE` | `0` | Redis database，仅后续预留，当前 MVP 不作为启动必需依赖 |

当前 `application.yml` 中的数据库连接配置如下，准交付环境优先通过环境变量覆盖：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${DB_URL:jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}
```

准交付配置基线：

- 数据库类型：MySQL。
- 当前实际库名：`coffee_manager`。
- 应用连接账号：本地可用默认 `root/123456`，准交付环境建议使用独立账号，例如 `coffee_manager_app`。
- 配置位置：`backend/src/main/resources/application.yml`；当前项目没有单独的 `application-dev.yml`。
- 关键配置项：`spring.datasource.driver-class-name`、`spring.datasource.url`、`spring.datasource.username`、`spring.datasource.password`。
- 覆盖方式：优先使用 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量，不把真实密码提交到 Git。

Windows PowerShell 临时配置示例：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='coffee_manager_app'
$env:DB_PASSWORD='你的数据库密码'
$env:FILE_UPLOAD_PATH='D:\dev\coffee-manager\backend\uploads'
$env:JWT_SECRET='请替换为准交付环境专用随机字符串'
```

Linux / macOS 临时配置示例：

```bash
export DB_URL='jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USERNAME='coffee_manager_app'
export DB_PASSWORD='你的数据库密码'
export FILE_UPLOAD_PATH='/var/lib/coffee-manager/uploads'
export JWT_SECRET='请替换为准交付环境专用随机字符串'
```

### JWT_SECRET 配置基线

当前 JWT 配置位于 `application.yml`：

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:coffee-manager-dev-secret-key-change-me-20260507-min-32-bytes}
    expiration-seconds: ${JWT_EXPIRATION_SECONDS:604800}
```

MVP 本地开发可以使用默认值或示例值，保证项目可运行即可。准交付 / 部署环境必须通过 `JWT_SECRET` 环境变量替换为更安全的随机字符串，不建议把真实 secret 写入配置文件或提交到 Git。

当前项目暂未实现正式登录系统、token 刷新、退出失效、黑名单等完整生命周期能力。本阶段只保证 MVP 可运行；正式登录和 token 生命周期后续统一处理。

### Redis 当前状态

`application.yml` 中保留了 `spring.data.redis` 配置项和 `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE` 环境变量占位，但 Redis 当前仅作为后续能力预留，不是 MVP 当前启动必需依赖。

本阶段不要把 Redis 写入必须启动项。正式缓存、token 黑名单或会话管理后续再统一设计。

## 后端启动

启动前请确认 MySQL 已启动，并已执行数据库初始化脚本。

```powershell
cd backend
mvn spring-boot:run
```

默认启动地址：

```text
http://localhost:8080
```

健康检查：

```powershell
curl http://localhost:8080/api/health
```

`/api/health` 会访问数据库并返回类似：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "ok",
    "database": "ok"
  }
}
```

可选后端构建：

```powershell
cd backend
mvn -DskipTests package
```

## 前端配置

前端请求统一使用相对路径：

- API 请求 baseURL：`/api`
- 上传文件静态访问：`/uploads`

Vite 代理配置位于：

```text
frontend/vite.config.ts
```

当前代理规则：

- `/api` -> `http://localhost:8080`
- `/uploads` -> `http://localhost:8080`

本地开发时，浏览器访问 Vite 服务 `http://localhost:5173`，前端代码中的 API baseURL 是 `/api`，由 Vite 代理转发到后端 `http://localhost:8080`。封面图片访问 `/uploads/...` 也由 Vite 代理转发到同一个后端。

如果修改后端端口，例如改为 `SERVER_PORT=8090`，需要同步调整 `frontend/vite.config.ts` 中 `/api` 和 `/uploads` 的代理目标。

如果准交付 / 部署环境的后端地址变化，当前项目没有正式环境变量体系；请按实际部署方式处理：

- 仍使用 Vite 开发服务联调时，修改 `frontend/vite.config.ts` 的 proxy target。
- 使用构建产物部署时，建议让前端与后端保持同源路径，或在 Web Server / 网关层把 `/api` 和 `/uploads` 转发到后端服务。
- 不建议在本阶段大规模重构前端配置体系。

当前前端没有单独的 `.env` 配置要求。

## 前端启动

```powershell
cd frontend
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

可选前端构建：

```powershell
cd frontend
npm run build
```

可选本地预览构建产物：

```powershell
cd frontend
npm run preview
```

## 默认登录账号

MVP 当前仍采用临时默认账号：

```text
username: admin
password: admin123456
```

前端进入 `/coffee` 后会调用临时自动登录逻辑：

1. 如果 `localStorage` 中已有 `coffee_manager_token`，先用该 token 请求 `/api/auth/me`。
2. 如果 token 不存在或失效，自动使用 `admin/admin123456` 请求 `/api/auth/login`。
3. 登录成功后把 token 写入 `localStorage`。

这不是正式登录系统。正式登录页面、用户体系、token 生命周期和刷新策略均延后统一处理。

## 文件上传与静态访问说明

上传接口：

```text
POST /api/files/coffee-cover
Content-Type: multipart/form-data
form field: file
```

当前上传接口用于咖啡豆包装封面。它会校验：

- 文件不能为空。
- 文件大小默认不能超过 5MB。
- 只允许 `image/jpeg`、`image/png`、`image/webp`。
- 会校验文件头，避免仅修改扩展名绕过类型判断。

默认存储规则：

```text
上传根目录: FILE_UPLOAD_PATH，默认 uploads
封面子目录: coffee-covers
本地默认目录: backend/uploads/coffee-covers
```

如果从 `backend` 目录执行 `mvn spring-boot:run`，默认上传目录就是：

```text
backend/uploads/coffee-covers
```

上传成功返回的 URL 标准固定为：

```text
/uploads/coffee-covers/{filename}
```

注意：

- `/api/files/coffee-cover` 是上传接口路径。
- `/uploads/coffee-covers/{filename}` 是图片静态访问路径。
- MVP 阶段不再混用 `/files` 作为静态访问前缀。
- 准交付环境建议将 `FILE_UPLOAD_PATH` 设置为绝对路径，并确保该目录持久化保存。
- Windows 示例：`D:\coffee-manager\uploads`。
- Linux / macOS 示例：`/var/lib/coffee-manager/uploads`。
- 如果未来部署到服务器或 Docker，应将该路径挂载为持久化目录，避免应用重启、发布或容器重建后文件丢失。
- 本地 uploads 存储策略 MVP 可接受；图片删除、旧文件清理、云存储迁移暂时延后。

## 核心接口概览

通用响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

除 `/api/auth/login`、`/api/health` 和 `/uploads/**` 静态资源外，当前业务接口需要 JWT。前端会通过临时自动登录获取并附加 `Authorization: Bearer {token}`。

认证与健康检查：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | 使用账号密码登录，当前默认账号为 `admin/admin123456` |
| `GET` | `/api/auth/me` | 获取当前用户信息 |
| `GET` | `/api/health` | 服务与数据库健康检查 |

咖啡豆：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/coffee-beans` | 咖啡豆分页列表，支持 keyword / roastLevel / processMethod / origin / page / pageSize |
| `POST` | `/api/coffee-beans` | 新增咖啡豆 |
| `GET` | `/api/coffee-beans/{id}` | 咖啡豆详情 |
| `PUT` | `/api/coffee-beans/{id}` | 更新咖啡豆 |
| `DELETE` | `/api/coffee-beans/{id}` | 删除咖啡豆 |

文件：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/files/coffee-cover` | 上传咖啡豆封面 |
| `GET` | `/uploads/coffee-covers/{filename}` | 访问上传后的封面图片 |

评价：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/coffee-beans/{coffeeBeanId}/reviews` | 指定咖啡豆的评价分页列表 |
| `POST` | `/api/coffee-beans/{coffeeBeanId}/reviews` | 为指定咖啡豆新增评价 |
| `GET` | `/api/reviews/{id}` | 评价详情 |
| `PUT` | `/api/reviews/{id}` | 更新评价 |
| `DELETE` | `/api/reviews/{id}` | 删除评价 |

冲煮记录：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/coffee-beans/{coffeeBeanId}/brew-records` | 指定咖啡豆的冲煮记录分页列表 |
| `POST` | `/api/coffee-beans/{coffeeBeanId}/brew-records` | 为指定咖啡豆新增冲煮记录 |
| `GET` | `/api/brew-records/{id}` | 冲煮记录详情 |
| `PUT` | `/api/brew-records/{id}` | 更新冲煮记录 |
| `DELETE` | `/api/brew-records/{id}` | 删除冲煮记录 |

## 当前已验证内容

根据当前项目收口状态，MVP 已完成并通过以下验证范围：

- 后端 coffee 模块闭环。
- 前端 coffee 页面闭环。
- coffee 新增 / 编辑弹窗化。
- 后端 file 封面上传模块。
- 前端封面上传联调。
- 后端 review 模块。
- 前端 review 评价功能联调。
- review 模块 MVP 闭环。
- 后端 brew 模块。
- 前端 brew 冲煮记录联调。
- brew 模块 MVP 闭环。
- 全流程 bugfix / MVP 收口检查通过，当前暂无必须修问题。

本 README 对应的是当前 MVP 准交付状态，不代表延后事项已经完成。

## MVP 已知限制

- 当前登录是 `admin/admin123456` 临时自动登录，没有正式登录页和完整用户体系。
- JWT 过期与刷新策略仍是开发阶段实现，正式 token 生命周期后续统一处理。
- 上传文件保存在本地目录，尚未接入云存储或生产级持久化方案。
- 上传新封面不会自动删除旧文件，图片删除和旧文件清理延后。
- `review_count`、`overall_rating`、`brew_count` 字段存在于咖啡豆表和响应中，但 MVP 当前不做聚合回写。
- roastLevel / processMethod / status 等字段当前以自由输入或前端固定选项为主，尚未统一枚举查询。
- 数据库脚本包含风味标签相关表和种子数据，但风味标签业务尚未接入。
- 部分后端错误文案已经中文化，仍存在英文文案；统一中文化延后。
- 当前 UI 是 MVP 原生样式，正式 UI 组件体系延后。

## 延后事项

- 正式登录系统。
- 用户体系、token 生命周期、刷新与退出策略。
- 云存储、容器持久化、上传文件删除、旧文件清理。
- `review_count` / `overall_rating` / `brew_count` 聚合回写。
- 风味标签关联与前后端闭环。
- 枚举查询与字段标准化。
- 错误文案统一中文化。
- 正式 UI 组件体系。
- 更完整的自动化测试与部署流水线。

## 后续开发建议

建议下一阶段先补“部署前配置固化”：明确生产数据库账号、JWT 密钥、上传目录持久化路径和前端代理 / 部署基准路径，再进入正式登录或聚合回写等业务增强。这样可以先把 MVP 的运行环境边界钉牢，后续改业务时不容易反复踩配置问题。

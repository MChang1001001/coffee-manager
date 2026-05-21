# Coffee Manager

## 项目简介

咖啡豆管理项目是一个面向个人使用的咖啡豆档案库，用来记录咖啡豆基础信息、包装封面、主观评价和实际冲煮复盘，帮助用户形成可回溯的饮用经验。

当前项目处于 MVP 本地部署 / 本地可复现启动阶段：coffee / file / review / brew 主流程已完成前后端闭环，全流程 bugfix 和 MVP 收口检查已通过。本 README 面向本地 MySQL、本地 Spring Boot 后端、本地 Vite 前端和本地 uploads 目录，记录当前已实现能力、启动方式、配置方式和后续边界，不包含服务器上线能力。

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
  scripts/
    local-smoke.mjs                本地 MVP 主链路烟测脚本
  docs/                            需求、数据库、接口文档与项目状态索引
  uploads/                         运行时上传目录，已 gitignore
```

## 本地环境要求

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

## 本地 MySQL 数据库准备

MVP 本地部署使用本机 MySQL。当前项目统一使用库名 `coffee_manager`，以 `backend/src/main/resources/db/init.sql` 和 `application.yml` 中的默认 JDBC 地址为准。

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
Get-Content -Raw -Encoding UTF8 backend\src\main\resources\db\init.sql | mysql -uroot -p123456 --default-character-set=utf8mb4
```

如果本机 MySQL 密码不是 `123456`，请改用自己的账号执行脚本，例如：

```powershell
Get-Content -Raw -Encoding UTF8 backend\src\main\resources\db\init.sql | mysql -u你的用户名 -p --default-character-set=utf8mb4
```

如果 `mysql` 命令没有加入 PATH，请使用本机 MySQL 客户端完整路径，例如 `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`。

执行后确认数据库存在：

```powershell
mysql -uroot -p123456 -e "SHOW DATABASES LIKE 'coffee_manager';"
```

本地开发可以使用 `root` 或个人本地 MySQL 账号。如果希望更接近真实运行环境，建议创建独立账号 `coffee_app`，并只授权访问 `coffee_manager` 库。独立账号是建议项，不是本地 MVP 必须项。

## 后端本地配置

主要配置文件：

```text
backend/src/main/resources/application.yml
```

常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端服务端口 |
| `DB_URL` | `jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true` | MySQL JDBC 地址 |
| `DB_USERNAME` | `root` | MySQL 用户名；本地可用 root、个人账号，或建议账号 `coffee_app` |
| `DB_PASSWORD` | `123456` | MySQL 密码；按本机 MySQL 实际密码配置 |
| `FILE_UPLOAD_PATH` | `uploads` | 上传根目录，默认相对后端进程启动目录 |
| `JWT_SECRET` | 开发默认值 | JWT 签名密钥，本地可使用示例值或本地配置值 |
| `JWT_EXPIRATION_SECONDS` | `604800` | JWT 过期时间，默认 7 天 |
| `REDIS_HOST` | `localhost` | Redis 主机，仅后续预留，当前 MVP 不作为启动必需依赖 |
| `REDIS_PORT` | `6379` | Redis 端口，仅后续预留，当前 MVP 不作为启动必需依赖 |
| `REDIS_DATABASE` | `0` | Redis database，仅后续预留，当前 MVP 不作为启动必需依赖 |

当前 `application.yml` 中的数据库连接配置如下，本地可直接使用默认值，也可通过环境变量覆盖：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: ${DB_URL:jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}
```

本地配置基线：

- 数据库类型：MySQL。
- 当前统一库名：`coffee_manager`。
- 应用连接账号：本地可用默认 `root/123456`、个人本地账号，或建议独立账号 `coffee_app`。
- 配置位置：`backend/src/main/resources/application.yml`；当前项目没有单独的 `application-dev.yml`。
- 关键配置项：`spring.datasource.driver-class-name`、`spring.datasource.url`、`spring.datasource.username`、`spring.datasource.password`。
- 覆盖方式：可使用 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量；不要把本机真实密码提交到 Git。

Windows PowerShell 临时配置示例：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='你的数据库密码'
$env:FILE_UPLOAD_PATH='D:\dev\coffee-manager\uploads'
$env:JWT_SECRET='coffee-manager-local-dev-secret-change-me'
```

Linux / macOS 临时配置示例：

```bash
export DB_URL='jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='你的数据库密码'
export FILE_UPLOAD_PATH="$HOME/dev/coffee-manager/uploads"
export JWT_SECRET='coffee-manager-local-dev-secret-change-me'
```

### JWT_SECRET 配置基线

当前 JWT 配置位于 `application.yml`：

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:coffee-manager-dev-secret-key-change-me-20260507-min-32-bytes}
    expiration-seconds: ${JWT_EXPIRATION_SECONDS:604800}
```

MVP 本地开发可以使用默认值、示例值或本地环境变量配置值，保证项目可运行即可。不建议把任何真实生产 secret 写入配置文件或提交到 Git。

当前项目暂未实现正式登录系统、token 刷新、退出失效、黑名单等完整生命周期能力。本阶段只保证 MVP 可运行；正式登录和 token 生命周期后续统一处理。

### Redis 当前状态

`application.yml` 中保留了 `spring.data.redis` 配置项和 `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE` 环境变量占位，但 Redis 当前仅作为后续能力预留，不是 MVP 当前启动必需依赖。

本阶段不要把 Redis 写入必须启动项。正式缓存、token 黑名单或会话管理后续再统一设计。

## 后端本地启动

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

## 前端本地配置

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

当前项目继续使用 Vite proxy 做本地联调。如果本地后端端口变化，修改 `frontend/vite.config.ts` 的 proxy target 即可。本阶段没有正式前端环境变量体系，也不做大规模前端配置重构。

当前前端没有单独的 `.env` 配置要求。

## Vite proxy 本地联调说明

本地联调拓扑固定为：

```text
Browser -> http://localhost:5173
Vite /api proxy -> http://localhost:8080/api
Vite /uploads proxy -> http://localhost:8080/uploads
Spring Boot -> MySQL coffee_manager + local uploads directory
```

因此前端代码保持相对路径即可：

- API 请求：`/api/...`
- 图片访问：`/uploads/coffee-covers/{filename}`

本阶段不写 Nginx / 同源网关转发方案，也不把服务器部署写成当前已支持能力。

## 前端本地启动

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

## 本地烟测

本地烟测脚本位于：

```text
scripts/local-smoke.mjs
```

脚本用途：

- 快速确认本地 MVP 主链路可用。
- 适用于本地开发前、README 启动步骤调整后、或继续开发前的轻量回归检查。
- 不是生产验证脚本，不是 CI/CD 方案，也不是完整自动化测试体系。

运行前提：

- MySQL 已启动，且已初始化 `coffee_manager`。
- 后端已启动，默认地址为 `http://localhost:8080`。
- `FILE_UPLOAD_PATH` 已配置或使用默认上传目录，且后端进程可写。
- 前端可选启动，默认地址为 `http://localhost:5173`；如果前端未启动，脚本可以跳过前端 / Vite proxy 检查。
- 默认登录账号仍为 `admin/admin123456`。

运行命令：

```powershell
node scripts\local-smoke.mjs
```

可选参数：

```powershell
node scripts\local-smoke.mjs --backend-url http://localhost:8080 --frontend-url http://localhost:5173
node scripts\local-smoke.mjs --username admin --password admin123456
node scripts\local-smoke.mjs --skip-frontend
```

也可以使用环境变量覆盖：

```powershell
$env:SMOKE_BACKEND_URL='http://localhost:8080'
$env:SMOKE_FRONTEND_URL='http://localhost:5173'
$env:SMOKE_USERNAME='admin'
$env:SMOKE_PASSWORD='admin123456'
$env:SMOKE_SKIP_FRONTEND='false'
node scripts\local-smoke.mjs
```

检查范围：

- 后端健康检查：`GET /api/health`。
- 登录 / token 获取：`POST /api/auth/login`。
- coffee 主链路：列表、新增、详情、编辑、删除。
- file 主链路：上传封面，校验 `/uploads/coffee-covers/{filename}` 静态访问。
- review 主链路：列表、新增、详情、编辑、删除。
- brew 主链路：列表、新增、详情、编辑、删除。
- 前端可选检查：访问 `/coffee`，并通过 Vite proxy 检查 `/api` 和 `/uploads`。

预期输出：

```text
SMOKE_TEST_RESULT: PASS
```

脚本结束时会输出：

- 是否通过。
- 创建的 coffee / review / brew 测试数据 ID。
- 上传封面 URL。
- 是否执行了删除调用。
- 是否可能留下逻辑删除记录。
- 是否可能留下本地上传文件。

已知副作用：

- 烟测数据会带 `[SMOKE_TEST]` 前缀。
- 删除接口当前是逻辑删除，烟测会留下 `deleted=1` 的数据库记录。
- 文件上传会落盘，烟测会在本地 uploads 目录留下测试上传文件。
- 脚本只用于本地开发 / MVP 回归检查，不扩展为完整自动化测试体系。

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
- 本地部署不强制固定唯一绝对路径，但建议显式配置 `FILE_UPLOAD_PATH`，避免不同启动目录导致文件分散。
- Windows 示例：`D:/dev/coffee-manager/uploads`。
- macOS / Linux 示例：`~/dev/coffee-manager/uploads`。
- 目录需要提前创建并确保后端进程可写。
- 本地 uploads 存储策略 MVP 可接受；图片删除、旧文件清理、云存储迁移暂时延后。

创建上传目录示例：

```powershell
New-Item -ItemType Directory -Force D:\dev\coffee-manager\uploads
```

```bash
mkdir -p ~/dev/coffee-manager/uploads
```

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

本轮本地部署验收已实际执行并通过：

- 使用本机 MySQL 8 初始化 `coffee_manager`，确认 `users`、`coffee_beans`、`coffee_reviews`、`brew_records` 等表存在。
- 使用 `root/123456`、本地 `JWT_SECRET` 示例值和 `FILE_UPLOAD_PATH=D:\dev\coffee-manager\uploads` 启动后端。
- `GET http://localhost:8080/api/health` 返回 `status=ok`、`database=ok`。
- 启动 Vite 前端 `http://localhost:5173`，并确认 `/api`、`/uploads` 通过 Vite proxy 转发到 `http://localhost:8080`。
- 使用默认账号 `admin/admin123456` 登录成功，浏览器打开 `/coffee` 后自动进入咖啡豆页面。
- coffee 新增 / 详情 / 编辑 / 删除通过本地烟测。
- file 封面上传通过本地烟测，返回 URL 符合 `/uploads/coffee-covers/{filename}`，并可通过该路径访问图片。
- review 列表 / 新增 / 详情 / 编辑 / 删除通过本地烟测，评分步进 0.5 保持有效。
- brew 列表 / 新增 / 详情 / 编辑 / 删除通过本地烟测。
- 本地一键烟测脚本 `node scripts\local-smoke.mjs` 已运行通过，输出 `SMOKE_TEST_RESULT: PASS`。

本 README 对应的是当前 MVP 本地部署 / 本地可复现启动状态，不代表延后事项已经完成。

## MVP 已知限制

- 当前登录是 `admin/admin123456` 临时自动登录，没有正式登录页和完整用户体系。
- JWT 过期与刷新策略仍是开发阶段实现，正式 token 生命周期后续统一处理。
- 上传文件保存在本地目录，尚未接入云存储或服务器持久化方案。
- 上传新封面不会自动删除旧文件，图片删除和旧文件清理延后。
- `review_count`、`overall_rating`、`brew_count` 字段存在于咖啡豆表和响应中，但 MVP 当前不做聚合回写。
- roastLevel / processMethod / status 等字段当前以自由输入或前端固定选项为主，尚未统一枚举查询。
- 数据库脚本包含风味标签相关表和种子数据，但风味标签业务尚未接入。
- 部分后端错误文案已经中文化，仍存在英文文案；统一中文化延后。
- 当前 UI 是 MVP 原生样式，正式 UI 组件体系延后。

## 延后事项

- 正式登录系统。
- 用户体系、token 生命周期、刷新与退出策略。
- 服务器部署。
- Nginx / 同源网关转发。
- Docker 化。
- HTTPS / 域名。
- 云存储。
- 自动备份。
- CI/CD。
- Redis 正式接入。
- 上传文件删除、旧文件清理。
- `review_count` / `overall_rating` / `brew_count` 聚合回写。
- 风味标签关联与前后端闭环。
- 枚举查询与字段标准化。
- 错误文案统一中文化。
- 正式 UI 组件体系。
- 更完整的自动化测试与部署流水线。

## 后续开发建议

建议下一阶段先把“本地一键复现启动检查”固化下来：保留本地 MySQL、后端、前端和 MVP 主链路烟测步骤，方便后续每次继续开发前快速确认基础链路仍然可用。

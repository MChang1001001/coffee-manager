# Coffee Manager

## 项目简介

咖啡豆管理项目是一个面向个人使用的咖啡豆档案库，用来记录咖啡豆基础信息、包装封面、主观评价和实际冲煮复盘，帮助用户形成可回溯的饮用经验。

当前项目已收口为 v0.3 本地版：v1 / MVP 主链路、v2 Coffee 首页 UI 与本地启动能力、v3 聚合回写、饮用状态筛选、本地 smoke 残留清理、枚举值 / 常用选项体系、`variety` 豆种字段、中文错误文案与提示体验统一一期均已完成。本 README 面向本地 MySQL、本地 Spring Boot 后端、本地 Vite 前端和本地 uploads 目录，记录当前已实现能力、启动方式、配置方式和后续边界，不包含服务器上线能力。

## 功能概览

v0.3 本地版已实现：

- 日系手账风 Coffee 首页：`/coffee` 作为当前主页面，保留搜索、筛选、分页和卡片式档案管理。
- Coffee CRUD 主链路：咖啡豆新增、编辑、删除、详情、列表、搜索、筛选、分页。
- 封面上传与展示：支持 jpg / png / webp，单文件默认最大 5MB；标准访问路径为 `/uploads/coffee-covers/{filename}`。
- 评价 CRUD：围绕指定咖啡豆新增、编辑、删除、分页查看评价；综合评分必填，维度评分可选，评分范围 0.0-5.0，步进 0.5。
- 冲煮记录 CRUD：围绕指定咖啡豆新增、编辑、删除、分页查看冲煮复盘。
- 养豆期 / 赏味期字段：Coffee 新增、编辑、列表、详情均支持 `roastDate`、`bestFromDate`、`bestToDate`。
- 饮用状态展示：前端根据赏味开始 / 结束日期展示养豆中、赏味期中、即将过赏味期、已过赏味期或未填写日期。
- 饮用状态筛选：Coffee 分页列表支持 `drinkStatus`，可与关键词、烘焙度、处理法、产地组合筛选。
- 豆种字段：Coffee 新增、编辑、列表、详情均支持 `variety`。
- Coffee 档案详情页：`/coffee-beans/:id` 展示封面、基础信息、日期、状态、评分、评价数、冲煮数和操作入口。
- 详情页最近评价摘要：详情页拉取当前咖啡豆最近 3 条评价摘要。
- 详情页最近冲煮摘要：详情页拉取当前咖啡豆最近 3 条冲煮摘要。
- 聚合回写：`review_count`、`overall_rating`、`brew_count` 在 review / brew 新增、编辑、删除后回写。
- 枚举值 / 常用选项体系：`GET /api/enums/coffee` 返回 Coffee 表单和筛选复用的烘焙度、处理法、产地、豆种常用选项。
- 中文错误文案与提示体验统一一期：主要表单校验、上传错误、资源不存在、网络异常等提示已统一成更适合本地使用的中文文案。
- 本地 exe 启动器一期：`launcher/喝咖啡.exe` 包装 `scripts/local-start.ps1`，用于 Windows 本机双击启动。
- 本地烟测脚本：`node scripts\local-smoke.mjs` 验证 health、登录、coffee、枚举、饮用状态筛选、file、review、brew、聚合回写主链路。
- 本地 smoke 残留清理脚本：`node scripts\clean-smoke-data.mjs` 默认 dry-run，只清理可证明属于 `[SMOKE_TEST]` 的本地残留。
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
    src/views/CoffeeView.vue       当前 Coffee 首页
    src/views/CoffeeBeanDetailView.vue
                                    Coffee 档案详情页
    vite.config.ts                 Vite 代理配置
  scripts/
    local-start.ps1                本地启动脚本
    local-stop.ps1                 本地停止脚本
    local-status.ps1               本地状态检查脚本
    local-smoke.mjs                本地 v0.3 主链路烟测脚本
    clean-smoke-data.mjs           本地 smoke 残留清理脚本，默认 dry-run
  launcher/
    喝咖啡.exe                     Windows 本地 exe 启动器
  docs/                            需求、数据库、接口文档、项目状态索引与 release notes
    v0.2-release-notes.md          v0.2 本地版定版记录
    v0.3-release-notes.md          v0.3 本地版定版记录
    v0.3-clean-smoke-data.md       本地 smoke 残留清理说明
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

## v0.3 本地版启动方式速查

开发启动方式：

```powershell
cd backend
mvn spring-boot:run
```

```powershell
cd frontend
npm install
npm run dev
```

默认访问：

```text
后端: http://localhost:8080
前端: http://localhost:5173/coffee
详情页: http://localhost:5173/coffee-beans/{id}
```

本地烟测方式：

```powershell
node scripts\local-smoke.mjs
```

本地 exe / 启动器方式：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-start.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-status.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-stop.ps1
```

也可以双击：

```text
launcher/喝咖啡.exe
```

日志查看方式：

```powershell
Get-Content .\logs\backend.log -Tail 120
Get-Content .\logs\frontend.log -Tail 120
Get-Content .\logs\backend.log -Wait
Get-Content .\logs\frontend.log -Wait
```

常见问题优先检查：

- MySQL 是否已启动，`DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 是否匹配。
- `8080` / `5173` 是否被其他进程占用，可运行 `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-status.ps1`。
- `mvn`、`node`、`npm` 是否在 PATH。
- `frontend/node_modules` 是否存在；缺失时在 `frontend` 目录运行 `npm install`。
- `FILE_UPLOAD_PATH` 是否可写；未设置时启动脚本默认使用项目根目录下的 `uploads`。

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

应用配置中 `spring.sql.init.mode=never`，所以后端启动时不会自动执行 SQL。当前仍不引入 migration 工具，首次启动或旧库补字段都需要手动执行 SQL。执行任何 SQL 前，建议先备份本地 `coffee_manager` 库。

v0.3 本地版继续采用手动 SQL。旧库补字段时请手动执行下面的 `ALTER TABLE`，不要期待后端启动时自动迁移表结构。

```powershell
Get-Content -Raw -Encoding UTF8 backend\src\main\resources\db\init.sql | mysql -uroot -p123456 --default-character-set=utf8mb4
```

如果本机 MySQL 密码不是 `123456`，请改用自己的账号执行脚本，例如：

```powershell
Get-Content -Raw -Encoding UTF8 backend\src\main\resources\db\init.sql | mysql -u你的用户名 -p --default-character-set=utf8mb4
```

如果本地已有旧版 `coffee_beans` 表，本轮 v2 养豆期 / 赏味期字段不会被 `CREATE TABLE IF NOT EXISTS` 自动补上。若旧表完全没有这 3 个日期列，执行：

```sql
ALTER TABLE coffee_beans
  ADD COLUMN roast_date date null comment '烘焙日期',
  ADD COLUMN best_from_date date null comment '赏味开始日期',
  ADD COLUMN best_to_date date null comment '赏味结束日期';
```

当前 MVP 代码库已有 `roast_date`，多数本地旧库只需要补下面两列：

```sql
ALTER TABLE coffee_beans
  ADD COLUMN best_from_date date null comment '赏味开始日期',
  ADD COLUMN best_to_date date null comment '赏味结束日期';
```

如果本地旧版 `coffee_beans` 表还没有 v3 豆种字段，手动补充：

```sql
ALTER TABLE coffee_beans
  ADD COLUMN variety varchar(128) null comment '品种' AFTER farm;
```

如果本地旧版 `coffee_beans` 表还没有 v4 AI 评测总结字段，手动补充：

```powershell
Get-Content -Raw -Encoding UTF8 backend\src\main\resources\db\add_coffee_ai_summary_fields.sql | mysql -uroot -p123456 --default-character-set=utf8mb4
```

对应 SQL 内容为：

```sql
ALTER TABLE coffee_beans
  ADD COLUMN summary_title VARCHAR(128) DEFAULT NULL COMMENT '一句话评测总结' AFTER brew_count,
  ADD COLUMN flavor_summary TEXT DEFAULT NULL COMMENT '风味总结' AFTER summary_title,
  ADD COLUMN brew_suggestion TEXT DEFAULT NULL COMMENT '冲煮建议' AFTER flavor_summary,
  ADD COLUMN repurchase_intention VARCHAR(32) DEFAULT NULL COMMENT '回购意向' AFTER brew_suggestion,
  ADD COLUMN summary_text TEXT DEFAULT NULL COMMENT '评测总结正文' AFTER repurchase_intention,
  ADD COLUMN summary_source VARCHAR(16) DEFAULT NULL COMMENT '总结来源：MANUAL/AI' AFTER summary_text,
  ADD COLUMN summary_generated_at DATETIME DEFAULT NULL COMMENT 'AI总结生成时间' AFTER summary_source;
```

如果旧库中的 `review_count`、`overall_rating`、`brew_count` 历史聚合字段与现有 review / brew 数据不一致，可以手动执行历史修复 SQL：

```powershell
Get-Content -Raw -Encoding UTF8 backend\src\main\resources\db\refresh_coffee_aggregates.sql | mysql -uroot -p123456 --default-character-set=utf8mb4
```

该 SQL 只修复已有聚合缓存字段，不创建表、不修改表结构、不替代后续手动 SQL 管理。

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
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key；仅后端读取，未配置时 AI 总结接口返回中文提示 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | DeepSeek OpenAI 兼容接口基础地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | AI 总结使用的模型 |
| `DEEPSEEK_ENABLED` | `true` | 是否启用 DeepSeek 调用；关闭或未配置 key 时不调用外部 API |
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
$env:DEEPSEEK_API_KEY='你的 DeepSeek API Key'
```

Linux / macOS 临时配置示例：

```bash
export DB_URL='jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='你的数据库密码'
export FILE_UPLOAD_PATH="$HOME/dev/coffee-manager/uploads"
export JWT_SECRET='coffee-manager-local-dev-secret-change-me'
export DEEPSEEK_API_KEY='你的 DeepSeek API Key'
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

### DeepSeek AI 总结配置

AI 评测总结只在后端调用 DeepSeek，不会把 API Key 暴露给前端。推荐把真实 key 写在本地文件：

```text
backend/application-local.yml
```

该文件已被 `.gitignore` 忽略，不要提交到 Git。可以复制模板后修改：

```powershell
Copy-Item backend\application-local.example.yml backend\application-local.yml
```

示例内容：

```yaml
ai:
  deepseek:
    api-key: "你的 DeepSeek API Key"
    base-url: https://api.deepseek.com
    model: deepseek-chat
    enabled: true
```

`application.yml` 会自动额外导入这个本地文件：

```yaml
spring:
  config:
    import:
      - optional:file:./application-local.yml
      - optional:file:./backend/application-local.yml
```

默认配置仍保留环境变量占位，适合作为备用覆盖方式：

```yaml
ai:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:}
    base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
    model: ${DEEPSEEK_MODEL:deepseek-chat}
    enabled: ${DEEPSEEK_ENABLED:true}
```

修改 `backend/application-local.yml` 后必须重启后端才会生效。未配置 key 或关闭 `enabled` 时，`POST /api/coffee-beans/{id}/ai-summary` 不会调用外部 API，会返回中文提示：`AI 总结功能未配置 DeepSeek API Key。`

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

- 快速确认本地 v0.3 主链路可用。
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
- 枚举 / 常用选项：`GET /api/enums/coffee`。
- coffee 主链路：列表、新增、详情、编辑、删除、`variety` 读写。
- 饮用状态筛选：`drinkStatus` 五类状态与组合筛选。
- file 主链路：上传封面，校验 `/uploads/coffee-covers/{filename}` 静态访问。
- review 主链路：列表、新增、详情、编辑、删除，并校验 `review_count` / `overall_rating` 聚合。
- brew 主链路：列表、新增、详情、编辑、删除，并校验 `brew_count` 聚合。
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
- 脚本只用于本地开发 / v0.3 回归检查，不扩展为完整自动化测试体系。

### 本地 smoke 残留清理

本地清理脚本位于：

```powershell
node scripts\clean-smoke-data.mjs
node scripts\clean-smoke-data.mjs --dry-run
node scripts\clean-smoke-data.mjs --execute
```

默认和 `--dry-run` 都只预览，不删除；只有 `--execute` 会物理清理明确带 `[SMOKE_TEST]` 的 coffee 记录、其关联 `coffee_reviews` / `brew_records`，以及这些 smoke coffee 数据库记录明确引用的上传封面文件。无法从数据库记录证明归属的 uploads 文件会跳过，不会按文件名猜测删除。执行前请先 dry-run，详细说明见 `docs/v0.3-clean-smoke-data.md`。

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

枚举 / 常用选项：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/enums/coffee` | 返回 Coffee 新增、编辑和筛选可复用的 roastLevels / processMethods / origins / varieties 常用选项 |

咖啡豆：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/coffee-beans` | 咖啡豆分页列表，支持 keyword / roastLevel / processMethod / origin / drinkStatus / status / page / pageSize |
| `POST` | `/api/coffee-beans` | 新增咖啡豆 |
| `GET` | `/api/coffee-beans/{id}` | 咖啡豆详情 |
| `PUT` | `/api/coffee-beans/{id}` | 更新咖啡豆 |
| `POST` | `/api/coffee-beans/{id}/ai-summary` | 根据基础信息、评价、冲煮记录生成 AI 评测总结草稿，不直接保存 |
| `PUT` | `/api/coffee-beans/{id}/summary` | 保存用户确认后的评测总结字段 |
| `DELETE` | `/api/coffee-beans/{id}` | 删除咖啡豆 |

Coffee 新增 / 更新 / 详情 / 列表当前支持 `variety`、`roastDate`、`bestFromDate`、`bestToDate`、`status` 字段，日期以前端 `YYYY-MM-DD` 字符串提交和展示；饮用状态展示由前端根据赏味开始 / 结束日期本地计算，`drinkStatus` 列表筛选由后端基于数据库 `CURRENT_DATE` 判断。Coffee 页面默认筛选为 `drinkStatus=READY` + `status=OPENED`，即优先展示赏味期中且已开封的豆子。

Coffee 详情接口返回 `summaryTitle`、`flavorSummary`、`brewSuggestion`、`repurchaseIntention`、`summaryText`、`summarySource`、`summaryGeneratedAt`。`PUT /summary` 可保存这些字段，字段可为空；`summarySource=AI` 时后端写入当前生成时间，`summarySource=MANUAL` 时清空生成时间。

Coffee 档案详情页位于 `/coffee-beans/{id}`。详情页会展示基础信息、封面、饮用状态、评分 / 评价数 / 冲煮数、评测总结，并通过评价列表和冲煮列表接口各读取最近 3 条摘要。当前摘要顺序依赖后端列表接口的既有排序：`created_at desc, id desc`。

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

根据当前项目收口状态，v0.3 本地版已完成并通过以下验证范围：

- v1 / MVP：coffee、file、review、brew 主链路。
- v2 / v0.2：Coffee 首页 UI、养豆期 / 赏味期字段、Coffee 档案详情页、详情页最近评价 / 最近冲煮摘要、本地 exe 启动器一期、本地 smoke 脚本。
- v3 / v0.3：`review_count` / `overall_rating` / `brew_count` 聚合回写、饮用状态筛选、本地 smoke 残留清理、枚举值 / 常用选项体系、`variety` 豆种字段、中文错误文案与提示体验统一一期。
- 全流程仍保持本地 MySQL、本地 Spring Boot、本地 Vite、本地 uploads 目录和手动 SQL 管理方式。

本轮 v0.3 定版验收要求：

- 前端构建：在 `frontend` 目录运行 `npm run build`。
- 本地烟测：在项目根目录运行 `node scripts\local-smoke.mjs`，预期输出 `SMOKE_TEST_RESULT: PASS`。
- 清理工具预览：在项目根目录运行 `node scripts\clean-smoke-data.mjs --dry-run`，预期输出 `CLEAN_SMOKE_RESULT: PASS`。

本 README 对应的是当前 v0.3 本地版 / 本地可复现启动状态，不代表延后事项已经完成。

## 当前已接受行为 / 风险

- 枚举选项一期写在后端静态代码里，没有字典表和后台管理。
- 枚举接口走现有 JWT 认证体系，除临时登录、健康检查和静态 uploads 外，业务接口仍需 token。
- 饮用状态筛选基于数据库 `CURRENT_DATE`，依赖本地 MySQL / 后端时区一致性。
- 历史聚合字段一致性依赖手动 SQL；旧库可执行 `backend/src/main/resources/db/refresh_coffee_aggregates.sql` 修复。
- `scripts\clean-smoke-data.mjs` 会跳过无法从 smoke coffee 数据库记录证明归属的 uploads 文件。
- 后端中文 message 需要重启服务后生效。
- 前端封面大小限制当前按后端 5MB 配置同步写死。
- 当前仍是本地个人使用定位，未按多人协作、生产安全或公网部署标准收口。
- 烟测会创建带 `[SMOKE_TEST]` 前缀的数据，删除接口当前是逻辑删除，MySQL 中可能留下 `deleted=1` 记录；上传 smoke 封面文件可能留在本地 uploads 目录。
- 历史旧封面文件缺失时，前端使用封面兜底占位展示，不阻断列表和详情页浏览。

## 当前明确延后事项

- 正式登录和 token 生命周期。
- migration 工具。
- Docker / 上线部署。
- 字典表 / 枚举后台管理。
- 风味标签体系。
- 统计图表。
- 推荐算法。
- 赏味期提醒。
- 移动端专项适配。
- OCR 自动识别。
- 完整桌面应用壳。

## 后续开发建议

v0.3 之后继续保持“先跑本地 smoke、再做小步变更、最后 build + smoke + clean dry-run”的节奏。下一阶段如要扩展功能，建议优先在不改变本地个人使用定位的前提下，单独开分支处理正式登录、migration 或字典表等较大边界变更。

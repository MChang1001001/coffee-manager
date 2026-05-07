# Coffee Manager

咖啡豆管理软件，目标是记录咖啡豆入库信息、评价、冲煮方案与冲煮复盘。

## Project Structure

```text
backend/   Spring Boot 后端
frontend/  Vue 3 前端
docs/      产品、数据库、接口设计文档
```

## Local Requirements

- JDK 17+（本机已验证 JDK 21 可用）
- Maven 3.9+
- Node.js 20+
- MySQL 8+

## VS Code JDK

项目已提供本地 VS Code 配置：

```text
.vscode/settings.json
```

它会让 VS Code Java 插件和集成终端优先使用：

```text
C:\Program Files\Java\jdk-21
```

如果你刚修改过 Windows 系统环境变量，请重启 VS Code，或者至少关闭并重新打开 VS Code 终端。确认 Maven 使用的 JDK：

```bash
mvn -version
```

输出中的 `Java version` 应为 `17.x` 或更高版本。

如果普通终端仍显示 JDK 15，说明当前 VS Code 进程还没有继承新的系统环境变量。可以先使用 VS Code Task 启动：

1. 按 `Ctrl + Shift + P`
2. 选择 `Tasks: Run Task`
3. 先运行 `backend: mvn -version`
4. 再运行 `backend: spring-boot:run`
5. 另开一个任务运行 `frontend: npm run dev`

## Next Steps

1. 配置 `backend/src/main/resources/application.yml` 中的数据库账号密码。
2. 在 `frontend` 目录安装依赖并启动开发服务。
3. 根据 `docs` 下的设计文档继续补充实体、接口和页面。

## Backend Stage 2 Check

初始化数据库：

```bash
# 如果本机 mysql 命令可用
mysql -uroot -p123456 < backend/src/main/resources/db/init.sql
```

后端启动：

```bash
cd backend
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

默认登录账号：

```text
username: admin
password: admin123456
```

前端启动：

```bash
cd frontend
npm install
npm run dev
```

阶段 2 以后端骨架为主，前端页面后续会改为登录和咖啡豆主流程。

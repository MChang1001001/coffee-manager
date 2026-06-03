# Coffee Manager 本地 exe 启动器说明

## 1. 本地启动器定位

本轮提供的是 Windows 本地启动器方案：核心逻辑仍然是 PowerShell 脚本，exe 只是把 `scripts/local-start.ps1` 包装成更容易双击的入口。

它不是完整单文件应用，不包含 MySQL，不内置 Java/Maven/Node/npm，也不会把前端静态资源合并进后端。当前目标是方便本机日常启动本项目的 Spring Boot 后端和 Vite 前端。

## 2. 前置条件

- Windows + PowerShell。
- JDK 17 或更高版本。
- Maven 可通过 `mvn` 命令访问。
- Node.js、npm 可通过 `node` / `npm` 命令访问。
- MySQL 8 已启动，并已按 `backend/src/main/resources/db/init.sql` 初始化本地库。
- `frontend` 目录已执行过 `npm install`。
- 默认端口可用：后端 `8080`，前端 `5173`。

后端数据库连接仍读取当前项目配置：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/coffee_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Hong_Kong&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='你的本机 MySQL 密码'
```

如果没有显式设置 `FILE_UPLOAD_PATH`，启动脚本会默认使用项目根目录下的 `uploads`。

## 3. 如何启动

在项目根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-start.ps1
```

脚本会：

- 检查 `8080` 和 `5173` 是否已被占用。
- 如果后端或前端已经可访问，就复用现有服务，不重复启动。
- 启动后端 `mvn spring-boot:run`。
- 启动前端 `npm run dev`。
- 等待后端 `/api/health` 和前端 `/coffee` 可访问。
- 自动打开 `http://localhost:5173/coffee`。
- 写入日志和 PID 文件。

不想自动打开浏览器时：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-start.ps1 -NoBrowser
```

## 4. 如何停止

在项目根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-stop.ps1
```

停止脚本只读取 `logs/pids/backend.pid` 和 `logs/pids/frontend.pid`，并停止对应 PID 及其子进程。没有 PID 文件时，它不会粗暴杀掉所有 `java` 或 `node` 进程，以免误伤其他项目。

## 5. 如何查看状态

在项目根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-status.ps1
```

状态脚本会检查：

- `http://localhost:8080/api/health`
- `http://localhost:5173/coffee`
- PID 文件是否存在且进程是否仍在运行
- `8080` / `5173` 当前监听 PID

如果服务未启动，脚本会提示下一步启动命令。

## 6. 如何查看日志

启动脚本会自动创建：

```text
logs/
  backend.log
  frontend.log
  pids/
    backend.pid
    frontend.pid
```

查看后端日志：

```powershell
Get-Content .\logs\backend.log -Wait
```

查看前端日志：

```powershell
Get-Content .\logs\frontend.log -Wait
```

`logs/` 属于本地运行产物，已被 `.gitignore` 忽略，不建议提交。

## 7. 如何生成 exe

推荐使用 PowerShell 脚本作为真实启动逻辑，再用 `ps2exe` 包装成 `喝咖啡.exe`。

在 Windows 本机项目根目录执行：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
Install-Module ps2exe -Scope CurrentUser
New-Item -ItemType Directory -Force .\launcher
Invoke-PS2EXE .\scripts\local-start.ps1 .\launcher\喝咖啡.exe -iconFile .\launcher\coffee-cup.ico -title "喝咖啡"
```

生成后可以双击：

```text
launcher/喝咖啡.exe
```

建议把 exe 放在项目根目录或项目根目录下一级目录，例如 `launcher/`。脚本会从 exe 所在目录、父目录或当前工作目录中寻找包含 `backend/pom.xml` 和 `frontend/package.json` 的项目根。

注意：

- exe 只是启动器，不是完整安装包。
- exe 不包含 MySQL，本机仍需启动 MySQL。
- exe 不包含 Java、Maven、Node、npm。
- exe 不会自动安装前端依赖；如果缺少 `frontend/node_modules`，请先运行 `npm install`。
- 如果 PowerShell Gallery 不可访问，无法安装 `ps2exe`，可以先直接使用 `scripts/local-start.ps1`。

## 8. 常见问题

### 端口被占用

运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\local-status.ps1
```

如果脚本显示端口被占用但服务不可访问，请确认占用 PID 是否来自本项目。停止脚本只停止本项目记录的 PID；没有 PID 文件时不会误杀其他进程。

### MySQL 未启动

后端可能启动失败，或 `/api/health` 无法返回 `database=ok`。先启动本机 MySQL，再检查 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。

### Java 未安装

执行：

```powershell
java -version
```

如果命令不存在，请安装 JDK 17+ 并配置 PATH。

### Node 未安装

执行：

```powershell
node -v
npm -v
```

如果命令不存在，请安装 Node.js 并配置 PATH。

### 前端依赖未安装

如果启动脚本提示缺少 `frontend/node_modules`，执行：

```powershell
cd frontend
npm install
```

然后回到项目根目录重新运行启动脚本。

### 后端启动失败

优先查看：

```powershell
Get-Content .\logs\backend.log -Tail 120
```

常见原因包括 MySQL 未启动、数据库未初始化、数据库账号密码不匹配、`mvn` 不在 PATH、端口 `8080` 被其他进程占用。

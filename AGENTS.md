# AGENTS.md

## 项目概览

Coffee Manager 是一个个人咖啡豆档案库项目，用于记录咖啡豆基础信息、包装封面、主观评价和冲煮复盘。当前 `/coffee` 是核心页面，承载 coffee CRUD、封面上传、评价入口、冲煮入口、搜索、筛选和分页等 MVP 主链路。

## 技术栈

- Backend: Java 17+, Spring Boot 3.3.5, MyBatis-Plus, MySQL 8, JWT。
- Frontend: Vue 3, Vite, TypeScript, Vue Router, Pinia, Axios。
- Styling: 使用项目内本地 CSS，不默认引入 UI 框架。
- Local smoke: `scripts\local-smoke.mjs`。

## 固定开发规则

- 每次继续开发前，先在项目根目录运行 `node scripts\local-smoke.mjs`，确认当前本地主链路基线；如果本地服务或 MySQL 未就绪，要在交付中说明。
- 前端修改后，在 `frontend` 目录运行 `npm run build`。
- 避免无关重构；优先沿用当前 Vue 组件、API 封装和本地 CSS 组织方式。

## Coffee 页面 UI 优化边界

- 不改后端接口。
- 不改数据库结构。
- 不新增养豆期 / 赏味期字段。
- 不引入 UI 框架。
- 不做详情页。
- 保持 coffee CRUD、封面上传、评价入口、冲煮入口、搜索、筛选、分页可用。

## 推荐 Skill

Coffee 页面 UI 优化任务推荐显式使用项目级 skill：`coffee-ui-homepage`。

后续可这样调用：

```text
使用 coffee-ui-homepage skill 优化 Coffee 页面 UI。
```

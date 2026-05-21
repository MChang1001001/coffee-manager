---
name: coffee-ui-homepage
description: Use this skill when improving the Coffee page UI for the coffee bean management project. It should guide Vue3/Vite frontend UI changes that make the Coffee page feel like a personal coffee bean archive homepage while preserving existing CRUD, cover upload, review, brew, search, filter, and pagination behavior. Do not use this skill for backend, database, auth, or schema changes.
---

# coffee-ui-homepage

## 项目背景

Coffee Manager 是一个面向个人使用的咖啡豆管理项目。`/coffee` 是当前核心页面，用来管理咖啡豆档案、包装封面、主观评价和冲煮复盘。后续优化 Coffee 页面时，要把它从 MVP 原生界面打磨成更像“个人咖啡豆档案库首页”的体验：能快速扫视藏豆、识别状态、搜索筛选、进入新增/编辑/评价/冲煮流程。

## 当前技术栈

- Frontend: Vue 3, Vite, TypeScript, Vue Router, Pinia, Axios.
- Styling: 当前项目使用本地 CSS，不引入新的 UI 框架。
- Backend: Java 17+, Spring Boot 3.3.5, MyBatis-Plus, MySQL 8, JWT。
- 主页面: `frontend/src/views/CoffeeView.vue`。
- 相关 API 封装: `frontend/src/api/coffee.ts`, `frontend/src/api/file.ts`, `frontend/src/api/review.ts`, `frontend/src/api/brew.ts`。

## 本轮硬边界

- 只做 Coffee 页面前端 UI 和交互层优化；不要改后端接口、数据库结构、认证逻辑或 schema。
- 不新增“养豆期”或“赏味期”字段，不伪造后端不存在的数据。
- 不引入 UI 框架或大型新依赖；优先使用现有 Vue/CSS 能力。
- 不做详情页，不把现有弹窗/入口拆成新的详情路由。
- 保持 coffee CRUD、封面上传、评价入口、冲煮入口、搜索、筛选、分页全部可用。
- 不删除已有业务字段、请求参数、响应处理、错误处理或自动登录链路。

## Coffee 页面视觉方向

- 把首屏塑造成个人咖啡豆档案库首页，而不是后台表格或营销落地页。
- 视觉应温暖、安静、适合长期记录；避免夸张 hero、过度装饰、单一棕色/米色主题或大面积渐变。
- 页面应优先支持扫描和管理：清楚显示当前咖啡豆数量、筛选状态、主要操作和档案列表。
- 使用真实封面图作为重要视觉锚点；没有封面时提供克制、稳定的占位样式。
- 布局应响应式适配桌面和移动端，避免按钮、标题、筛选项、卡片内容互相挤压或重叠。

## Preferred visual reference

Use a Japanese notebook-style coffee archive dashboard as the preferred visual reference. The Coffee page should feel like a personal coffee bean notebook / coffee bean archive, not a plain backend CRUD screen.

Translate the reference into maintainable Vue 3 + CSS rather than reproducing it pixel-for-pixel. Carry over the atmosphere, layout rhythm, information hierarchy, and visual language, while keeping the page efficient for real coffee management work.

Prefer these visual cues:

- warm paper-like background.
- subtle dotted or grid paper texture built with CSS, not external images.
- hand-drawn or slightly imperfect borders.
- coffee archive / personal notebook feeling.
- light manga or journal-style decorative touches.
- deep green primary actions.
- warm cream, coffee brown, ink black, and muted red accents.
- cover images that feel like coffee bean cards pasted into a notebook.
- search and filters that feel like a stationery toolbar.
- archive-sheet-like list or table treatment, polished but not corporate.

The page structure can reference:

- Hero area with the main title “咖啡豆档案”.
- Featured coffee card, such as “今日档案” or “今日推荐”, using the first available coffee item only. Do not implement a recommendation algorithm.
- Search/filter toolbar for keyword, roastLevel, processMethod, origin, query, reset, and refresh.
- Polished archive table or card/table hybrid for the coffee bean list.

Recommended copy tone:

- “每天一杯冰美式，life 才能 moving on.”
- Supporting copy may explain that the archive records each bean’s source, flavor, and status so each brew has more direction.

Avoid:

- Pixel-level copying of any reference image.
- Heavy illustration work.
- External image dependencies for decoration.
- Overly complex CSS drawings.
- Decoration that reduces usability, scanability, accessibility, or maintainability.
- Turning the page into a poster; it must remain an efficient management page.

## Coffee 卡片/列表展示要求

- 卡片或列表项必须保留能识别咖啡豆的核心信息：名称、产地/地区、处理法、烘焙度、烘焙商、状态、关键日期、重量/价格等已有字段中适合展示的内容。
- 封面图要稳定占位，不因图片缺失、加载失败或长文本造成布局跳动。
- 重要标签和元信息要适合扫视；避免把所有字段等权堆满。
- 操作入口要清楚但不喧宾夺主：编辑、删除、评价、冲煮应能从每个咖啡豆条目自然进入。
- 保持分页行为与现有数据流一致，不用前端假分页替代后端分页。

## 搜索筛选区要求

- 搜索和筛选应像档案库的工具栏：清楚、紧凑、可反复使用。
- 保留现有 keyword、roastLevel、processMethod、origin、page、pageSize 等查询行为。
- 筛选控件的标签、占位文案和清空/重置入口要明确。
- 搜索、筛选、分页之间的状态更新不能互相覆盖或丢失。
- 移动端筛选区可以换行或折叠，但不能牺牲可用性。

## loading / empty / error 状态要求

- loading 状态应保持页面结构稳定，避免列表区域突然坍塌。
- empty 状态要表达“档案库暂时为空”或“没有匹配结果”，并保留新增咖啡豆或清空筛选的自然路径。
- error 状态要可读、可恢复，保留重试或重新触发查询的入口。
- 不要用假数据掩盖真实空状态或错误状态。

## 新增、编辑、删除、评价、冲煮入口要求

- 新增咖啡豆入口应在页面中足够显眼，适合个人档案库首页的主要操作。
- 编辑仍使用现有编辑流程和数据回填逻辑。
- 删除必须保留确认或防误触保护，不改变现有删除 API 语义。
- 评价入口必须继续指向指定 coffee bean 的评价流程，不能丢失 coffeeBeanId 关联。
- 冲煮入口必须继续指向指定 coffee bean 的冲煮记录流程，不能丢失 coffeeBeanId 关联。
- 封面上传入口和上传后的预览/保存逻辑必须保持可用。

## 验证要求

完成 Coffee 页面 UI 修改后，至少运行：

```powershell
cd frontend
npm run build
```

并回到项目根目录运行本地烟测：

```powershell
node scripts\local-smoke.mjs
```

如果本地后端、MySQL 或前端服务未启动导致烟测无法完整执行，要在交付报告中说明阻塞条件、已运行的替代检查以及未覆盖风险。

## 交付报告要求

交付 Coffee 页面 UI 优化时，报告必须包含：

- 改动文件列表。
- UI 方向和主要交互变化摘要。
- 明确说明未改后端接口、数据库结构、认证和 schema。
- 明确说明 CRUD、封面上传、评价入口、冲煮入口、搜索、筛选、分页是否保持可用。
- 验证结果：`npm run build` 和 `node scripts\local-smoke.mjs` 的执行结果；如未运行，说明原因。

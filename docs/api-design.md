# API Design

本文档记录当前 MVP 已实现的核心接口。早期草案中出现但未在 MVP 实现的接口，不在此处列为已完成能力。

## Base Path

`/api`

除 `/api/auth/login`、`/api/health` 和 `/uploads/**` 静态资源外，业务接口需要 JWT。前端当前通过 `admin/admin123456` 临时自动登录获取 token。

## Response

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## Auth

- `POST /api/auth/login` 登录。
- `GET /api/auth/me` 获取当前用户信息。
- `GET /api/health` 健康检查。

## Enums

- `GET /api/enums/coffee` 获取 Coffee 表单和筛选复用的常用选项，包含 `roastLevels`、`processMethods`、`origins`、`varieties`。
- 枚举接口走现有 JWT 认证体系；当前选项写在后端静态代码里，暂不提供字典表或后台管理。

## Coffee Beans

- `GET /api/coffee-beans` 咖啡豆列表，支持 `keyword`、`roastLevel`、`processMethod`、`origin`、`drinkStatus`、`status`、`page`、`pageSize`。
- `POST /api/coffee-beans` 新增咖啡豆。
- `GET /api/coffee-beans/{id}` 咖啡豆详情。
- `PUT /api/coffee-beans/{id}` 更新咖啡豆。
- `POST /api/coffee-beans/{id}/ai-summary` 生成 AI 评测总结草稿，只返回 `summaryTitle`、`flavorSummary`、`brewSuggestion`、`repurchaseIntention`、`summaryText`，不直接保存。
- `PUT /api/coffee-beans/{id}/summary` 保存用户确认后的评测总结字段，支持字段为空。
- `DELETE /api/coffee-beans/{id}` 删除咖啡豆。
- 咖啡豆新增 / 更新 / 详情 / 列表支持 `variety`、`roastDate`、`bestFromDate`、`bestToDate`，日期格式为 `YYYY-MM-DD`，可为空。
- 咖啡豆详情返回 `summaryTitle`、`flavorSummary`、`brewSuggestion`、`repurchaseIntention`、`summaryText`、`summarySource`、`summaryGeneratedAt`；列表仅返回轻量 `summaryTitle`。
- `drinkStatus` 支持 `NO_DATE`、`RESTING`、`READY`、`EXPIRING_SOON`、`EXPIRED`，筛选基于数据库 `CURRENT_DATE`。
- `status` 支持按当前 Coffee 豆子状态等值筛选，页面使用 `UNOPENED`、`OPENED`、`FINISHED`。

AI 评测总结使用后端 `DEEPSEEK_API_KEY` 调用 DeepSeek OpenAI 兼容 `POST /chat/completions`。未配置 key 或关闭 `DEEPSEEK_ENABLED` 时，接口返回中文提示 `AI 总结功能未配置 DeepSeek API Key。`，不会调用外部 API。

## Files

- `POST /api/files/coffee-cover` 上传咖啡豆封面，multipart 字段名为 `file`。
- `GET /uploads/coffee-covers/{filename}` 访问上传后的封面图片。

封面静态 URL 标准固定为 `/uploads/coffee-covers/{filename}`。`/api/files/coffee-cover` 只是上传接口，不作为图片静态访问前缀。

## Reviews

- `GET /api/coffee-beans/{coffeeBeanId}/reviews` 指定咖啡豆的评价列表。
- `POST /api/coffee-beans/{coffeeBeanId}/reviews` 新增评价。
- `GET /api/reviews/{id}` 评价详情。
- `PUT /api/reviews/{id}` 更新评价。
- `DELETE /api/reviews/{id}` 删除评价。

评价评分范围为 0.0-5.0，步进为 0.5；综合评分必填，维度评分可选。
新增、编辑、删除评价后会刷新咖啡豆的 `review_count` / `overall_rating` 聚合字段。

## Brew Records

- `GET /api/coffee-beans/{coffeeBeanId}/brew-records` 指定咖啡豆的冲煮记录列表。
- `POST /api/coffee-beans/{coffeeBeanId}/brew-records` 新增冲煮记录。
- `GET /api/brew-records/{id}` 冲煮记录详情。
- `PUT /api/brew-records/{id}` 更新冲煮记录。
- `DELETE /api/brew-records/{id}` 删除冲煮记录。
新增、编辑、删除冲煮记录后会刷新咖啡豆的 `brew_count` 聚合字段。

## Deferred

- `PATCH /api/coffee-beans/{id}/status` 未实现，当前通过更新咖啡豆接口整体保存状态字段。
- 风味标签查询、新增和关联未形成 MVP 闭环。
- 字典表 / 枚举后台管理未实现，当前仅提供后端静态常用选项。

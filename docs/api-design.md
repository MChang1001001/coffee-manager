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

## Coffee Beans

- `GET /api/coffee-beans` 咖啡豆列表，支持 `keyword`、`roastLevel`、`processMethod`、`origin`、`page`、`pageSize`。
- `POST /api/coffee-beans` 新增咖啡豆。
- `GET /api/coffee-beans/{id}` 咖啡豆详情。
- `PUT /api/coffee-beans/{id}` 更新咖啡豆。
- `DELETE /api/coffee-beans/{id}` 删除咖啡豆。

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

## Brew Records

- `GET /api/coffee-beans/{coffeeBeanId}/brew-records` 指定咖啡豆的冲煮记录列表。
- `POST /api/coffee-beans/{coffeeBeanId}/brew-records` 新增冲煮记录。
- `GET /api/brew-records/{id}` 冲煮记录详情。
- `PUT /api/brew-records/{id}` 更新冲煮记录。
- `DELETE /api/brew-records/{id}` 删除冲煮记录。

## Deferred

- `PATCH /api/coffee-beans/{id}/status` 未实现，当前通过更新咖啡豆接口整体保存状态字段。
- 风味标签查询、新增和关联未形成 MVP 闭环。
- `review_count` / `overall_rating` / `brew_count` 聚合回写延后。
- 枚举查询与字段标准化延后。

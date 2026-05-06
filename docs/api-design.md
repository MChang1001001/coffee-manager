# API Design

## Base Path

`/api`

## Coffee Beans

- `GET /api/coffee-beans` 咖啡豆列表
- `POST /api/coffee-beans` 新增咖啡豆
- `GET /api/coffee-beans/{id}` 咖啡豆详情
- `PUT /api/coffee-beans/{id}` 更新咖啡豆
- `PATCH /api/coffee-beans/{id}/status` 更新状态
- `DELETE /api/coffee-beans/{id}` 删除咖啡豆

## Reviews

- `GET /api/coffee-beans/{beanId}/reviews` 评价列表
- `POST /api/coffee-beans/{beanId}/reviews` 新增评价
- `PUT /api/reviews/{id}` 更新评价
- `DELETE /api/reviews/{id}` 删除评价

## Brew Records

- `GET /api/coffee-beans/{beanId}/brew-records` 冲煮记录列表
- `POST /api/coffee-beans/{beanId}/brew-records` 新增冲煮记录
- `PUT /api/brew-records/{id}` 更新冲煮记录
- `DELETE /api/brew-records/{id}` 删除冲煮记录

## Tags

- `GET /api/flavor-tags` 风味标签列表
- `POST /api/flavor-tags` 新增风味标签

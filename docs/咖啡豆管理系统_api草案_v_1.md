# 咖啡豆管理系统 API 草案 v1

## 1. API 设计目标

第一版 API 目标是支撑咖啡豆管理系统 MVP 的完整主流程：

1. 登录和获取当前用户信息。
2. 咖啡豆新增、编辑、删除、详情、列表查询。
3. 咖啡豆包装封面上传、替换、删除。
4. 咖啡豆评价新增、编辑、删除、列表查询。
5. 冲煮记录新增、编辑、删除、列表查询。
6. 风味标签查询和创建。
7. 基础枚举查询。

API 设计优先考虑简单、清晰、方便前后端联调。

---

## 2. 基础约定

### 2.1 Base URL

开发环境建议：

```text
http://localhost:8080/api
```

### 2.2 数据格式

请求和响应默认使用 JSON。

文件上传接口使用 `multipart/form-data`。

### 2.3 认证方式

第一版建议使用 Token 认证。

登录成功后返回 token。

前端后续请求在 Header 中携带：

```http
Authorization: Bearer {token}
```

### 2.4 时间格式

日期字段使用：

```text
YYYY-MM-DD
```

日期时间字段使用：

```text
YYYY-MM-DD HH:mm:ss
```

### 2.5 分页参数

列表接口统一使用：

```text
page
pageSize
```

示例：

```http
GET /api/coffees?page=1&pageSize=20
```

---

## 3. 统一响应结构

### 3.1 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 3.2 失败响应

```json
{
  "code": 40001,
  "message": "参数错误",
  "data": null
}
```

### 3.3 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 100,
    "totalPages": 5
  }
}
```

---

## 4. 错误码草案

| 错误码 | 含义 |
|---|---|
| 0 | 成功 |
| 40000 | 请求错误 |
| 40001 | 参数错误 |
| 40100 | 未登录或 token 无效 |
| 40300 | 无权限 |
| 40400 | 资源不存在 |
| 40900 | 数据冲突 |
| 50000 | 服务端错误 |

---

## 5. Auth 登录模块

### 5.1 登录

```http
POST /auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "admin123456"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token-example",
    "user": {
      "id": 1,
      "username": "admin",
      "nickname": "mian"
    }
  }
}
```

### 5.2 获取当前用户信息

```http
GET /auth/me
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "mian",
    "avatarUrl": null
  }
}
```

### 5.3 登出

```http
POST /auth/logout
```

第一版如果使用无状态 JWT，可以由前端直接删除 token。

后端接口可以保留，但不强制实现复杂黑名单。

---

## 6. Coffee 咖啡豆模块

### 6.1 咖啡豆列表

```http
GET /coffees
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | number | 否 | 页码，默认 1 |
| pageSize | number | 否 | 每页数量，默认 20 |
| keyword | string | 否 | 关键词 |
| status | string | 否 | 状态 |
| origin | string | 否 | 产地 |
| processMethod | string | 否 | 处理法 |
| roastLevel | string | 否 | 烘焙度 |
| flavorTagIds | string | 否 | 风味标签 ID，逗号分隔 |
| minRating | number | 否 | 最低评分 |
| maxRating | number | 否 | 最高评分 |
| sortBy | string | 否 | 排序字段 |
| sortOrder | string | 否 | asc / desc |

示例：

```http
GET /coffees?page=1&pageSize=20&keyword=耶加雪菲&status=OPENED&sortBy=createdAt&sortOrder=desc
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "name": "Ethiopia Yirgacheffe",
        "origin": "Ethiopia",
        "region": "Yirgacheffe",
        "processMethod": "WASHED",
        "roastLevel": "LIGHT",
        "roaster": "Example Roaster",
        "roastDate": "2026-05-01",
        "purchaseDate": "2026-05-03",
        "status": "OPENED",
        "coverImageUrl": "/files/coffee-covers/1/20260507/example.jpg",
        "overallRating": 4.5,
        "reviewCount": 2,
        "brewCount": 5,
        "flavorTags": [
          { "id": 1, "name": "柑橘" },
          { "id": 10, "name": "花香" }
        ],
        "createdAt": "2026-05-07 10:00:00"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

### 6.2 咖啡豆详情

```http
GET /coffees/{id}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "Ethiopia Yirgacheffe",
    "origin": "Ethiopia",
    "region": "Yirgacheffe",
    "farm": "Example Farm",
    "variety": "Heirloom",
    "processMethod": "WASHED",
    "roastLevel": "LIGHT",
    "roaster": "Example Roaster",
    "roastDate": "2026-05-01",
    "purchaseDate": "2026-05-03",
    "openDate": "2026-05-06",
    "finishDate": null,
    "netWeightGrams": 200,
    "price": 128,
    "currency": "CNY",
    "status": "OPENED",
    "coverImageUrl": "/files/coffee-covers/1/20260507/example.jpg",
    "overallRating": 4.5,
    "reviewCount": 2,
    "brewCount": 5,
    "notes": "适合手冲。",
    "flavorTags": [
      { "id": 1, "name": "柑橘" },
      { "id": 10, "name": "花香" }
    ],
    "createdAt": "2026-05-07 10:00:00",
    "updatedAt": "2026-05-07 10:30:00"
  }
}
```

### 6.3 新增咖啡豆

```http
POST /coffees
```

请求体：

```json
{
  "name": "Ethiopia Yirgacheffe",
  "origin": "Ethiopia",
  "region": "Yirgacheffe",
  "farm": "Example Farm",
  "variety": "Heirloom",
  "processMethod": "WASHED",
  "roastLevel": "LIGHT",
  "roaster": "Example Roaster",
  "roastDate": "2026-05-01",
  "purchaseDate": "2026-05-03",
  "openDate": null,
  "finishDate": null,
  "netWeightGrams": 200,
  "price": 128,
  "currency": "CNY",
  "status": "UNOPENED",
  "flavorTagIds": [1, 10],
  "notes": "适合手冲。"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

说明：

- 新增咖啡豆接口不直接上传图片。
- 包装封面通过独立文件接口上传。
- 前端可以先创建咖啡豆，再上传封面。

### 6.4 编辑咖啡豆

```http
PUT /coffees/{id}
```

请求体同新增咖啡豆。

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

### 6.5 删除咖啡豆

```http
DELETE /coffees/{id}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

说明：

第一版建议逻辑删除。

删除咖啡豆时，可以同步逻辑删除关联评价和冲煮记录。

### 6.6 更新咖啡豆状态

```http
PATCH /coffees/{id}/status
```

请求体：

```json
{
  "status": "OPENED"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

---

## 7. File 文件模块

### 7.1 上传咖啡豆包装封面

```http
POST /coffees/{id}/cover
```

请求类型：

```text
multipart/form-data
```

表单字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | file | 是 | 图片文件 |

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "coverImageUrl": "/files/coffee-covers/1/20260507/example.jpg",
    "coverImageObjectKey": "coffee-covers/1/20260507/example.jpg"
  }
}
```

说明：

- 上传成功后，后端更新 `coffee_beans.cover_image_url` 和 `coffee_beans.cover_image_object_key`。
- 如果原来已有封面，后端可以删除旧文件或保留旧文件。
- 第一版建议替换时删除旧文件，避免本地文件堆积。

### 7.2 删除咖啡豆包装封面

```http
DELETE /coffees/{id}/cover
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

说明：

删除后清空：

```text
cover_image_url
cover_image_object_key
```

### 7.3 访问图片文件

```http
GET /files/**
```

说明：

该接口用于访问本地上传文件。

示例：

```text
/files/coffee-covers/1/20260507/example.jpg
```

后端可通过静态资源映射实现。

---

## 8. Review 评价模块

### 8.1 查询某个咖啡豆的评价列表

```http
GET /coffees/{coffeeBeanId}/reviews
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "coffeeBeanId": 1,
        "overallRating": 4.5,
        "aromaRating": 4.5,
        "acidityRating": 4.0,
        "sweetnessRating": 4.0,
        "bitternessRating": 2.0,
        "bodyRating": 3.5,
        "aftertasteRating": 4.0,
        "flavorTags": [
          { "id": 1, "name": "柑橘" },
          { "id": 10, "name": "花香" }
        ],
        "content": "干净明亮，酸质舒服，尾段有花香。",
        "createdAt": "2026-05-07 11:00:00"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

### 8.2 新增评价

```http
POST /coffees/{coffeeBeanId}/reviews
```

请求体：

```json
{
  "overallRating": 4.5,
  "aromaRating": 4.5,
  "acidityRating": 4.0,
  "sweetnessRating": 4.0,
  "bitternessRating": 2.0,
  "bodyRating": 3.5,
  "aftertasteRating": 4.0,
  "flavorTagIds": [1, 10],
  "content": "干净明亮，酸质舒服，尾段有花香。"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

说明：

新增评价后，需要重新计算该咖啡豆的：

```text
overall_rating
review_count
```

### 8.3 编辑评价

```http
PUT /reviews/{id}
```

请求体同新增评价。

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

说明：

编辑评价后，需要重新计算该咖啡豆的综合评分缓存。

### 8.4 删除评价

```http
DELETE /reviews/{id}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

说明：

删除评价后，需要重新计算该咖啡豆的：

```text
overall_rating
review_count
```

---

## 9. Brew 冲煮记录模块

### 9.1 查询某个咖啡豆的冲煮记录列表

```http
GET /coffees/{coffeeBeanId}/brews
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| page | number | 否 | 页码 |
| pageSize | number | 否 | 每页数量 |
| brewMethod | string | 否 | 冲煮方式 |
| recommendedOnly | boolean | 否 | 是否只看推荐记录 |

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "coffeeBeanId": 1,
        "brewMethod": "V60",
        "beanAmountGrams": 15,
        "waterAmountMl": 240,
        "ratio": "1:16",
        "waterTemperature": 92,
        "grindSize": "中细研磨",
        "brewTimeSeconds": 150,
        "resultSummary": "明亮、干净、甜感不错",
        "resultNotes": "前段柑橘明显，尾段有一点茶感。下次可以降水温到91℃。",
        "isRecommended": true,
        "createdAt": "2026-05-07 12:00:00"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

### 9.2 新增冲煮记录

```http
POST /coffees/{coffeeBeanId}/brews
```

请求体：

```json
{
  "brewMethod": "V60",
  "beanAmountGrams": 15,
  "waterAmountMl": 240,
  "ratio": "1:16",
  "waterTemperature": 92,
  "grindSize": "中细研磨",
  "brewTimeSeconds": 150,
  "resultSummary": "明亮、干净、甜感不错",
  "resultNotes": "前段柑橘明显，尾段有一点茶感。下次可以降水温到91℃。",
  "isRecommended": true
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

说明：

新增冲煮记录后，需要更新该咖啡豆的：

```text
brew_count
```

### 9.3 编辑冲煮记录

```http
PUT /brews/{id}
```

请求体同新增冲煮记录。

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

### 9.4 删除冲煮记录

```http
DELETE /brews/{id}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

说明：

删除冲煮记录后，需要更新该咖啡豆的：

```text
brew_count
```

### 9.5 更新推荐状态

```http
PATCH /brews/{id}/recommended
```

请求体：

```json
{
  "recommended": true
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": true
}
```

---

## 10. Tag 标签模块

### 10.1 查询风味标签列表

```http
GET /tags/flavors
```

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 关键词 |
| category | string | 否 | 标签分类 |

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "id": 1, "name": "柑橘", "category": "水果" },
    { "id": 2, "name": "柠檬", "category": "水果" },
    { "id": 10, "name": "花香", "category": "花香" }
  ]
}
```

### 10.2 新增风味标签

```http
POST /tags/flavors
```

请求体：

```json
{
  "name": "白桃",
  "category": "水果"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 24
  }
}
```

说明：

第一版允许用户创建新风味标签。

如果标签名称已存在，直接返回已有标签 ID 或提示重复均可。

建议：返回已有标签 ID，减少前端处理成本。

---

## 11. Enum 枚举模块

### 11.1 查询所有枚举

```http
GET /enums
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "beanStatuses": [
      { "value": "UNOPENED", "label": "未开封" },
      { "value": "OPENED", "label": "已开封" },
      { "value": "FINISHED", "label": "已喝完" },
      { "value": "DISCARDED", "label": "已废弃" },
      { "value": "REPURCHASE_CANDIDATE", "label": "回购候选" }
    ],
    "processMethods": [
      { "value": "WASHED", "label": "水洗" },
      { "value": "NATURAL", "label": "日晒" },
      { "value": "HONEY", "label": "蜜处理" },
      { "value": "ANAEROBIC", "label": "厌氧发酵" },
      { "value": "OTHER", "label": "其他" }
    ],
    "roastLevels": [
      { "value": "LIGHT", "label": "浅烘" },
      { "value": "MEDIUM_LIGHT", "label": "中浅烘" },
      { "value": "MEDIUM", "label": "中烘" },
      { "value": "MEDIUM_DARK", "label": "中深烘" },
      { "value": "DARK", "label": "深烘" }
    ],
    "brewMethods": [
      { "value": "V60", "label": "V60" },
      { "value": "ORIGAMI", "label": "Origami" },
      { "value": "KALITA", "label": "Kalita" },
      { "value": "CHEMEX", "label": "Chemex" },
      { "value": "FRENCH_PRESS", "label": "法压壶" },
      { "value": "AEROPRESS", "label": "爱乐压" },
      { "value": "ESPRESSO", "label": "意式" },
      { "value": "MOKA_POT", "label": "摩卡壶" },
      { "value": "COLD_BREW", "label": "冷萃" },
      { "value": "OTHER", "label": "其他" }
    ]
  }
}
```

说明：

第一版枚举可以由后端静态返回。

后续如果需要后台配置，再迁移到字典表。

---

## 12. DTO 命名建议

### 12.1 Auth

```text
LoginRequest
LoginResponse
CurrentUserResponse
```

### 12.2 Coffee

```text
CoffeeBeanCreateRequest
CoffeeBeanUpdateRequest
CoffeeBeanListQuery
CoffeeBeanListItemResponse
CoffeeBeanDetailResponse
CoffeeBeanStatusUpdateRequest
```

### 12.3 Cover

```text
CoffeeCoverUploadResponse
```

### 12.4 Review

```text
CoffeeReviewCreateRequest
CoffeeReviewUpdateRequest
CoffeeReviewListItemResponse
```

### 12.5 Brew

```text
BrewRecordCreateRequest
BrewRecordUpdateRequest
BrewRecordListQuery
BrewRecordListItemResponse
BrewRecordRecommendedUpdateRequest
```

### 12.6 Tag

```text
FlavorTagCreateRequest
FlavorTagResponse
```

---

## 13. 后端模块建议

第一版后端建议按以下包结构：

```text
com.example.coffeebean
├── auth
├── user
├── coffee
├── review
├── brew
├── tag
├── file
├── common
└── config
```

### 13.1 common

放置：

- 统一响应 `ApiResponse`
- 分页响应 `PageResponse`
- 错误码 `ErrorCode`
- 业务异常 `BusinessException`
- 全局异常处理 `GlobalExceptionHandler`

### 13.2 config

放置：

- 安全配置
- Web MVC 配置
- 文件上传配置
- 跨域配置

---

## 14. 第一版接口开发顺序建议

建议按以下顺序实现：

```text
1. 统一响应和异常处理
2. 用户登录 / 当前用户
3. 枚举查询
4. 风味标签查询
5. 咖啡豆 CRUD
6. 包装封面上传 / 删除 / 访问
7. 评价 CRUD
8. 冲煮记录 CRUD
9. 列表搜索筛选优化
10. 参数校验和测试
```

这个顺序可以保证后端尽快跑出闭环。

---

## 15. 第一版 API 结论

第一版 API 以咖啡豆详情页为核心组织资源。

核心路径设计：

```text
/auth/login
/auth/me

/coffees
/coffees/{id}
/coffees/{id}/status
/coffees/{id}/cover

/coffees/{coffeeBeanId}/reviews
/reviews/{id}

/coffees/{coffeeBeanId}/brews
/brews/{id}
/brews/{id}/recommended

/tags/flavors
/enums
/files/**
```

这套接口足够支撑第一版前端 MVP，也方便 Codex 继续生成后端骨架代码。


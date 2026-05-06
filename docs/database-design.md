# Database Design

## Direction

第一版以 MySQL 关系模型为主，Redis 暂用于登录态、缓存或后续提醒任务辅助。

## Core Tables

- users
- coffee_beans
- coffee_reviews
- brew_records
- flavor_tags
- coffee_bean_flavor_tags

## Optional Tables

- coffee_bean_batches
- attachments
- recipe_templates
- purchase_records

## Design Notes

- 咖啡豆基础信息、评价、冲煮记录分表存储。
- 冲煮记录保存完整参数快照。
- 风味标签使用多对多关系。
- 第一版枚举字段可以先用字符串或 Java enum，后续再字典表化。
